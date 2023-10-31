package chat.revolt.screens.settings

import android.content.Context
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import chat.revolt.R
import chat.revolt.api.RevoltAPI
import chat.revolt.api.routes.microservices.autumn.uploadToAutumn
import chat.revolt.api.routes.user.fetchUserProfile
import chat.revolt.api.routes.user.patchSelf
import chat.revolt.api.schemas.Profile
import chat.revolt.components.generic.InlineMediaPicker
import chat.revolt.components.generic.PageHeader
import chat.revolt.components.screens.settings.RawUserOverview
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.http.ContentType
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
@Suppress("StaticFieldLeak")
class ProfileSettingsScreenViewModel @Inject constructor(@ApplicationContext val context: Context) :
    ViewModel() {
    var pfpModel by mutableStateOf<Any?>(null)
    var currentProfile by mutableStateOf<Profile?>(null)
    var pendingProfile by mutableStateOf<Profile?>(null)
    var backgroundModel by mutableStateOf<Any?>(null)
    var uploadProgress by mutableFloatStateOf(0f)
    var uploadError by mutableStateOf<String?>(null)

    init {
        RevoltAPI.selfId?.let { self ->
            RevoltAPI.userCache[self]?.avatar?.id?.let {
                pfpModel = "https://autumn.revolt.chat/avatars/${it}"
            }
            viewModelScope.launch {
                currentProfile = fetchUserProfile(self)
                currentProfile!!.background?.id?.let {
                    backgroundModel = "https://autumn.revolt.chat/backgrounds/${it}"
                }

                pendingProfile = currentProfile!!.copy()
            }
        }

    }

    fun saveNewPfp() {
        uploadError = null

        val uri = when (pfpModel) {
            is Uri -> pfpModel as Uri
            is String -> Uri.parse(pfpModel as String)
            else -> return
        }

        val mFile = File(context.cacheDir, uri.lastPathSegment ?: "avatar")

        mFile.outputStream().use { output ->
            context.contentResolver.openInputStream(uri)?.use { input ->
                input.copyTo(output)
            }
        }

        val mime = context.contentResolver.getType(uri)

        if (mime?.endsWith("webp") == true) {
            uploadError = "WebP is not supported"
            return
        }

        viewModelScope.launch {
            try {
                val id = uploadToAutumn(
                    mFile,
                    uri.lastPathSegment ?: "avatar",
                    "avatars",
                    ContentType.parse(mime ?: "image/*"),
                    onProgress = { soFar, outOf ->
                        uploadProgress = soFar.toFloat() / outOf.toFloat()
                    }
                )

                patchSelf(avatar = id)
            } catch (e: Exception) {
                uploadError = e.message
                uploadProgress = 0f
                return@launch
            }

            pfpModel = RevoltAPI.userCache[RevoltAPI.selfId]?.avatar?.id?.let {
                "https://autumn.revolt.chat/avatars/${it}"
            }

            uploadProgress = 0f
        }
    }

    fun saveNewBackground() {
        uploadError = null

        val uri = when (backgroundModel) {
            is Uri -> backgroundModel as Uri
            is String -> Uri.parse(backgroundModel as String)
            else -> return
        }

        val mFile = File(context.cacheDir, uri.lastPathSegment ?: "background")

        mFile.outputStream().use { output ->
            context.contentResolver.openInputStream(uri)?.use { input ->
                input.copyTo(output)
            }
        }

        val mime = context.contentResolver.getType(uri)

        if (mime?.endsWith("webp") == true) {
            uploadError = "WebP is not supported"
            return
        }

        viewModelScope.launch {
            try {
                val id = uploadToAutumn(
                    mFile,
                    uri.lastPathSegment ?: "background",
                    "backgrounds",
                    ContentType.parse(mime ?: "image/*"),
                    onProgress = { soFar, outOf ->
                        uploadProgress = soFar.toFloat() / outOf.toFloat()
                    }
                )

                patchSelf(background = id)
            } catch (e: Exception) {
                uploadError = e.message
                uploadProgress = 0f
                return@launch
            }

            backgroundModel = RevoltAPI.selfId?.let {
                val profile = fetchUserProfile(it)
                currentProfile = profile
                pendingProfile = profile

                profile.background?.id?.let {
                    "https://autumn.revolt.chat/backgrounds/${it}"
                }
            }

            uploadProgress = 0f
        }
    }

    fun removePfp() {
        viewModelScope.launch {
            patchSelf(remove = listOf("Avatar"))
            pfpModel = null
        }
    }

    fun removeBackground() {
        viewModelScope.launch {
            patchSelf(remove = listOf("ProfileBackground"))
            pfpModel = null
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileSettingsScreen(
    navController: NavController,
    viewModel: ProfileSettingsScreenViewModel = hiltViewModel()
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        PageHeader(
            text = stringResource(id = R.string.settings_profile),
            showBackButton = true,
            onBackButtonClicked = {
                navController.popBackStack()
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            RevoltAPI.userCache[RevoltAPI.selfId]?.let {
                RawUserOverview(
                    it,
                    viewModel.pendingProfile,
                    viewModel.pfpModel?.toString(),
                    viewModel.backgroundModel?.toString()
                )
            }

            AnimatedVisibility(visible = viewModel.uploadProgress > 0f) {
                LinearProgressIndicator(
                    progress = viewModel.uploadProgress,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 0.dp)
                )
            }

            AnimatedVisibility(visible = viewModel.uploadError != null) {
                Text(
                    text = viewModel.uploadError ?: "",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier
                        .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 0.dp)
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(
                    modifier = Modifier
                        .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 0.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.settings_profile_profile_picture),
                        style = MaterialTheme.typography.labelLarge
                    )

                    Spacer(Modifier.height(10.dp))

                    InlineMediaPicker(
                        currentModel = viewModel.pfpModel,
                        circular = true,
                        onPick = {
                            viewModel.pfpModel = it.toString()
                            viewModel.saveNewPfp()
                        },
                        canRemove = true,
                        onRemove = {
                            viewModel.removePfp()
                        }
                    )
                }

                Column(
                    modifier = Modifier
                        .padding(20.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.settings_profile_custom_background),
                        style = MaterialTheme.typography.labelLarge,
                    )

                    Spacer(Modifier.height(10.dp))

                    InlineMediaPicker(
                        currentModel = viewModel.backgroundModel,
                        onPick = {
                            viewModel.backgroundModel = it.toString()
                            viewModel.saveNewBackground()
                        },
                        canRemove = true,
                        onRemove = {
                            viewModel.removeBackground()
                        }
                    )
                }
            }
        }
    }
}
