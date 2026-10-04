package com.sensebridge.input.vision

import com.sensebridge.core.model.PriorityLevel
import com.sensebridge.core.model.SpatialDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SentenceGeneratorTest {

    private lateinit var generator: SentenceGenerator

    @Before
    fun setUp() {
        generator = SentenceGenerator()
    }

    @Test
    fun generateDescription_personOnLeft_producesAccurateSentence() {
        val context = SpatialObjectContext(
            direction = SpatialDirection.LEFT,
            isNear = false,
            isFar = false,
            normalizedX = 0.2f,
            areaRatio = 0.15f
        )

        val result = generator.generateDescription("person", context)

        assertEquals("Người", result.vietnameseTitle)
        assertEquals(PriorityLevel.ATTENTION_P2, result.priority)
        assertTrue(result.spokenSentence.contains("người bên trái"))
    }

    @Test
    fun generateDescription_approachingCarCenterNear_elevatesToWarning() {
        val context = SpatialObjectContext(
            direction = SpatialDirection.CENTER,
            isNear = true,
            isFar = false,
            normalizedX = 0.5f,
            areaRatio = 0.35f
        )

        val result = generator.generateDescription("car", context)

        assertEquals("Xe ô tô", result.vietnameseTitle)
        assertEquals(PriorityLevel.WARNING_P1, result.priority)
        assertTrue(result.spokenSentence.contains("Cẩn thận"))
        assertTrue(result.spokenSentence.contains("ở gần"))
    }

    @Test
    fun generateDescription_stairs_elevatesToWarning() {
        val context = SpatialObjectContext(
            direction = SpatialDirection.CENTER,
            isNear = false,
            isFar = false,
            normalizedX = 0.5f,
            areaRatio = 0.15f
        )

        val result = generator.generateDescription("stairs", context)

        assertEquals(PriorityLevel.WARNING_P1, result.priority)
        assertTrue(result.spokenSentence.contains("bậc thang"))
        assertTrue(result.spokenSentence.contains("Cẩn thận"))
    }

    @Test
    fun generateDescription_doorOnRight_producesDoorSentence() {
        val context = SpatialObjectContext(
            direction = SpatialDirection.RIGHT,
            isNear = false,
            isFar = false,
            normalizedX = 0.8f,
            areaRatio = 0.18f
        )

        val result = generator.generateDescription("door", context)

        assertEquals("Cửa", result.vietnameseTitle)
        assertEquals(PriorityLevel.ATTENTION_P2, result.priority)
        assertTrue(result.spokenSentence.contains("cửa bên phải"))
    }

    @Test
    fun generateDescription_defaultMlKitCategories_translatedProperly() {
        val context = SpatialObjectContext(
            direction = SpatialDirection.CENTER,
            isNear = false,
            isFar = false,
            normalizedX = 0.5f,
            areaRatio = 0.15f
        )

        val homeGoodResult = generator.generateDescription("home good", context)
        assertEquals("Vật dụng trong nhà", homeGoodResult.vietnameseTitle)
        assertTrue(homeGoodResult.spokenSentence.contains("vật dụng trong nhà"))

        val plantResult = generator.generateDescription("plant", context)
        assertEquals("Cây cối", plantResult.vietnameseTitle)
        assertTrue(plantResult.spokenSentence.contains("cây cối"))

        val foodResult = generator.generateDescription("food", context)
        assertEquals("Thực phẩm", foodResult.vietnameseTitle)
        assertTrue(foodResult.spokenSentence.contains("thực phẩm"))
    }
}
