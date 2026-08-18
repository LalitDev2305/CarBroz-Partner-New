package com.carbroz.partner.domain.capabilities.camera

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CapturedImageTest {

    @Test
    fun verifyValidCapturedImage() {
        val img = CapturedImage("file:///tmp/photo.jpg", "image/jpeg", 2048L)
        assertEquals("file:///tmp/photo.jpg", img.uri)
        assertEquals("image/jpeg", img.mimeType)
        assertEquals(2048L, img.sizeBytes)
    }

    @Test
    fun verifyZeroSizeAccepted() {
        val img = CapturedImage("file:///tmp/empty.jpg", "image/jpeg", 0L)
        assertEquals(0L, img.sizeBytes)
    }

    @Test
    fun verifyBlankUriRejection() {
        assertFailsWith<IllegalArgumentException> {
            CapturedImage("", "image/jpeg", 100L)
        }
        assertFailsWith<IllegalArgumentException> {
            CapturedImage("   ", "image/jpeg", 100L)
        }
    }

    @Test
    fun verifyBlankMimeTypeRejection() {
        assertFailsWith<IllegalArgumentException> {
            CapturedImage("file:///tmp/photo.jpg", "", 100L)
        }
        assertFailsWith<IllegalArgumentException> {
            CapturedImage("file:///tmp/photo.jpg", "   ", 100L)
        }
    }

    @Test
    fun verifyNegativeSizeBytesRejection() {
        assertFailsWith<IllegalArgumentException> {
            CapturedImage("file:///tmp/photo.jpg", "image/jpeg", -1L)
        }
    }
}
