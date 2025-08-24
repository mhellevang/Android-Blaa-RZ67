package dev.hellevang.openrz67.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import dev.hellevang.openrz67.ui.theme.Dimens

@Composable
fun HeaderComponent() {
    Text(
        text = "OpenRZ67",
        fontSize = Dimens.HeaderTextSize,
        color = MaterialTheme.colors.onBackground,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentWidth(Alignment.CenterHorizontally)
            .padding(start = Dimens.HeaderStartPadding, top = Dimens.HeaderTopPadding)
    )
    Text(
        text = "A Mamiya RZ67 bluetooth trigger",
        fontSize = Dimens.SubHeaderTextSize,
        color = MaterialTheme.colors.onBackground,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Dimens.StandardPadding, end = Dimens.StandardPadding)
    )
}