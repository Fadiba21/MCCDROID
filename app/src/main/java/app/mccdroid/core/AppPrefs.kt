package app.mccdroid.core

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import app.mccdroid.logic.CommandCatalog
import app.mccdroid.logic.Json
import app.mccdroid.logic.asArr

/** Pengaturan aplikasi. Properti berbasis state Compose sehingga UI otomatis ikut berubah. */
object AppPrefs {
    private lateinit var sp: SharedPreferences

    private val _theme = mutableStateOf("dark")
    private val _dynamic = mutableStateOf(false)
    private val _accent = mutableStateOf(0)
    private val _fontSp = mutableStateOf(12f)
    private val _maxLines = mutableStateOf(3000)
    private val _keepOn = mutableStateOf(false)
    private val _wake = mutableStateOf(true)
    private val _wifi = mutableStateOf(true)
    private val _boot = mutableStateOf(false)
    private val _ts = mutableStateOf(false)
    private val _wrap = mutableStateOf(true)
    private val _heap = mutableStateOf(0)
    private val _saveLog = mutableStateOf(false)
    private val _favs = mutableStateOf(CommandCatalog.defaultFavorites)

    fun init(c: Context) {
        sp = c.getSharedPreferences("mcc_prefs", Context.MODE_PRIVATE)
        _theme.value = sp.getString("theme", "dark") ?: "dark"
        _dynamic.value = sp.getBoolean("dynamic", false)
        _accent.value = sp.getInt("accent", 0)
        _fontSp.value = sp.getFloat("fontSp", 12f)
        _maxLines.value = sp.getInt("maxLines", 3000)
        _keepOn.value = sp.getBoolean("keepOn", false)
        _wake.value = sp.getBoolean("wake", true)
        _wifi.value = sp.getBoolean("wifi", true)
        _boot.value = sp.getBoolean("boot", false)
        _ts.value = sp.getBoolean("ts", false)
        _wrap.value = sp.getBoolean("wrap", true)
        _heap.value = sp.getInt("heap", 0)
        _saveLog.value = sp.getBoolean("saveLog", false)
        val fav = Json.parseOrNull(sp.getString("favs", "") ?: "").asArr().mapNotNull { it as? String }
        if (fav.isNotEmpty()) _favs.value = fav
    }

    /** "system", "dark", "light", atau "amoled". */
    var themeMode: String
        get() = _theme.value
        set(v) { _theme.value = v; sp.edit().putString("theme", v).apply() }

    var dynamicColor: Boolean
        get() = _dynamic.value
        set(v) { _dynamic.value = v; sp.edit().putBoolean("dynamic", v).apply() }

    var accentIdx: Int
        get() = _accent.value
        set(v) { _accent.value = v; sp.edit().putInt("accent", v).apply() }

    var consoleFontSp: Float
        get() = _fontSp.value
        set(v) { _fontSp.value = v; sp.edit().putFloat("fontSp", v).apply() }

    var maxLogLines: Int
        get() = _maxLines.value
        set(v) { _maxLines.value = v; sp.edit().putInt("maxLines", v).apply() }

    var keepScreenOn: Boolean
        get() = _keepOn.value
        set(v) { _keepOn.value = v; sp.edit().putBoolean("keepOn", v).apply() }

    var wakeLock: Boolean
        get() = _wake.value
        set(v) { _wake.value = v; sp.edit().putBoolean("wake", v).apply() }

    var wifiLock: Boolean
        get() = _wifi.value
        set(v) { _wifi.value = v; sp.edit().putBoolean("wifi", v).apply() }

    var bootStart: Boolean
        get() = _boot.value
        set(v) { _boot.value = v; sp.edit().putBoolean("boot", v).apply() }

    var showTimestamps: Boolean
        get() = _ts.value
        set(v) { _ts.value = v; sp.edit().putBoolean("ts", v).apply() }

    var wrapLines: Boolean
        get() = _wrap.value
        set(v) { _wrap.value = v; sp.edit().putBoolean("wrap", v).apply() }

    /** Batas heap .NET dalam MB; 0 = tanpa batas. */
    var heapLimitMb: Int
        get() = _heap.value
        set(v) { _heap.value = v; sp.edit().putInt("heap", v).apply() }

    var saveConsoleLog: Boolean
        get() = _saveLog.value
        set(v) { _saveLog.value = v; sp.edit().putBoolean("saveLog", v).apply() }

    var favorites: List<String>
        get() = _favs.value
        set(v) { _favs.value = v; sp.edit().putString("favs", Json.stringify(v)).apply() }
}
