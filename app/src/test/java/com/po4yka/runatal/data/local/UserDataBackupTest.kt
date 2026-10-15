package com.po4yka.runatal.data.local

import android.app.Application
import android.app.backup.BackupAgent
import android.app.backup.BackupAgentHelper
import android.content.Context
import android.os.Build
import android.os.ParcelFileDescriptor
import com.google.common.truth.Truth.assertThat
import com.po4yka.runatal.R
import com.po4yka.runatal.data.local.entity.QuoteEntity
import com.po4yka.runatal.data.preferences.UserPreferencesManager
import com.po4yka.runatal.di.DataStoreModule
import com.po4yka.runatal.di.DatabaseModule
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode
import org.robolectric.util.ReflectionHelpers
import org.w3c.dom.Element

/** Checks system restore eligibility using the production database, preferences and XML rules. */
@RunWith(RobolectricTestRunner::class)
// A separate instrumentation sandbox models a fresh process for the production DataStore singleton.
@Config(
    application = CredentialStorageApplication::class,
    sdk = [30, 34],
    instrumentedPackages = ["com.po4yka.runatal.di", "com.po4yka.runatal.data.preferences"]
)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class UserDataBackupTest {

    @Before
    fun resetFrameworkProcessCache() {
        val context = RuntimeEnvironment.getApplication()
        val manifest = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            .newDocumentBuilder().parse(File("src/main/AndroidManifest.xml"))
        val application = manifest.getElementsByTagName("application").item(0) as Element
        val namespace = "http://schemas.android.com/apk/res/android"
        assertThat(application.getAttributeNS(namespace, "fullBackupContent")).isEqualTo("@xml/backup_rules")
        if (Build.VERSION.SDK_INT >= 31) {
            assertThat(application.getAttributeNS(namespace, "dataExtractionRules"))
                .isEqualTo("@xml/data_extraction_rules")
            // Robolectric omits this manifest field. Populate verified metadata, retaining the real XML parser.
            val packageInfo = shadowOf(context.packageManager).getInternalMutablePackageInfo(context.packageName)
            ReflectionHelpers.setField(
                packageInfo.applicationInfo, "dataExtractionRulesRes", R.xml.data_extraction_rules
            )
            ReflectionHelpers.setField(context.applicationInfo, "dataExtractionRulesRes", R.xml.data_extraction_rules)
        }
        // Each test has a new app directory, but FullBackup retains canonical paths by package name.
        ReflectionHelpers.getStaticField<MutableMap<*, *>>(
            Class.forName("android.app.backup.FullBackup"),
            "kPackageBackupSchemeMap"
        ).clear()
    }

    @Test
    fun `system restore retains user quotes favorites and DataStore preferences`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val quote = QuoteEntity(
            textLatin = "A user quote survives restore",
            author = "User",
            isUserCreated = true,
            isFavorite = true
        )
        val originalDatabase = DatabaseModule.provideRunatalDatabase(context)
        val quoteId = try {
            originalDatabase.quoteDao().insert(quote)
        } finally {
            originalDatabase.close()
        }
        UserPreferencesManager(DataStoreModule.provideDataStore(context)).updateSelectedFont("babelstone")

        val databaseFile = context.getDatabasePath("runic_quotes.db")
        val preferencesFile = context.filesDir.walkTopDown().single {
            it.isFile && it.name.endsWith(".preferences_pb")
        }
        val preferencesBytes = preferencesFile.readBytes()
        val agent = Robolectric.setupBackupAgent(BackupAgentHelper::class.java)
        assertThat(context.credentialStorageContext().filesDir.canonicalFile)
            .isEqualTo(context.filesDir.canonicalFile)
        assertThat(agent.filesDir).isEqualTo(context.filesDir)
        assertThat(ReflectionHelpers.getField<Int>(agent.applicationInfo, "fullBackupContent"))
            .isEqualTo(R.xml.backup_rules)
        if (Build.VERSION.SDK_INT >= 31) {
            assertThat(ReflectionHelpers.getField<Int>(agent.applicationInfo, "dataExtractionRulesRes"))
                .isEqualTo(R.xml.data_extraction_rules)
        }
        restoreFile(agent, databaseFile)
        assertThat(databaseFile.exists()).isTrue()
        restoreFile(agent, preferencesFile)

        assertThat(preferencesFile.readBytes()).isEqualTo(preferencesBytes)
        val restoredDatabase = DatabaseModule.provideRunatalDatabase(context)
        try {
            assertThat(restoredDatabase.quoteDao().getById(quoteId)).isEqualTo(quote.copy(id = quoteId))
        } finally {
            restoredDatabase.close()
        }
    }

    @Test
    fun `system restore excludes unrelated files from cloud backup`() {
        val context = RuntimeEnvironment.getApplication()
        val unrelated = File(context.filesDir, "widget-render-cache.bin").apply { writeText("Cached rendering") }

        restoreFile(Robolectric.setupBackupAgent(BackupAgentHelper::class.java), unrelated)

        assertThat(unrelated.exists()).isFalse()
    }

    private fun restoreFile(agent: BackupAgent, destination: File) {
        val context = RuntimeEnvironment.getApplication()
        val snapshot = File.createTempFile("backup-", ".bin", context.cacheDir)
        destination.copyTo(snapshot, overwrite = true)
        check(destination.delete())
        try {
            ParcelFileDescriptor.open(snapshot, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                // The system's domain/path restore entry point canonicalizes its destination before this overload.
                agent.onRestoreFile(
                    descriptor, snapshot.length(), destination.canonicalFile, BackupAgent.TYPE_FILE, 384L, 0L
                )
            }
        } finally {
            snapshot.delete()
        }
    }
}

/** Models Android's default credential-protected app storage in Robolectric's separate directories. */
class CredentialStorageApplication : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base.credentialStorageContext())
    }
}

// FullBackup uses this hidden framework method; the public SDK omits it from Context's stubs.
private fun Context.credentialStorageContext(): Context = ReflectionHelpers.callInstanceMethod(
    this,
    "createCredentialProtectedStorageContext"
)
