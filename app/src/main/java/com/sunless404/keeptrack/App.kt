package com.sunless404.keeptrack

import android.app.Application
import com.sunless404.keeptrack.ai.SecretStore
import com.sunless404.keeptrack.cloud.Cloud
import com.sunless404.keeptrack.data.AppDatabase
import com.sunless404.keeptrack.data.Prefs
import com.sunless404.keeptrack.data.Repository
import com.sunless404.keeptrack.data.SampleRoutine
import com.sunless404.keeptrack.notify.Alarms
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class App : Application() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    lateinit var prefs: Prefs
        private set
    lateinit var repo: Repository
        private set
    lateinit var alarms: Alarms
        private set
    lateinit var secrets: SecretStore
        private set
    lateinit var cloud: Cloud
        private set

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        repo = Repository(AppDatabase.create(this))
        alarms = Alarms(this)
        secrets = SecretStore(prefs.sp)
        cloud = Cloud(this)
        repo.onChanged = {
            scope.launch { alarms.sync() }
            cloud.scheduleUpload()
        }
        scope.launch {
            if (!prefs.seeded) {
                repo.replaceTemplate(SampleRoutine.blocks())
                prefs.seeded = true
            }
            alarms.sync()
        }
    }
}
