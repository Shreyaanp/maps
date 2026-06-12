package work.shreyaan.dwell

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                WatchScreen()
            }
        }
    }
}

private fun prefs(c: Context): SharedPreferences =
    c.getSharedPreferences("dwell", Context.MODE_PRIVATE)

@Composable
fun WatchScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var timerEnd by remember { mutableLongStateOf(prefs(context).getLong("timer_end", 0L)) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        // Pull the current state from the Data Layer in case we missed
        // change events while the watch app wasn't running.
        runCatching {
            val items = Wearable.getDataClient(context).dataItems.await()
            for (item in items) {
                if (item.uri.path == "/dwell/state") {
                    val map = DataMapItem.fromDataItem(item).dataMap
                    prefs(context).edit()
                        .putLong("timer_end", map.getLong("end"))
                        .apply()
                }
            }
            items.release()
        }
        while (true) {
            now = System.currentTimeMillis()
            timerEnd = prefs(context).getLong("timer_end", 0L)
            delay(1000)
        }
    }

    val running = timerEnd > now
    Scaffold(timeText = { TimeText() }) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (running) {
                val left = timerEnd - now
                val h = left / 3_600_000
                val m = (left / 60_000) % 60
                val s = (left / 1000) % 60
                Text("Dwell", style = MaterialTheme.typography.caption1)
                Text("${h}h ${m}m ${s}s", style = MaterialTheme.typography.title1)
                Text(
                    "ends " + DateFormat.getTimeInstance(DateFormat.SHORT)
                        .format(Date(timerEnd)),
                    style = MaterialTheme.typography.caption2
                )
                Spacer(Modifier.height(8.dp))
                Chip(
                    onClick = {
                        prefs(context).edit().putLong("timer_end", 0L).apply()
                        timerEnd = 0L
                        scope.launch {
                            runCatching {
                                val nodes = Wearable.getNodeClient(context)
                                    .connectedNodes.await()
                                for (node in nodes) {
                                    Wearable.getMessageClient(context)
                                        .sendMessage(node.id, "/dwell/cancel", null)
                                        .await()
                                }
                            }
                        }
                    },
                    label = { Text("Cancel") },
                    colors = ChipDefaults.secondaryChipColors()
                )
            } else {
                Text("No timer running", style = MaterialTheme.typography.body1)
                Text("Arm it on your phone", style = MaterialTheme.typography.caption2)
            }
        }
    }
}
