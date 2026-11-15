package ua.diary.voice.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Topic::class, DiaryEntry::class, VoiceNote::class, DutyUnit::class, DutyPosition::class],
    version = 2,
    exportSchema = false,
)
abstract class DiaryDatabase : RoomDatabase() {
    abstract fun dao(): DiaryDao

    companion object {
        /** Підрозділи, які створюються при першому запуску. Їх можна перейменувати або видалити в додатку. */
        val DEFAULT_UNITS = listOf(
            "Взвод управління",
            "Взвод БпАК розвідки",
            "Взвод НРК",
            "Взвод важких БпЛА «Вампір»",
            "Група забезпечення",
        )

        private fun createDutyTables(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `duty_units` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`name` TEXT NOT NULL, `sortOrder` INTEGER NOT NULL)"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `duty_positions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`unitId` INTEGER NOT NULL, `name` TEXT NOT NULL, `sortOrder` INTEGER NOT NULL, " +
                    "`docTitle` TEXT, `docFileName` TEXT, `docMime` TEXT)"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_duty_positions_unitId` ON `duty_positions` (`unitId`)")
        }

        private fun seedUnits(db: SupportSQLiteDatabase) {
            DEFAULT_UNITS.forEachIndexed { i, name ->
                db.execSQL("INSERT INTO duty_units (name, sortOrder) VALUES (?, ?)", arrayOf<Any>(name, i))
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                createDutyTables(db)
                seedUnits(db)
            }
        }

        @Volatile private var instance: DiaryDatabase? = null

        fun get(context: Context): DiaryDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    DiaryDatabase::class.java,
                    "diary.db",
                )
                    .addMigrations(MIGRATION_1_2)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) = seedUnits(db)
                    })
                    .build().also { instance = it }
            }
    }
}
