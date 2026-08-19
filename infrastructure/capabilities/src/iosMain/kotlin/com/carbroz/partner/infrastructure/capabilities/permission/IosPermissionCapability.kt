package com.carbroz.partner.infrastructure.capabilities.permission

import com.carbroz.partner.domain.capabilities.permission.Permission
import com.carbroz.partner.domain.capabilities.permission.PermissionGateway
import com.carbroz.partner.domain.capabilities.permission.PermissionState

/**
 * iOS native Apple implementation of [PermissionGateway].
 */
public class IosPermissionCapability : PermissionGateway {

    override suspend fun checkPermission(permission: Permission): PermissionState {
        return PermissionState.UNSUPPORTED
    }

    override suspend fun requestPermission(permission: Permission): PermissionState {
        return PermissionState.UNSUPPORTED
    }
}
