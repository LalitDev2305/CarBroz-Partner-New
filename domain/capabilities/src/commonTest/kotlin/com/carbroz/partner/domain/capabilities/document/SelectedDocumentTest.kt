package com.carbroz.partner.domain.capabilities.document

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SelectedDocumentTest {

    @Test
    fun verifyValidSelectedDocument() {
        val doc = SelectedDocument(
            uri = "content://com.android.providers/document/456",
            fileName = "registration.pdf",
            mimeType = "application/pdf",
            sizeBytes = 50000L
        )

        assertEquals("content://com.android.providers/document/456", doc.uri)
        assertEquals("registration.pdf", doc.fileName)
        assertEquals("application/pdf", doc.mimeType)
        assertEquals(50000L, doc.sizeBytes)
    }

    @Test
    fun verifyBlankFieldsRejection() {
        assertFailsWith<IllegalArgumentException> {
            SelectedDocument("", "doc.pdf", "application/pdf", 100L)
        }
        assertFailsWith<IllegalArgumentException> {
            SelectedDocument("content://doc/1", "", "application/pdf", 100L)
        }
        assertFailsWith<IllegalArgumentException> {
            SelectedDocument("content://doc/1", "doc.pdf", "   ", 100L)
        }
    }

    @Test
    fun verifyNegativeSizeRejection() {
        assertFailsWith<IllegalArgumentException> {
            SelectedDocument("content://doc/1", "doc.pdf", "application/pdf", -10L)
        }
    }
}
