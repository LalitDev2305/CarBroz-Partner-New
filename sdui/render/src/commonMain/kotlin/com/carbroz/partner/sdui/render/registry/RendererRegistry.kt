package com.carbroz.partner.sdui.render.registry

public class RendererRegistry<T>(
    entries: Map<String, T>
) {
    private val map: Map<String, T>

    init {
        val normalized = mutableMapOf<String, T>()
        entries.forEach { (rawKey, renderer) ->
            val key = rawKey.trim().lowercase()
            require(!normalized.containsKey(key)) {
                "Duplicate normalized registry key: '$key' (raw: '$rawKey')"
            }
            normalized[key] = renderer
        }
        this.map = normalized.toMap()
    }

    public fun resolve(type: String): T? = map[type.trim().lowercase()]

    public fun contains(type: String): Boolean = map.containsKey(type.trim().lowercase())
}
