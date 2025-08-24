package dev.hellevang.openrz67.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.hellevang.openrz67.ui.theme.Dimens

@Composable
fun CountdownDisplay(
    startDelayedTrigger: Boolean,
    countdownTimeLeft: Int
) {
    if (startDelayedTrigger) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                fontSize = Dimens.BodyTextSize,
                color = MaterialTheme.colors.onBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentWidth(Alignment.CenterHorizontally)
                    .padding(start = Dimens.StandardPadding, end = Dimens.StandardPadding),
                text = "Arduino countdown in progress..."
            )
            if (countdownTimeLeft > 0) {
                Text(
                    fontSize = Dimens.CountdownTextSize,
                    color = MaterialTheme.colors.secondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentWidth(Alignment.CenterHorizontally)
                        .padding(top = Dimens.SmallPadding),
                    text = "$countdownTimeLeft"
                )
            }
        }
    } else {
        Text(
            fontSize = Dimens.BodyTextSize,
            color = MaterialTheme.colors.onBackground,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .padding(start = Dimens.StandardPadding, end = Dimens.StandardPadding),
            text = "Press to start 10s countdown on Arduino"
        )
    }
}