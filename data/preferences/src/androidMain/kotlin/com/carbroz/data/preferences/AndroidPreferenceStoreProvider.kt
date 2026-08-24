package com.carbroz.data.preferences

import android.content.Context
import androidx.datastore.core.FileStorage
import androidx.datastore.preferences.core.PreferencesFileSerializer

/** Android Preferences DataStore backed by the application's private files directory. */
class AndroidPreferenceStoreProvider(
    context: Context,
) : PreferenceStoreProvider {
    private val applicationContext = context.applicationContext
    private val store: PreferenceStore by lazy {
        DataStorePreferenceStore(
            createPreferencesDataStore(
                storage = FileStorage(
                    serializer = PreferencesFileSerializer,
                    produceFile = { applicationContext.filesDir.resolve(PREFERENCES_FILE_NAME) },
                ),
            ),
        )
    }

    override fun get(): PreferenceStore = store
}
