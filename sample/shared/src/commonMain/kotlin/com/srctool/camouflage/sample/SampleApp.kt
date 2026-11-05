@file:Suppress("TooManyFunctions") // one demo function per catalog page

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
import com.srctool.camouflage.core.toolbar.CamoToolbar
import com.srctool.camouflage.core.toolbar.CamoToolbarAction
import com.srctool.camouflage.core.toolbar.ToolbarPlacement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import com.srctool.camouflage.core.card.CamoCard
import com.srctool.camouflage.core.topbar.CamoTopBar
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
import com.srctool.camouflage.skin.cupertino.CupertinoBackdrop
import com.srctool.camouflage.skin.cupertino.CupertinoSkin
import com.srctool.camouflage.skin.cupertino.CupertinoSurface
import com.srctool.camouflage.skin.cupertino.cupertinoSkin
import com.srctool.camouflage.skin.cupertino.cupertinoTheme
import com.srctool.camouflage.skin.minimal.MinimalSkin
import com.srctool.camouflage.skin.material.MaterialSkin
import com.srctool.camouflage.skin.material.materialTheme
import com.srctool.camouflage.skin.minimal.minimalTheme
import kotlinx.coroutines.delay

private val DebugSeed = Color(0xFF0B57D0)
private val PromoSeed = Color(0xFFE8710A)
private const val FAKE_REQUEST_MILLIS = 2000L

// The skins the sample switches between, each with its own starter theme.
internal enum class SampleSkin(val skin: CamoSkin, val theme: (dark: Boolean) -> CamoTheme) {
    Minimal(MinimalSkin, { minimalTheme(dark = it) }),
    Fluent(FluentSkin, { fluentTheme(dark = it) }),
    FluentAcrylic(fluentSkin(FluentSurface.Acrylic), { fluentTheme(dark = it) }),
    Material(MaterialSkin, { materialTheme(dark = it) }),
    Cupertino(CupertinoSkin, { cupertinoTheme(dark = it) }),
    CupertinoGlass(cupertinoSkin(CupertinoSurface.LiquidGlass), { cupertinoTheme(dark = it) }),
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
    } else if (skin == SampleSkin.Material && seed != null) {
        materialTheme(seed = seed, dark = dark)
    } else if ((skin == SampleSkin.Cupertino || skin == SampleSkin.CupertinoGlass) && seed != null) {
        cupertinoTheme(accent = seed, dark = dark)
    } else {
        skin.theme(dark)
    }
    return brand.on(base, dark)
}

private const val BRAND_SEED_STEP = 600

/** A component page of the catalog: a title, one line about it and its demo. */
private class CatalogEntry(val title: String, val description: String, val content: @Composable () -> Unit)

private val Entries = listOf(
    CatalogEntry("Checkout", "A screen built from the components: a bar, rows, text, a button, a link.") { Checkout() },
    CatalogEntry("Buttons", "Six appearances in two tones, with icons, a disabled state and loading.") { Buttons() },
    CatalogEntry("Icon buttons", "Icon-only buttons: plain, tonal, toggled, destructive, tooltips.") { IconButtons() },
    CatalogEntry("Text", "The type scale, bold runs and links inside a sentence.") { Texts() },
    CatalogEntry("Surfaces", "Surfaces, levels and dividers.") { Surfaces() },
    CatalogEntry("Progress", "Linear and circular indicators, determinate and not.") { Progress() },
    CatalogEntry("Selection controls", "Checkboxes, switches, radios, a segmented control, a slider and a stepper.") {
        SelectionControls()
    },
    CatalogEntry("Forms", "A sign-up form: text fields, validation and a submit button.") { SignUpForm() },
    CatalogEntry("Search", "A search field filtering a list of countries.") { CountrySearch() },
    CatalogEntry("Dates and times", "Date and time pickers, as fields and as wheels.") { DatesAndTimes() },
    CatalogEntry("Settings", "A settings screen with grouped rows and toggles.") { SettingsScreen() },
    CatalogEntry("Cards", "Cards in every appearance and tone, clickable and selectable.") { Cards() },
    CatalogEntry("Accordions", "Disclosure sections that open and close.") { Faq() },
    CatalogEntry("Loading placeholders", "Skeletons that stand in for content.") { LoadingProfile() },
    CatalogEntry("Paged list", "An endless list with pull to refresh, retry and empty states.") { Products() },
    CatalogEntry("Overlays", "Dialogs, sheets and the things that stack over the page.") { OverlayDemo() },
    CatalogEntry("Menus and dialogs", "Menus, selects, dialogs and sheets over documents.") { DocumentsScreen() },
    CatalogEntry("Chips, badges, avatars", "Small data display: tokens, counts and people.") { DataDisplayDemo() },
    CatalogEntry("Banners and snackbars", "Messages: progress, snackbars and banners in an inbox.") { InboxScreen() },
    CatalogEntry("Tabs and FAB", "Tabs and the floating action button.") { NavigationDemo() },
    CatalogEntry("Navigation", "A bar, a rail and a drawer, by window size.") { NavigationShellDemo() },
    CatalogEntry("Popover and side sheet", "Panels anchored to a control or to an edge.") { PanelsDemo() },
    CatalogEntry("Toolbars", "Toolbars, button groups and a split button in an editor.") { EditorDemo() },
    CatalogEntry("Rating and steps", "Stars, page controls, a path and progress steps.") { SmallDataDemo() },
    CatalogEntry("Table and tree", "A data table and an outline tree.") { ListsDemo() },
    CatalogEntry("File upload", "A folder, an uploader and a review.") { AdminDemo() },
    CatalogEntry("Charts", "A dashboard of line, bar and donut charts.") { ChartsDemo() },
)

/**
 * The sample app, laid out like a component catalog: a home of component cards, a page per component (a title,
 * a line about it and its demo), and a theme page for the skin, the brand and the dark mode, switched live at
 * the provider. The top bar is drawn over the page, so under the Cupertino skins it is glass. [backHandler] is the
 * platform's back button (Android's), wired by the host.
 */
@Composable
fun SampleApp(backHandler: @Composable (enabled: Boolean, onBack: () -> Unit) -> Unit = { _, _ -> }) {
    val systemDark = isSystemInDarkTheme()
    var dark by remember { mutableStateOf(systemDark) }
    var choice by remember { mutableStateOf(SampleSkin.Minimal) }
    var brand by remember { mutableStateOf(Brand.Default) }
    var open by remember { mutableStateOf<CatalogEntry?>(null) }
    var themePage by remember { mutableStateOf(false) }
    var barHeight by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    // The system back returns to the home, like the bar's back arrow.
    backHandler(themePage || open != null) {
        themePage = false
        open = null
    }
    val title = when {
        themePage -> "Theme"
        open != null -> open!!.title
        else -> "Camouflage"
    }
    CamouflageProvider(skin = choice.skin, theme = animateCamoTheme(sampleTheme(choice, brand, dark))) {
        // The sources Fluent Acrylic and Cupertino's materials blur; with other skins they only record.
        FluentBackdrop(Modifier.fillMaxSize()) {
            CupertinoBackdrop(
                Modifier.fillMaxSize(),
                overlay = {
                    CamoTopBar(
                        title,
                        Modifier
                            .align(Alignment.TopCenter)
                            .onSizeChanged { barHeight = with(density) { it.height.toDp() } },
                        navigation = if (themePage || open != null) {
                            { CamoIconButton({ AppIcon.Back() }, "Back", { themePage = false; open = null }) }
                        } else {
                            null
                        },
                        actions = {
                            if (!themePage) {
                                CamoIconButton({ AppIcon.Palette() }, "Theme", { open = null; themePage = true })
                            }
                        },
                    )
                },
            ) {
                CamoSurface(Modifier.fillMaxSize()) {
                    Column(
                        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = Camouflage.spacing.l),
                        verticalArrangement = Arrangement.spacedBy(Camouflage.spacing.l),
                    ) {
                        Spacer(Modifier.height(barHeight))
                        when {
                            themePage -> ThemePage(choice, { choice = it }, brand, { brand = it }, dark) { dark = it }
                            open != null -> ComponentPage(open!!, choice, brand, dark)
                            else -> Home { open = it }
                        }
                        Spacer(Modifier.height(Camouflage.spacing.xl))
                    }
                }
            }
        }
    }
}

// The home: the component cards, two to a row.
@Composable
private fun Home(onOpen: (CatalogEntry) -> Unit) {
    CamoText("Components", style = Camouflage.typography.titleMedium)
    Entries.chunked(2).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(Camouflage.spacing.m)) {
            row.forEach { entry ->
                CamoCard(Modifier.weight(1f), onClick = { onOpen(entry) }) {
                    Column(
                        Modifier.fillMaxWidth().height(CardHeight).padding(Camouflage.spacing.m),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        AppIcon.Star(Modifier.size(CardIcon))
                        CamoText(entry.title, style = Camouflage.typography.labelLarge)
                    }
                }
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

private val CardHeight = 112.dp
private val CardIcon = 28.dp

// A component's page: what it is, then its demo (the promo nested provider is part of the checkout's page).
@Composable
private fun ComponentPage(entry: CatalogEntry, choice: SampleSkin, brand: Brand, dark: Boolean) {
    CamoText("Description", style = Camouflage.typography.titleMedium)
    CamoText(entry.description, color = Camouflage.colors.onSurfaceVariant)
    CamoText("Examples", style = Camouflage.typography.titleMedium)
    Section(entry.title) { entry.content() }
    if (entry.title == "Checkout") {
        val promo = sampleTheme(choice, brand, dark).copy(colors = CamoColors.fromSeed(PromoSeed, dark = dark))
        CamouflageProvider(theme = promo) {
            Section("Nested provider: a promo theme") {
                CamoButton("Claim offer", {}, suffixIcon = { AppIcon.ChevronRight() })
            }
        }
    }
}

// The theme page, like the catalog's: the skin, the brand and light or dark.
@Composable
private fun ThemePage(
    choice: SampleSkin,
    onChoice: (SampleSkin) -> Unit,
    brand: Brand,
    onBrand: (Brand) -> Unit,
    dark: Boolean,
    onDark: (Boolean) -> Unit,
) {
    CamoText("Skin", style = Camouflage.typography.titleMedium)
    Switcher(SampleSkin.entries, choice, { it.name }, onChoice)
    CamoText("Brand", style = Camouflage.typography.titleMedium)
    Switcher(Brand.entries, brand, { it.label }, onBrand)
    CamoText("Appearance", style = Camouflage.typography.titleMedium)
    Switcher(listOf(false, true), dark, { if (it) "Dark" else "Light" }, onDark)
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
