package com.eyalm.adns.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class FloatingMenuItem<T>(
    val id: T,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

@Composable
fun <T> ExpressiveFloatingMenu(
    items: List<FloatingMenuItem<T>>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 6.dp,
            shadowElevation = 10.dp,
        ) {
            Row(
                modifier = Modifier.padding(all = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val isSelected = selectedItem == item.id
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()

                    val activeBgColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            Color.Transparent
                        },
                        animationSpec = spring(
                            dampingRatio = 0.85f,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "FloatingMenuBgColor"
                    )

                    val activeContentColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        animationSpec = spring(
                            dampingRatio = 0.85f,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "FloatingMenuContentColor"
                    )

                    val itemHorizontalPadding by animateDpAsState(
                        targetValue = if (isSelected) 18.dp else 14.dp,
                        animationSpec = spring(
                            dampingRatio = 0.85f,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "FloatingMenuItemPadding"
                    )

                    val iconScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.1f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = 0.7f,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "FloatingMenuIconScale"
                    )

                    val pressScale by animateFloatAsState(
                        targetValue = if (isPressed) 0.94f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = 0.8f,
                            stiffness = Spring.StiffnessHigh
                        ),
                        label = "FloatingMenuPressScale"
                    )

                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = pressScale
                                scaleY = pressScale
                            }
                            .clip(CircleShape)
                            .background(activeBgColor)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = ripple(bounded = true),
                                onClick = { onItemSelected(item.id) }
                            )
                            .padding(horizontal = itemHorizontalPadding, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label,
                                tint = activeContentColor,
                                modifier = Modifier
                                    .size(22.dp)
                                    .graphicsLayer {
                                        scaleX = iconScale
                                        scaleY = iconScale
                                    }
                            )
                            AnimatedVisibility(
                                visible = isSelected,
                                enter = fadeIn(
                                    animationSpec = spring(
                                        dampingRatio = 0.85f,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                ) + expandHorizontally(
                                    expandFrom = Alignment.Start,
                                    clip = true,
                                    animationSpec = spring(
                                        dampingRatio = 0.85f,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                ) + androidx.compose.animation.slideInHorizontally(
                                    initialOffsetX = { -it },
                                    animationSpec = spring(
                                        dampingRatio = 0.85f,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                ),
                                exit = fadeOut(
                                    animationSpec = spring(
                                        dampingRatio = 0.85f,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                ) + shrinkHorizontally(
                                    shrinkTowards = Alignment.Start,
                                    clip = true,
                                    animationSpec = spring(
                                        dampingRatio = 0.85f,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                ) + androidx.compose.animation.slideOutHorizontally(
                                    targetOffsetX = { -it },
                                    animationSpec = spring(
                                        dampingRatio = 0.85f,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = item.label,
                                        color = activeContentColor,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


