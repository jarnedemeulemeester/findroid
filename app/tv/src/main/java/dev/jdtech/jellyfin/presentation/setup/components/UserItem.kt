package dev.jdtech.jellyfin.presentation.setup.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.jdtech.jellyfin.core.presentation.dummy.dummyUser
import dev.jdtech.jellyfin.models.User
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import org.jellyfin.sdk.model.api.ImageType
import dev.jdtech.jellyfin.core.R as CoreR

@Composable
fun UserItem(
    user: User,
    modifier: Modifier = Modifier,
    onClick: (User) -> Unit = {},
    baseUrl: String = "",
) {
    val context = LocalContext.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(120.dp),
    ) {
        Surface(
            onClick = {
                onClick(user)
            },
            colors = ClickableSurfaceDefaults.colors(
                containerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
            ),
            border = ClickableSurfaceDefaults.border(
                border = Border(BorderStroke(1.dp, Color.White), shape = CircleShape),
                focusedBorder = Border(BorderStroke(4.dp, Color.White), shape = CircleShape),
            ),
            shape = ClickableSurfaceDefaults.shape(
                shape = CircleShape,
                focusedShape = CircleShape,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
        ) {
            Icon(
                painter = painterResource(id = CoreR.drawable.ic_user),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .width(48.dp)
                    .height(48.dp)
                    .align(Alignment.Center),
            )
            AsyncImage(
                model = "$baseUrl/users/${user.id}/Images/${ImageType.PRIMARY}",
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(modifier = Modifier.height(MaterialTheme.spacings.medium))
        Text(
            text = user.name,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Preview
@Composable
private fun UserComponentPreview() {
    FindroidTheme {
        UserItem(
            user = dummyUser,
        )
    }
}
