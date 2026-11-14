package dev.dimension.flare.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Brands
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.brands.Github
import compose.icons.fontawesomeicons.solid.CodeBranch
import dev.dimension.flare.BuildConfig
import dev.dimension.flare.R
import dev.dimension.flare.ui.component.BackButton
import dev.dimension.flare.ui.component.FlareLargeFlexibleTopAppBar
import dev.dimension.flare.ui.component.FlareScaffold
import dev.dimension.flare.ui.theme.segmentedShapes2

private const val KAGARI_SOURCE_URL = "https://github.com/shiro-ka/kagari"
private const val FLARE_SOURCE_URL = "https://github.com/DimensionDev/Flare"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun KagariAboutScreen(onBack: () -> Unit) {
    val topAppBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val uriHandler = LocalUriHandler.current
    FlareScaffold(
        topBar = {
            FlareLargeFlexibleTopAppBar(
                title = {
                    Text(text = stringResource(id = R.string.settings_about_title))
                },
                navigationIcon = {
                    BackButton(onBack = onBack)
                },
                scrollBehavior = topAppBarScrollBehavior,
            )
        },
        modifier = Modifier.nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
    ) {
        Column(
            modifier =
                Modifier
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(it),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(id = R.string.kagari_app_name),
                style = MaterialTheme.typography.displaySmall,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(id = R.string.kagari_about_description),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                SegmentedListItem(
                    content = {
                        Text(text = stringResource(id = R.string.settings_about_source_code))
                    },
                    supportingContent = {
                        Text(text = KAGARI_SOURCE_URL)
                    },
                    onClick = {
                        uriHandler.openUri(KAGARI_SOURCE_URL)
                    },
                    shapes = ListItemDefaults.segmentedShapes2(0, 2),
                    leadingContent = {
                        Icon(
                            imageVector = FontAwesomeIcons.Brands.Github,
                            contentDescription = "GitHub",
                            modifier = Modifier.size(24.dp),
                        )
                    },
                )
                SegmentedListItem(
                    content = {
                        Text(text = stringResource(id = R.string.kagari_about_based_on))
                    },
                    supportingContent = {
                        Text(text = FLARE_SOURCE_URL)
                    },
                    onClick = {
                        uriHandler.openUri(FLARE_SOURCE_URL)
                    },
                    shapes = ListItemDefaults.segmentedShapes2(1, 2),
                    leadingContent = {
                        Icon(
                            imageVector = FontAwesomeIcons.Solid.CodeBranch,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                    },
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
