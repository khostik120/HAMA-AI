package com.hama.ai

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private val navy = Color.rgb(12, 18, 34)
    private val panel = Color.rgb(24, 33, 54)
    private val accent = Color.rgb(91, 124, 250)
    private val muted = Color.rgb(170, 181, 202)
    private val apiUrl = "https://raspy-pine-732d.afhhanking222q.workers.dev/chat"
    private val prefs by lazy { getSharedPreferences("hama_ai", MODE_PRIVATE) }
    private val languages = listOf("Auto-detect", "پښتو", "دري", "English", "العربية", "اردو", "فارسی", "हिन्दी", "বাংলা", "中文", "日本語", "한국어", "Türkçe", "Русский", "Français", "Deutsch", "Español", "Bahasa Indonesia", "Bahasa Melayu", "தமிழ்", "తెలుగు", "Italiano", "Português", "Nederlands", "Polski", "Українська", "Ελληνικά", "עברית", "Svenska", "Norsk", "Dansk", "Suomi", "Čeština", "Română", "Magyar", "Български", "Kiswahili", "ไทย", "Tiếng Việt", "नेपाली", "O‘zbekcha")

    private lateinit var root: LinearLayout
    private lateinit var messages: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var input: EditText
    private lateinit var sendButton: Button
    private lateinit var languagePicker: Spinner
    private lateinit var speech: TextToSpeech
    private var speechReady = false
    private var recognizer: SpeechRecognizer? = null
    private var selectedLanguage = "Auto-detect"
    private var currentChatId = System.currentTimeMillis().toString()
    private val chatMessages = mutableListOf<JSONObject>()
    private var waitingRow: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = navy
        window.navigationBarColor = navy
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        speech = TextToSpeech(this, this)
        showChat()
    }

    override fun onInit(status: Int) {
        speechReady = status == TextToSpeech.SUCCESS
        if (speechReady) speech.language = Locale.getDefault()
    }

    private fun baseScreen(titleText: String, subtitleText: String? = null) {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 10, 16, 8)
            setBackgroundColor(navy)
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(4, 8, 4, 12)
        }
        header.addView(TextView(this).apply {
            text = titleText
            textSize = 27f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        })
        if (subtitleText != null) header.addView(TextView(this).apply {
            text = subtitleText
            textSize = 13f
            setTextColor(muted)
            gravity = Gravity.CENTER
        })
        root.addView(header)
        setContentView(root)
    }

    private fun showChat() {
        baseScreen("HAMA AI", "ستاسو څو ژبی هوښیار مرستیال")
        root.addView(TextView(this).apply {
            text = "● آنلاین څو ژبی AI"
            textSize = 12f
            setTextColor(Color.rgb(112, 220, 160))
            setPadding(2, 0, 0, 6)
        })
        languagePicker = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_item, languages).also {
                it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            setSelection(languages.indexOf(selectedLanguage).coerceAtLeast(0))
            onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    selectedLanguage = languages[position]
                }
            }
        }
        root.addView(languagePicker, LinearLayout.LayoutParams(-1, -2))

        scroll = ScrollView(this)
        messages = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 8, 0, 12)
        }
        scroll.addView(messages)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        if (chatMessages.isEmpty()) {
            addMessage("سلام! زه HAMA AI یم. خپله ژبه وټاکئ یا Auto-detect پرېږدئ.", false, true)
        } else {
            chatMessages.forEach { item -> addMessage(item.optString("text"), item.optBoolean("user"), false) }
        }

        val composer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(7, 3, 4, 3)
            setBackgroundColor(panel)
        }
        input = EditText(this).apply {
            hint = "پیغام ولیکئ..."
            setTextColor(Color.WHITE)
            setHintTextColor(muted)
            textSize = 16f
            background = null
            setPadding(8, 8, 8, 8)
            maxLines = 4
        }
        composer.addView(input, LinearLayout.LayoutParams(0, -2, 1f))
        val mic = smallButton("🎙") { startVoiceInput() }
        composer.addView(mic)
        sendButton = smallButton("ولېږه") { sendMessage() }
        composer.addView(sendButton)
        root.addView(composer)
        root.addView(bottomNav("chat"))
    }

    private fun smallButton(label: String, action: () -> Unit): Button = Button(this).apply {
        text = label
        textSize = 12f
        isAllCaps = false
        setTextColor(Color.WHITE)
        setBackgroundColor(accent)
        setPadding(5, 0, 5, 0)
        setOnClickListener { action() }
    }

    private fun addMessage(text: String, fromUser: Boolean, save: Boolean): LinearLayout {
        val bubble = TextView(this).apply {
            this.text = text
            textSize = 16f
            setTextColor(Color.WHITE)
            setPadding(15, 11, 15, 11)
            setBackgroundColor(if (fromUser) accent else panel)
        }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = if (fromUser) Gravity.END else Gravity.START
            setPadding(0, 5, 0, 5)
            addView(bubble, LinearLayout.LayoutParams((resources.displayMetrics.widthPixels * 0.78f).toInt(), -2))
        }
        messages.addView(row)
        if (save) {
            chatMessages.add(JSONObject().put("text", text).put("user", fromUser))
            saveCurrentChat()
        }
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
        return row
    }

    private fun sendMessage() {
        val message = input.text.toString().trim()
        if (message.isEmpty()) return
        addMessage(message, true, true)
        input.text.clear()
        sendButton.isEnabled = false
        sendButton.text = "..."
        waitingRow = addMessage("ځواب ترلاسه کوم...", false, false)
        Thread {
            val answer = try {
                val connection = URL(apiUrl).openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.connectTimeout = 20000
                connection.readTimeout = 90000
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                val body = JSONObject().put("message", message).put("language", selectedLanguage)
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val raw = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                connection.disconnect()
                val json = JSONObject(raw)
                if (code !in 200..299) throw IllegalStateException(json.optString("error", "د سرور تېروتنه"))
                json.optString("reply", "ځواب ترلاسه نه شو.")
            } catch (e: Exception) {
                e.message ?: "انټرنېټ یا سرور کې ستونزه ده."
            }
            runOnUiThread {
                waitingRow?.let { messages.removeView(it) }
                waitingRow = null
                addMessage(answer, false, true)
                sendButton.isEnabled = true
                sendButton.text = "ولېږه"
            }
        }.start()
    }

    private fun startVoiceInput() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "ستاسو په موبایل کې غږ پېژندنه نشته.", Toast.LENGTH_LONG).show()
            return
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 501)
            return
        }
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { Toast.makeText(this@MainActivity, "خبرې وکړئ...", Toast.LENGTH_SHORT).show() }
            override fun onResults(results: Bundle?) {
                val words = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!words.isNullOrBlank()) input.setText(words)
                recognizer?.destroy()
                recognizer = null
            }
            override fun onError(error: Int) {
                Toast.makeText(this@MainActivity, "غږ ونه پېژندل شو. بیا هڅه وکړئ.", Toast.LENGTH_SHORT).show()
                recognizer?.destroy()
                recognizer = null
            }
            override fun onBeginningOfSpeech() {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onRmsChanged(rmsdB: Float) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        recognizer?.startListening(intent)
    }

    private fun bottomNav(active: String): LinearLayout {
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 2)
        }
        listOf(Triple("چټ", "chat") { showChat() }, Triple("تاریخچه", "history") { showHistory() },
            Triple("پروفایل", "profile") { showProfile() }, Triple("تنظیمات", "settings") { showSettings() })
            .forEach { item ->
                val b = TextView(this).apply {
                    text = item.first
                    gravity = Gravity.CENTER
                    textSize = 12f
                    setTextColor(if (active == item.second) Color.WHITE else muted)
                    setPadding(8, 8, 8, 8)
                    setOnClickListener { item.third() }
                }
                nav.addView(b, LinearLayout.LayoutParams(0, -2, 1f))
            }
        return nav
    }

    private fun showHistory() {
        baseScreen("د خبرو تاریخچه", "خپل پخواني چټونه بېرته پرانیزئ")
        val list = loadChats()
        if (list.length() == 0) root.addView(TextView(this).apply {
            text = "تر اوسه کوم پخوانی چټ نشته."
            setTextColor(Color.WHITE)
            textSize = 16f
        })
        for (i in list.length() - 1 downTo 0) {
            val item = list.optJSONObject(i) ?: continue
            val row = TextView(this).apply {
                text = "💬  " + item.optString("title", "نوی چټ") + "\n" + item.optString("date", "")
                textSize = 15f
                setTextColor(Color.WHITE)
                setPadding(14, 14, 14, 14)
                setBackgroundColor(panel)
                setOnClickListener { openChat(item) }
                setOnLongClickListener {
                    AlertDialog.Builder(this@MainActivity).setMessage("دا چټ حذف شي؟")
                        .setNegativeButton("نه", null).setPositiveButton("هو") { _, _ ->
                            list.remove(i); prefs.edit().putString("chats", list.toString()).apply(); showHistory()
                        }.show()
                    true
                }
            }
            val lp = LinearLayout.LayoutParams(-1, -2); lp.setMargins(0, 5, 0, 5)
            root.addView(row, lp)
        }
        root.addView(smallButton("＋ نوی چټ") { newChat() })
        root.addView(bottomNav("history"))
    }

    private fun openChat(item: JSONObject) {
        currentChatId = item.optString("id", System.currentTimeMillis().toString())
        selectedLanguage = item.optString("language", "Auto-detect")
        chatMessages.clear()
        val arr = item.optJSONArray("messages") ?: JSONArray()
        for (i in 0 until arr.length()) arr.optJSONObject(i)?.let { chatMessages.add(it) }
        showChat()
    }

    private fun newChat() {
        currentChatId = System.currentTimeMillis().toString()
        chatMessages.clear()
        showChat()
    }

    private fun showProfile() {
        baseScreen("پروفایل", "ستاسو د HAMA AI حساب")
        val name = prefs.getString("profile_name", "HAMA AI کاروونکی") ?: "HAMA AI کاروونکی"
        root.addView(TextView(this).apply {
            text = "👤\n$name\n\nدا پروفایل په همدې موبایل کې ساتل کېږي."
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(15, 25, 15, 25)
        })
        root.addView(smallButton("د نوم بدلول") {
            val field = EditText(this); field.setText(name)
            AlertDialog.Builder(this).setTitle("ستاسو نوم").setView(field)
                .setNegativeButton("لغوه", null).setPositiveButton("ساتل") { _, _ ->
                    prefs.edit().putString("profile_name", field.text.toString()).apply(); showProfile()
                }.show()
        })
        root.addView(bottomNav("profile"))
    }

    private fun showSettings() {
        baseScreen("تنظیمات", "د HAMA AI شخصي تنظیمات")
        root.addView(TextView(this).apply {
            text = "د ځواب ژبه"
            textSize = 15f
            setTextColor(Color.WHITE)
        })
        val picker = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_item, languages).also {
                it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            setSelection(languages.indexOf(selectedLanguage).coerceAtLeast(0))
            onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    selectedLanguage = languages[position]
                }
            }
        }
        root.addView(picker)
        root.addView(smallButton("د ځواب غږ ازموینه") {
            if (speechReady) speech.speak("سلام! زه HAMA AI یم.", TextToSpeech.QUEUE_FLUSH, null, "hama-test")
            else Toast.makeText(this, "غږیز سیستم چمتو نه دی.", Toast.LENGTH_SHORT).show()
        })
        root.addView(smallButton("ټولې خبرې حذفول") {
            AlertDialog.Builder(this).setMessage("ایا د خبرو ټوله تاریخچه حذف کړئ؟")
                .setNegativeButton("لغوه", null).setPositiveButton("حذف") { _, _ ->
                    prefs.edit().remove("chats").apply(); chatMessages.clear(); showHistory()
                }.show()
        })
        root.addView(TextView(this).apply {
            text = "نسخه 1.1\nHAMA AI — څو ژبی هوښیار مرستیال"
            textSize = 12f
            setTextColor(muted)
            setPadding(0, 20, 0, 10)
        })
        root.addView(bottomNav("settings"))
    }

    private fun loadChats(): JSONArray = try { JSONArray(prefs.getString("chats", "[]")) } catch (_: Exception) { JSONArray() }

    private fun saveCurrentChat() {
        if (chatMessages.isEmpty()) return
        val all = loadChats()
        val item = JSONObject()
            .put("id", currentChatId)
            .put("title", chatMessages.firstOrNull()?.optString("text")?.take(36) ?: "نوی چټ")
            .put("date", java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(java.util.Date()))
            .put("language", selectedLanguage)
            .put("messages", JSONArray(chatMessages))
        var found = false
        for (i in 0 until all.length()) {
            if (all.optJSONObject(i)?.optString("id") == currentChatId) {
                all.put(i, item); found = true; break
            }
        }
        if (!found) all.put(item)
        prefs.edit().putString("chats", all.toString()).apply()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 501 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) startVoiceInput()
        else if (requestCode == 501) Toast.makeText(this, "د غږ لپاره اجازه اړینه ده.", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        recognizer?.destroy()
        if (::speech.isInitialized) speech.shutdown()
        super.onDestroy()
    }
}
