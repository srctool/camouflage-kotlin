package com.srctool.camouflage.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.srctool.camouflage.core.avatar.AvatarSize
import com.srctool.camouflage.core.avatar.CamoAvatar
import com.srctool.camouflage.core.avatar.CamoAvatarGroup
import com.srctool.camouflage.core.avatar.Presence
import com.srctool.camouflage.core.badge.CamoBadge
import com.srctool.camouflage.core.badge.CamoBadgedBox
import com.srctool.camouflage.core.chip.CamoChip
import com.srctool.camouflage.core.chip.CamoChipGroup
import com.srctool.camouflage.core.chip.ChipLayout
import com.srctool.camouflage.core.chip.ChipOption
import com.srctool.camouflage.core.iconbutton.CamoIconButton
import com.srctool.camouflage.core.list.CamoListItem
import com.srctool.camouflage.core.list.CamoListSection
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.text.CamoText
import com.srctool.camouflage.core.theme.Appearance
import com.srctool.camouflage.core.theme.Tone

private val Filters = listOf("Open now", "Top rated", "Free delivery", "Vegetarian", "Under $10")
private val Sorts = listOf("Relevance", "Distance", "Rating", "Price", "Newest")

private data class Contact(val name: String, val presence: Presence?, val photo: Brush? = null)

// A "photo": a gradient stands in for an image the app would load (Coil's AsyncImage, say).
private val Contacts = listOf(
    Contact("Ada Lovelace", Presence.Online, Brush.linearGradient(listOf(Color(0xFFE6A15C), Color(0xFF8E3B2E)))),
    Contact("Grace Hopper", Presence.Busy),
    Contact("Alan Turing", Presence.Away),
    Contact("Barbara Liskov", null, Brush.linearGradient(listOf(Color(0xFF5C8DE6), Color(0xFF2E3F8E)))),
    Contact("Edsger Dijkstra", Presence.Offline),
)

/** Chips, badges and avatars: filters, a sort row, recipients, suggestions, status tags, badges and contacts. */
@Composable
internal fun DataDisplayDemo() {
    val spacing = Arrangement.spacedBy(Camouflage.spacing.s)
    Column(verticalArrangement = Arrangement.spacedBy(Camouflage.spacing.m)) {
        var filters by remember { mutableStateOf(setOf("Open now")) }
        CamoText("Filters (several)", style = Camouflage.typography.labelLarge)
        CamoChipGroup(Filters.map { ChipOption(it, it) }, filters, { filters = it })

        var sort by remember { mutableStateOf(setOf("Relevance")) }
        CamoText("Sort by (one, scrolls)", style = Camouflage.typography.labelLarge)
        CamoChipGroup(Sorts.map { ChipOption(it, it) }, sort, { sort = it }, multiSelect = false,
            layout = ChipLayout.Scroll, required = true)

        var recipients by remember { mutableStateOf(listOf("Ada Lovelace", "Grace Hopper", "Alan Turing")) }
        CamoText("To", style = Camouflage.typography.labelLarge)
        FlowRow(horizontalArrangement = spacing, verticalArrangement = spacing) {
            recipients.forEach { name ->
                CamoChip(name, onRemove = { recipients = recipients - name },
                    leading = { CamoAvatar(name = name, size = AvatarSize.Small) })
            }
        }

        Row(horizontalArrangement = spacing, verticalAlignment = Alignment.CenterVertically) {
            CamoChip("Add to calendar", onClick = {}, leading = { AppIcon.Star() })
            CamoChip("Share", onClick = {}, leading = { AppIcon.Share() })
        }

        FlowRow(horizontalArrangement = spacing, verticalArrangement = spacing) {
            CamoChip("Paid", appearance = Appearance.Soft, tone = Tone.Success)
            CamoChip("Pending", appearance = Appearance.Soft, tone = Tone.Warning)
            CamoChip("Overdue", appearance = Appearance.Soft, tone = Tone.Error)
            CamoChip("Draft")
        }

        var unread by remember { mutableStateOf(3) }
        Row(
            horizontalArrangement = Arrangement.spacedBy(Camouflage.spacing.l),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CamoBadgedBox(badge = { CamoBadge(count = unread, contentDescription = "$unread unread") }) {
                CamoIconButton({ AppIcon.Star() }, "Notifications", { unread = 0 })
            }
            CamoBadgedBox(badge = { CamoBadge(count = 128) }) { CamoIconButton({ AppIcon.Share() }, "Shared", {}) }
            CamoBadgedBox(badge = { CamoBadge() }) { CamoAvatar(name = "Grace Hopper") }
            CamoAvatarGroup(Contacts.map { c -> { CamoAvatar(name = c.name, image = c.photo?.let { photo(it) }) } })
        }

        CamoListSection(title = "Contacts") {
            Contacts.forEach { c ->
                CamoListItem(
                    c.name,
                    supporting = c.presence?.name ?: "No status",
                    // The row says the name; the avatar in its leading slot stays decorative.
                    leading = { CamoAvatar(name = c.name, image = c.photo?.let { photo(it) }, presence = c.presence) },
                    onClick = {},
                )
            }
        }
    }
}

private fun photo(brush: Brush): @Composable () -> Unit = { Box(Modifier.fillMaxSize().background(brush)) }
