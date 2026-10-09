package com.po4yka.runatal.data.local

import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import com.google.common.truth.Truth.assertThat
import com.po4yka.runatal.data.local.migration.ElderFutharkSequenceMigration
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import android.app.Application
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class ElderFutharkSequenceMigrationTest {
    @Test
    fun `recognizes only old direct x q renderings and preserves every other field and historical FK record`() {
        AndroidSQLiteDriver().open(":memory:").use { connection ->
            connection.execSQL("PRAGMA foreign_keys=ON")
            connection.execSQL("CREATE TABLE quotes (id INTEGER PRIMARY KEY,textLatin TEXT,runicElder TEXT," +
                "author TEXT,runicYounger TEXT,runicCirth TEXT,isFavorite INTEGER,isUserCreated INTEGER," +
                "createdAt INTEGER,canonicalKey TEXT)")
            connection.execSQL("CREATE TABLE translation_records (id INTEGER PRIMARY KEY,quoteId INTEGER," +
                "glyphOutput TEXT,engineVersion TEXT,FOREIGN KEY(quoteId) REFERENCES quotes(id) ON DELETE CASCADE)")
            val source = "x q qu X Q QU axe queen thing nxg tqh"
            val old = "ᚲ ᚲ ᚲᚢ ᚲ ᚲ ᚲᚢ ᚨᚲᛖ ᚲᚢᛖᛖᚾ ᚦᛁᛜ ᚾᚲᚷ ᛏᚲᚻ"
            val current = "ᚲᛊ ᚲᚹ ᚲᚹ ᚲᛊ ᚲᚹ ᚲᚹ ᚨᚲᛊᛖ ᚲᚹᛖᛖᚾ ᚦᛁᛜ ᚾᚲᛊᚷ ᛏᚲᚹᚻ"
            connection.prepare("INSERT INTO quotes VALUES (41,?,?,'Author','keep younger','keep cirth',1,1,123,NULL)")
                .use { statement ->
                    statement.bindText(1, source)
                    statement.bindText(2, old)
                    statement.step()
                }
            connection.prepare("INSERT INTO translation_records VALUES(700,41,?,'ef-translation-v6')").use {
                it.bindText(1, old)
                it.step()
            }
            connection.execSQL("INSERT INTO quotes VALUES(42,'Q','ᚲ','Seed','Y','C',0,0,124,'canonical')")
            connection.execSQL("INSERT INTO quotes VALUES(43,'queen','manual ᚲ','Manual','Y','C',1,1,125,NULL)")
            connection.execSQL("INSERT INTO quotes VALUES(44,'axe',NULL,'Null','Y','C',1,1,126,NULL)")
            connection.execSQL("INSERT INTO quotes VALUES(45,'queen','ᚲᚹᛖᛖᚾ','Current','Y','C',1,1,127,NULL)")
            connection.execSQL("INSERT INTO quotes VALUES(46,'axe','ᚲᚢᛖᛖᚾ','Mismatch','Y','C',1,1,128,NULL)")
            connection.execSQL("INSERT INTO quotes VALUES(47,'thing','ᚦᛁᛜ','Unaffected','Y','C',1,1,129,NULL)")
            ElderFutharkSequenceMigration.migrate(connection)
            ElderFutharkSequenceMigration.migrate(connection)
            connection.prepare("SELECT * FROM quotes WHERE id=41").use {
                assertThat(it.step()).isTrue()
                assertThat(it.getLong(0)).isEqualTo(41L)
                assertThat(it.getText(1)).isEqualTo(source)
                assertThat(it.getText(2)).isEqualTo(current)
                assertThat(it.getText(3)).isEqualTo("Author")
                assertThat(it.getText(4)).isEqualTo("keep younger")
                assertThat(it.getText(5)).isEqualTo("keep cirth")
                assertThat(it.getLong(6)).isEqualTo(1L)
                assertThat(it.getLong(7)).isEqualTo(1L)
                assertThat(it.getLong(8)).isEqualTo(123L)
                assertThat(it.isNull(9)).isTrue()
            }
            connection.prepare("SELECT id,runicElder,canonicalKey FROM quotes WHERE id>41 ORDER BY id").use {
                assertThat(it.step()).isTrue()
                assertThat(it.getLong(0)).isEqualTo(42L)
                assertThat(it.getText(1)).isEqualTo("ᚲᚹ")
                assertThat(it.getText(2)).isEqualTo("canonical")
                assertThat(it.step()).isTrue()
                assertThat(it.getText(1)).isEqualTo("manual ᚲ")
                assertThat(it.step()).isTrue()
                assertThat(it.isNull(1)).isTrue()
                assertThat(it.step()).isTrue()
                assertThat(it.getText(1)).isEqualTo("ᚲᚹᛖᛖᚾ")
                assertThat(it.step()).isTrue()
                assertThat(it.getText(1)).isEqualTo("ᚲᚢᛖᛖᚾ")
                assertThat(it.step()).isTrue()
                assertThat(it.getText(1)).isEqualTo("ᚦᛁᛜ")
                assertThat(it.step()).isFalse()
            }
            connection.prepare("SELECT id,quoteId,glyphOutput,engineVersion FROM translation_records").use {
                assertThat(it.step()).isTrue()
                assertThat(it.getLong(0)).isEqualTo(700L)
                assertThat(it.getLong(1)).isEqualTo(41L)
                assertThat(it.getText(2)).isEqualTo(old)
                assertThat(it.getText(3)).isEqualTo("ef-translation-v6")
                assertThat(it.step()).isFalse()
            }
            connection.prepare("PRAGMA foreign_key_check").use { assertThat(it.step()).isFalse() }
        }
    }
}
