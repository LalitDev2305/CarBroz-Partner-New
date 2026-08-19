package com.carbroz.partner.infrastructure.capabilities.permission

import com.carbroz.partner.domain.capabilities.permission.Permission
import com.carbroz.partner.domain.capabilities.permission.PermissionGateway
import com.carbroz.partner.domain.capabilities.permission.PermissionState

/**
 * Desktop JVM implementation of [PermissionGateway].
 *
 * Desktop applications do not enforce Android/iOS-style runtime permission prompts.
 * Returns [PermissionState.UNSUPPORTED] for platform capabilities.
 */
public class DesktopPermissionCapability : PermissionGateway {

    override suspend fun checkPermission(permission: Permission): PermissionState {
        return PermissionState.UNSUPPORTED
    }

    override suspend fun requestPermission(permission: Permission): PermissionState {
        return PermissionState.UNSUPPORTED
    }
}
