package com.sensebridge.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sensebridge.core.model.PriorityLevel

@Entity(tableName = "saved_phrases")
data class SavedPhraseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val vietnameseText: String,
    val category: String,
    val isFavorite: Boolean = false,
    val priority: PriorityLevel = PriorityLevel.ATTENTION_P2,
    val usageCount: Int = 0
)
