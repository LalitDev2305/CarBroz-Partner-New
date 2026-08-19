package com.carbroz.partner.domain.session.refresh

import com.carbroz.partner.domain.session.model.SessionRefreshResult

public fun interface SessionRefreshGateway {
    public suspend fun refreshToken(refreshToken: String): SessionRefreshResult
}
