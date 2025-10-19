package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.jdtech.jellyfin.core.presentation.dummy.dummyPerson
import dev.jdtech.jellyfin.models.JellyCastItemPerson
import dev.jdtech.jellyfin.presentation.theme.JellyCastTheme
import dev.jdtech.jellyfin.presentation.theme.spacings

@Composable
fun PersonItem(
    person: JellyCastItemPerson,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(110.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = person.image.uri,
            contentDescription = null,
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .background(
                    MaterialTheme.colorScheme.surfaceContainer,
                )
                .fillMaxWidth()
                .height(160.dp),
            contentScale = ContentScale.Crop,
        )
        Spacer(Modifier.height(MaterialTheme.spacings.extraSmall))
        Text(
            text = person.name,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = person.role,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(2.dp))
    }
}

@Composable
@Preview(showBackground = true)
private fun PersonItemPreview() {
    JellyCastTheme {
        PersonItem(
            person = dummyPerson,
            onClick = {},
        )
    }
}
