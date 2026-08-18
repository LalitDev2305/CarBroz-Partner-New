package com.carbroz.partner.domain.storage.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class StorageResultTest {

    @Test
    fun verifySuccessVariantRetainsValue() {
        val result = StorageResult.Success("test_payload")
        assertEquals("test_payload", result.value)
    }

    @Test
    fun verifyNotFoundVariantIsSingleton() {
        val result1 = StorageResult.NotFound
        val result2 = StorageResult.NotFound
        assertEquals(result1, result2)
    }

    @Test
    fun verifyFailureVariantRetainsFailure() {
        val failure = StorageFailure(StorageFailure.FailureCode.READ_FAILED, "File not readable")
        val result = StorageResult.Failure(failure)
        assertEquals(StorageFailure.FailureCode.READ_FAILED, result.failure.code)
        assertEquals("File not readable", result.failure.message)
    }

    @Test
    fun verifyStorageFailureValidMessageAccepted() {
        val failure = StorageFailure(StorageFailure.FailureCode.WRITE_FAILED, "Disk quota exceeded")
        assertEquals("Disk quota exceeded", failure.message)
    }

    @Test
    fun verifyStorageFailureBlankMessageRejected() {
        assertFailsWith<IllegalArgumentException> {
            StorageFailure(StorageFailure.FailureCode.UNKNOWN, "")
        }
        assertFailsWith<IllegalArgumentException> {
            StorageFailure(StorageFailure.FailureCode.UNKNOWN, "   ")
        }
    }

    @Test
    fun verifyAllFailureCodesRepresentable() {
        val codes = StorageFailure.FailureCode.entries
        assertEquals(5, codes.size)
        assertEquals(
            listOf(
                StorageFailure.FailureCode.READ_FAILED,
                StorageFailure.FailureCode.WRITE_FAILED,
                StorageFailure.FailureCode.DELETE_FAILED,
                StorageFailure.FailureCode.SECURITY_HARDWARE_UNAVAILABLE,
                StorageFailure.FailureCode.UNKNOWN
            ),
            codes
        )
    }

    @Test
    fun verifyEqualityAndHashCodeSemantics() {
        val failure1 = StorageFailure(StorageFailure.FailureCode.DELETE_FAILED, "Unable to delete key")
        val failure2 = StorageFailure(StorageFailure.FailureCode.DELETE_FAILED, "Unable to delete key")
        val failure3 = StorageFailure(StorageFailure.FailureCode.DELETE_FAILED, "Different message")

        assertEquals(failure1, failure2)
        assertEquals(failure1.hashCode(), failure2.hashCode())
        assertNotEquals(failure1, failure3)
    }
}
