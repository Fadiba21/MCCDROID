@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
)

package app.mccdroid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.mccdroid.core.AppPrefs
import app.mccdroid.core.Paths
import app.mccdroid.core.ProfileStore
import app.mccdroid.core.SState
import app.mccdroid.core.SessionManager
import app.mccdroid.core.ShellSession
import app.mccdroid.logic.CommandCatalog
import app.mccdroid.logic.LineKind
import app.mccdroid.logic.LogBuffer
import app.mccdroid.logic.LogLine
import app.mccdroid.logic.McText
import app.mccdroid.ui.Nav
import app.mccdroid.ui.components.DeviceCodeCard
import app.mccdroid.ui.components.EmptyState
import app.mccdroid.ui.components.ScreenHeader
import app.mccdroid.ui.components.StateChip
import app.mccdroid.ui.components.copyToClipboard
import app.mccdroid.ui.components.openUrl
import app.mccdroid.ui.components.toast
import app.mccdroid.ui.isDarkTheme
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class Mode { MCC, SHELL }

/** Warna gelap di latar gelap dibuat lebih terang agar terbaca. */
private fun readable(rgb: Int, dark: Boolean): Color {
    val r = (rgb shr 16) and 0xFF
    val g = (rgb shr 8) and 0xFF
    val b = rgb and 0xFF
    val lum = (0.299 * r + 0.587 * g + 0.114 * b)
    return if (dark && lum < 70) Color(
        minOf(255, r + 90), minOf(255, g + 90), minOf(255, b + 90),
    ) else if (!dark && lum > 190) Color(r * 6 / 10, g * 6 / 10, b * 6 / 10) else Color(r, g, b)
}

private val TIME_FMT = SimpleDateFormat("HH:mm:ss", Locale.US)

private fun render(line: LogLine, dark: Boolean, timestamps: Boolean): AnnotatedString {
    val base = MaterialTheme_onSurfaceFallback(dark)
    return buildAnnotatedString {
        if (timestamps) {
            withStyle(SpanStyle(color = base.copy(alpha = 0.45f))) { append(TIME_FMT.format(Date(line.ts)) + " ") }
        }
        when (line.kind) {
            LineKind.IN -> withStyle(SpanStyle(color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)) { append("› " + line.clean) }
            LineKind.SYS -> withStyle(SpanStyle(color = Color(0xFFFBBF24), fontStyle = FontStyle.Italic)) { append(line.clean) }
            LineKind.ERR -> withStyle(SpanStyle(color = Color(0xFFF87171))) { append(line.clean) }
            LineKind.OUT -> {
                for (s in line.spans) {
                    val deco = when {
                        s.underline && s.strike -> TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))
                        s.underline -> TextDecoration.Underline
                        s.strike -> TextDecoration.LineThrough
                        else -> null
                    }
                    withStyle(
                        SpanStyle(
                            color = s.rgb?.let { readable(it, dark) } ?: base,
                            fontWeight = if (s.bold) FontWeight.Bold else null,
                            fontStyle = if (s.italic) FontStyle.Italic else null,
                            textDecoration = deco,
                        ),
                    ) { append(s.text) }
                }
            }
        }
    }
}

private fun MaterialTheme_onSurfaceFallback(dark: Boolean): Color = if (dark) Color(0xFFD7E2DB) else Color(0xFF1B2620)

@Composable
fun ConsoleScreen(nav: Nav) {
    val ctx = LocalContext.current
    val profiles by ProfileStore.profiles.collectAsState()
    var mode by remember { mutableStateOf(Mode.MCC) }
    val profile = nav.pick(profiles)
    var menu by remember { mutableStateOf(false) }
    var catalog by remember { mutableStateOf(false) }
    var pickerOpen by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            "Terminal",
            if (mode == Mode.MCC) profile?.name ?: "Belum ada profil" else "Shell Android (sh)",
            actions = {
                if (mode == Mode.MCC && profile != null) {
                    val s = SessionManager.session(profile.id)
                    val st by s.state.collectAsState()
                    StateChip(st)
                    val alive = st != SState.STOPPED && st != SState.CRASHED
                    IconButton(onClick = {
                        if (alive) SessionManager.stop(profile.id) else SessionManager.start(profile.id)?.let { toast(ctx, it) }
                    }) { Icon(if (alive) Icons.Rounded.Stop else Icons.Rounded.PlayArrow, if (alive) "Hentikan" else "Mulai") }
                }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.Menu, "Menu") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Katalog perintah") }, onClick = { menu = false; catalog = true })
                        DropdownMenuItem(text = { Text("Ganti profil") }, onClick = { menu = false; pickerOpen = true })
                    }
                }
            },
        )
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = mode == Mode.MCC, onClick = { mode = Mode.MCC }, label = { Text("MCC") })
            FilterChip(selected = mode == Mode.SHELL, onClick = { mode = Mode.SHELL }, label = { Text("Shell") })
        }
        Spacer(Modifier.padding(top = 6.dp))

        if (mode == Mode.MCC) {
            if (profile == null) {
                EmptyState(Icons.Rounded.Terminal, "Belum ada profil", "Buat profil di tab Beranda terlebih dahulu.")
            } else {
                val s = SessionManager.session(profile.id)
                val dc by s.deviceCode.collectAsState()
                dc?.let { Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) { DeviceCodeCard(it) } }
                TerminalPane(
                    log = s.log,
                    placeholder = "Perintah MCC, mis. /health atau teks chat",
                    enabled = s.isAlive,
                    suggestions = true,
                    onSend = { s.send(it) },
                    logName = profile.name,
                    logDir = Paths.profileDir(ctx, profile.id),
                    onClear = { s.log.clear() },
                )
            }
        } else {
            val cwd = remember { Paths.dataRoot(ctx) }
            LaunchedEffect(Unit) { ShellSession.ensure(ctx, cwd) }
            TerminalPane(
                log = ShellSession.log,
                placeholder = "Perintah shell, mis. ls -la",
                enabled = true,
                suggestions = false,
                onSend = { ShellSession.run(ctx, cwd, it) },
                logName = "shell",
                logDir = Paths.homeDir(ctx),
                onClear = { ShellSession.log.clear() },
            )
        }
    }

    if (catalog) CatalogDialog(onDismiss = { catalog = false }) { cmd ->
        catalog = false
        if (profile != null) SessionManager.session(profile.id).send(cmd)
    }
    if (pickerOpen) AlertDialog(
        onDismissRequest = { pickerOpen = false },
        title = { Text("Pilih profil") },
        text = {
            Column {
                for (p in profiles) {
                    TextButton(onClick = { nav.profileId = p.id; pickerOpen = false }) { Text(p.name) }
                }
            }
        },
        confirmButton = { TextButton(onClick = { pickerOpen = false }) { Text("Tutup") } },
    )
}

@Composable
private fun CatalogDialog(onDismiss: () -> Unit, onPick: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Katalog perintah MCC") },
        text = {
            LazyColumn(Modifier.fillMaxWidth()) {
                items(CommandCatalog.all) { c ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onPick("/" + c.name) }
                            .padding(vertical = 6.dp),
                    ) {
                        Text("/" + c.usage, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("${c.group} · ${c.desc}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Tutup") } },
    )
}

@Composable
private fun TerminalPane(
    log: LogBuffer,
    placeholder: String,
    enabled: Boolean,
    suggestions: Boolean,
    onSend: (String) -> Unit,
    logName: String,
    logDir: File,
    onClear: () -> Unit,
) {
    val ctx = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val lines by log.flow.collectAsState()
    val dark = isDarkTheme()
    val listState = rememberLazyListState()
    var follow by remember { mutableStateOf(true) }
    var input by remember { mutableStateOf("") }
    val history = remember { mutableStateListOf<String>() }
    var histIdx by remember { mutableStateOf(-1) }
    val fontSp = AppPrefs.consoleFontSp
    val wrap = AppPrefs.wrapLines

    // Berhenti mengikuti bila pengguna menggulir ke atas.
    val atBottom by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            last >= info.totalItemsCount - 2
        }
    }
    LaunchedEffect(atBottom) { if (atBottom) follow = true else if (listState.isScrollInProgress) follow = false }
    LaunchedEffect(lines.size, follow) {
        if (follow && lines.isNotEmpty()) listState.scrollToItem(lines.lastIndex)
    }

    fun submit(text: String) {
        val t = text.trim()
        if (t.isEmpty()) return
        onSend(t)
        if (history.lastOrNull() != t) history.add(t)
        if (history.size > 100) history.removeAt(0)
        histIdx = -1
        input = ""
        follow = true
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (dark) Color(0xFF080D0B) else Color(0xFFEFF4F1)),
        ) {
            if (lines.isEmpty()) {
                Text(
                    "Belum ada keluaran. Mulai profil dari tab Beranda atau tombol ▶ di atas.",
                    Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().let { if (wrap) it else it.horizontalScroll(rememberScrollState()) },
                contentPadding = PaddingValues(8.dp),
            ) {
                items(lines, key = { it.id }) { l ->
                    val text = render(l, dark, AppPrefs.showTimestamps)
                    val urls = remember(l.id) { McText.findUrls(l.clean) }
                    Text(
                        text,
                        fontFamily = FontFamily.Monospace,
                        fontSize = fontSp.sp,
                        lineHeight = (fontSp * 1.35f).sp,
                        softWrap = wrap,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (urls.isNotEmpty()) openUrl(ctx, l.clean.substring(urls[0].first, urls[0].last + 1))
                                else copyToClipboard(ctx, "Baris", l.clean)
                            },
                    )
                }
            }
            Row(Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                IconButton(onClick = { copyToClipboard(ctx, "Log", lines.joinToString("\n") { it.clean }) }) {
                    Icon(Icons.Rounded.ContentCopy, "Salin semua", Modifier.size(18.dp))
                }
                IconButton(onClick = {
                    try {
                        val f = File(logDir, "console-" + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date()) + ".txt")
                        f.writeText(lines.joinToString("\n") { it.clean })
                        toast(ctx, "Disimpan: ${f.name} (folder profil $logName)")
                    } catch (e: Exception) {
                        toast(ctx, "Gagal menyimpan: ${e.message}")
                    }
                }) { Icon(Icons.Rounded.Save, "Simpan", Modifier.size(18.dp)) }
                IconButton(onClick = onClear) { Icon(Icons.Rounded.DeleteSweep, "Bersihkan", Modifier.size(18.dp)) }
            }
            if (!follow) {
                FloatingActionButton(
                    onClick = { follow = true; scope.launch { if (lines.isNotEmpty()) listState.scrollToItem(lines.lastIndex) } },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp).size(44.dp),
                ) { Icon(Icons.Rounded.ArrowDownward, "Ke bawah") }
            }
        }

        if (suggestions) {
            val sug = CommandCatalog.suggest(input)
            if (sug.isNotEmpty()) {
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (c in sug) SuggestionChip(onClick = { input = "/" + c.name + " " }, label = { Text("/" + c.name) })
                }
            } else if (input.isEmpty()) {
                val favs = AppPrefs.favorites
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (f in favs) AssistChip(onClick = { submit(f) }, label = { Text(f) }, enabled = enabled)
                }
            }
        }

        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                if (history.isNotEmpty()) {
                    histIdx = if (histIdx < 0) history.lastIndex else maxOf(0, histIdx - 1)
                    input = history[histIdx]
                }
            }) { Icon(Icons.Rounded.ArrowUpward, "Riwayat") }
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text(placeholder, maxLines = 1) },
                singleLine = true,
                enabled = enabled,
                shape = RoundedCornerShape(16.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { submit(input) }),
            )
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = { submit(input) }, enabled = enabled && input.isNotBlank()) { Icon(Icons.Rounded.Send, "Kirim") }
        }
    }
}
