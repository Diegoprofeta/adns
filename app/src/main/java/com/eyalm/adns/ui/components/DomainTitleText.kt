package com.eyalm.adns.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle

@Composable
fun DomainTitleText(
    domain: String,
    root: String? = null,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    color: Color = MaterialTheme.colorScheme.onSurface,
    subdomainColor: Color = color.copy(alpha = 0.6f),
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Ellipsis,
) {
    val effectiveRoot = root ?: remember(domain) {
        val parts = domain.split(".")
        if (parts.size >= 2) parts.takeLast(2).joinToString(".") else null
    }

    val annotatedString = remember(domain, effectiveRoot, color, subdomainColor) {
        buildAnnotatedString {
            if (effectiveRoot != null && domain.endsWith(effectiveRoot) && domain != effectiveRoot) {
                val prefix = domain.substring(0, domain.length - effectiveRoot.length)
                withStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Normal,
                        color = subdomainColor
                    )
                ) {
                    append(prefix)
                }
                withStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                ) {
                    append(effectiveRoot)
                }
            } else {
                withStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                ) {
                    append(domain)
                }
            }
        }
    }

    Text(
        text = annotatedString,
        style = style,
        modifier = modifier,
        maxLines = maxLines,
        overflow = overflow
    )
}
