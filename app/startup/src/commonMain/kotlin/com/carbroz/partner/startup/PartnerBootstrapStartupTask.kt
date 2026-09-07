package com.carbroz.partner.startup

import com.carbroz.foundation.session.SessionProvider
import com.carbroz.foundation.session.SessionState
import com.carbroz.runtime.application.startup.StartupFailure
import com.carbroz.runtime.application.startup.StartupTask
import com.carbroz.runtime.application.startup.StartupTaskResult

/** Thin final startup task: fetch -> auth consistency check -> policy evaluation. */
class PartnerBootstrapStartupTask(
    private val client: PartnerBootstrapClient,
    private val policyEvaluator: PartnerBootstrapPolicyEvaluator,
    private val sessionProvider: SessionProvider,
) : StartupTask {
    override val id: String = "partner.bootstrap"

    override suspend fun execute(): StartupTaskResult = when (val result = client.fetch()) {
        is PartnerBootstrapClientResult.Failure -> StartupTaskResult.Failure(result.reason.toStartupFailure())
        is PartnerBootstrapClientResult.Success -> resolve(result.data)
    }

    private suspend fun resolve(data: PartnerBootstrapData): StartupTaskResult {
        val locallyAuthenticated = sessionProvider.current() is SessionState.Authenticated
        if (data.startup.authenticated != locallyAuthenticated) {
            return StartupTaskResult.Failure(
                StartupFailure.Expected("bootstrap_session_mismatch", recoverable = false),
            )
        }

        return when (val decision = policyEvaluator.evaluate(data)) {
            is PartnerBootstrapPolicyResult.Invalid -> StartupTaskResult.Failure(
                StartupFailure.Expected(decision.code, recoverable = false),
            )
            is PartnerBootstrapPolicyResult.Resolved -> StartupTaskResult.Resolved(decision.resolution)
        }
    }

    private fun PartnerBootstrapClientFailure.toStartupFailure(): StartupFailure.Expected = when (this) {
        PartnerBootstrapClientFailure.Offline -> StartupFailure.Expected("bootstrap_offline", true)
        PartnerBootstrapClientFailure.Timeout -> StartupFailure.Expected("bootstrap_timeout", true)
        PartnerBootstrapClientFailure.Transport -> StartupFailure.Expected("bootstrap_transport", true)
        is PartnerBootstrapClientFailure.Http -> StartupFailure.Expected(
            code = "bootstrap_http_$statusCode",
            recoverable = statusCode == 401 || statusCode == 408 || statusCode == 429 || statusCode in 500..599,
        )
        is PartnerBootstrapClientFailure.InvalidPayload -> StartupFailure.Expected(code, false)
    }
}
