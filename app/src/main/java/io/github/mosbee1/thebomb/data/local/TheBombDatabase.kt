package io.github.mosbee1.thebomb.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [ScreenshotEntity::class, AppSettingsEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class TheBombDatabase : RoomDatabase() {

    abstract fun screenshotDao(): ScreenshotDao

    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        private const val DB_NAME = "thebomb.db"

        @Volatile
        private var instance: TheBombDatabase? = null

        fun get(context: Context): TheBombDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    TheBombDatabase::class.java,
                    DB_NAME,
                ).build().also { instance = it }
            }
    }
}
