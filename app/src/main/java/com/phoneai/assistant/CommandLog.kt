package com.phoneai.assistant

object CommandLog {
    private val listeners = mutableListOf<(String) -> Unit>()

    fun addListener(listener: (String) -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: (String) -> Unit) {
        listeners.remove(listener)
    }

    fun add(message: String) {
        listeners.forEach { it(message) }
    }
}
