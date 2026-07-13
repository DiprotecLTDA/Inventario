package com.diprotec.inventario.ui.theme

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private val InventoryButtonMinHeight = 86.dp
private val InventoryButtonCircleSize = 56.dp
private val InventoryButtonIconSize = 26.dp
private val InventoryButtonBorderWidth = 2.dp
private val InventoryButtonTextSpacing = 6.dp

@Composable
fun InventoryMenuButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    contentDescription: String = text,
    containerColor: Color = BrandSurfaceTint,
    disabledContainerColor: Color = BrandSurfaceTint,
    borderColor: Color = BrandAccent,
    disabledBorderColor: Color = BorderGray,
    iconColor: Color = BrandAccent,
    disabledIconColor: Color = LabelGray,
    textColor: Color = TextPrimary,
    disabledTextColor: Color = LabelGray,
    loadingColor: Color = BrandAccent
) {
    Column(
        modifier = modifier.heightIn(min = InventoryButtonMinHeight),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            onClick = onClick,
            enabled = enabled && !loading,
            shape = CircleShape,
            color = if (enabled) containerColor else disabledContainerColor,
            modifier = Modifier.size(InventoryButtonCircleSize)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = InventoryButtonBorderWidth,
                        color = if (enabled) borderColor else disabledBorderColor,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = loadingColor
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = contentDescription,
                        tint = if (enabled) iconColor else disabledIconColor,
                        modifier = Modifier.size(InventoryButtonIconSize)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.size(InventoryButtonTextSpacing))

        Text(
            text = text,
            color = if (enabled) textColor else disabledTextColor,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
