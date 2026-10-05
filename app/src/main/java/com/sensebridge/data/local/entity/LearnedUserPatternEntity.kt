package com.sensebridge.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "learned_user_patterns",
    indices = [Index(value = ["userPhrase"], unique = true)]
)
data class LearnedUserPatternEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userPhrase: String,
    val mappedIntent: String,
    val frequency: Int = 1,
    val confidenceWeight: Float = 0.5f,
    val lastUsedTimestamp: Long = System.currentTimeMillis()
)
