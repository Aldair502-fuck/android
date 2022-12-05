package chat.revolt.screens.login

import chat.revolt.R
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import chat.revolt.api.RevoltAPI
import chat.revolt.components.generic.RemoteImage
import chat.revolt.components.generic.drawableResource
import kotlinx.coroutines.runBlocking

class GreeterViewModel() : ViewModel() {
    private var _skipLogin by mutableStateOf(false)
    val skipLogin: Boolean
        get() = _skipLogin

    private var _finishedLoading by mutableStateOf(false)
    val finishedLoading: Boolean
        get() = _finishedLoading

    fun setSkipLogin(value: Boolean) {
        _skipLogin = value
    }

    fun setFinishedLoading(value: Boolean) {
        _finishedLoading = value
    }

    init {
        // runBlocking prevents the greeter from showing up at all, if the user is already logged in
        // (It is normally a bad idea to use runBlocking outside of a coroutine scope)
        runBlocking {
            RevoltAPI.initialize()
            if (RevoltAPI.isLoggedIn()) {
                _skipLogin = true
            }
            setFinishedLoading(true)
        }
    }
}

@Composable
fun GreeterScreen(navController: NavController, viewModel: GreeterViewModel = viewModel()) {
    if (viewModel.skipLogin) {
        navController.navigate("chat/home") {
            popUpTo("setup/greeting") {
                inclusive = true
            }
        }
        viewModel.setSkipLogin(false)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RemoteImage(
                url = drawableResource(R.drawable.revolt_logo_wide),
                description = "Revolt Logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(bottom = 30.dp)
            )

            Text(
                text = stringResource(R.string.login_onboarding_heading),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .fillMaxWidth(),
            )

            Text(
                text = stringResource(R.string.login_onboarding_body),
                color = Color(0xaaffffff),
                style = MaterialTheme.typography.titleMedium.copy(
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Normal,
                ),
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .fillMaxWidth()
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 30.dp)
        ) {
            ElevatedButton(
                onClick = { navController.navigate("about") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.about))
            }

            Button(
                onClick = { navController.navigate("setup/login") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.login))
            }
        }
    }
}