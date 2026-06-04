package com.culoo.cusagl_4android.accessibility

interface AccessibilityServiceListener {
    fun onServiceAvailable(service: LyreAccessibilityService)
    fun onServiceUnavailable()
}

object AccessibilityServiceBridge {
    private val lock = Any()
    @Volatile
    private var service: LyreAccessibilityService? = null
    private val listeners = linkedSetOf<AccessibilityServiceListener>()

    fun getService(): LyreAccessibilityService? = service

    fun isConnected(): Boolean = service != null

    fun registerListener(newListener: AccessibilityServiceListener) {
        val currentService = synchronized(lock) {
            listeners.add(newListener)
            service
        }
        currentService?.let { newListener.onServiceAvailable(it) }
    }

    fun unregisterListener(existing: AccessibilityServiceListener) {
        synchronized(lock) {
            listeners.remove(existing)
        }
    }

    internal fun bind(newService: LyreAccessibilityService) {
        val listenerSnapshot = synchronized(lock) {
            service = newService
            listeners.toList()
        }
        listenerSnapshot.forEach { it.onServiceAvailable(newService) }
    }

    internal fun unbind(oldService: LyreAccessibilityService) {
        val listenerSnapshot = synchronized(lock) {
            if (service == oldService) {
                service = null
                listeners.toList()
            } else {
                emptyList()
            }
        }
        listenerSnapshot.forEach { it.onServiceUnavailable() }
    }
}

