package chat.revolt.screens.labs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import chat.revolt.api.settings.FeatureFlags
import chat.revolt.screens.labs.ui.mockups.CallScreenMockup
import chat.revolt.screens.labs.ui.sandbox.CryptographicAgeVerificationSandbox
import chat.revolt.screens.labs.ui.sandbox.SettingsDslSandbox

annotation class LabsFeature

@Composable
fun LabsGuard(onTurnBack: () -> Unit = {}, content: @Composable () -> Unit) {
    if (!FeatureFlags.labsAccessControlGranted) {
        AlertDialog(
            onDismissRequest = { onTurnBack() },
            confirmButton = {
                TextButton(onClick = { onTurnBack() }) {
                    Text("Turn back")
                }
            },
            title = {
                Text("You don't have access to Labs.")
            },
            text = {
                Text("Labs is where we test new features. However, these features may be unstable and may not work as expected. Hence, access to Labs is restricted.")
            }
        )
    } else {
        content()
    }
}

@Composable
fun LabsRootScreen(topNav: NavController) {
    val labsNav = rememberNavController()

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        LabsGuard(
            onTurnBack = {
                topNav.popBackStack()
            }
        ) {
            NavHost(
                navController = labsNav,
                startDestination = "home",
            ) {
                composable("home") {
                    LabsHomeScreen(labsNav)
                }

                composable("mockups/call") {
                    CallScreenMockup()
                }

                composable("sandboxes/cryptoageverif") {
                    CryptographicAgeVerificationSandbox(labsNav)
                }
                composable("sandboxes/settingsdsl") {
                    SettingsDslSandbox(labsNav)
                }
            }
        }
    }
}