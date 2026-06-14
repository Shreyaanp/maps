package work.shreyaan.dwell

import org.junit.Assert.assertEquals
import org.junit.Test

class BackgroundLocationFlowTest {
    @Test
    fun preAndroid10DoesNotNeedBackgroundLocationPermission() {
        assertEquals(
            BackgroundLocationFlow.AlreadyAllowed,
            backgroundLocationFlowForSdk(28),
        )
    }

    @Test
    fun android10CanUseRuntimeBackgroundPermissionRequest() {
        assertEquals(
            BackgroundLocationFlow.RequestPermission,
            backgroundLocationFlowForSdk(29),
        )
    }

    @Test
    fun android11AndNewerUseAppSettingsForBackgroundLocation() {
        assertEquals(
            BackgroundLocationFlow.OpenAppSettings,
            backgroundLocationFlowForSdk(30),
        )
        assertEquals(
            BackgroundLocationFlow.OpenAppSettings,
            backgroundLocationFlowForSdk(35),
        )
    }
}
