package com.sensebridge.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_ai_sessions")
data class UserAiSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userInput: String,
    val aiResponse: String,
    val intent: String,
    val situation: String,
    val sceneText: String? = null,
    val sceneObjects: String? = null,
    val sceneSounds: String? = null,
    val ambientDecibels: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)
