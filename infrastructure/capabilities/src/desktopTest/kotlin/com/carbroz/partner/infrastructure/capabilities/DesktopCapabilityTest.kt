package com.carbroz.partner.infrastructure.capabilities

import com.carbroz.partner.domain.capabilities.clipboard.ClipboardGateway
import com.carbroz.partner.domain.capabilities.core.CapabilityResult
import com.carbroz.partner.domain.capabilities.external.ExternalUriLauncher
import com.carbroz.partner.infrastructure.capabilities.clipboard.DesktopClipboardCapability
import com.carbroz.partner.infrastructure.capabilities.external.DesktopExternalUriCapability
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopCapabilityTest {

    @Test
    fun testDesktopClipboardCapabilitySuccess() = runTest {
        val clipboard: ClipboardGateway = DesktopClipboardCapability()
        val result = clipboard.copyText("Hello CarBroz")
        assertTrue(result is CapabilityResult.Success)
    }

    @Test
    fun testDesktopExternalUriCapabilityUnsupportedOrSuccess() = runTest {
        val launcher: ExternalUriLauncher = DesktopExternalUriCapability()
        val result = launcher.openUri("https://carbroz.com")
        // Headless desktop CI might return Unsupported or Success depending on Desktop.isDesktopSupported()
        assertTrue(result is CapabilityResult.Success || result is CapabilityResult.Unsupported)
    }
}
