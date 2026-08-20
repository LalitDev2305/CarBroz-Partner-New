package com.carbroz.partner.app.composition.graph

import com.carbroz.partner.app.composition.config.AppConfig
import com.carbroz.partner.app.composition.config.SduiEndpointConfig
import com.carbroz.partner.core.navigation.NavDestination
import com.carbroz.partner.core.navigation.Router
import com.carbroz.partner.core.navigation.createRouter
import com.carbroz.partner.core.observability.logger.DefaultPipelineLogger
import com.carbroz.partner.core.observability.logger.StructuredLogger
import com.carbroz.partner.domain.session.credential.PersistedSessionCredentialProvider
import com.carbroz.partner.domain.session.credential.SessionCredentialPersistence
import com.carbroz.partner.domain.session.operation.ClearSession
import com.carbroz.partner.domain.session.provider.SessionCredentialProvider
import com.carbroz.partner.domain.session.refresh.SessionRefreshCoordinator
import com.carbroz.partner.domain.session.refresh.SessionRefreshGateway
import com.carbroz.partner.domain.session.restore.SessionRestorer
import com.carbroz.partner.domain.session.store.SessionStore
import com.carbroz.partner.engine.execution.binding.DefaultBindingResolver
import com.carbroz.partner.engine.execution.dispatcher.ActionDispatcher
import com.carbroz.partner.engine.execution.dispatcher.DefaultActionDispatcher
import com.carbroz.partner.engine.execution.executor.ActionRegistry
import com.carbroz.partner.engine.execution.executor.ApiActionExecutor
import com.carbroz.partner.engine.execution.executor.LogoutActionExecutor
import com.carbroz.partner.feature.splash.orchestrator.ImmediateStartupOrchestrator
import com.carbroz.partner.feature.splash.orchestrator.StartupOrchestrator
import com.carbroz.partner.feature.splash.store.SplashStore
import com.carbroz.partner.infrastructure.network.client.KtorNetworkClient
import com.carbroz.partner.infrastructure.network.client.NetworkClient
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
    public val credentialPersistence: SessionCredentialPersistence,
    public val endpointConfig: SduiEndpointConfig = SduiEndpointConfig(),
    public val sessionStore: SessionStore = SessionStore(),
    public val refreshGateway: SessionRefreshGateway? = null,
    public val logger: StructuredLogger = DefaultPipelineLogger(
        sinks = listOf(com.carbroz.partner.core.observability.sink.ConsoleLogSink())
    )
) {
    public val credentialProvider: SessionCredentialProvider = PersistedSessionCredentialProvider(credentialPersistence)
    public val clearSession: ClearSession = ClearSession(credentialPersistence, sessionStore)
    public val sessionRestorer: SessionRestorer = SessionRestorer(credentialPersistence, sessionStore)
    public val refreshCoordinator: SessionRefreshCoordinator? = refreshGateway?.let {
        SessionRefreshCoordinator(credentialPersistence, it, clearSession)
    }

    public val networkClient: NetworkClient = KtorNetworkClient(
        baseUrl = config.baseUrl,
        credentialProvider = credentialProvider,
        refreshCoordinator = refreshCoordinator
    )

    public val startupOrchestrator: StartupOrchestrator = ImmediateStartupOrchestrator(sessionRestorer)

    public val sduiProcessor: SduiProcessor = SduiProcessor()
    public val router: Router = createRouter(
        initialDestination = NavDestination.create("splash"),
        logger = logger
    )
    public val apiActionExecutor: ApiActionExecutor = ApiActionExecutor(networkClient)
    public val logoutActionExecutor: LogoutActionExecutor = LogoutActionExecutor(clearSession)
    public val actionRegistry: ActionRegistry = ActionRegistry.create(listOf(apiActionExecutor, logoutActionExecutor))
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
