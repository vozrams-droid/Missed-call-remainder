package com.example.missedcallreminder

object CallTracker {
    private val callMap = mutableMapOf<String, MutableList<Long>>()
    private val pendingMissedCalls = mutableListOf<String>()

    // Window: 10 minutes (600,000 ms)
    private const val ESCALATION_WINDOW_MS = 10 * 60 * 1000L

    @Synchronized
    fun recordCall(incomingNumber: String?): Boolean {
        if (incomingNumber.isNullOrEmpty()) return false

        val currentTime = System.currentTimeMillis()
        val history = callMap.getOrPut(incomingNumber) { mutableListOf() }

        // Remove calls older than 10 minutes
        history.removeAll { currentTime - it > ESCALATION_WINDOW_MS }
        history.add(currentTime)

        if (!pendingMissedCalls.contains(incomingNumber)) {
            pendingMissedCalls.add(incomingNumber)
        }

        // Return true if caller hit 3+ calls within window
        return history.size >= 3
    }

    @Synchronized
    fun getPendingMissedCount(): Int = pendingMissedCalls.size

    @Synchronized
    fun clearPending() {
        pendingMissedCalls.clear()
    }
}
