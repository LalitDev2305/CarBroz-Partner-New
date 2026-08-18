package com.carbroz.partner.core.navigation.router

import com.carbroz.partner.core.navigation.command.NavCommand
import com.carbroz.partner.core.navigation.command.PopToTarget
import com.carbroz.partner.core.navigation.destination.NavDestination
import com.carbroz.partner.core.navigation.result.NavResult
import com.carbroz.partner.core.navigation.stack.DefaultNavEntryIdGenerator
import com.carbroz.partner.core.navigation.stack.NavEntry
import com.carbroz.partner.core.navigation.stack.NavEntryIdGenerator
import com.carbroz.partner.core.navigation.stack.NavStack
import com.carbroz.partner.core.navigation.state.NavState
import com.carbroz.partner.core.observability.logger.StructuredLogger
import com.carbroz.partner.core.observability.model.LogCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Standard implementation of [Router] providing thread-safe, Mutex-serialized navigation updates.
 */
class DefaultRouter(
    initialDestination: NavDestination,
    private val idGenerator: NavEntryIdGenerator = DefaultNavEntryIdGenerator(),
    private val logger: StructuredLogger? = null
) : Router {

    private val mutex = Mutex()
    private val initialEntry = NavEntry(idGenerator.generateId(), initialDestination)
    private val _state = MutableStateFlow(NavState(NavStack.create(initialEntry)))

    override val state: StateFlow<NavState> = _state.asStateFlow()

    override val currentState: NavState
        get() = _state.value

    init {
        logger?.withSource("DefaultRouter")?.info(
            sourceFunction = "init",
            category = LogCategory.NAVIGATION,
            event = "ROUTER_INITIALIZED",
            message = "Router initialized with root destination: ${initialDestination.route}"
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
        if (destination.route.isBlank()) {
            return reject(NavResult.Rejected.InvalidRoute(destination.route), "PUSH_REJECTED", "Blank route")
        }
        val newEntry = NavEntry(idGenerator.generateId(), destination)
        val currentEntries = currentState.stack.entries
        val newStack = NavStack.create(currentEntries + newEntry)
        _state.value = NavState(newStack)
        logExecuted("PUSH_EXECUTED", "Pushed destination: ${destination.route}")
        return NavResult.Executed(newEntry)
    }

    private fun handleReplace(destination: NavDestination): NavResult {
        if (destination.route.isBlank()) {
            return reject(NavResult.Rejected.InvalidRoute(destination.route), "REPLACE_REJECTED", "Blank route")
        }
        val newEntry = NavEntry(idGenerator.generateId(), destination)
        val currentEntries = currentState.stack.entries
        val newEntries = currentEntries.dropLast(1) + newEntry
        val newStack = NavStack.create(newEntries)
        _state.value = NavState(newStack)
        logExecuted("REPLACE_EXECUTED", "Replaced top with destination: ${destination.route}")
        return NavResult.Executed(newEntry)
    }

    private fun handlePop(): NavResult {
        val currentEntries = currentState.stack.entries
        if (currentEntries.size <= 1) {
            return reject(NavResult.Rejected.CannotPopRoot, "POP_REJECTED", "Cannot pop root entry")
        }
        val newStack = NavStack.create(currentEntries.dropLast(1))
        _state.value = NavState(newStack)
        logExecuted("POP_EXECUTED", "Popped top entry")
        return NavResult.Executed(newStack.current)
    }

    private fun handlePopTo(target: PopToTarget, inclusive: Boolean): NavResult {
        val currentEntries = currentState.stack.entries
        val targetIndex = when (target) {
            is PopToTarget.ByEntryId -> currentEntries.indexOfLast { it.entryId == target.entryId }
            is PopToTarget.ByRoute -> currentEntries.indexOfLast { it.destination.route == target.route }
        }

        if (targetIndex == -1) {
            return reject(NavResult.Rejected.TargetNotFound(target), "POP_TO_REJECTED", "Target not found in stack")
        }

        val keepCount = if (inclusive) targetIndex else targetIndex + 1
        if (keepCount < 1) {
            return reject(NavResult.Rejected.CannotPopRoot, "POP_TO_REJECTED", "PopTo inclusive would clear root")
        }

        val newEntries = currentEntries.take(keepCount)
        val newStack = NavStack.create(newEntries)
        _state.value = NavState(newStack)
        logExecuted("POP_TO_EXECUTED", "Popped to target, active: ${newStack.current.destination.route}")
        return NavResult.Executed(newStack.current)
    }

    private fun handleResetTo(destination: NavDestination): NavResult {
        if (destination.route.isBlank()) {
            return reject(NavResult.Rejected.InvalidRoute(destination.route), "RESET_TO_REJECTED", "Blank route")
        }
        val newEntry = NavEntry(idGenerator.generateId(), destination)
        val newStack = NavStack.create(newEntry)
        _state.value = NavState(newStack)
        logExecuted("RESET_TO_EXECUTED", "Reset stack to root: ${destination.route}")
        return NavResult.Executed(newEntry)
    }

    private fun logExecuted(event: String, message: String) {
        logger?.withSource("DefaultRouter")?.info(
            sourceFunction = "execute",
            category = LogCategory.NAVIGATION,
            event = event,
            message = message
        )
    }

    private fun reject(result: NavResult.Rejected, event: String, message: String): NavResult {
        logger?.withSource("DefaultRouter")?.error(
            sourceFunction = "execute",
            category = LogCategory.NAVIGATION,
            event = event,
            message = message
        )
        return result
    }

}
