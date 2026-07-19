package com.example.smoothvolume

import android.accessibilityservice.AccessibilityService
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

class VolumeAccessibilityService : AccessibilityService() {

    private lateinit var engine: SmoothVolumeEngine

    override fun onServiceConnected() {
        super.onServiceConnected()
        engine = SmoothVolumeEngine(applicationContext)
        engine.start()
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        // Реагируем только на нажатие (ACTION_DOWN), отпускание игнорируем,
        // иначе будет двойное срабатывание на каждый клик
        if (event.action != KeyEvent.ACTION_DOWN) return false

        return when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> engine.onVolumeUp()
            KeyEvent.KEYCODE_VOLUME_DOWN -> engine.onVolumeDown()
            else -> false
        }
        // return true = событие "съедено", система не применяет свой стандартный шаг
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // не нужно для этой задачи, оставлено пустым
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        if (::engine.isInitialized) engine.stop()
    }
}
