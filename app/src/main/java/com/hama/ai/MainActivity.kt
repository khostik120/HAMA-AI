package com.hama.ai

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : Activity() {

    private lateinit var messages: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var input: EditText
    private lateinit var send: Button
    private val navy = Color.rgb(12, 18, 34)
    private val panel = Color.rgb(24, 33, 54)
    private val accent = Color.rgb(91, 124, 250)

    // Replace this URL after deploying the Cloudflare Worker.
    private val apiUrl = "https://YOUR-WORKER-NAME.YOUR-SUBDOMAIN.workers.dev/chat"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = navy
        window.navigationBarColor = navy
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 12, 18, 8)
            setBackgroundColor(navy)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(8, 8, 8, 14)
        }

        val title = TextView(this).apply {
            text = "HAMA AI"
            textSize = 30f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        header.addView(title)

        val subtitle = TextView(this).apply {
            text = "ستاسو هوښیار مرستیال"
            textSize = 14f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 4, 0, 0)
        }
        header.addView(subtitle)
        root.addView(header)

        val status = TextView(this).apply {
            text = "●  د OpenAI چټ"
            textSize = 12f
            setTextColor(Color.rgb(112, 220, 160))
            setPadding(4, 0, 0, 8)
        }
        root.addView(status)

        scroll = ScrollView(this)
        messages = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 8, 0, 12)
        }
        scroll.addView(messages)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        addMessage("سلام! زه HAMA AI یم. د OpenAI په مرسته به ستا پوښتنو ته ځواب درکوم.", false)

        val hint = TextView(this).apply {
            text = "یوه پوښتنه ولیکئ"
            textSize = 12f
            setTextColor(Color.LTGRAY)
            setPadding(2, 6, 2, 4)
        }
        root.addView(hint)

        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(8, 4, 5, 4)
            setBackgroundColor(panel)
        }

        input = EditText(this).apply {
            setHint("پیغام ولیکئ...")
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            textSize = 16f
            setSingleLine(true)
            background = null
            setPadding(8, 8, 8, 8)
        }
        bottom.addView(input, LinearLayout.LayoutParams(0, -2, 1f))

        send = Button(this).apply {
            text = "ولېږه"
            textSize = 13f
            setTextColor(Color.WHITE)
            isAllCaps = false
            setBackgroundColor(accent)
        }
        bottom.addView(send)
        root.addView(bottom)

        send.setOnClickListener { sendMessage() }
        setContentView(root)
    }

    private fun sendMessage() {
        val message = input.text.toString().trim()
        if (message.isEmpty()) return

        addMessage(message, true)
        input.text.clear()
        send.isEnabled = false
        send.text = "..."
        val waiting = addMessage("ځواب ترلاسه کوم...", false)

        Thread {
            var reply: String
            try {
                if (apiUrl.contains("YOUR-WORKER-NAME")) {
                    throw IllegalStateException("سرور لا نه دی وصل شوی. د Worker URL باید تنظیم شي.")
                }
                val connection = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 20000
                    readTimeout = 60000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                }
                connection.outputStream.use {
                    it.write(JSONObject().put("message", message).toString().toByteArray(Charsets.UTF_8))
                }
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val responseText = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val json = JSONObject(responseText)
                if (code !in 200..299) {
                    throw IllegalStateException(json.optString("error", "د AI خدمت کې ستونزه ده."))
                }
                reply = json.optString("reply", "ځواب ترلاسه نه شو.")
                connection.disconnect()
            } catch (e: Exception) {
                reply = e.message ?: "انټرنېټ یا سرور کې ستونزه ده."
            }

            runOnUiThread {
                messages.removeView(waiting)
                addMessage(reply, false)
                send.isEnabled = true
                send.text = "ولېږه"
            }
        }.start()
    }

    private fun addMessage(text: String, fromUser: Boolean): LinearLayout {
        val bubble = TextView(this).apply {
            this.text = text
            textSize = 16f
            setTextColor(Color.WHITE)
            setPadding(16, 12, 16, 12)
            setBackgroundColor(if (fromUser) accent else panel)
        }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = if (fromUser) Gravity.END else Gravity.START
            setPadding(0, 5, 0, 5)
            addView(bubble, LinearLayout.LayoutParams(
                (resources.displayMetrics.widthPixels * 0.78f).toInt(), -2
            ))
        }
        messages.addView(row)
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
        return row
    }
}
