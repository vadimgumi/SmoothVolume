package com.example.smoothvolume

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : Activity() {

    private val SHIZUKU_REQUEST_CODE = 1001
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        status = TextView(this).apply { text = "Статус: ожидание" }
        val grantBtn = Button(this).apply { text = "Выдать разрешение через Shizuku" }
        val a11yBtn = Button(this).apply { text = "Открыть настройки Спец. возможностей" }

        grantBtn.setOnClickListener { requestShizukuAndGrant() }
        a11yBtn.setOnClickListener {
            startActivity(android.content.Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        layout.addView(status)
        layout.addView(grantBtn)
        layout.addView(a11yBtn)
        setContentView(layout)
    }

    private fun requestShizukuAndGrant() {
        if (!Shizuku.pingBinder()) {
            status.text = "Shizuku не запущен. Открой приложение Shizuku и активируй сервис."
            return
        }

        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            Shizuku.requestPermission(SHIZUKU_REQUEST_CODE)
            status.text = "Запрошено разрешение Shizuku, повтори нажатие после подтверждения."
            return
        }

        grantCaptureAudioOutput()
    }

    /**
     * Выполняем "pm grant <package> android.permission.CAPTURE_AUDIO_OUTPUT"
     * от имени shell через Shizuku.newProcess — именно так это делает RootlessJamesDSP.
     * На части сборок shell-идентификатору разрешено выдавать это permission,
     * т.к. исторически оно использовалось для отладки/тестирования (adb).
     * Если производитель прошивки закрыл эту лазейку — команда завершится с ошибкой,
     * и тогда без root этот путь не сработает.
     */
    private fun grantCaptureAudioOutput() {
        try {
            val cmd = arrayOf(
                "pm", "grant", packageName, "android.permission.CAPTURE_AUDIO_OUTPUT"
            )
            val process = Shizuku.newProcess(cmd, null, null)
            val output = BufferedReader(InputStreamReader(process.inputStream)).readText()
            val error = BufferedReader(InputStreamReader(process.errorStream)).readText()
            process.waitFor()

            status.text = if (error.isBlank()) {
                "Разрешение выдано. Теперь включи Accessibility-сервис и перезапусти его."
            } else {
                "Ошибка: $error"
            }
        } catch (e: Exception) {
            status.text = "Не удалось выполнить pm grant: ${e.message}"
        }
    }
}
