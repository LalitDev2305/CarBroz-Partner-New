package com.carbroz.partner.domain.capabilities.permission

/**
 * Domain gateway for querying and requesting product permissions.
 */
interface PermissionGateway {
    suspend fun checkPermission(permission: Permission): PermissionState
    suspend fun requestPermission(permission: Permission): PermissionState
}
