package com.carbroz.partner.core.navigation.internal

import com.carbroz.partner.core.navigation.NavCommand
import com.carbroz.partner.core.navigation.NavDestination
import com.carbroz.partner.core.navigation.NavEntry
import com.carbroz.partner.core.navigation.NavResult
import com.carbroz.partner.core.navigation.NavState
import com.carbroz.partner.core.navigation.PopToTarget
import com.carbroz.partner.core.navigation.Router
import com.carbroz.partner.core.observability.logger.BoundLogger
import com.carbroz.partner.core.observability.logger.StructuredLogger
import com.carbroz.partner.core.observability.model.LogCategory
import com.carbroz.partner.core.observability.model.LogLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Internal canonical implementation of [Router] providing thread-safe, Mutex-serialized navigation updates.
 */
internal class DefaultRouter(
    initialDestination: NavDestination,
    structuredLogger: StructuredLogger
) : Router {

    private val boundLogger: BoundLogger = structuredLogger.withSource("DefaultRouter")
    private val mutex = Mutex()
    private var nextEntryNumber = 0L

    private fun generateNextEntryId(): String {
        nextEntryNumber++
        return "entry_$nextEntryNumber"
    }

    private val initialEntry = NavEntry(generateNextEntryId(), initialDestination)
    private val _state = MutableStateFlow(NavState.create(initialEntry))

    override val state: StateFlow<NavState> = _state.asStateFlow()

    init {
        boundLogger.info(
            sourceFunction = "init",
            category = LogCategory.NAVIGATION,
            event = "ROUTER_INITIALIZED",
            message = "Router initialized"
        )
    }

    override suspend fun execute(command: NavCommand): NavResult = mutex.withLock {
        when (command) {
            is NavCommand.Push -> handlePush(command.destination)
            is NavCommand.Replace -> handleReplace(command.destination)
            is NavCommand.Pop -> handlePop()
            is NavCommand.PopTo -> handlePopTo(command.target, command.inclusive)
            is NavCommand.ResetTo -> handleResetTo(command.destination)
        }
    }

    private fun handlePush(destination: NavDestination): NavResult {
        val newEntry = NavEntry(generateNextEntryId(), destination)
        val currentEntries = _state.value.entries
        val newNavState = NavState.create(currentEntries + newEntry)
        _state.value = newNavState
        logExecuted("PUSH_EXECUTED", "Navigation push executed")
        return NavResult.Executed(newEntry)
    }

    private fun handleReplace(destination: NavDestination): NavResult {
        val newEntry = NavEntry(generateNextEntryId(), destination)
        val currentEntries = _state.value.entries
        val newEntries = currentEntries.dropLast(1) + newEntry
        val newNavState = NavState.create(newEntries)
        _state.value = newNavState
        logExecuted("REPLACE_EXECUTED", "Navigation replace executed")
        return NavResult.Executed(newEntry)
    }

    private fun handlePop(): NavResult {
        val currentEntries = _state.value.entries
        if (currentEntries.size <= 1) {
            return reject(NavResult.Rejected.CannotPopRoot, "POP_REJECTED", "Navigation pop rejected: cannot pop root entry")
        }
        val newNavState = NavState.create(currentEntries.dropLast(1))
        _state.value = newNavState
        logExecuted("POP_EXECUTED", "Navigation pop executed")
        return NavResult.Executed(newNavState.activeEntry)
    }

    private fun handlePopTo(target: PopToTarget, inclusive: Boolean): NavResult {
        val currentEntries = _state.value.entries
        val targetIndex = when (target) {
            is PopToTarget.ByEntryId -> currentEntries.indexOfLast { it.entryId == target.entryId }
            is PopToTarget.ByRoute -> currentEntries.indexOfLast { it.destination.route == target.route }
        }

        if (targetIndex == -1) {
            return reject(NavResult.Rejected.TargetNotFound(target), "POP_TO_REJECTED", "Navigation pop-to rejected")
        }

        val keepCount = if (inclusive) targetIndex else targetIndex + 1
        if (keepCount < 1) {
            return reject(NavResult.Rejected.CannotPopRoot, "POP_TO_REJECTED", "Navigation pop-to rejected")
        }

        val newEntries = currentEntries.take(keepCount)
        val newNavState = NavState.create(newEntries)
        _state.value = newNavState
        logExecuted("POP_TO_EXECUTED", "Navigation pop-to executed")
        return NavResult.Executed(newNavState.activeEntry)
    }

    private fun handleResetTo(destination: NavDestination): NavResult {
        val newEntry = NavEntry(generateNextEntryId(), destination)
        val newNavState = NavState.create(newEntry)
        _state.value = newNavState
        logExecuted("RESET_TO_EXECUTED", "Navigation reset executed")
        return NavResult.Executed(newEntry)
    }

    private fun logExecuted(event: String, message: String) {
        boundLogger.info(
            sourceFunction = "execute",
            category = LogCategory.NAVIGATION,
            event = event,
            message = message
        )
    }

    private fun reject(result: NavResult.Rejected, event: String, message: String): NavResult {
        boundLogger.log(
            level = LogLevel.WARN,
            category = LogCategory.NAVIGATION,
            sourceFunction = "execute",
            event = event,
            message = message
        )
        return result
    }
}
