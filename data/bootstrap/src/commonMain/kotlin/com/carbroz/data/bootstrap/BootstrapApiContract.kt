package com.carbroz.data.bootstrap

import com.carbroz.data.network.NetworkEndpoint

/** Partner bootstrap transport contract. Route ownership stays with the API-specific data adapter. */
internal object BootstrapApiContract {
    val endpoint: NetworkEndpoint = NetworkEndpoint("/api/v1/partner/config/bootstrap")
}
