package com.carbroz.feature.dynamic

import com.carbroz.data.network.NetworkAuthentication
import com.carbroz.data.network.NetworkDataSource
import com.carbroz.data.network.NetworkEndpoint
import com.carbroz.data.network.NetworkMethod
import com.carbroz.data.network.NetworkRequest
import com.carbroz.data.network.NetworkResult
import com.carbroz.foundation.capabilities.CapabilityRegistry
import com.carbroz.foundation.capabilities.CapabilityResult
import com.carbroz.platform.background.BackgroundConstraints
import com.carbroz.platform.background.BackgroundScheduleResult
import com.carbroz.platform.background.BackgroundScheduler
import com.carbroz.platform.background.BackgroundTaskId
import com.carbroz.platform.background.BackgroundTaskKind
import com.carbroz.platform.background.BackgroundTaskRequest
import com.carbroz.platform.background.ContinuousExecutionController
import com.carbroz.platform.background.ContinuousExecutionRequest
import com.carbroz.platform.background.ContinuousExecutionStartResult
import com.carbroz.platform.background.ExistingTaskPolicy
import com.carbroz.platform.background.NetworkRequirement
import com.carbroz.runtime.action.PreparedAction
import com.carbroz.runtime.sdui.model.BackgroundNetworkRequirement
import com.carbroz.runtime.sdui.model.BackgroundOperation
import com.carbroz.runtime.sdui.model.BackgroundWorkKind
import com.carbroz.runtime.sdui.model.RequestAuthentication
import com.carbroz.runtime.sdui.model.RequestMethod
import kotlinx.serialization.json.JsonPrimitive

/** Dynamic-feature adapter from a prepared request action to the canonical network data source. */
class NetworkActionExecutor(private val dataSource: NetworkDataSource) {
    suspend fun execute(action: PreparedAction.Request): NetworkResult = dataSource.execute(
        NetworkRequest(
            method = action.method.toNetworkMethod(),
            endpoint = NetworkEndpoint(action.endpoint),
            payload = action.payload,
            authentication = action.authentication.toNetworkAuthentication(),
        ),
    )

    private fun RequestMethod.toNetworkMethod(): NetworkMethod = when (this) {
        RequestMethod.GET -> NetworkMethod.GET
        RequestMethod.POST -> NetworkMethod.POST
        RequestMethod.PUT -> NetworkMethod.PUT
        RequestMethod.PATCH -> NetworkMethod.PATCH
        RequestMethod.DELETE -> NetworkMethod.DELETE
    }

    private fun RequestAuthentication.toNetworkAuthentication(): NetworkAuthentication = when (this) {
        RequestAuthentication.NONE -> NetworkAuthentication.NONE
        RequestAuthentication.SESSION -> NetworkAuthentication.SESSION
        RequestAuthentication.OPTIONAL_SESSION -> NetworkAuthentication.OPTIONAL_SESSION
    }
}

/** Dynamic-feature adapter from a prepared capability action to the canonical capability registry. */
class CapabilityActionExecutor(private val registry: CapabilityRegistry) {
    suspend fun execute(action: PreparedAction.Capability): CapabilityResult = registry.execute(action.request)
}

sealed interface BackgroundActionResult {
    data object Success : BackgroundActionResult
    data class Unsupported(val reason: String) : BackgroundActionResult
    data class Rejected(val reason: String) : BackgroundActionResult
}

/** Dynamic-feature adapter from prepared background semantics to platform background services. */
class BackgroundActionExecutor(
    private val scheduler: BackgroundScheduler?,
    private val continuous: ContinuousExecutionController?,
) {
    suspend fun execute(action: PreparedAction.Background): BackgroundActionResult {
        val id = BackgroundTaskId(action.id)
        return when (action.operation) {
            BackgroundOperation.SCHEDULE -> {
                val target = scheduler ?: return BackgroundActionResult.Unsupported("background_scheduler_unavailable")
                when (val result = target.schedule(action.toTaskRequest(id))) {
                    BackgroundScheduleResult.Scheduled,
                    BackgroundScheduleResult.AlreadyScheduled -> BackgroundActionResult.Success
                    is BackgroundScheduleResult.Unsupported -> BackgroundActionResult.Unsupported(result.reason)
                    is BackgroundScheduleResult.Rejected -> BackgroundActionResult.Rejected(result.reason)
                }
            }
            BackgroundOperation.CANCEL -> {
                val target = scheduler ?: return BackgroundActionResult.Unsupported("background_scheduler_unavailable")
                target.cancel(id)
                BackgroundActionResult.Success
            }
            BackgroundOperation.START_CONTINUOUS -> {
                val target = continuous ?: return BackgroundActionResult.Unsupported("continuous_execution_unavailable")
                val title = action.title ?: return BackgroundActionResult.Rejected("continuous_title_missing")
                val description = action.description ?: return BackgroundActionResult.Rejected("continuous_description_missing")
                when (val result = target.start(ContinuousExecutionRequest(id, title, description))) {
                    ContinuousExecutionStartResult.Started,
                    ContinuousExecutionStartResult.AlreadyRunning -> BackgroundActionResult.Success
                    is ContinuousExecutionStartResult.Unsupported -> BackgroundActionResult.Unsupported(result.reason)
                    is ContinuousExecutionStartResult.Rejected -> BackgroundActionResult.Rejected(result.reason)
                }
            }
            BackgroundOperation.STOP_CONTINUOUS -> {
                val target = continuous ?: return BackgroundActionResult.Unsupported("continuous_execution_unavailable")
                target.stop(id)
                BackgroundActionResult.Success
            }
        }
    }

    private fun PreparedAction.Background.toTaskRequest(id: BackgroundTaskId): BackgroundTaskRequest =
        BackgroundTaskRequest(
            id = id,
            kind = when (workKind) {
                BackgroundWorkKind.REFRESH -> BackgroundTaskKind.REFRESH
                BackgroundWorkKind.PROCESSING -> BackgroundTaskKind.PROCESSING
            },
            earliestStartDelayMillis = earliestStartDelayMillis,
            constraints = BackgroundConstraints(
                network = when (networkRequirement) {
                    BackgroundNetworkRequirement.NOT_REQUIRED -> NetworkRequirement.NOT_REQUIRED
                    BackgroundNetworkRequirement.CONNECTED -> NetworkRequirement.CONNECTED
                    BackgroundNetworkRequirement.UNMETERED -> NetworkRequirement.UNMETERED
                },
                requiresCharging = requiresCharging,
            ),
            existingTaskPolicy = ExistingTaskPolicy.REPLACE,
            input = input.mapValues { (_, value) ->
                val primitive = value as? JsonPrimitive
                    ?: throw IllegalArgumentException("Background input must contain primitive values only")
                primitive.content
            },
        )
}
