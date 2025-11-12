package com.example.focusgate.overlay

import java.util.concurrent.atomic.AtomicBoolean

object OverlayState {

    private val showing = AtomicBoolean(false)

    fun tryAcquire(): Boolean = showing.compareAndSet(false, true)

    fun release() {
        showing.set(false)
    }

    fun isShowing(): Boolean = showing.get()
}
