package com.portalhomebase.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portalhomebase.app.ui.theme.Ink

private val FlapTop = Color(0xFF3B3B44)
private val FlapBottom = Color(0xFF232329)
private val FlapInk = Color(0xFFF5EFE0)

private val CellH = 66.dp

// Split-flap clock: date + time + AM/PM, all flaps. Each cell is a two-tone
// card; on change the old top half falls away and the new bottom half lands.
@Composable
fun FlipClock(
    dayWord: String,
    monWord: String,
    dayNum: String,
    hms: String,
    ampm: String,
    tick: Int,
    modifier: Modifier = Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FlipCell(dayWord, 74.dp, 26.sp)
            FlipCell(monWord, 74.dp, 26.sp)
            FlipCell(dayNum, 52.dp, 32.sp)
        }
        Spacer(Modifier.width(12.dp))
        val groups = remember(hms) { hms.split(":") }
        groups.forEachIndexed { gi, g ->
            if (gi > 0) FlipColon(tick, Modifier.padding(horizontal = 5.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                g.forEach { FlipCell("$it", 46.dp, 44.sp) }
            }
        }
        Spacer(Modifier.width(12.dp))
        FlipCell(ampm, 58.dp, 26.sp)
    }
}

@Composable
private fun FlipCell(text: String, width: Dp, fontSize: TextUnit) {
    var settled by remember { mutableStateOf(text) }
    var outgoing by remember { mutableStateOf(text) }
    val progress = remember { Animatable(1f) }
    LaunchedEffect(text) {
        if (text != settled) {
            // Settle first: when the animation lands on 1f the cell must
            // already read the new value, or one frame flashes the old one.
            outgoing = settled
            settled = text
            progress.snapTo(0f)
            progress.animateTo(1f, tween(550, easing = LinearEasing))
        }
    }
    val p = progress.value
    val animating = p < 1f
    val density = LocalDensity.current.density
    Box(Modifier.size(width, CellH)) {
        // Revealed halves: new value on top, old value below.
        FlapHalf(if (animating) text else settled, width, fontSize, top = true)
        FlapHalf(if (animating) outgoing else settled, width, fontSize, top = false)
        // Rotating flap hinged at the seam: old top falls first, then the
        // new bottom lands.
        if (animating && p < 0.5f) {
            FlapHalf(outgoing, width, fontSize, top = true, Modifier.graphicsLayer {
                cameraDistance = 10 * density
                transformOrigin = TransformOrigin(0.5f, 0.5f)
                rotationX = -90f * (p / 0.5f)
            })
        } else if (animating) {
            FlapHalf(text, width, fontSize, top = false, Modifier.graphicsLayer {
                cameraDistance = 10 * density
                transformOrigin = TransformOrigin(0.5f, 0.5f)
                rotationX = 90f * (1f - (p - 0.5f) / 0.5f)
            })
        }
        // Seam + hinge pins.
        Box(
            Modifier.align(Alignment.Center).fillMaxWidth().height(2.dp)
                .background(Color.Black.copy(alpha = 0.55f)),
        )
        Box(
            Modifier.align(Alignment.CenterStart).offset(x = 2.dp).size(6.dp)
                .clip(CircleShape).background(Color(0xFF101014)),
        )
        Box(
            Modifier.align(Alignment.CenterEnd).offset(x = -2.dp).size(6.dp)
                .clip(CircleShape).background(Color(0xFF101014)),
        )
    }
}

// One visible half of a cell: a half-height card plus the full text drawn
// once, clipped to that half so top and bottom always align at the seam.
@Composable
private fun FlapHalf(
    text: String,
    width: Dp,
    fontSize: TextUnit,
    top: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = if (top) RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)
    else RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp)
    Box(modifier.size(width, CellH), contentAlignment = Alignment.Center) {
        Box(
            Modifier.align(if (top) Alignment.TopCenter else Alignment.BottomCenter)
                .width(width).height(CellH / 2).clip(shape)
                .background(if (top) FlapTop else FlapBottom),
        )
        Text(
            text,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = FlapInk,
            modifier = Modifier.drawWithContent {
                val mid = size.height / 2f
                clipRect(
                    top = if (top) 0f else mid,
                    bottom = if (top) mid else size.height,
                ) { this@drawWithContent.drawContent() }
            },
        )
    }
}

@Composable
private fun FlipColon(tick: Int, modifier: Modifier = Modifier) {
    val alpha = if (tick % 2 == 0) 0.9f else 0.25f
    Column(
        modifier.width(14.dp).height(CellH),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(Ink.copy(alpha = alpha)))
        Spacer(Modifier.height(10.dp))
        Box(Modifier.size(8.dp).clip(CircleShape).background(Ink.copy(alpha = alpha)))
    }
}
