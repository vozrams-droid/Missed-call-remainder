package com.example.missedcallreminder

object CallTracker {
    private val callHistory = mutableMapOf<String, MutableList<Long>>()
    val pendingNumbers = mutableSetOf<String>()

    fun recordCall(number: String): Int {
        val now = System.currentTimeMillis()
        val list = callHistory.getOrPut(number) { mutableListOf() }
        // Keep calls inside a 10-minute rolling window
        list.removeAll { now - it > 10 * 60 * 1000 }
        list.add(now)
        return list.size
    }

    fun markPending(number: String) {
        pendingNumbers.add(number)
    }

    fun clear(number: String) {
        callHistory.remove(number)
        pendingNumbers.remove(number)
    }
}
