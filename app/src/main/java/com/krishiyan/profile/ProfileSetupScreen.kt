package com.krishiyan.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.krishiyan.R
import com.krishiyan.ui.components.BigButton

@Composable
fun ProfileSetupScreen(viewModel: ProfileSetupViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val words = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.profile_setup_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.profile_setup_hint), style = MaterialTheme.typography.bodyLarge)
        OutlinedTextField(
            value = state.name,
            onValueChange = viewModel::onNameChange,
            label = { Text(stringResource(R.string.profile_name)) },
            singleLine = true,
            keyboardOptions = words,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.phone,
            onValueChange = viewModel::onPhoneChange,
            label = { Text(stringResource(R.string.profile_phone)) },
            supportingText = { Text(stringResource(R.string.profile_phone_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.village,
            onValueChange = viewModel::onVillageChange,
            label = { Text(stringResource(R.string.profile_village)) },
            singleLine = true,
            keyboardOptions = words.copy(imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )
        state.error?.let {
            Text(stringResource(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
        BigButton(stringResource(R.string.profile_save), viewModel::createProfile, loading = state.saving)
    }
}
