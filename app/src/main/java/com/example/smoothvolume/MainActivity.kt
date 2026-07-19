package com.example.smoothvolume

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper

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

    private fun grantCaptureAudioOutput() {
        try {
            val serviceManagerClass = Class.forName("android.os.ServiceManager")
            val getServiceMethod = serviceManagerClass.getMethod("getService", String::class.java)
            val rawBinder = getServiceMethod.invoke(null, "package") as IBinder

            val wrappedBinder: IBinder = ShizukuBinderWrapper(rawBinder)

            val iPackageManagerStub = Class.forName("android.content.pm.IPackageManager\$Stub")
            val asInterfaceMethod = iPackageManagerStub.getMethod("asInterface", IBinder::class.java)
            val packageManager = asInterfaceMethod.invoke(null, wrappedBinder)

            val grantMethod = packageManager.javaClass.getMethod(
                "grantRuntimePermission",
                String::class.java,
                String::class.java,
                Int::class.javaPrimitiveType
            )
            grantMethod.invoke(
                packageManager,
                packageName,
                "android.permission.CAPTURE_AUDIO_OUTPUT",
                0
            )

            status.text = "Разрешение выдано. Теперь включи Accessibility-сервис и перезапусти его."
        } catch (e: Exception) {
            status.text = "Ошибка: ${e.message}"
        }
    }
}
