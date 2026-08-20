package com.carbroz.partner.domain.session.refresh

import com.carbroz.partner.domain.session.model.SessionCredentials

/**
 * Domain outcome returned by [SessionRefreshGateway] when invoking remote refresh.
 */
public sealed interface SessionRefreshResult {
    public data class Success(val credentials: SessionCredentials) : SessionRefreshResult
    public data object Rejected : SessionRefreshResult
    public data object Unavailable : SessionRefreshResult
}
