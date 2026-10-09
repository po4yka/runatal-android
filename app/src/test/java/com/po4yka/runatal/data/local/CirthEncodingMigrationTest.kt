package com.po4yka.runatal.data.local

import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import com.google.common.truth.Truth.assertThat
import com.po4yka.runatal.data.local.migration.CirthEncodingMigration
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class CirthEncodingMigrationTest {
    @Test
    fun `repair changes only proven glyph layers and keeps identities metadata and manual values`() {
        AndroidSQLiteDriver().open(":memory:").use { connection ->
            connection.execSQL("PRAGMA foreign_keys=ON")
            connection.execSQL("CREATE TABLE quotes (id INTEGER PRIMARY KEY,textLatin TEXT,author TEXT," +
                "runicCirth TEXT,isFavorite INTEGER)")
            connection.execSQL("CREATE TABLE translation_records (id INTEGER PRIMARY KEY,quoteId INTEGER," +
                "sourceText TEXT,script TEXT,engineVersion TEXT,glyphOutput TEXT,tokenBreakdownJson TEXT," +
                "normalizedForm TEXT,diplomaticForm TEXT,provenanceJson TEXT,notesJson TEXT," +
                "FOREIGN KEY(quoteId) REFERENCES quotes(id))")
            connection.execSQL("CREATE TABLE rune_references (id INTEGER PRIMARY KEY,character TEXT,name TEXT," +
                "pronunciation TEXT,meaning TEXT,history TEXT,script TEXT)")
            connection.execSQL("INSERT INTO quotes VALUES(71,'test','Author','\uE088\uE0C9\uE09C\uE088',1)")
            connection.execSQL("INSERT INTO quotes VALUES(72,'test','Author','manual \uE088',1)")
            connection.execSQL("INSERT INTO quotes VALUES(73,'test','Author',NULL,1)")
            val tokens = """[{"sourceToken":"test","normalizedToken":"test","diplomaticToken":"t·e·s·t",""" +
                """"glyphToken":"\uE088\uE0C9\uE09C\uE088","customField":"preserve"}]"""
            connection.prepare("INSERT INTO translation_records VALUES(90,71,'test','CIRTH'," +
                "'cirth-translation-v4',?,?, 'test','t·e·s·t','keep provenance','keep notes')").use {
                it.bindText(1, "\uE088\uE0C9\uE09C\uE088")
                it.bindText(2, tokens)
                it.step()
            }
            connection.execSQL("INSERT INTO translation_records VALUES(91,72,'test','CIRTH'," +
                "'manual-engine','manual \uE088','[]','test','test','manual provenance','manual notes')")
            connection.execSQL("INSERT INTO rune_references VALUES(51,'\uE088','Certh 9','t'," +
                "'old meaning','old history','cirth')")
            connection.execSQL("INSERT INTO rune_references VALUES(52,'\uE088','My custom rune','custom'," +
                "'custom meaning','custom history','cirth')")
            connection.execSQL("INSERT INTO quotes VALUES(74,'literal \uE088','Author','literal \uE088',1)")
            connection.execSQL("INSERT INTO translation_records VALUES(92,72,'test','CIRTH'," +
                "'cirth-translation-v4','\uE088\uE0C9\uE09C\uE088'," +
                "'[{\"normalizedToken\":\"test\",\"glyphToken\":\"manual\"}]'," +
                "'test','test','custom','custom')")
            CirthEncodingMigration.migrate(connection)
            CirthEncodingMigration.migrate(connection)
            connection.prepare("SELECT id,runicCirth,isFavorite FROM quotes ORDER BY id").use {
                assertThat(it.step()).isTrue()
                assertThat(it.getLong(0)).isEqualTo(71L)
                assertThat(it.getText(1)).isEqualTo("\uE087\uE0AF\uE0A1\uE087")
                assertThat(it.getLong(2)).isEqualTo(1L)
                assertThat(it.step()).isTrue()
                assertThat(it.getText(1)).isEqualTo("manual \uE088")
                assertThat(it.step()).isTrue()
                assertThat(it.isNull(1)).isTrue()
            }
            connection.prepare("SELECT id,quoteId,glyphOutput,tokenBreakdownJson,normalizedForm," +
                "diplomaticForm,provenanceJson,notesJson FROM translation_records WHERE id=90").use {
                assertThat(it.step()).isTrue()
                assertThat(it.getLong(0)).isEqualTo(90L)
                assertThat(it.getLong(1)).isEqualTo(71L)
                assertThat(it.getText(2)).isEqualTo("\uE087\uE0AF\uE0A1\uE087")
                assertThat(it.getText(3)).contains("customField")
                assertThat(it.getText(3)).contains("preserve")
                assertThat(it.getText(3)).contains("\uE087\uE0AF\uE0A1\uE087")
                assertThat(it.getText(4)).isEqualTo("test")
                assertThat(it.getText(5)).isEqualTo("t·e·s·t")
                assertThat(it.getText(6)).isEqualTo("keep provenance")
                assertThat(it.getText(7)).isEqualTo("keep notes")
            }
            connection.prepare("SELECT glyphOutput FROM translation_records WHERE id=91").use {
                assertThat(it.step()).isTrue()
                assertThat(it.getText(0)).isEqualTo("manual \uE088")
            }
            connection.prepare("SELECT glyphOutput FROM translation_records WHERE id=92").use {
                assertThat(it.step()).isTrue()
                assertThat(it.getText(0)).isEqualTo("\uE088\uE0C9\uE09C\uE088")
            }
            connection.prepare("SELECT runicCirth FROM quotes WHERE id=74").use {
                assertThat(it.step()).isTrue()
                assertThat(it.getText(0)).isEqualTo("literal \uE088")
            }
            connection.prepare("SELECT character,name FROM rune_references WHERE id=51").use {
                assertThat(it.step()).isTrue()
                assertThat(it.getText(0)).isEqualTo("\uE087")
                assertThat(it.getText(1)).isEqualTo("Cirth T")
            }
            connection.prepare("SELECT name FROM rune_references WHERE id=52").use {
                assertThat(it.step()).isTrue()
                assertThat(it.getText(0)).isEqualTo("My custom rune")
            }
            connection.prepare("PRAGMA foreign_key_check").use { assertThat(it.step()).isFalse() }
        }
    }
}
