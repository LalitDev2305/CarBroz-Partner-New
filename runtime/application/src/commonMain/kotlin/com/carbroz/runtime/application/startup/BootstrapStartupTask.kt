package com.carbroz.runtime.application.startup

import com.carbroz.runtime.application.bootstrap.ResolveBootstrapResult
import com.carbroz.runtime.application.bootstrap.ResolveBootstrapUseCase

/** Final startup task; application policy lives in [ResolveBootstrapUseCase]. */
class BootstrapStartupTask(
    private val resolveBootstrap: ResolveBootstrapUseCase,
) : StartupTask {
    override val id: String = "bootstrap"

    override suspend fun execute(): StartupTaskResult = when (val result = resolveBootstrap()) {
        is ResolveBootstrapResult.Resolved -> StartupTaskResult.Resolved(result.resolution)
        is ResolveBootstrapResult.Failed -> StartupTaskResult.Failure(result.failure)
    }
}
