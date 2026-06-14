package work.shreyaan.dwell

import android.content.Context
import android.util.Log
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.android.gms.wearable.Wearable
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

class DwellTileService : TileService() {
    override fun onTileRequest(
        requestParams: RequestBuilders.TileRequest,
    ): ListenableFuture<TileBuilders.Tile> {
        requestFreshPhoneState()
        val state = tileState(this)
        return Futures.immediateFuture(
            TileBuilders.Tile.Builder()
                .setResourcesVersion(RESOURCES_VERSION)
                .setFreshnessIntervalMillis(60_000)
                .setTileTimeline(
                    TimelineBuilders.Timeline.Builder()
                        .addTimelineEntry(
                            TimelineBuilders.TimelineEntry.Builder()
                                .setLayout(
                                    LayoutElementBuilders.Layout.Builder()
                                        .setRoot(tileLayout(state))
                                        .build(),
                                )
                                .build(),
                        )
                        .build(),
                )
                .build(),
        )
    }

    private fun requestFreshPhoneState() {
        Wearable.getNodeClient(this).connectedNodes
            .addOnSuccessListener { nodes ->
                nodes.forEach { node ->
                    Wearable.getMessageClient(this)
                        .sendMessage(node.id, "/dwell/request_state", ByteArray(0))
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Failed to request phone state from ${node.displayName}", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Failed to load connected phone nodes", e)
            }
    }

    override fun onTileResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest,
    ): ListenableFuture<ResourceBuilders.Resources> =
        Futures.immediateFuture(
            ResourceBuilders.Resources.Builder()
                .setVersion(RESOURCES_VERSION)
                .build(),
        )

    private fun tileLayout(state: TileState): LayoutElementBuilders.LayoutElement =
        LayoutElementBuilders.Box.Builder()
            .setWidth(DimensionBuilders.expand())
            .setHeight(DimensionBuilders.expand())
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setClickable(openAppClickable())
                    .setSemantics(
                        ModifiersBuilders.Semantics.Builder()
                            .setContentDescription("Open Dwell timer")
                            .build(),
                    )
                    .build(),
            )
            .addContent(
                LayoutElementBuilders.Column.Builder()
                    .setWidth(DimensionBuilders.expand())
                    .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                    .addContent(text(state.title, 16f, 500, 0xFFBFC8D8.toInt()))
                    .addContent(spacer(8f))
                    .addContent(text(state.primary, 34f, 700, 0xFFFFFFFF.toInt()))
                    .addContent(spacer(6f))
                    .addContent(text(state.secondary, 15f, 500, 0xFFD8DEE8.toInt()))
                    .addContent(spacer(14f))
                    .addContent(pill(state.action))
                    .build(),
            )
            .build()

    private fun openAppClickable(): ModifiersBuilders.Clickable =
        ModifiersBuilders.Clickable.Builder()
            .setId("open_dwell")
            .setOnClick(
                ActionBuilders.LaunchAction.Builder()
                    .setAndroidActivity(
                        ActionBuilders.AndroidActivity.Builder()
                            .setPackageName(packageName)
                            .setClassName("$packageName.MainActivity")
                            .build(),
                    )
                    .build(),
            )
            .build()

    private fun pill(label: String): LayoutElementBuilders.LayoutElement =
        LayoutElementBuilders.Box.Builder()
            .setWidth(DimensionBuilders.dp(128f))
            .setHeight(DimensionBuilders.dp(36f))
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setBackground(
                        ModifiersBuilders.Background.Builder()
                            .setColor(ColorBuilders.argb(0xFF2E333D.toInt()))
                            .setCorner(
                                ModifiersBuilders.Corner.Builder()
                                    .setRadius(DimensionBuilders.dp(18f))
                                    .build(),
                            )
                            .build(),
                    )
                    .build(),
            )
            .addContent(text(label, 15f, 600, 0xFFFFFFFF.toInt()))
            .build()

    private fun text(
        value: String,
        sizeSp: Float,
        weight: Int,
        color: Int,
    ): LayoutElementBuilders.LayoutElement =
        LayoutElementBuilders.Text.Builder()
            .setText(value)
            .setMaxLines(1)
            .setFontStyle(
                LayoutElementBuilders.FontStyle.Builder()
                    .setSize(DimensionBuilders.sp(sizeSp))
                    .setWeight(weight)
                    .setColor(ColorBuilders.argb(color))
                    .build(),
            )
            .build()

    private fun spacer(heightDp: Float): LayoutElementBuilders.LayoutElement =
        LayoutElementBuilders.Spacer.Builder()
            .setHeight(DimensionBuilders.dp(heightDp))
            .build()

    companion object {
        private const val TAG = "DwellTile"
        private const val RESOURCES_VERSION = "1"
    }
}

private fun tileState(context: Context): TileState {
    val p = context.getSharedPreferences("dwell", Context.MODE_PRIVATE)
    val hasPlace = p.getBoolean("has_place", false)
    val place = p.getString("place_label", "").orEmpty()
    val armed = p.getBoolean("armed", false)
    val needsSetup = p.getBoolean("needs_setup", false)
    val registeredPlaceCount = p.getInt("registered_place_count", 0)
    val timerEnd = p.getLong("timer_end", 0L)
    val prompt = p.getString("prompt", TileStateCalculator.PROMPT_NONE)
        ?: TileStateCalculator.PROMPT_NONE
    val now = System.currentTimeMillis()

    return TileStateCalculator.state(
        hasPlace = hasPlace,
        placeLabel = place,
        armed = armed,
        needsSetup = needsSetup,
        registeredPlaceCount = registeredPlaceCount,
        timerEnd = timerEnd,
        prompt = prompt,
        now = now,
    )
}
