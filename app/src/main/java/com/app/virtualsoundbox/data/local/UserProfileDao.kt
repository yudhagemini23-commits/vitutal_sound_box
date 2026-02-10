package com.app.virtualsoundbox.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.app.virtualsoundbox.model.UserProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: UserProfile)

    @Query("SELECT * FROM user_profile WHERE uid = :uid")
    fun getUserProfile(uid: String): Flow<UserProfile?>

    // Nanti dipakai buat cari data yang belum di-sync ke server
    @Query("SELECT * FROM user_profile WHERE isSynced = 0")
    suspend fun getUnsyncedProfiles(): List<UserProfile>
}