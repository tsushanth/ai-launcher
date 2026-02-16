package com.launcher.data

import androidx.room.*
import com.launcher.data.models.HomeScreenLayout
import kotlinx.coroutines.flow.Flow

@Dao
interface HomeScreenDao {
    @Query("SELECT * FROM home_screen_layouts WHERE id = 1")
    fun getLayout(): Flow<HomeScreenLayout?>

    @Query("SELECT * FROM home_screen_layouts WHERE id = 1")
    suspend fun getLayoutOnce(): HomeScreenLayout?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLayout(layout: HomeScreenLayout)

    @Update
    suspend fun updateLayout(layout: HomeScreenLayout)

    @Query("DELETE FROM home_screen_layouts")
    suspend fun deleteAll()
}
