package com.srctool.camouflage.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.srctool.camouflage.core.avatar.CamoAvatar
import com.srctool.camouflage.core.avatar.Presence
import com.srctool.camouflage.core.badge.CamoBadge
import com.srctool.camouflage.core.badge.CamoBadgedBox
import com.srctool.camouflage.core.banner.CamoBanner
import com.srctool.camouflage.core.button.CamoButton
import com.srctool.camouflage.core.chip.CamoChip
import com.srctool.camouflage.core.chip.CamoChipGroup
import com.srctool.camouflage.core.chip.ChipLayout
import com.srctool.camouflage.core.chip.ChipOption
import com.srctool.camouflage.core.iconbutton.CamoIconButton
import com.srctool.camouflage.core.list.CamoListItem
import com.srctool.camouflage.core.list.CamoListSection
import com.srctool.camouflage.core.progress.CamoProgressBar
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.snackbar.SnackbarResult
import com.srctool.camouflage.core.snackbar.messenger
import com.srctool.camouflage.core.text.CamoText
import com.srctool.camouflage.core.theme.Appearance
import com.srctool.camouflage.core.theme.Tone
import com.srctool.camouflage.core.topbar.CamoTopBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// The M7 milestone screen: status everywhere. Tags, badges, progress, snackbars with Undo and banners by tone.

private data class Mail(val from: String, val subject: String, val tag: String?, val tone: Tone, val unread: Boolean)

private val InitialMail = listOf(
    Mail("Ada Lovelace", "Notes on the analytical engine", null, Tone.Default, unread = true),
    Mail("Billing", "Invoice #1042", "Overdue", Tone.Error, unread = true),
    Mail("Grace Hopper", "Compiler review on Friday", "Meeting", Tone.Info, unread = false),
    Mail("Billing", "Invoice #1039", "Paid", Tone.Success, unread = false),
    Mail("Alan Turing", "Re: the imitation game", null, Tone.Default, unread = true),
)

private val Presences = mapOf("Ada Lovelace" to Presence.Online, "Grace Hopper" to Presence.Busy)

private enum class Sync { Failed, Syncing, Done }

private const val SYNC_MILLIS = 1500L
private const val UPLOAD_STEP_MILLIS = 120L
private const val UPLOAD_STEP = 0.04f
private const val FAILS_AT = 0.6f

/** An inbox: a badged top bar, sync and trial banners, filters, mail with tags, Archive with Undo, uploads. */
@Composable
internal fun InboxScreen() {
    val mail = remember { mutableStateListOf(*InitialMail.toTypedArray()) }
    var sync by remember { mutableStateOf(Sync.Failed) }
    var trial by remember { mutableStateOf(true) }
    var filter by remember { mutableStateOf(setOf("All")) }
    val messenger = Camouflage.messenger
    val scope = rememberCoroutineScope()
    val unread = mail.count { it.unread }

    fun archive(item: Mail) = scope.launch {
        val index = mail.indexOf(item)
        mail.remove(item)
        if (messenger.show("Message archived", action = "Undo") == SnackbarResult.ActionPerformed) {
            mail.add(index.coerceAtMost(mail.size), item)
        }
    }

    LaunchedEffect(sync) {
        if (sync == Sync.Syncing) {
            delay(SYNC_MILLIS)
            sync = Sync.Done
            messenger.show("Inbox synced", tone = Tone.Success)
        }
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Camouflage.spacing.m)) {
        CamoTopBar(
            "Inbox",
            actions = {
                CamoBadgedBox(badge = { CamoBadge(count = unread, contentDescription = "$unread unread") }) {
                    CamoIconButton({ AppIcon.Notifications() }, "Notifications", {
                        mail.indices.forEach { mail[it] = mail[it].copy(unread = false) }
                    })
                }
            },
            windowInsets = WindowInsets(0),
        )
        Banners(sync, trial, onRetry = { sync = Sync.Syncing }, onDismissTrial = { trial = false })
        CamoChipGroup(
            listOf("All", "Unread", "Tagged").map { ChipOption(it, it) },
            filter,
            { filter = it },
            multiSelect = false,
            layout = ChipLayout.Scroll,
            required = true,
        )
        val shown = when {
            "Unread" in filter -> mail.filter { it.unread }
            "Tagged" in filter -> mail.filter { it.tag != null }
            else -> mail
        }
        MailList(shown, ::archive)
        Uploads()
        CamoButton(
            "Send 3 notifications",
            {
                listOf("Ada replied", "Grace mentioned you", "Alan shared a file").forEach { text ->
                    scope.launch { messenger.show(text) }
                }
            },
            appearance = Appearance.Outline,
        )
    }
}

// The sync state (a bar while syncing, a warning with Retry after a failure) and the trial banner.
@Composable
private fun Banners(sync: Sync, trial: Boolean, onRetry: () -> Unit, onDismissTrial: () -> Unit) {
    if (sync == Sync.Syncing) CamoProgressBar(Modifier.fillMaxWidth(), label = "Syncing")
    if (sync == Sync.Failed) {
        CamoBanner(
            "2 messages couldn't be downloaded.",
            title = "Couldn't sync",
            tone = Tone.Warning,
            actions = { CamoButton("Retry", onRetry, appearance = Appearance.Ghost) },
        )
    }
    if (trial) {
        CamoBanner(
            "Your trial ends in 3 days.",
            actions = { CamoButton("Upgrade", {}, appearance = Appearance.Ghost) },
            onDismiss = onDismissTrial,
        )
    }
}

@Composable
private fun MailList(mail: List<Mail>, onArchive: (Mail) -> Unit) {
    CamoListSection(title = "Messages") {
        mail.forEach { item ->
            CamoListItem(
                item.from,
                supporting = item.subject,
                leading = { CamoAvatar(name = item.from, presence = Presences[item.from]) },
                trailing = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        item.tag?.let { CamoChip(it, appearance = Appearance.Soft, tone = item.tone) }
                        CamoIconButton({ AppIcon.Archive() }, "Archive", { onArchive(item) })
                    }
                },
                onClick = {},
            )
        }
    }
}

// Two attachments uploading; the second fails part way, with a Retry snackbar.
@Composable
private fun Uploads() {
    val messenger = Camouflage.messenger
    var photo by remember { mutableStateOf(0f) }
    var report by remember { mutableStateOf(0f) }
    var failed by remember { mutableStateOf(false) }
    var attempt by remember { mutableStateOf(0) }
    LaunchedEffect(attempt) {
        failed = false
        report = 0f
        // The first attempt stops part way and fails; a retry finishes.
        val stop = if (attempt == 0) FAILS_AT else 1f
        while (photo < 1f || report < stop) {
            delay(UPLOAD_STEP_MILLIS)
            photo = (photo + UPLOAD_STEP).coerceAtMost(1f)
            report = (report + UPLOAD_STEP).coerceAtMost(stop)
        }
        if (attempt == 0) {
            failed = true
            val result = messenger.show("Couldn't upload report.pdf", action = "Retry", tone = Tone.Error)
            if (result == SnackbarResult.ActionPerformed) attempt++
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(Camouflage.spacing.s)) {
        CamoText("Uploads", style = Camouflage.typography.titleSmall)
        Upload("photo.jpg", photo, Tone.Default)
        Upload(if (failed) "report.pdf · failed" else "report.pdf", report, if (failed) Tone.Error else Tone.Default)
    }
}

@Composable
private fun Upload(name: String, progress: Float, tone: Tone) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Camouflage.spacing.s),
    ) {
        AppIcon.Upload()
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Camouflage.spacing.xs)) {
            CamoText(name)
            CamoProgressBar(Modifier.fillMaxWidth(), progress = progress, tone = tone, label = name)
        }
    }
}
