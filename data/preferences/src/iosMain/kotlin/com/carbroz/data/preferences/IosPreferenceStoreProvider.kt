package com.carbroz.data.preferences

import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferencesSerializer
import kotlinx.cinterop.ExperimentalForeignApi
import okio.FileSystem
import okio.Path.Companion.toPath
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/** iOS Preferences DataStore backed by the application's Application Support directory. */
class IosPreferenceStoreProvider : PreferenceStoreProvider {
    private val store: PreferenceStore by lazy { createStore() }

    override fun get(): PreferenceStore = store

    @OptIn(ExperimentalForeignApi::class)
    private fun createStore(): PreferenceStore {
        val supportDirectory = NSFileManager.defaultManager.URLForDirectory(
            directory = NSApplicationSupportDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null,
        ) ?: error("Application Support directory is unavailable")
        val fileUrl = supportDirectory.URLByAppendingPathComponent(PREFERENCES_FILE_NAME)
            ?: error("Preferences URL could not be resolved")
        val filePath = fileUrl.path ?: error("Preferences path is unavailable")

        return DataStorePreferenceStore(
            createPreferencesDataStore(
                storage = OkioStorage(
                    fileSystem = FileSystem.SYSTEM,
                    serializer = PreferencesSerializer,
                    producePath = { filePath.toPath() },
                ),
            ),
        )
    }
}
