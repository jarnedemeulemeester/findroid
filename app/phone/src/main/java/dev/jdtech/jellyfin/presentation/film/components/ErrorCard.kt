package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.core.R as CoreR

@Composable
fun ErrorCard(
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        modifier = modifier,
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
        border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.error),
    ) {
        Row {
            Spacer(modifier = Modifier.width(MaterialTheme.spacings.small))
            Icon(
                painter = painterResource(CoreR.drawable.ic_alert_circle),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterVertically),
            )
            Spacer(modifier = Modifier.width(MaterialTheme.spacings.small))
            Text(
                text = stringResource(CoreR.string.error_loading_data),
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically),
            )
            TextButton(
                onClick = onRetryClick,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
            ) {
                Text("Retry")
            }
        }
    }
}
