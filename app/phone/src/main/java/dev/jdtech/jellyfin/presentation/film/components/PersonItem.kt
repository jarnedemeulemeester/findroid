package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.jdtech.jellyfin.core.R
import dev.jdtech.jellyfin.core.presentation.dummy.dummyPerson
import dev.jdtech.jellyfin.models.FindroidItemPerson
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.toBlurHash
import dev.jdtech.jellyfin.presentation.utils.toLocalFilesUri
import dev.jdtech.jellyfin.presentation.utils.withJellyfinResize

@Composable
fun PersonItem(person: FindroidItemPerson, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Column(
        modifier =
            modifier
                .width(110.dp)
                .clip(MaterialTheme.shapes.small)
                .clickable(onClick = onClick)
    ) {
        BoxWithConstraints(
            modifier =
                Modifier
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .fillMaxWidth()
                    .height(160.dp),
            contentAlignment = Alignment.Center,
        ) {
            val image = person.image

            val imageUri =
                image.uri
                    .withJellyfinResize(widthDp = maxWidth, heightDp = maxHeight)
                    .toLocalFilesUri(context)

            val blurPlaceholder = remember(image.blurHash) {
                image.blurHash.toBlurHash()
            }

            Icon(
                painter = painterResource(R.drawable.ic_user),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )

            AsyncImage(
                model = imageUri,
                contentDescription = null,
                placeholder = blurPlaceholder,
                error = blurPlaceholder,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
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
    FindroidTheme { PersonItem(person = dummyPerson, onClick = {}) }
}
