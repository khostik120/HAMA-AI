package com.hama.ai

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.*

class MainActivity : Activity() {

    private lateinit var messages: LinearLayout
    private lateinit var scroll: ScrollView
    private val navy = Color.rgb(12, 18, 34)
    private val panel = Color.rgb(24, 33, 54)
    private val accent = Color.rgb(91, 124, 250)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = navy
        window.navigationBarColor = navy

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 22, 18, 14)
            setBackgroundColor(navy)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(8, 12, 8, 20)
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
            text = "●  چمتو دی"
            textSize = 12f
            setTextColor(Color.rgb(112, 220, 160))
            setPadding(4, 0, 0, 10)
        }
        root.addView(status)

        scroll = ScrollView(this)
        messages = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 8, 0, 16)
        }
        scroll.addView(messages)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        addMessage("سلام! زه HAMA AI یم. څنګه مرسته درسره وکړم؟", false)

        val hint = TextView(this).apply {
            text = "یوه پوښتنه ولیکئ"
            textSize = 12f
            setTextColor(Color.LTGRAY)
            setPadding(2, 8, 2, 6)
        }
        root.addView(hint)

        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(8, 5, 5, 5)
            setBackgroundColor(panel)
        }

        val input = EditText(this).apply {
            setHint("پیغام ولیکئ...")
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            textSize = 16f
            setSingleLine(true)
            background = null
            setPadding(8, 10, 8, 10)
        }
        bottom.addView(input, LinearLayout.LayoutParams(0, -2, 1f))

        val send = Button(this).apply {
            text = "ولېږه"
            textSize = 13f
            setTextColor(Color.WHITE)
            isAllCaps = false
            setBackgroundColor(accent)
        }
        bottom.addView(send)
        root.addView(bottom)

        send.setOnClickListener {
            val text = input.text.toString().trim()
            if (text.isNotEmpty()) {
                addMessage(text, true)
                addMessage("ستا پیغام ترلاسه شو. د ریښتیني AI ځواب لپاره به په بل پړاو کې د AI خدمت وصل کړو.", false)
                input.text.clear()
            }
        }

        setContentView(root)
    }

    private fun addMessage(text: String, fromUser: Boolean) {
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
    }
}
