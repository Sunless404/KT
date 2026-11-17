package com.sunless404.keeptrack

import android.app.Application
import com.sunless404.keeptrack.ai.SecretStore
import com.sunless404.keeptrack.cloud.Cloud
import com.sunless404.keeptrack.data.AppDatabase
import com.sunless404.keeptrack.data.Prefs
import com.sunless404.keeptrack.data.ProfileStore
import com.sunless404.keeptrack.data.Repository
import com.sunless404.keeptrack.data.SampleRoutine
import com.sunless404.keeptrack.notify.Alarms
import com.sunless404.keeptrack.update.Updater
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
    lateinit var profile: ProfileStore
        private set
    lateinit var updater: Updater
        private set

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        repo = Repository(AppDatabase.create(this))
        alarms = Alarms(this)
        secrets = SecretStore(prefs.sp)
        profile = ProfileStore(this, prefs)
        updater = Updater(this)
        cloud = Cloud(this)
        profile.onChanged = { cloud.pushProfile(it) }
        repo.onChanged = {
            scope.launch { alarms.sync() }
            cloud.scheduleUpload()
            scheduleMilestones()
        }
        scope.launch {
            if (!prefs.seeded) {
                repo.replaceTemplate(SampleRoutine.blocks())
                prefs.seeded = true
            }
            alarms.sync()
            repo.evaluateMilestones()
        }
    }

    private var milestonesJob: kotlinx.coroutines.Job? = null

    /** Rachas y logros se revisan poco después de cada cambio (agrupando cambios seguidos). */
    private fun scheduleMilestones() {
        milestonesJob?.cancel()
        milestonesJob = scope.launch {
            kotlinx.coroutines.delay(1500)
            runCatching { repo.evaluateMilestones() }
        }
    }
}
