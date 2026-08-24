package com.carbroz.data.preferences

import androidx.datastore.core.FileStorage
import androidx.datastore.preferences.core.PreferencesFileSerializer
import java.io.File

/** Desktop Preferences DataStore backed by an application-owned persistent directory. */
class DesktopPreferenceStoreProvider(
    storageDirectory: File,
) : PreferenceStoreProvider {
    private val directory = storageDirectory.absoluteFile
    private val store: PreferenceStore by lazy {
        require(directory.exists() || directory.mkdirs()) {
            "Unable to create preferences directory: ${directory.path}"
        }
        DataStorePreferenceStore(
            createPreferencesDataStore(
                storage = FileStorage(
                    serializer = PreferencesFileSerializer,
                    produceFile = { directory.resolve(PREFERENCES_FILE_NAME) },
                ),
            ),
        )
    }

    override fun get(): PreferenceStore = store
}
