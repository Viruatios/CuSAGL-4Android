package com.culoo.cusagl_4android.accessibility

interface AccessibilityServiceListener {
    fun onServiceAvailable(service: LyreAccessibilityService)
    fun onServiceUnavailable()
}

object AccessibilityServiceBridge {
    private val lock = Any()
    @Volatile
    private var service: LyreAccessibilityService? = null
    @Volatile
    private var listener: AccessibilityServiceListener? = null

    fun getService(): LyreAccessibilityService? = service

    fun isConnected(): Boolean = service != null

    fun registerListener(newListener: AccessibilityServiceListener) {
        synchronized(lock) {
            listener = newListener
            service?.let { newListener.onServiceAvailable(it) }
        }
    }

    fun unregisterListener(existing: AccessibilityServiceListener) {
        synchronized(lock) {
            if (listener == existing) {
                listener = null
            }
        }
    }

    internal fun bind(newService: LyreAccessibilityService) {
        synchronized(lock) {
            service = newService
            listener?.onServiceAvailable(newService)
        }
    }

    internal fun unbind(oldService: LyreAccessibilityService) {
        synchronized(lock) {
            if (service == oldService) {
                service = null
                listener?.onServiceUnavailable()
            }
        }
    }
}

