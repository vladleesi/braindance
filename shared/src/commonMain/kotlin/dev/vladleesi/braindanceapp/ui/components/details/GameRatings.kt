// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.about_critic
import dev.vladleesi.braindanceapp.resources.about_user
import dev.vladleesi.braindanceapp.resources.badge_adult
import dev.vladleesi.braindanceapp.resources.game_score_accessibility
import dev.vladleesi.braindanceapp.resources.game_score_percent
import dev.vladleesi.braindanceapp.ui.components.Tag
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.CornerShapes
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.hotMagenta
import dev.vladleesi.braindanceapp.ui.style.hotMagentaLight
import dev.vladleesi.braindanceapp.ui.style.neonCyan
import dev.vladleesi.braindanceapp.ui.style.secondary
import dev.vladleesi.braindanceapp.ui.style.secondaryText
import dev.vladleesi.braindanceapp.ui.style.secondaryVariant
import dev.vladleesi.braindanceapp.ui.style.signalYellow
import dev.vladleesi.braindanceapp.ui.style.void
import dev.vladleesi.braindanceapp.ui.style.white
import dev.vladleesi.braindanceapp.ui.viewmodels.GameBadge
import dev.vladleesi.braindanceapp.utils.placeholder
import dev.vladleesi.braindanceapp.utils.placeholderSemantics
import org.jetbrains.compose.resources.stringResource
import kotlin.math.PI
import kotlin.math.asin

private const val MAX_SCORE = 100
private const val SCORE_RING_TOP_ANGLE = -90f
private const val FULL_CIRCLE_DEGREES = 360f
private const val FULL_RING_BLEND_FRACTION = 0.025f
private const val STATUS_TINT_FRACTION = 0.12f
private val ScoreRingWidth = Dimens.tiny
private val ScoreTextInset = ScoreRingWidth + Dimens.micro + Dimens.one
private val MinimumScoreDiameter = Dimens.large + Dimens.compact
private val MinimumScoreFontSize = 11.sp

@Composable
internal fun GameRatings(
    badges: List<GameBadge>,
    placeholder: Boolean = false,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Dimens.compact),
        verticalArrangement = Arrangement.spacedBy(Dimens.small),
    ) {
        badges.forEach { badge ->
            when (badge) {
                is GameBadge.Score -> GameScore(stringResource(badge.label), badge.score, placeholder = placeholder)
                is GameBadge.Status ->
                    StatusTag(
                        stringResource(badge.label),
                        badge.tagColor(),
                        placeholder,
                        Modifier.align(Alignment.CenterVertically),
                    )
                GameBadge.Adult ->
                    StatusTag(
                        stringResource(Res.string.badge_adult),
                        hotMagentaLight,
                        placeholder,
                        Modifier.align(Alignment.CenterVertically),
                    )
            }
        }
    }
}

@Composable
private fun StatusTag(
    text: String,
    color: Color,
    placeholder: Boolean,
    modifier: Modifier,
) {
    Tag(
        text,
        modifier =
            modifier
                .placeholder(visible = placeholder, shape = CornerShapes.pill)
                .background(lerp(secondary, color, STATUS_TINT_FRACTION), CornerShapes.pill),
        color = color,
    )
}

private fun GameBadge.Status.tagColor() =
    when (this) {
        // Still being made: playable, but not finished.
        GameBadge.Status.Alpha, GameBadge.Status.Beta, GameBadge.Status.EarlyAccess -> signalYellow
        // No longer, or not yet, something to play or buy.
        GameBadge.Status.Offline, GameBadge.Status.Cancelled, GameBadge.Status.Delisted -> neonCyan
        GameBadge.Status.Rumored -> neonCyan
    }

@Composable
private fun GameScore(
    label: String,
    score: Int,
    placeholder: Boolean = false,
) {
    val description = stringResource(Res.string.game_score_accessibility, label, score)
    val scoreStyle = MaterialTheme.typography.h4
    // Grow the ring with the font scale and fit the percentage within its inner edge.
    val scaledDiameter = with(LocalDensity.current) { scoreStyle.fontSize.toDp() * 2 + Dimens.small }
    val accessibility =
        if (placeholder) {
            Modifier.placeholderSemantics()
        } else {
            Modifier.clearAndSetSemantics { text = AnnotatedString(description) }
        }
    Column(
        modifier = accessibility,
        verticalArrangement = Arrangement.spacedBy(Dimens.tiny),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier
                    .size(maxOf(MinimumScoreDiameter, scaledDiameter))
                    .placeholder(visible = placeholder, shape = CircleShape)
                    .scoreRing(score),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                text = stringResource(Res.string.game_score_percent, score),
                modifier = Modifier.padding(horizontal = ScoreTextInset),
                style = scoreStyle.copy(color = white),
                softWrap = false,
                maxLines = 1,
                autoSize =
                    TextAutoSize.StepBased(
                        minFontSize = MinimumScoreFontSize,
                        maxFontSize = scoreStyle.fontSize,
                        stepSize = 1.sp,
                    ),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.caption,
            color = secondaryText,
            modifier = Modifier.placeholder(visible = placeholder, shape = CornerShapes.small),
        )
    }
}

private fun Modifier.scoreRing(score: Int): Modifier =
    drawWithCache {
        val radius = size.minDimension / 2
        val ringWidth = ScoreRingWidth.toPx().coerceAtMost(radius)
        val ringRadius = radius - ringWidth / 2
        val stroke = Stroke(ringWidth)
        val center = Offset(size.width / 2, size.height / 2)
        val arcTopLeft = center - Offset(ringRadius, ringRadius)
        val arcSize = Size(ringRadius * 2, ringRadius * 2)
        val fraction = score.toFloat() / MAX_SCORE
        val sweepAngle = FULL_CIRCLE_DEGREES * fraction
        val capAngle =
            if (ringRadius > 0f) {
                asin(ringWidth / (ringRadius * 2)) * FULL_CIRCLE_DEGREES / (PI.toFloat() * 2)
            } else {
                0f
            }
        // Inset rounded ends by their angular radius to keep the visible fill proportional to the score.
        val roundEnds = score < MAX_SCORE && sweepAngle > capAngle * 2
        val inset = if (roundEnds) capAngle else 0f
        val fillStroke = if (roundEnds) Stroke(ringWidth, cap = StrokeCap.Round) else stroke
        val unfilled = 1f - fraction
        val gradient =
            if (score > 0) {
                // Sweep stops run clockwise, so the filled sector reads pink to yellow to cyan counterclockwise.
                Brush.sweepGradient(
                    0f to hotMagenta,
                    maxOf(unfilled, FULL_RING_BLEND_FRACTION) to neonCyan,
                    (unfilled + 1f) / 2 to signalYellow,
                    1f to hotMagenta,
                    center = center,
                )
            } else {
                null
            }
        onDrawBehind {
            drawCircle(void, radius = radius)
            if (score < MAX_SCORE) drawCircle(secondaryVariant, radius = ringRadius, style = stroke)
            if (gradient != null && ringRadius > 0f) {
                rotate(SCORE_RING_TOP_ANGLE, pivot = center) {
                    drawArc(
                        brush = gradient,
                        startAngle = -sweepAngle + inset,
                        sweepAngle = sweepAngle - inset * 2,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = fillStroke,
                    )
                }
            }
        }
    }

private const val PREVIEW_CRITIC_SCORE = 74
private const val PREVIEW_PLAYER_SCORE = 53

@Preview
@Composable
internal fun GameRatingsPreview() {
    BraindanceTheme {
        GameRatings(
            listOf(
                GameBadge.Score(Res.string.about_critic, PREVIEW_CRITIC_SCORE),
                GameBadge.Score(Res.string.about_user, PREVIEW_PLAYER_SCORE),
                GameBadge.Status.EarlyAccess,
            ),
        )
    }
}

@Preview
@Composable
internal fun GameScorePreview() {
    BraindanceTheme {
        GameScore("Players", MAX_SCORE)
    }
}
