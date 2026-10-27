package com.lumify.editor

import com.lumify.editor.core.AspectRatio
import com.lumify.editor.core.CropRect
import com.lumify.editor.core.EditState
import com.lumify.editor.core.HistoryManager
import com.lumify.editor.core.HslAdjustment
import com.lumify.editor.core.HslChannel
import com.lumify.editor.core.ImageProcessor
import com.lumify.editor.core.ToneCurves
import com.lumify.editor.presets.PresetRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageProcessorTest {

    @Test
    fun defaultEditStateIsNotModified() {
        val state = EditState()
        assertFalse(state.isModified)
        assertEquals(0f, state.exposure, 0.001f)
        assertEquals(0f, state.contrast, 0.001f)
        assertEquals(0f, state.temperature, 0.001f)
    }

    @Test
    fun modifiedEditStateFlagsTrue() {
        val state = EditState(exposure = 15f)
        assertTrue(state.isModified)
    }

    @Test
    fun historyManagerUndoRedoFlow() {
        val history = HistoryManager()
        assertFalse(history.canUndo)
        assertFalse(history.canRedo)

        val state1 = EditState(exposure = 20f)
        history.pushState(state1)
        assertTrue(history.canUndo)
        assertFalse(history.canRedo)

        val state2 = EditState(exposure = 20f, contrast = 10f)
        history.pushState(state2)

        val undone = history.undo()
        assertNotNull(undone)
        assertEquals(20f, undone!!.exposure, 0.001f)
        assertEquals(0f, undone.contrast, 0.001f)
        assertTrue(history.canRedo)

        val redone = history.redo()
        assertNotNull(redone)
        assertEquals(10f, redone!!.contrast, 0.001f)
    }

    @Test
    fun presetsAreProperlyConfigured() {
        val presets = PresetRepository.allPresets
        assertTrue("Preset catalog must contain presets", presets.isNotEmpty())

        val goldenHour = presets.firstOrNull { it.id == "golden_hour" }
        assertNotNull("Golden Hour preset should exist", goldenHour)

        val applied = goldenHour!!.applyState(EditState())
        assertTrue(applied.isModified)
        assertTrue(applied.temperature > 0f) // Warm temperature shift
    }

    @Test
    fun cropRectCalculations() {
        val crop = CropRect(left = 0.1f, top = 0.2f, right = 0.8f, bottom = 0.9f)
        assertEquals(0.7f, crop.width, 0.001f)
        assertEquals(0.7f, crop.height, 0.001f)
    }

    @Test
    fun aspectRatiosMatchDefinitions() {
        assertEquals(1f, AspectRatio.SQUARE.ratio!!, 0.001f)
        assertEquals(16f / 9f, AspectRatio.LANDSCAPE_16_9.ratio!!, 0.001f)
        assertEquals(9f / 16f, AspectRatio.STORY_9_16.ratio!!, 0.001f)
        assertEquals(4f / 5f, AspectRatio.PORTRAIT_4_5.ratio!!, 0.001f)
    }

    @Test
    fun hslChannelsEnumCompleteness() {
        assertEquals(8, HslChannel.values().size)
        val defaultAdj = HslAdjustment()
        assertTrue(defaultAdj.isDefault)
    }

    @Test
    fun toneCurvesDefaultAndLutGeneration() {
        val curves = ToneCurves()
        assertTrue(curves.isDefault)

        val lut = ImageProcessor.buildCurveLut(curves.rgbPoints)
        assertEquals(256, lut.size)
        assertEquals(0, lut[0])
        assertEquals(255, lut[255])
        // Linear identity curve should map midpoint closely
        assertTrue(lut[128] in 125..131)
    }

    @Test
    fun recipeCopyStripsGeometry() {
        val original = EditState(
            exposure = 25f,
            temperature = 10f,
            rotationDegrees = 90f,
            cropRect = CropRect(0.1f, 0.1f, 0.9f, 0.9f)
        )
        val recipe = original.createRecipeCopy()
        assertEquals(25f, recipe.exposure, 0.001f)
        assertEquals(10f, recipe.temperature, 0.001f)
        assertEquals(0f, recipe.rotationDegrees, 0.001f)
        assertFalse(recipe.flipHorizontal)
        assertEquals(null, recipe.cropRect)
    }

    @Test
    fun parseCubeLutValidContent() {
        val cubeData = """
            # Lumify Test LUT
            LUT_3D_SIZE 2
            0.0 0.0 0.0
            1.0 0.0 0.0
            0.0 1.0 0.0
            1.0 1.0 0.0
            0.0 0.0 1.0
            1.0 0.0 1.0
            0.0 1.0 1.0
            1.0 1.0 1.0
        """.trimIndent()

        val parsed = ImageProcessor.parseCubeLut(cubeData.byteInputStream(), "TestLUT")
        assertNotNull(parsed)
        assertEquals(2, parsed!!.size)
        assertEquals(2 * 2 * 2 * 3, parsed.table.size)
    }
}
