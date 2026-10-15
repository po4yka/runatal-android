package com.po4yka.runatal.data.local

import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import com.google.common.truth.Truth.assertThat
import com.po4yka.runatal.data.local.migration.QuoteRenderingSelectionMigration
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import android.app.Application
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class QuoteRenderingSelectionMigrationTest {
    @Test
    fun `only unambiguous manual exact source records infer historical intent and manual Younger variant`() {
        AndroidSQLiteDriver().open(":memory:").use { connection ->
            connection.execSQL("PRAGMA foreign_keys=ON")
            connection.execSQL("CREATE TABLE quotes(id INTEGER PRIMARY KEY,textLatin TEXT,isFavorite INTEGER)")
            connection.execSQL("CREATE TABLE translation_records(id INTEGER PRIMARY KEY,quoteId INTEGER," +
                "sourceText TEXT,script TEXT,fidelity TEXT,variant TEXT,isBackfilled INTEGER,resolutionStatus TEXT," +
                "glyphOutput TEXT,FOREIGN KEY(quoteId) REFERENCES quotes(id))")
            (1..8).forEach { connection.execSQL("INSERT INTO quotes VALUES($it,'source',1)") }
            connection.execSQL("INSERT INTO translation_records VALUES" +
                "(1,1,'source','YOUNGER_FUTHARK','READABLE','SHORT_TWIG',0,'RECONSTRUCTED','manual')," +
                "(2,1,'source','ELDER_FUTHARK','READABLE','',0,'APPROXIMATED','manual elder')," +
                "(3,2,'source','YOUNGER_FUTHARK','STRICT','SHORT_TWIG',1,'RECONSTRUCTED','auto')," +
                "(4,3,'changed source','YOUNGER_FUTHARK','READABLE','SHORT_TWIG',0,'RECONSTRUCTED','stale')," +
                "(5,4,'source','ELDER_FUTHARK','STRICT','',0,'RECONSTRUCTED','manual strict')," +
                "(6,4,'source','ELDER_FUTHARK','READABLE','',0,'RECONSTRUCTED','newer manual readable')," +
                "(7,5,'source','YOUNGER_FUTHARK','STRICT','LONG_BRANCH',0,'RECONSTRUCTED','manual long')," +
                "(8,5,'source','YOUNGER_FUTHARK','STRICT','SHORT_TWIG',0,'RECONSTRUCTED','newer manual short')," +
                "(9,6,'source','CIRTH','DECORATIVE','',0,'APPROXIMATED','manual cirth')," +
                "(10,7,'source','YOUNGER_FUTHARK','STRICT','invalid',0,'RECONSTRUCTED','invalid variant')," +
                "(11,8,'source','ELDER_FUTHARK','STRICT','',0,'UNAVAILABLE','')")
            QuoteRenderingSelectionMigration.migrate(connection)
            connection.prepare("SELECT id,renderingMode,renderingFidelity,renderingYoungerVariant,isFavorite " +
                "FROM quotes ORDER BY id").use { statement ->
                while (statement.step()) {
                    val id = statement.getLong(0)
                    if (id == 1L || id == 6L) {
                        assertThat(statement.getText(1)).isEqualTo("TRANSLATE")
                        assertThat(statement.getText(2)).isEqualTo(if (id == 1L) "READABLE" else "DECORATIVE")
                        assertThat(statement.getText(3)).isEqualTo(if (id == 1L) "SHORT_TWIG" else "LONG_BRANCH")
                    } else {
                        assertThat(statement.getText(1)).isEqualTo("TRANSLITERATE")
                        assertThat(statement.getText(2)).isEqualTo("STRICT")
                        assertThat(statement.getText(3)).isEqualTo("LONG_BRANCH")
                    }
                    assertThat(statement.getLong(4)).isEqualTo(1L)
                }
            }
            connection.prepare("SELECT COUNT(*) FROM translation_records").use {
                assertThat(it.step()).isTrue()
                assertThat(it.getLong(0)).isEqualTo(11L)
            }
            connection.prepare("SELECT sourceText,glyphOutput FROM translation_records WHERE id=4").use {
                assertThat(it.step()).isTrue()
                assertThat(it.getText(0)).isEqualTo("changed source")
                assertThat(it.getText(1)).isEqualTo("stale")
            }
            connection.prepare("PRAGMA foreign_key_check").use { assertThat(it.step()).isFalse() }
        }
    }
}
