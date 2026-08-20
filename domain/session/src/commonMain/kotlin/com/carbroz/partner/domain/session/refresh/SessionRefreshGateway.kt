package com.carbroz.partner.domain.session.refresh

/**
 * Interface for remote credential refresh execution.
 */
public fun interface SessionRefreshGateway {
    public suspend fun refreshToken(refreshToken: String): SessionRefreshResult
}
