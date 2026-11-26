

package com.hama.ai

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.*

class MainActivity : Activity() {

    private lateinit var messages: LinearLayout
    private lateinit var scroll: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(20, 30, 20, 15)
        root.setBackgroundColor(Color.rgb(15, 20, 38))

        val title = TextView(this)
        title.text = "HAMA AI"
        title.textSize = 28f
        title.setTextColor(Color.WHITE)
        title.gravity = Gravity.CENTER
        root.addView(title)

        val subtitle = TextView(this)
        subtitle.text = "Your Smart AI Assistant"
        subtitle.textSize = 14f
        subtitle.setTextColor(Color.LTGRAY)
        subtitle.gravity = Gravity.CENTER
        root.addView(subtitle)

        scroll = ScrollView(this)
        messages = LinearLayout(this)
        messages.orientation = LinearLayout.VERTICAL
        messages.setPadding(0, 25, 0, 15)

        scroll.addView(messages)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                -1, 0, 1f
            )
        )

        addMessage("سلام! زه HAMA AI یم. څنګه مرسته درسره وکړم؟")

        val input = EditText(this)
        input.hint = "خپل پیغام ولیکئ..."
        input.setTextColor(Color.WHITE)
        input.setHintTextColor(Color.LTGRAY)
        input.setSingleLine(true)
        input.setPadding(15, 10, 15, 10)

        val send = Button(this)
        send.text = "Send"

        val bottom = LinearLayout(this)
        bottom.orientation = LinearLayout.HORIZONTAL

        bottom.addView(input,
            LinearLayout.LayoutParams(0, -2, 1f))
        bottom.addView(send)

        root.addView(bottom)

        send.setOnClickListener {
            val text = input.text.toString().trim()

            if (text.isNotEmpty()) {
                addMessage("You: $text")
                addMessage("HAMA AI: ستا پیغام ترلاسه شو!")
                input.text.clear()
            }
        }

        setContentView(root)
    }

    private fun addMessage(text: String) {
        val message = TextView(this)
        message.text = text
        message.textSize = 17f
        message.setTextColor(Color.WHITE)
        message.setPadding(15, 15, 15, 15)

        messages.addView(message)
        scroll.post {
            scroll.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }
}
