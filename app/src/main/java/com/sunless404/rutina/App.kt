package com.sunless404.rutina

import android.app.Application
import com.sunless404.rutina.data.AppDatabase
import com.sunless404.rutina.data.Prefs
import com.sunless404.rutina.data.Repository
import com.sunless404.rutina.data.SampleRoutine
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

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        repo = Repository(AppDatabase.create(this))
        scope.launch {
            if (!prefs.seeded) {
                repo.replaceTemplate(SampleRoutine.blocks())
                prefs.seeded = true
            }
            repo.ensureMaterialized()
        }
    }
}
