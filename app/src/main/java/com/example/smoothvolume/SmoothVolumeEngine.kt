package com.example.smoothvolume

import android.content.Context
import android.media.AudioManager
import android.media.audiofx.LoudnessEnhancer
import android.util.Log

/**
 * Идея: система квантует громкость до N шагов (обычно 15 для STREAM_MUSIC).
 * Между реальными шагами N и N+1 мы добавляем SUBSTEPS промежуточных
 * уровней за счёт программного gain через LoudnessEnhancer, подключённый
 * к глобальной audio session (0) — это возможно только если приложению
 * выдано разрешение CAPTURE_AUDIO_OUTPUT (см. MainActivity/ShizukuGrant).
 *
 * Логика одной "нажатой" кнопки:
 *  - если внутри текущего реального шага ещё есть запас под-шагов — просто
 *    подстраиваем gain эффекта (звук меняется плавно, без щелчка)
 *  - если под-шаги кончились — переключаем реальный системный шаг на ±1
 *    и сбрасываем gain в 0, чтобы не было двойного усиления
 */
class SmoothVolumeEngine(private val context: Context) {

    companion object {
        private const val TAG = "SmoothVolumeEngine"
        const val SUBSTEPS = 4          // сколько промежуточных уровней на 1 системный шаг
        const val MAX_GAIN_MB = 600     // макс. добавка громкости в милибелах (~6 dB) на под-шаг вверх
        const val GLOBAL_SESSION = 0    // 0 = системный микс (нужен CAPTURE_AUDIO_OUTPUT)
    }

    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var enhancer: LoudnessEnhancer? = null
    private var subStepIndex = 0 // 0..SUBSTEPS, где SUBSTEPS/2 — "нейтральный" центр

    fun start() {
        try {
            enhancer = LoudnessEnhancer(GLOBAL_SESSION).apply {
                setTargetGain(0)
                enabled = true
            }
            subStepIndex = SUBSTEPS / 2
            Log.i(TAG, "LoudnessEnhancer подключён к глобальной сессии")
        } catch (e: Exception) {
            // Если CAPTURE_AUDIO_OUTPUT не выдан — сюда упадём с SecurityException
            Log.e(TAG, "Не удалось подключить эффект к глобальной сессии: ${e.message}")
            enhancer = null
        }
    }

    fun stop() {
        enhancer?.release()
        enhancer = null
    }

    /** true = кнопку "съели" сами, система громкость не трогает */
    fun onVolumeUp(): Boolean = adjust(direction = 1)

    fun onVolumeDown(): Boolean = adjust(direction = -1)

    private fun adjust(direction: Int): Boolean {
        val eff = enhancer ?: return false // эффект недоступен — отдаём управление системе

        val half = SUBSTEPS / 2
        val nextIndex = subStepIndex + direction

        return if (nextIndex in 0..SUBSTEPS) {
            // остаёмся внутри текущего системного шага — просто плавно двигаем gain
            subStepIndex = nextIndex
            val gainMb = ((subStepIndex - half).toFloat() / half) * MAX_GAIN_MB
            eff.setTargetGain(gainMb.toInt())
            true
        } else {
            // под-шаги кончились — переключаем реальный системный шаг
            val stream = AudioManager.STREAM_MUSIC
            val current = audioManager.getStreamVolume(stream)
            val max = audioManager.getStreamMaxVolume(stream)
            val newVol = (current + direction).coerceIn(0, max)
            audioManager.setStreamVolume(stream, newVol, 0)

            // сбрасываем под-шаг в центр, gain обнуляем — переход должен быть незаметным,
            // т.к. реальный шаг уже сдвинул громкость на одну "ступеньку"
            subStepIndex = half
            eff.setTargetGain(0)
            true
        }
    }
}
