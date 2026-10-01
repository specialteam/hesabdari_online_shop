package ir.hesabdari.shop

import android.app.Application
import ir.hesabdari.shop.data.AppDatabase
import ir.hesabdari.shop.data.BackupManager
import ir.hesabdari.shop.data.DealRepository
import ir.hesabdari.shop.data.SettingsStore

class App : Application() {
    val database: AppDatabase by lazy { AppDatabase.build(this) }
    val repository: DealRepository by lazy { DealRepository(database) }
    val settings: SettingsStore by lazy { SettingsStore(this) }
    val backup: BackupManager by lazy { BackupManager(this, repository, settings) }
}
