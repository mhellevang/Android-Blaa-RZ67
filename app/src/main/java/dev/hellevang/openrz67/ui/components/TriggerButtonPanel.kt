package dev.hellevang.openrz67.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.hellevang.openrz67.ui.theme.Dimens
import dev.hellevang.openrz67.viewmodel.TriggerControlViewModel

@Composable
fun TriggerButtonPanel(
    viewModel: TriggerControlViewModel,
    isConnected: Boolean
) {
    val triggerType by viewModel.triggerType.collectAsState()
    val startDelayedTrigger by viewModel.startDelayedTrigger.collectAsState()
    val countdownTimeLeft by viewModel.countdownTimeLeft.collectAsState()

    Row(
        modifier = Modifier.padding(top = Dimens.ModeButtonTopPadding)
    ) {
        ToggleButton(
            toggleButton = {
                viewModel.toggleTriggerType()
            },
            text = "Mode"
        )
    }
    
    Text(
        fontSize = Dimens.SubHeaderTextSize,
        color = MaterialTheme.colors.onBackground,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentWidth(Alignment.CenterHorizontally)
            .padding(start = Dimens.StandardPadding, end = Dimens.StandardPadding),
        text = triggerType.name
    )
    
    Spacer(modifier = Modifier.padding(top = Dimens.SpacerTopPadding))
    
    when (triggerType) {
        TriggerControlViewModel.TriggerType.Direct -> {
            Text(
                fontSize = Dimens.BodyTextSize,
                color = MaterialTheme.colors.onBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentWidth(Alignment.CenterHorizontally)
                    .padding(start = Dimens.StandardPadding, end = Dimens.StandardPadding),
                text = "Press the button to take a picture"
            )
        }
        TriggerControlViewModel.TriggerType.Countdown -> {
            CountdownDisplay(
                startDelayedTrigger = startDelayedTrigger,
                countdownTimeLeft = countdownTimeLeft
            )
        }
    }

    Button(
        onClick = {
            viewModel.handleTriggerButtonClick()
        },
        modifier = Modifier
            .padding(top = Dimens.ButtonTopPadding),
        enabled = isConnected,
    ) {
        Text(text = "Trigger shutter", modifier = Modifier.padding(end = Dimens.ButtonEndPadding))
        Icon(
            imageVector = Icons.Default.Camera,
            contentDescription = null,
            tint = MaterialTheme.colors.onPrimary
        )
    }
}

@Composable
private fun ToggleButton(
    toggleButton: () -> Unit,
    modifier: Modifier = Modifier,
    text: String
) {
    Button(onClick = { toggleButton() }, modifier = modifier) { Text(text) }
}