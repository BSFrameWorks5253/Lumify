package com.lumify.editor

import com.lumify.editor.core.AspectRatio
import com.lumify.editor.core.CropRect
import com.lumify.editor.core.EditState
import com.lumify.editor.core.HistoryManager
import com.lumify.editor.core.HslAdjustment
import com.lumify.editor.core.HslChannel
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
}
