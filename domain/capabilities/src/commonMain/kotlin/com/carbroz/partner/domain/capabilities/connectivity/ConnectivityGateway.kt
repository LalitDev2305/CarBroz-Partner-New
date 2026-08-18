package com.carbroz.partner.domain.capabilities.connectivity

import kotlinx.coroutines.flow.Flow

/**
 * Domain gateway for observing network reachability state.
 */
interface ConnectivityGateway {
    fun observeConnectivity(): Flow<ConnectivityState>
}
