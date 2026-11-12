package imgui.example.android

import android.app.NativeActivity
import android.os.Bundle
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Color
import android.os.Build
import android.provider.Settings
import android.net.Uri
import android.view.WindowManager
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.view.KeyEvent
import android.widget.TextView
import android.widget.Toast
import java.util.concurrent.LinkedBlockingQueue

class MainActivity : NativeActivity() {
    private var floatingButton: TextView? = null
    private var windowManager: WindowManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName"))
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        if (Settings.canDrawOverlays(this)) showFloatingButton()
    }

    private fun showFloatingButton() {
        if (floatingButton != null) return
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val button = TextView(this)
        button.text = "N"
        button.textSize = 22f
        button.setTextColor(Color.WHITE)
        button.gravity = Gravity.CENTER
        button.setBackgroundColor(Color.rgb(15, 100, 220))
        val type = if (Build.VERSION.SDK_INT >= 26)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else WindowManager.LayoutParams.TYPE_PHONE
        val params = WindowManager.LayoutParams(
            52, 52, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 30
        params.y = 250
        var startX = 0
        var startY = 0
        var downX = 0f
        var downY = 0f
        var moved = false
        button.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    downX = event.rawX
                    downY = event.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - downX).toInt()
                    val dy = (event.rawY - downY).toInt()
                    if (kotlin.math.abs(dx) > 5 || kotlin.math.abs(dy) > 5) moved = true
                    params.x = startX + dx
                    params.y = startY + dy
                    windowManager?.updateViewLayout(button, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) {
                        val launch = packageManager.getLaunchIntentForPackage(packageName)
                        launch?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                        if (launch != null) startActivity(launch)
                    }
                    true
                }
                else -> true
            }
        }
        floatingButton = button
        windowManager?.addView(button, params)
    }

    override fun onDestroy() {
        floatingButton?.let { windowManager?.removeView(it) }
        floatingButton = null
        super.onDestroy()
    }

    fun showSoftInput() {
        val manager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        manager.showSoftInput(window.decorView, 0)
    }

    fun hideSoftInput() {
        val manager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        manager.hideSoftInputFromWindow(window.decorView.windowToken, 0)
    }

    private val unicodeCharacterQueue = LinkedBlockingQueue<Int>()

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            unicodeCharacterQueue.offer(event.getUnicodeChar(event.metaState))
        }
        return super.dispatchKeyEvent(event)
    }

    fun pollUnicodeChar(): Int {
        return unicodeCharacterQueue.poll() ?: 0
    }
}
