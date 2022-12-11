package chat.revolt.screens.login

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import chat.revolt.R
import chat.revolt.api.REVOLT_SUPPORT
import chat.revolt.api.routes.account.EmailPasswordAssessment
import chat.revolt.api.routes.account.negotiateAuthentication
import chat.revolt.api.routes.user.fetchSelfWithNewToken
import chat.revolt.components.generic.AnyLink
import chat.revolt.components.generic.FormTextField
import chat.revolt.components.generic.Weblink
import chat.revolt.persistence.KVStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val kvStorage: KVStorage,
) : ViewModel() {
    private var _email by mutableStateOf("")
    val email: String
        get() = _email

    private var _password by mutableStateOf("")
    val password: String
        get() = _password

    private var _error by mutableStateOf<String?>(null)
    val error: String?
        get() = _error

    private var _navigateTo by mutableStateOf<String?>(null)
    val navigateTo: String?
        get() = _navigateTo

    private var _mfaResponse by mutableStateOf<EmailPasswordAssessment?>(null)
    val mfaResponse: EmailPasswordAssessment?
        get() = _mfaResponse

    fun doLogin() {
        _error = null

        viewModelScope.launch {
            val response = negotiateAuthentication(_email, _password)
            if (response.error != null) {
                _error = response.error.type
            } else {
                Log.d("Login", "Checking for MFA")
                if (response.proceedMfa) {
                    Log.d("Login", "MFA required. Navigating to MFA screen")
                    _mfaResponse = response
                    _navigateTo = "mfa"
                } else {
                    Log.d(
                        "Login",
                        "No MFA required. Login is complete! We have a session token: ${response.firstUserHints!!.token}"
                    )

                    fetchSelfWithNewToken(response.firstUserHints.token)
                    kvStorage.set("sessionToken", response.firstUserHints.token)

                    _navigateTo = "home"
                }
            }
        }
    }

    fun navigationComplete() {
        _navigateTo = null
    }

    fun setEmail(email: String) {
        _email = email
    }

    fun setPassword(password: String) {
        _password = password
    }
}

@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: LoginViewModel = hiltViewModel()
) {
    if (viewModel.navigateTo == "mfa") {
        navController.navigate(
            "login/mfa/${viewModel.mfaResponse!!.mfaSpec!!.ticket}/${
                viewModel.mfaResponse!!.mfaSpec!!.allowedMethods.joinToString(
                    ","
                )
            }"
        )
        viewModel.navigationComplete()
    } else if (viewModel.navigateTo == "home") {
        navController.navigate("chat/home") {
            popUpTo("login/greeting") { inclusive = true }
        }
        viewModel.navigationComplete()
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
            Text(
                text = stringResource(R.string.login_heading),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .fillMaxWidth(),
            )


            Column(
                modifier = Modifier
                    .width(270.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FormTextField(
                    value = viewModel.email,
                    label = stringResource(R.string.email),
                    onChange = { viewModel.setEmail(it) },
                    modifier = Modifier.padding(vertical = 25.dp)
                )
                FormTextField(
                    value = viewModel.password,
                    label = stringResource(R.string.password),
                    type = KeyboardType.Password,
                    onChange = { viewModel.setPassword(it) })

                AnyLink(
                    text = stringResource(R.string.password_forgot),
                    action = { navController.navigate("about/placeholder") },
                    modifier = Modifier.padding(vertical = 7.dp)
                )

                if (viewModel.error != null) {
                    Text(
                        text = viewModel.error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.titleMedium.copy(
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Normal,
                            fontSize = 15.sp
                        ),
                        modifier = Modifier.padding(vertical = 7.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {


            Weblink(
                text = stringResource(R.string.password_manager_hint),
                url = "$REVOLT_SUPPORT/kb/interface/android/using-a-password-manager",
            )

            AnyLink(
                text = stringResource(R.string.resend_verification),
                action = { navController.navigate("about/placeholder") },
                modifier = Modifier.padding(vertical = 7.dp)
            )

            ElevatedButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.back))
            }

            Button(
                onClick = { viewModel.doLogin() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.login))
            }
        }
    }
}