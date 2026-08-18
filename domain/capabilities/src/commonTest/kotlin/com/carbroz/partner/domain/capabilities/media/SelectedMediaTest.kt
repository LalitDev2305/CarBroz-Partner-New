package com.carbroz.partner.domain.capabilities.media

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SelectedMediaTest {

    @Test
    fun verifyValidSelectedMedia() {
        val media = SelectedMedia(
            uri = "content://media/external/123",
            mediaType = MediaType.IMAGE,
            fileName = "car.png",
            mimeType = "image/png",
            sizeBytes = 1024L
        )

        assertEquals("content://media/external/123", media.uri)
        assertEquals(MediaType.IMAGE, media.mediaType)
        assertEquals("car.png", media.fileName)
        assertEquals("image/png", media.mimeType)
        assertEquals(1024L, media.sizeBytes)
    }

    @Test
    fun verifyBlankUriFileNameOrMimeRejection() {
        assertFailsWith<IllegalArgumentException> {
            SelectedMedia("", MediaType.IMAGE, "file.png", "image/png", 100L)
        }
        assertFailsWith<IllegalArgumentException> {
            SelectedMedia("content://media/1", MediaType.IMAGE, "", "image/png", 100L)
        }
        assertFailsWith<IllegalArgumentException> {
            SelectedMedia("content://media/1", MediaType.IMAGE, "file.png", "   ", 100L)
        }
    }

    @Test
    fun verifyNegativeSizeBytesRejection() {
        assertFailsWith<IllegalArgumentException> {
            SelectedMedia("content://media/1", MediaType.IMAGE, "file.png", "image/png", -5L)
        }
    }
}
