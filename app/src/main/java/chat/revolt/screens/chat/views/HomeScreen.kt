package chat.revolt.screens.chat.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import chat.revolt.R
import chat.revolt.api.RevoltAPI
import chat.revolt.components.generic.PageHeader
import chat.revolt.components.screens.home.LinkOnHome
import chat.revolt.persistence.KVStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val kvStorage: KVStorage
) : ViewModel() {
    fun logout() {
        runBlocking {
            kvStorage.remove("sessionToken")
            RevoltAPI.logout()
        }
    }
}

@Composable
fun HomeScreen(navController: NavController, viewModel: HomeScreenViewModel = hiltViewModel()) {
    Column {
        PageHeader(text = stringResource(id = R.string.home))

        LinkOnHome(
            heading = stringResource(id = R.string.logout),
            icon = Icons.Default.Close,
            onClick = {
                viewModel.logout()
                navController.navigate("login/greeting") {
                    popUpTo("chat") {
                        inclusive = true
                    }
                }
            })

        LinkOnHome(
            heading = stringResource(id = R.string.settings),
            icon = Icons.Default.Settings,
            onClick = {
                navController.navigate("settings")
            })
        Text(buildAnnotatedString {
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                append("Note: ")
            }
            append("Settings are accessible from the top left status icon in the drawer. The link here is temporary until a tutorial is in place.")
        }, modifier = Modifier.padding(16.dp))
    }
}