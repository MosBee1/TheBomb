package io.github.mosbee1.thebomb.util

fun interface TimeProvider {
    fun nowMillis(): Long
}

object SystemTimeProvider : TimeProvider {
    override fun nowMillis(): Long = System.currentTimeMillis()
}
