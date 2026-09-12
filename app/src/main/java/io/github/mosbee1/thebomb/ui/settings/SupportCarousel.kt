package io.github.mosbee1.thebomb.ui.settings

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.mosbee1.thebomb.R
import kotlinx.coroutines.delay

private class Slide(
    val icon: ImageVector,
    val titleRes: Int,
    val subRes: Int,
    val url: String,
)

private val slides = listOf(
    Slide(Icons.Rounded.Favorite, R.string.carousel_support_title, R.string.carousel_support_sub, "https://ko-fi.com/mosbee1"),
    Slide(Icons.Rounded.LocalCafe, R.string.carousel_bmc_title, R.string.carousel_bmc_sub, "https://buymeacoffee.com/MosBee1"),
    Slide(Icons.Rounded.Star, R.string.carousel_star_title, R.string.carousel_star_sub, "https://github.com/MosBee1/TheBomb/stargazers"),
    Slide(Icons.Rounded.BugReport, R.string.carousel_bug_title, R.string.carousel_bug_sub, "https://github.com/MosBee1/TheBomb/issues"),
)

/**
 * Auto-rotating support banner: Ko-fi, Buy Me a Coffee, GitHub star, and
 * the issue tracker. Swipeable, advances itself every 4 seconds, with
 * animated dot indicators below.
 */
@Composable
fun SupportCarousel() {
    val uriHandler = LocalUriHandler.current
    val pagerState = rememberPagerState(pageCount = { slides.size })

    LaunchedEffect(pagerState) {
        while (true) {
            delay(4_000)
            if (!pagerState.isScrollInProgress) {
                val next = (pagerState.currentPage + 1) % slides.size
                runCatching { pagerState.animateScrollToPage(next) }
            }
        }
    }

    Column {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth()) { page ->
            val slide = slides[page]
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { runCatching { uriHandler.openUri(slide.url) } },
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(slide.icon, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(stringResource(slide.titleRes), style = MaterialTheme.typography.titleSmall)
                        Text(
                            stringResource(slide.subRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(slides.size) { index ->
                val selected = pagerState.currentPage == index
                val dotWidth by animateDpAsState(if (selected) 18.dp else 6.dp, label = "dot")
                Box(
                    Modifier
                        .padding(horizontal = 3.dp)
                        .size(width = dotWidth, height = 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        ),
                )
            }
        }
    }
}
