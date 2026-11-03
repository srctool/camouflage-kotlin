package com.srctool.camouflage.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.breadcrumb.CamoBreadcrumb
import com.srctool.camouflage.core.breadcrumb.CamoBreadcrumbItem
import com.srctool.camouflage.core.button.CamoButton
import com.srctool.camouflage.core.carousel.CamoCarousel
import com.srctool.camouflage.core.datatable.CamoColumn
import com.srctool.camouflage.core.datatable.CamoDataTable
import com.srctool.camouflage.core.datatable.ColumnAlign
import com.srctool.camouflage.core.datatable.ColumnWidth
import com.srctool.camouflage.core.datatable.TableSort
import com.srctool.camouflage.core.fileuploader.CamoFileUploader
import com.srctool.camouflage.core.fileuploader.UploadFile
import com.srctool.camouflage.core.fileuploader.UploadStatus
import com.srctool.camouflage.core.pagination.CamoPagination
import com.srctool.camouflage.core.progresssteps.CamoProgressSteps
import com.srctool.camouflage.core.progresssteps.CamoStep
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.text.CamoText
import com.srctool.camouflage.core.theme.Appearance
import com.srctool.camouflage.core.tree.CamoTree
import com.srctool.camouflage.core.tree.CamoTreeNode

// The M10 milestone screen: an admin tool that uploads files in three steps: choose a folder in a tree (with its
// path as breadcrumbs), add files to upload (the uploader, with fake progress), then review them in a sortable,
// paged table next to a carousel of previews.

private val Steps = listOf("Folder", "Upload", "Review")
private val TreeHeight = 180.dp
private val TableHeight = 250.dp
private val SlideHeight = 100.dp
private val AgeWidth = 96.dp
private const val PAGE_SIZE = 4
private const val KILOBYTE = 1024L
private const val SCAN_KB = 300
private const val SAMPLE_KB = 100
private const val PARTIAL = 0.6f
private val Slides = listOf(Color(0xFF6D8FB5), Color(0xFFB58F6D), Color(0xFF8FB56D))
private val Folders = listOf(
    CamoTreeNode("docs", "Documents", listOf(CamoTreeNode("reports", "Reports"), CamoTreeNode("invoices", "Invoices"))),
    CamoTreeNode("media", "Media", listOf(CamoTreeNode("photos", "Photos"))),
)
private val Paths = mapOf(
    "docs" to listOf("Home", "Documents"), "reports" to listOf("Home", "Documents", "Reports"),
    "invoices" to listOf("Home", "Documents", "Invoices"), "media" to listOf("Home", "Media"),
    "photos" to listOf("Home", "Media", "Photos"),
)

@Composable
internal fun AdminDemo() {
    var step by remember { mutableIntStateOf(0) }
    var folder by remember { mutableStateOf("docs") }
    var expanded by remember { mutableStateOf(setOf<Any>("docs")) }
    var files by remember { mutableStateOf(listOf<UploadFile>()) }
    Column(verticalArrangement = Arrangement.spacedBy(Camouflage.spacing.m)) {
        CamoProgressSteps(
            Steps.mapIndexed { i, name -> CamoStep(name, name, onClick = if (i < step) ({ step = i }) else null) },
            current = step,
            modifier = Modifier.fillMaxWidth(),
        )
        when (step) {
            0 -> FolderStep(folder, expanded, { expanded = it }, { folder = it })
            1 -> UploadStep(files, { files = it })
            else -> ReviewStep(files)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Camouflage.spacing.s)) {
            CamoButton("Back", { step-- }, appearance = Appearance.Outline, enabled = step > 0)
            CamoButton("Next", { step++ }, enabled = step < Steps.lastIndex)
        }
    }
}

@Composable
private fun FolderStep(folder: String, expanded: Set<Any>, setExpanded: (Set<Any>) -> Unit,
    setFolder: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Camouflage.spacing.s)) {
        CamoBreadcrumb(Paths.getValue(folder).map { CamoBreadcrumbItem(it, it, onClick = {}) })
        CamoTree(
            Folders, expanded, setExpanded, setOf(folder), { setFolder(it.first() as String) }, { it }, "Folders",
            Modifier.height(TreeHeight),
        )
    }
}

@Composable
private fun UploadStep(files: List<UploadFile>, setFiles: (List<UploadFile>) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Camouflage.spacing.s)) {
        CamoFileUploader(
            files,
            onPick = {
                val n = files.size + 1
                setFiles(files + UploadFile(n, "scan-$n.pdf", n * SCAN_KB * KILOBYTE,
                    progress = if (n % 2 == 0) PARTIAL else 1f,
                    status = if (n % 2 == 0) UploadStatus.Uploading else UploadStatus.Done))
            },
            onRemove = { gone -> setFiles(files - gone) },
            label = "Add files",
            hint = "Clicking adds a sample file; the app runs the real upload",
        )
    }
}

private data class Entry(val id: Int, val name: String, val kb: Int)

@Composable
private fun ReviewStep(files: List<UploadFile>) {
    var sort by remember { mutableStateOf(TableSort("name")) }
    var page by remember { mutableIntStateOf(0) }
    val entries = (files.ifEmpty { List(9) { UploadFile(it, "sample-${it + 1}.png",
        (it + 1) * SAMPLE_KB * KILOBYTE) } })
        .mapIndexed { i, f -> Entry(i, f.name, ((f.sizeBytes ?: 0) / KILOBYTE).toInt()) }
    val sorted = entries.sortedWith { a, b ->
        val order = if (sort.columnKey == "kb") a.kb.compareTo(b.kb) else a.name.compareTo(b.name)
        if (sort.ascending) order else -order
    }
    val pages = (sorted.size + PAGE_SIZE - 1) / PAGE_SIZE
    val shown = sorted.drop(page * PAGE_SIZE).take(PAGE_SIZE)
    Column(verticalArrangement = Arrangement.spacedBy(Camouflage.spacing.s)) {
        CamoDataTable(
            listOf(
                CamoColumn<Entry>("name", "File", sortable = true, cell = { CamoText(it.name) }),
                CamoColumn<Entry>("kb", "Size (KB)", ColumnWidth.Fixed(AgeWidth), ColumnAlign.End, true,
                    { CamoText("${it.kb}") }),
            ),
            shown, { it.id }, "Uploaded files", Modifier.height(TableHeight),
            sort = sort, onSortChange = { sort = it; page = 0 },
        )
        CamoPagination(pages, page, { page = it })
        CamoCarousel(Slides, { it.value }, "Previews", Modifier.fillMaxWidth()) { color ->
            Box(Modifier.fillMaxWidth().height(SlideHeight).background(color)) { CamoText("Preview") }
        }
    }
}
