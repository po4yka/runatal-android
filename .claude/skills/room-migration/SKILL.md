---
name: room-migration
description: "Guide for creating safe Room database migrations in the Runic Quotes app. Use when: (1) adding/modifying database tables or columns, (2) creating new entities, (3) writing migration code, (4) updating the database version, (5) adding new DAOs. Triggers on: migration, database change, new table, new column, Room entity, schema change, DAO."
---

# Room Migration

## Migration Checklist

1. Create/modify the entity in `data/local/entity/`
2. Add DAO interface in `data/local/dao/` (if new entity)
3. Write migration SQL in `RunicQuotesDatabase.companion`
4. Bump database version in `@Database(version = N+1)`
5. Register entity in `@Database(entities = [...])`
6. Register migration in `DatabaseModule.addMigrations(...)`
7. Expose DAO via abstract function in `RunicQuotesDatabase`
8. Provide DAO in `DatabaseModule`
9. Export schema: `./gradlew kspDebugKotlin`
10. Verify: `./gradlew testDebugUnitTest`

## Current State

- **Database version**: 4
- **Database file**: `runic_quotes.db`
- **Schema export**: `app/schemas/` (JSON, checked into git)
- **Entities**: QuoteEntity, QuotePackEntity, PackQuoteEntity, ArchivedQuoteEntity, RuneReferenceEntity

## File Locations

| What | Path |
|------|------|
| Database class | `data/local/RunicQuotesDatabase.kt` |
| Entities | `data/local/entity/*.kt` |
| DAOs | `data/local/dao/*.kt` |
| DI module | `di/DatabaseModule.kt` |
| Schemas | `app/schemas/` |

All source paths relative to `app/src/main/java/com/po4yka/runicquotes/`.

## Migration Pattern

Follow existing style in `RunicQuotesDatabase.companion`:

```kotlin
val MIGRATION_N_N1 = object : Migration(N, N + 1) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // ALTER TABLE for column additions
        db.execSQL("ALTER TABLE table_name ADD COLUMN colName TYPE NOT NULL DEFAULT value")
        // CREATE TABLE for new tables
        db.execSQL("""CREATE TABLE IF NOT EXISTS `new_table` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `name` TEXT NOT NULL
        )""".trimIndent())
        // CREATE INDEX for query performance
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_table_col` ON `table_name` (`col`)")
    }
}
```

## Entity Pattern

```kotlin
@Entity(
    tableName = "table_name",
    indices = [Index(value = ["columnName"])]
)
data class NewEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = 0L
)
```

For foreign keys, use `foreignKeys` parameter with `onDelete = ForeignKey.CASCADE`.

## Registration Steps

In `RunicQuotesDatabase.kt`:
- Add entity to `@Database(entities = [..., NewEntity::class])`
- Add `abstract fun newEntityDao(): NewEntityDao`
- Add migration to companion object

In `DatabaseModule.kt`:
- Add migration: `.addMigrations(..., RunicQuotesDatabase.MIGRATION_N_N1)`
- Add DAO provider: `@Provides @Singleton fun provideNewEntityDao(db): NewEntityDao = db.newEntityDao()`

## Destructive Migrations

Never use `fallbackToDestructiveMigration()`. Always write explicit migrations to preserve user data. SQLite only supports `ALTER TABLE ADD COLUMN` -- for column removal or type changes, recreate the table with a temp copy.
