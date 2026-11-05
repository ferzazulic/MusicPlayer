package com.ferzazulic.carmusic.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ferzazulic.carmusic.ui.theme.LocalCarTheme

@Composable
fun SettingsCard(title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    val t = LocalCarTheme.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .clip(RoundedCornerShape(t.corner + 6.dp))
            .background(t.surface.copy(alpha = 0.82f))
            .border(1.dp, t.outline.copy(alpha = 0.6f), RoundedCornerShape(t.corner + 6.dp))
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        if (title != null) {
            Text(t.label(title), color = t.accent, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
        }
        content()
    }
}

@Composable
fun SettingSwitch(title: String, subtitle: String? = null, checked: Boolean, onChange: (Boolean) -> Unit) {
    val t = LocalCarTheme.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(t.corner))
            .clickable { onChange(!checked) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = t.text, style = MaterialTheme.typography.titleMedium)
            if (subtitle != null) Text(subtitle, color = t.textMuted, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked, onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = t.onAccent, checkedTrackColor = t.accent,
                uncheckedThumbColor = t.textMuted, uncheckedTrackColor = t.surfaceHigh, uncheckedBorderColor = t.outline,
            ),
        )
    }
}

@Composable
fun SettingSlider(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    label: (Float) -> String,
    onChange: (Float) -> Unit,
) {
    val t = LocalCarTheme.current
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row {
            Text(title, color = t.text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(label(value), color = t.accent, style = MaterialTheme.typography.titleMedium)
        }
        Slider(
            value, onChange, valueRange = range, steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = t.accent, activeTrackColor = t.accent, inactiveTrackColor = t.surfaceHigh,
                activeTickColor = t.onAccent.copy(alpha = 0.4f), inactiveTickColor = t.textMuted.copy(alpha = 0.4f),
            ),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> SettingChoice(title: String, subtitle: String? = null, options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    val t = LocalCarTheme.current
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(title, color = t.text, style = MaterialTheme.typography.titleMedium)
        if (subtitle != null) Text(subtitle, color = t.textMuted, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (value, name) -> Chip(name, value == selected) { onSelect(value) } }
        }
    }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    val t = LocalCarTheme.current
    val bg by animateColorAsState(if (selected) t.accent else t.surfaceHigh, tween(t.duration(250)), label = "chip")
    val fg by animateColorAsState(if (selected) t.onAccent else t.text, tween(t.duration(250)), label = "chipFg")
    Box(
        Modifier
            .clip(RoundedCornerShape(if (t.corner >= 8.dp) 50.dp else t.corner))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 11.dp)
    ) {
        Text(text, color = fg, style = MaterialTheme.typography.labelLarge)
    }
}

/** Vertical equaliser band slider with a glowing fill. */
@Composable
fun VerticalBandSlider(value: Int, min: Int, max: Int, enabled: Boolean, height: Dp, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val t = LocalCarTheme.current
    val accent = if (enabled) t.accent else t.textMuted
    fun fromY(y: Float, h: Float): Int = (max - (y / h).coerceIn(0f, 1f) * (max - min)).toInt().let { (it / 50) * 50 }
    Canvas(
        modifier
            .width(56.dp)
            .height(height)
            .pointerInput(enabled) {
                if (enabled) detectTapGestures { onChange(fromY(it.y, size.height.toFloat())) }
            }
            .pointerInput(enabled) {
                if (enabled) detectDragGestures { change, _ -> onChange(fromY(change.position.y, size.height.toFloat())) }
            }
    ) {
        val cx = size.width / 2
        val trackW = 8.dp.toPx()
        val frac = (value - min).toFloat() / (max - min)
        val y = size.height * (1f - frac)
        val zeroY = size.height * (1f - (0 - min).toFloat() / (max - min))
        drawRoundRect(t.surfaceHigh, Offset(cx - trackW / 2, 0f), Size(trackW, size.height), CornerRadius(trackW / 2))
        drawLine(t.textMuted.copy(alpha = 0.5f), Offset(cx - 18.dp.toPx(), zeroY), Offset(cx + 18.dp.toPx(), zeroY), 1.dp.toPx())
        val top = minOf(y, zeroY)
        val bottom = maxOf(y, zeroY)
        drawRoundRect(accent, Offset(cx - trackW / 2, top), Size(trackW, bottom - top), CornerRadius(trackW / 2))
        drawCircle(accent.copy(alpha = 0.25f), 20.dp.toPx(), Offset(cx, y))
        drawCircle(Color.White, 11.dp.toPx(), Offset(cx, y))
        drawCircle(accent, 6.dp.toPx(), Offset(cx, y))
    }
}
