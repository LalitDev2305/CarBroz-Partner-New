package com.carbroz.sdui.parser

class SduiVersionPolicy(
    private val supported: Set<String> = setOf("3.0.0", "3.0"),
) {
    fun supports(version: String): Boolean = version in supported
}
