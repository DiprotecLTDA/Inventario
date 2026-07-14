package com.diprotec.inventario.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun InventoryTopBar(
    title: String,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BrandPrimary)
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )

        actions()
    }
}

@Composable
fun StatusChip(
    dotColor: Color,
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    valueTextStyle: TextStyle? = null,
    valueTextAlign: TextAlign? = null
) {
    Surface(
        modifier = modifier
            .widthIn(min = 150.dp)
            .heightIn(min = 40.dp),
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 1.dp,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(dotColor, CircleShape)
            )

            Spacer(modifier = Modifier.size(8.dp))

            Text(
                text = "$title:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )

            Spacer(modifier = Modifier.size(6.dp))

            Text(
                text = value,
                style = valueTextStyle ?: MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = valueTextAlign,
                maxLines = 1
            )
        }
    }
}

@Composable
fun SegmentedToggle(
    firstText: String,
    secondText: String,
    firstSelected: Boolean,
    onFirstClick: () -> Unit,
    onSecondClick: () -> Unit,
    modifier: Modifier = Modifier,
    firstOptionModifier: Modifier = Modifier,
    secondOptionModifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(56.dp)
            .border(1.dp, BorderGray, RoundedCornerShape(50.dp))
            .clip(RoundedCornerShape(50.dp))
            .background(White),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SegmentedToggleOption(
            text = firstText,
            selected = firstSelected,
            onClick = onFirstClick,
            modifier = firstOptionModifier.weight(1f)
        )

        SegmentedToggleOption(
            text = secondText,
            selected = !firstSelected,
            onClick = onSecondClick,
            modifier = secondOptionModifier.weight(1f)
        )
    }
}

@Composable
private fun SegmentedToggleOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(50.dp))
            .background(if (selected) BrandPrimary else White)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) White else TextPrimary,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun OutlinedInfoCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = White,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
        modifier = modifier
            .border(1.dp, BorderGray, RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Box(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

@Composable
fun AppPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconContentDescription: String? = text,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    Surface(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = RoundedCornerShape(16.dp),
        color = if (enabled) BrandPrimary else BorderGray,
        modifier = modifier.height(52.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                icon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = iconContentDescription,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))
                }

                Text(
                    text = text,
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun inventoryTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = White,
    unfocusedContainerColor = White,
    disabledContainerColor = White,
    errorContainerColor = White,
    focusedBorderColor = BrandPrimary,
    unfocusedBorderColor = BorderGray,
    disabledBorderColor = BorderGray,
    errorBorderColor = StatusError,
    focusedLabelColor = LabelGray,
    unfocusedLabelColor = LabelGray,
    disabledLabelColor = LabelGray,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    disabledTextColor = TextPrimary,
    focusedLeadingIconColor = BrandPrimary,
    unfocusedLeadingIconColor = BrandPrimary,
    disabledLeadingIconColor = BrandPrimary,
    focusedTrailingIconColor = BrandPrimary,
    unfocusedTrailingIconColor = BrandPrimary,
    disabledTrailingIconColor = BrandPrimary,
    cursorColor = BrandPrimary
)
