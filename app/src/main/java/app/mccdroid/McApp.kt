package app.mccdroid

import android.app.Application
import app.mccdroid.core.AppPrefs
import app.mccdroid.core.Notifier
import app.mccdroid.core.ProfileStore
import app.mccdroid.core.RuleStore
import app.mccdroid.core.NotifyStore
import app.mccdroid.core.RuntimeInstaller
import app.mccdroid.core.SessionManager

class McApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppPrefs.init(this)
        ProfileStore.init(this)
        RuleStore.init(this)
        NotifyStore.init(this)
        Notifier.init(this)
        SessionManager.init(this)
        RuntimeInstaller.start(this)
    }
}
