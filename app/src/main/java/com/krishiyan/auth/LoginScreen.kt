package com.krishiyan.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.krishiyan.R
import com.krishiyan.ui.components.BigButton

@Composable
fun LoginScreen(viewModel: LoginViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, alignment = androidx.compose.ui.Alignment.CenterVertically),
    ) {
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            stringResource(R.string.login_tagline),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        if (!state.codeSent) {
            OutlinedTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                label = { Text(stringResource(R.string.login_email)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )
            ErrorText(state.error)
            BigButton(stringResource(R.string.login_send_code), viewModel::sendCode, loading = state.loading)
        } else {
            Text(stringResource(R.string.login_code_sent, state.email), style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(
                value = state.code,
                onValueChange = viewModel::onCodeChange,
                label = { Text(stringResource(R.string.login_code)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )
            ErrorText(state.error)
            BigButton(stringResource(R.string.login_verify), viewModel::verifyCode, loading = state.loading)
            TextButton(onClick = viewModel::sendCode, enabled = !state.loading, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.login_resend))
            }
            TextButton(onClick = viewModel::changeEmail, enabled = !state.loading, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.login_change_email))
            }
        }
    }
}

@Composable
private fun ErrorText(error: Int?) {
    if (error != null) {
        Text(stringResource(error), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
    }
}
