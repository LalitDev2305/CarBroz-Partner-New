package com.carbroz.partner.core.navigation

import com.carbroz.partner.core.navigation.internal.DefaultRouter
import com.carbroz.partner.core.observability.logger.StructuredLogger

/**
 * Canonical factory function for creating [Router] instances.
 */
fun createRouter(
    initialDestination: NavDestination,
    logger: StructuredLogger
): Router = DefaultRouter(
    initialDestination = initialDestination,
    structuredLogger = logger
)
