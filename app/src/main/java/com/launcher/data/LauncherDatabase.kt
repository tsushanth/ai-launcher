package com.launcher.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.launcher.data.models.Converters
import com.launcher.data.models.HomeScreenLayout

@Database(
    entities = [HomeScreenLayout::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class LauncherDatabase : RoomDatabase() {
    abstract fun homeScreenDao(): HomeScreenDao

    companion object {
        @Volatile
        private var INSTANCE: LauncherDatabase? = null

        fun getDatabase(context: Context): LauncherDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LauncherDatabase::class.java,
                    "launcher_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
