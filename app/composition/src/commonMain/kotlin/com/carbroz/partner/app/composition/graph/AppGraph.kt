package com.carbroz.partner.app.composition.graph

import com.carbroz.partner.app.composition.config.AppConfig
import com.carbroz.partner.app.composition.config.SduiEndpointConfig
import com.carbroz.partner.core.navigation.destination.NavDestination
import com.carbroz.partner.core.navigation.router.DefaultRouter
import com.carbroz.partner.core.navigation.router.Router
import com.carbroz.partner.core.observability.logger.DefaultPipelineLogger
import com.carbroz.partner.core.observability.logger.StructuredLogger
import com.carbroz.partner.domain.storage.secure.SecureStorageGateway
import com.carbroz.partner.engine.execution.binding.DefaultBindingResolver
import com.carbroz.partner.engine.execution.dispatcher.ActionDispatcher
import com.carbroz.partner.engine.execution.dispatcher.DefaultActionDispatcher
import com.carbroz.partner.engine.execution.executor.ActionRegistry
import com.carbroz.partner.engine.execution.executor.ApiActionExecutor
import com.carbroz.partner.feature.splash.orchestrator.ImmediateStartupOrchestrator
import com.carbroz.partner.feature.splash.orchestrator.StartupDestination
import com.carbroz.partner.feature.splash.orchestrator.StartupOrchestrator
import com.carbroz.partner.feature.splash.store.SplashStore
import com.carbroz.partner.infrastructure.network.client.KtorNetworkClient
import com.carbroz.partner.infrastructure.network.client.NetworkClient
import com.carbroz.partner.infrastructure.persistence.secure.SecureStorageFactory
import com.carbroz.partner.sdui.engine.processor.SduiProcessor
import com.carbroz.partner.sdui.host.controller.SduiScreenHostController
import com.carbroz.partner.sdui.host.repository.CachedSduiScreenRepository
import com.carbroz.partner.sdui.host.repository.NetworkSduiScreenRepository
import com.carbroz.partner.sdui.host.repository.SduiScreenRepository
import kotlinx.coroutines.CoroutineScope

/**
 * Application Composition Graph.
 *
 * Serves strictly as the dependency wiring boundary instantiating application-scoped
 * instances ([NetworkClient], [ActionDispatcher], [SduiProcessor], [SduiScreenRepository], [Router], [StartupOrchestrator]).
 */
public class AppGraph(
    public val config: AppConfig,
    public val endpointConfig: SduiEndpointConfig = SduiEndpointConfig(),
    public val sessionStore: com.carbroz.partner.domain.session.store.SessionStore = com.carbroz.partner.domain.session.store.SessionStore(),
    public val secureStorage: SecureStorageGateway = SecureStorageFactory.create(),
    public val networkClient: NetworkClient = KtorNetworkClient(config.baseUrl),
    public val logger: StructuredLogger = DefaultPipelineLogger(
        sinks = listOf(com.carbroz.partner.core.observability.sink.ConsoleLogSink())
    ),
    public val startupOrchestrator: StartupOrchestrator = ImmediateStartupOrchestrator()
) {
    public val sduiProcessor: SduiProcessor = SduiProcessor()
    public val router: Router = DefaultRouter(
        initialDestination = NavDestination.create("splash"),
        logger = logger
    )
    public val apiActionExecutor: ApiActionExecutor = ApiActionExecutor(networkClient)
    public val actionRegistry: ActionRegistry = ActionRegistry.create(listOf(apiActionExecutor))
    public val actionDispatcher: ActionDispatcher = DefaultActionDispatcher(
        registry = actionRegistry,
        bindingResolver = DefaultBindingResolver(),
        logger = logger
    )
    public val rawScreenRepository: SduiScreenRepository = NetworkSduiScreenRepository(networkClient)
    public val screenRepository: SduiScreenRepository = CachedSduiScreenRepository(
        delegate = rawScreenRepository,
        sessionStore = sessionStore
    )

    /**
     * Factory function creating a new [SplashStore] managed by the specified [scope].
     */
    public fun createSplashStore(scope: CoroutineScope): SplashStore {
        return SplashStore(
            scope = scope,
            logger = logger,
            orchestrator = startupOrchestrator
        )
    }

    /**
     * Factory function creating a new [SduiScreenHostController] managed by the specified [scope].
     */
    public fun createHostController(scope: CoroutineScope): SduiScreenHostController {
        return SduiScreenHostController(
            scope = scope,
            endpoint = endpointConfig.entryEndpoint,
            repository = screenRepository,
            actionDispatcher = actionDispatcher,
            router = router,
            processor = sduiProcessor
        )
    }
}
