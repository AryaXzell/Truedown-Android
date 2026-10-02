package com.aryaxzell.truedown.theme

import androidx.compose.ui.graphics.Color
import com.aryaxzell.truedown.ui.theme.DarkBackground
import com.aryaxzell.truedown.ui.theme.DarkOnBackground
import com.aryaxzell.truedown.ui.theme.DarkOnPrimary
import com.aryaxzell.truedown.ui.theme.DarkOnSurface
import com.aryaxzell.truedown.ui.theme.DarkOnSurfaceVariant
import com.aryaxzell.truedown.ui.theme.DarkOutline
import com.aryaxzell.truedown.ui.theme.DarkPrimary
import com.aryaxzell.truedown.ui.theme.DarkSurface
import com.aryaxzell.truedown.ui.theme.DarkSurfaceContainerHighest
import com.aryaxzell.truedown.ui.theme.LightBackground
import com.aryaxzell.truedown.ui.theme.LightOnBackground
import com.aryaxzell.truedown.ui.theme.LightOnPrimary
import com.aryaxzell.truedown.ui.theme.LightOnSecondary
import com.aryaxzell.truedown.ui.theme.LightOnSurface
import com.aryaxzell.truedown.ui.theme.LightOutline
import com.aryaxzell.truedown.ui.theme.LightPrimary
import com.aryaxzell.truedown.ui.theme.LightSecondary
import com.aryaxzell.truedown.ui.theme.LightSurface
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class ColorContrastTest {

    private fun linearize(channel: Float): Double {
        return if (channel <= 0.03928) {
            channel / 12.92
        } else {
            ((channel + 0.055) / 1.055).toDouble().pow(2.4)
        }
    }

    private fun relativeLuminance(color: Color): Double {
        val r = linearize(color.red)
        val g = linearize(color.green)
        val b = linearize(color.blue)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun calculateContrastRatio(c1: Color, c2: Color): Double {
        val l1 = relativeLuminance(c1)
        val l2 = relativeLuminance(c2)
        val lighter = max(l1, l2)
        val darker = min(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    @Test
    fun testLightSecondaryContrast_atLeast4_5() {
        val ratio = calculateContrastRatio(LightOnSecondary, LightSecondary)
        assertTrue("LightSecondary vs White ratio $ratio must be >= 4.5", ratio >= 4.5)
    }

    @Test
    fun testLightPrimaryContrast_atLeast4_5() {
        val ratio = calculateContrastRatio(LightOnPrimary, LightPrimary)
        assertTrue("LightPrimary vs White ratio $ratio must be >= 4.5", ratio >= 4.5)
    }

    @Test
    fun testLightTextContrast_atLeast4_5() {
        val ratio = calculateContrastRatio(LightOnBackground, LightBackground)
        assertTrue("Light text on background ratio $ratio must be >= 4.5", ratio >= 4.5)

        val surfaceRatio = calculateContrastRatio(LightOnSurface, LightSurface)
        assertTrue("Light text on surface ratio $surfaceRatio must be >= 4.5", surfaceRatio >= 4.5)
    }

    @Test
    fun testDarkTextContrast_atLeast4_5() {
        val ratio = calculateContrastRatio(DarkOnBackground, DarkBackground)
        assertTrue("Dark text on background ratio $ratio must be >= 4.5", ratio >= 4.5)

        val surfaceRatio = calculateContrastRatio(DarkOnSurface, DarkSurface)
        assertTrue("Dark text on surface ratio $surfaceRatio must be >= 4.5", surfaceRatio >= 4.5)

        val onSurfaceVariantRatio = calculateContrastRatio(DarkOnSurfaceVariant, DarkSurfaceContainerHighest)
        assertTrue("Dark onSurfaceVariant on surfaceContainerHighest ratio $onSurfaceVariantRatio must be >= 4.5", onSurfaceVariantRatio >= 4.5)
    }

    @Test
    fun testBorderContrasts_atLeast3_0() {
        val lightBorderRatio = calculateContrastRatio(LightOutline, LightBackground)
        assertTrue("Light outline vs background ratio $lightBorderRatio must be >= 3.0", lightBorderRatio >= 3.0)

        val darkBorderRatio = calculateContrastRatio(DarkOutline, DarkSurface)
        assertTrue("Dark outline vs surface ratio $darkBorderRatio must be >= 3.0", darkBorderRatio >= 3.0)
    }

    @Test
    fun testWarnBadgeContrast_atLeast4_5() {
        val badgeBg = Color(0xFFFFE0B2)
        val badgeText = Color(0xFF8D2F00)
        val ratio = calculateContrastRatio(badgeText, badgeBg)
        assertTrue("WARN badge ratio $ratio must be >= 4.5", ratio >= 4.5)
    }
}
