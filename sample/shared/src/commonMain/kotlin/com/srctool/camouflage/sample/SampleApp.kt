package com.srctool.camouflage.sample

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.button.CamoButton
import com.srctool.camouflage.core.divider.CamoDivider
import com.srctool.camouflage.core.divider.DividerInset
import com.srctool.camouflage.core.iconbutton.CamoIconButton
import com.srctool.camouflage.core.progress.CamoProgressCircle
import com.srctool.camouflage.core.progress.ProgressSize
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.provider.CamouflageProvider
import com.srctool.camouflage.core.skin.CamoSkin
import com.srctool.camouflage.core.surface.CamoSurface
import com.srctool.camouflage.core.text.CamoSpan
import com.srctool.camouflage.core.text.CamoText
import com.srctool.camouflage.core.theme.Appearance
import com.srctool.camouflage.core.theme.CamoColors
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.CamoTypography
import com.srctool.camouflage.core.theme.ElevationLevel
import com.srctool.camouflage.core.theme.ShapeRole
import com.srctool.camouflage.core.theme.SurfaceLevel
import com.srctool.camouflage.core.theme.Tone
import com.srctool.camouflage.core.theme.animateCamoTheme
import com.srctool.camouflage.skin.fluent.FluentBackdrop
import com.srctool.camouflage.skin.fluent.FluentSkin
import com.srctool.camouflage.skin.fluent.FluentSurface
import com.srctool.camouflage.skin.fluent.fluentSkin
import com.srctool.camouflage.skin.fluent.fluentTheme
import com.srctool.camouflage.skin.minimal.MinimalSkin
import com.srctool.camouflage.skin.minimal.minimalTheme
import kotlinx.coroutines.delay

private val DebugSeed = Color(0xFF0B57D0)
private val PromoSeed = Color(0xFFE8710A)
private const val FAKE_REQUEST_MILLIS = 2000L

// The three skins the sample switches between, each with its own starter theme.
internal enum class SampleSkin(val skin: CamoSkin, val theme: (dark: Boolean) -> CamoTheme) {
    Minimal(MinimalSkin, { minimalTheme(dark = it) }),
    Fluent(FluentSkin, { fluentTheme(dark = it) }),
    FluentAcrylic(fluentSkin(FluentSurface.Acrylic), { fluentTheme(dark = it) }),
    Debug(DebugSkin, {
        CamoTheme(colors = CamoColors.fromSeed(primary = DebugSeed, dark = it), typography = CamoTypography.system())
    }),
}

/**
 * A brand on a skin's starter theme, light or dark: everything the sample can switch. A skin with its own token
 * set takes the brand color too (Fluent derives its hover and pressed steps from it), so the brand reaches every
 * state, not only the roles.
 */
internal fun sampleTheme(skin: SampleSkin, brand: Brand, dark: Boolean): CamoTheme {
    val seed = brand.tokens?.brand?.get(BRAND_SEED_STEP)
    val fluent = skin == SampleSkin.Fluent || skin == SampleSkin.FluentAcrylic
    val base = if (fluent && seed != null) {
        fluentTheme(brand = seed, dark = dark)
    } else {
        skin.theme(dark)
    }
    return brand.on(base, dark)
}

private const val BRAND_SEED_STEP = 600

/**
 * The sample app. M4: one screen, two brands, two skins and dark mode, switched live at the provider; then the
 * component gallery.
 */
@Composable
fun SampleApp() {
    val systemDark = isSystemInDarkTheme()
    var dark by remember { mutableStateOf(systemDark) }
    var choice by remember { mutableStateOf(SampleSkin.Minimal) }
    var brand by remember { mutableStateOf(Brand.Default) }
    CamouflageProvider(skin = choice.skin, theme = animateCamoTheme(sampleTheme(choice, brand, dark))) {
        // The source Fluent Acrylic blurs; with other skins it only records.
        FluentBackdrop(Modifier.fillMaxSize()) {
            CamoSurface(Modifier.fillMaxSize()) {
                Column(
                    Modifier.verticalScroll(rememberScrollState()).padding(Camouflage.spacing.l),
                    verticalArrangement = Arrangement.spacedBy(Camouflage.spacing.l),
                ) {
                    Switcher(SampleSkin.entries, choice, { it.name }) { choice = it }
                    Switcher(Brand.entries, brand, { it.label }) { brand = it }
                    CamoButton(if (dark) "Light" else "Dark", { dark = !dark }, appearance = Appearance.Ghost)
                    Checkout()
                    Section("Buttons") { Buttons() }
                    Section("Icon buttons") { IconButtons() }
                    Section("Text") { Texts() }
                    Section("Surfaces and dividers") { Surfaces() }
                    Section("Progress") { Progress() }
                    Section("Selection controls") { SelectionControls() }
                    Section("Sign-up form") { SignUpForm() }
                    Section("Search") { CountrySearch() }
                    Section("Dates and times") { DatesAndTimes() }
                    Section("Settings screen") { SettingsScreen() }
                    Section("Cards") { Cards() }
                    Section("Accordions") { Faq() }
                    Section("Loading placeholders") { LoadingProfile() }
                    Section("Paged list") { Products() }
                    Section("Overlays") { OverlayDemo() }
                    val promo = sampleTheme(choice, brand, dark)
                        .copy(colors = CamoColors.fromSeed(PromoSeed, dark = dark))
                    CamouflageProvider(theme = promo) {
                        Section("Nested provider: a promo theme") {
                            CamoButton("Claim offer", {}, suffixIcon = { AppIcon.ChevronRight() })
                        }
                    }
                }
            }
        }
    }
}

// A row of buttons, one per option; the chosen one is solid.
@Composable
private fun <T> Switcher(options: List<T>, chosen: T, label: (T) -> String, onChoose: (T) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Camouflage.spacing.s),
        verticalArrangement = Arrangement.spacedBy(Camouflage.spacing.s),
    ) {
        options.forEach {
            CamoButton(
                label(it),
                { onChoose(it) },
                appearance = if (it == chosen) Appearance.Solid else Appearance.Outline,
            )
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    CamoSurface(Modifier.fillMaxWidth(), level = SurfaceLevel.Low, shape = ShapeRole.Large, border = true) {
        Column(
            Modifier.padding(Camouflage.spacing.l),
            verticalArrangement = Arrangement.spacedBy(Camouflage.spacing.m),
        ) {
            CamoText(title, style = Camouflage.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun Buttons() {
    val spacing = Arrangement.spacedBy(Camouflage.spacing.s)
    for (tone in listOf(Tone.Default, Tone.Error)) {
        for (enabled in listOf(true, false)) {
            FlowRow(horizontalArrangement = spacing, verticalArrangement = spacing) {
                Appearance.entries.forEach { CamoButton(it.name, {}, appearance = it, tone = tone, enabled = enabled) }
            }
        }
    }
    FlowRow(horizontalArrangement = spacing, verticalArrangement = spacing) {
        CamoButton("Search", {}, prefixIcon = { AppIcon.Search() })
        CamoButton("Next", {}, appearance = Appearance.Outline, suffixIcon = { AppIcon.ChevronRight() })
        CamoButton("Delete", {}, appearance = Appearance.Ghost, tone = Tone.Error, prefixIcon = { AppIcon.Delete() })
    }
    var loading by remember { mutableStateOf(false) }
    LaunchedEffect(loading) {
        if (loading) {
            delay(FAKE_REQUEST_MILLIS)
            loading = false
        }
    }
    CamoButton("Submit", { loading = true }, Modifier.fillMaxWidth(), loading = loading)
}

@Composable
private fun IconButtons() {
    var favorite by remember { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(Camouflage.spacing.s)) {
        CamoIconButton({ AppIcon.Back() }, "Back", {}, tooltip = "Back")
        CamoIconButton({ AppIcon.Search() }, "Search", {}, appearance = Appearance.Soft, tooltip = "Search the library")
        CamoIconButton(
            { if (favorite) AppIcon.Star() else AppIcon.StarOutline() },
            "Favorite",
            { favorite = !favorite },
            checked = favorite,
            tooltip = "Add to favorites",
        )
        CamoIconButton({ AppIcon.Delete() }, "Delete", {}, tone = Tone.Error, tooltip = "Delete")
        CamoIconButton({ AppIcon.Search() }, "Search", {}, appearance = Appearance.Solid)
        CamoIconButton({ AppIcon.Search() }, "Search", {}, enabled = false)
    }
}

@Composable
private fun Texts() {
    val t = Camouflage.typography
    listOf(
        "Display small" to t.displaySmall, "Headline medium" to t.headlineMedium, "Title large" to t.titleLarge,
        "Title medium" to t.titleMedium, "Body large" to t.bodyLarge, "Body medium" to t.bodyMedium,
        "Body small" to t.bodySmall, "Label large" to t.labelLarge, "Label small" to t.labelSmall,
    ).forEach { (name, style) -> CamoText(name, style = style) }
    CamoText("Bold body", style = t.bodyMedium.copy(fontWeight = FontWeight.Bold))
    CamoText(
        listOf(
            CamoSpan("I agree to the "),
            CamoSpan("Terms", link = {}),
            CamoSpan(" and the "),
            CamoSpan("Privacy Policy", link = {}),
            CamoSpan("."),
        ),
    )
    CamoText("Saved", color = Camouflage.colors.success)
}

@Composable
private fun Surfaces() {
    Row(horizontalArrangement = Arrangement.spacedBy(Camouflage.spacing.s)) {
        SurfaceLevel.entries.forEach {
            CamoSurface(Modifier.size(48.dp), level = it, shape = ShapeRole.Medium, border = true) {}
        }
        CamoSurface(Modifier.size(48.dp), shape = ShapeRole.Medium, elevation = ElevationLevel.Level3) {}
    }
    CamoDivider()
    CamoDivider(inset = DividerInset.Start)
    CamoDivider(inset = DividerInset.Middle)
    Row(Modifier.height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
        CamoText("Left")
        CamoDivider(Modifier.padding(horizontal = Camouflage.spacing.m), orientation = Orientation.Vertical)
        CamoText("Right")
    }
}

@Composable
private fun Progress() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Camouflage.spacing.l),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CamoProgressCircle(size = ProgressSize.Small, label = "Loading")
        CamoProgressCircle(label = "Loading")
        CamoProgressCircle(progress = 0.6f, size = ProgressSize.Large, label = "Uploading")
        CamoProgressCircle(progress = 0.3f, tone = Tone.Error, label = "Upload failed")
    }
}
