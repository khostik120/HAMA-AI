
package com.hama.ai

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.widget.TextView
import android.widget.LinearLayout

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(30, 60, 30, 30)
        layout.setBackgroundColor(Color.WHITE)

        val title = TextView(this)
        title.text = "Welcome to HAMA AI"
        title.textSize = 28f
        title.setTextColor(Color.BLACK)

        val subtitle = TextView(this)
        subtitle.text = "Your AI Assistant"
        subtitle.textSize = 18f
        subtitle.setTextColor(Color.DKGRAY)

        layout.addView(title)
        layout.addView(subtitle)

        setContentView(layout)
    }
}
