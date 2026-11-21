package com.krishiyan.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.krishiyan.KrishiYanApp
import com.krishiyan.R
import com.krishiyan.auth.LoginScreen
import com.krishiyan.auth.LoginViewModel
import com.krishiyan.profile.ProfileSetupScreen
import com.krishiyan.profile.ProfileSetupViewModel
import com.krishiyan.ui.components.BigButton

@Composable
fun AppRoot() {
    val container = (LocalContext.current.applicationContext as KrishiYanApp).container
    val startViewModel = viewModel { StartViewModel(container.authRepository, container.profileRepository) }
    val state by startViewModel.state.collectAsStateWithLifecycle()

    when (state) {
        StartState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        StartState.LoggedOut -> LoginScreen(viewModel { LoginViewModel(container.authRepository) })
        StartState.NeedsProfile -> ProfileSetupScreen(
            viewModel { ProfileSetupViewModel(container.profileRepository, startViewModel::onProfileCreated) },
        )
        StartState.Ready -> KrishiYanNavHost(onSignOut = startViewModel::signOut)
        StartState.Offline -> Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.error_no_internet),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            BigButton(stringResource(R.string.try_again), startViewModel::checkProfile)
        }
    }
}
