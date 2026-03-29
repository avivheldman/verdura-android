package com.verdura.app

import android.app.Application
import com.verdura.app.data.AppDatabase
import com.verdura.app.repository.FirebasePostRepository
import com.verdura.app.util.AndroidNetworkChecker
import com.verdura.app.util.ApiConfig
import com.verdura.app.util.OfflineSyncManager
import com.verdura.app.util.PicassoConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class VerduraApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var offlineSyncManager: OfflineSyncManager
        private set

    override fun onCreate() {
        super.onCreate()

        PicassoConfig.initialize(this)
        ApiConfig.init(this)

        val db = AppDatabase.getInstance(this)
        val networkChecker = AndroidNetworkChecker(this)

        offlineSyncManager = OfflineSyncManager(
            pendingOperationDao = db.pendingOperationDao(),
            firebaseRepository = FirebasePostRepository(),
            networkChecker = networkChecker
        )

        applicationScope.launch {
            networkChecker.observeNetworkStatus().collect { isOnline ->
                if (isOnline) {
                    offlineSyncManager.syncPendingOperations()
                }
            }
        }
    }
}
