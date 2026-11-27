package com.hama.ai

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
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
import android.view.inputmethod.EditorInfo
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
    private lateinit var speech: TextToSpeech
    private var speechReady = false
    private var speakReplies = false
    private var recognizer: SpeechRecognizer? = null
    private var selectedLanguage = "Auto-detect"
    private var currentChatId = System.currentTimeMillis().toString()
    private val chatMessages = mutableListOf<JSONObject>()
    private var waitingRow: View? = null
    private var pendingAttachmentText: String? = null
    private var pendingAttachmentName: String? = null
    @Volatile private var requestRunning = false
    @Volatile private var requestCancelled = false
    @Volatile private var activeConnection: HttpURLConnection? = null
    private val libraryItems get() = try { JSONArray(prefs.getString("library_items", "[]")) } catch (_: Exception) { JSONArray() }
    private val projectItems get() = try { JSONArray(prefs.getString("project_items", "[]")) } catch (_: Exception) { JSONArray() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        speakReplies = prefs.getBoolean("speak_replies", false)
        selectedLanguage = prefs.getString("selected_language", "Auto-detect") ?: "Auto-detect"
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

    private fun rounded(color: Int, radius: Float = 22f): android.graphics.drawable.GradientDrawable =
        android.graphics.drawable.GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius * resources.displayMetrics.density
        }

    private fun glossy(colors: IntArray, radius: Float = 22f, strokeColor: Int = Color.TRANSPARENT): android.graphics.drawable.GradientDrawable =
        android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.TL_BR, colors
        ).apply {
            cornerRadius = radius * resources.displayMetrics.density
            if (strokeColor != Color.TRANSPARENT) {
                setStroke((1.2f * resources.displayMetrics.density).toInt(), strokeColor)
            }
        }

    private fun baseScreen(titleText: String, subtitleText: String? = null) {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14, 8, 14, 8)
            background = glossy(intArrayOf(Color.rgb(3, 7, 20), Color.rgb(8, 19, 43), Color.rgb(2, 8, 24)), 0f)
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 5, 0, 12)
        }
        val menu = TextView(this).apply {
            text = "☰"
            textSize = 23f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = rounded(panel, 28f)
            setPadding(15, 10, 15, 10)
            setOnClickListener { showDrawer() }
        }
        header.addView(menu)
        val heading = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(8, 0, 8, 0)
        }
        heading.addView(TextView(this).apply {
            text = titleText
            textSize = 21f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        })
        if (subtitleText != null) heading.addView(TextView(this).apply {
            text = subtitleText
            textSize = 11f
            setTextColor(muted)
            gravity = Gravity.CENTER
        })
        header.addView(heading, LinearLayout.LayoutParams(0, -2, 1f))
        val newChat = TextView(this).apply {
            text = "＋"
            textSize = 25f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = rounded(panel, 28f)
            setPadding(12, 7, 12, 7)
            setOnClickListener { newChat() }
        }
        header.addView(newChat)
        root.addView(header)
        setContentView(root)
    }

    private fun showDrawer() {
        val dialog = AlertDialog.Builder(this).create()
        val panelView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 22, 22, 18)
            background = rounded(Color.rgb(29, 31, 36), 26f)
        }
        panelView.addView(TextView(this).apply {
            text = "HAMA AI"
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(4, 8, 4, 18)
        })
        val entries = listOf(
            "＋     نوی چټ" to { newChat() },
            "▤     تاریخچه" to { showHistory() },
            "▦     Library" to { showLibrary() },
            "▣     Projects" to { showProjects() },
            "⚙     تنظیمات" to { showSettings() },
            "◉     پروفایل" to { showProfile() }
        )
        entries.forEach { entry ->
            val row = TextView(this).apply {
                text = entry.first
                textSize = 17f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER_VERTICAL
                setPadding(12, 15, 12, 15)
                isClickable = true
                isFocusable = true
                setOnClickListener { dialog.dismiss(); entry.second() }
            }
            panelView.addView(row, LinearLayout.LayoutParams(-1, -2))
        }
        val close = TextView(this).apply {
            text = "بندول"
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(muted)
            setPadding(10, 14, 10, 8)
            setOnClickListener { dialog.dismiss() }
        }
        panelView.addView(close)
        dialog.setView(panelView, 18, 12, 18, 12)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun showLibrary() {
        baseScreen("Library", "ستاسو خوندي شوي فایلونه")
        root.addView(smallButton("＋ فایل اضافه کړئ") {
            val pick = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }
            startActivityForResult(pick, 602)
        })
        val items = libraryItems
        if (items.length() == 0) root.addView(TextView(this).apply {
            text = "تر اوسه فایل نشته. د فایل اضافه کولو تڼۍ وکاروئ."
            textSize = 15f; setTextColor(muted); setPadding(12, 24, 12, 24)
        })
        for (i in 0 until items.length()) {
            val item = items.optJSONObject(i) ?: continue
            val row = TextView(this).apply {
                text = "📄  " + item.optString("name", "فایل") + "\n" + item.optString("uri")
                textSize = 13f; setTextColor(Color.WHITE); setPadding(12, 14, 12, 14)
                setOnClickListener {
                    try { startActivity(Intent(Intent.ACTION_VIEW).setData(android.net.Uri.parse(item.optString("uri"))).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) }
                    catch (_: Exception) { Toast.makeText(this@MainActivity, "فایل نه خلاصیږي.", Toast.LENGTH_SHORT).show() }
                }
                setOnLongClickListener {
                    AlertDialog.Builder(this@MainActivity).setMessage("دا فایل له Library څخه لرې شي؟")
                        .setNegativeButton("نه", null).setPositiveButton("هو") { _, _ ->
                            items.remove(i); prefs.edit().putString("library_items", items.toString()).apply(); showLibrary()
                        }.show(); true
                }
            }
            root.addView(row)
        }
        root.addView(bottomNav("library"))
    }

    private fun showProjects() {
        baseScreen("Projects", "خپل کارونه منظم کړئ")
        root.addView(smallButton("＋ نوې پروژه") {
            val fields = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(8, 4, 8, 0)
            }
            val nameField = EditText(this).apply { hint = "د پروژې نوم" }
            val descriptionField = EditText(this).apply {
                hint = "د پروژې تشریح (اختیاري)"
                minLines = 2
                gravity = Gravity.TOP
            }
            fields.addView(nameField)
            fields.addView(descriptionField)
            AlertDialog.Builder(this).setTitle("نوې پروژه").setView(fields)
                .setNegativeButton("لغوه", null)
                .setPositiveButton("جوړول") { _, _ ->
                    val name = nameField.text.toString().trim()
                    if (name.isNotEmpty()) {
                        val all = projectItems
                        all.put(JSONObject()
                            .put("id", java.util.UUID.randomUUID().toString())
                            .put("name", name)
                            .put("description", descriptionField.text.toString().trim())
                            .put("created", System.currentTimeMillis())
                            .put("updated", System.currentTimeMillis()))
                        prefs.edit().putString("project_items", all.toString()).apply()
                        showProjects()
                    } else Toast.makeText(this, "د پروژې نوم ولیکئ.", Toast.LENGTH_SHORT).show()
                }.show()
        })
        val all = projectItems
        if (all.length() == 0) root.addView(TextView(this).apply {
            text = "تر اوسه پروژه نشته. خپله لومړۍ پروژه جوړه کړئ."
            textSize = 15f; setTextColor(muted); setPadding(12, 24, 12, 24)
        })
        for (i in 0 until all.length()) {
            val item = all.optJSONObject(i) ?: continue
            val row = TextView(this).apply {
                val description = item.optString("description")
                text = "▣  " + item.optString("name") +
                    (if (description.isNotBlank()) "\n$description" else "") +
                    "\n" + if (item.has("updated")) "تازه شوی: " + java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(java.util.Date(item.optLong("updated"))) else "پخوانۍ پروژه"
                textSize = 15f; setTextColor(Color.WHITE); setPadding(14, 16, 14, 16)
                background = rounded(panel, 16f)
                setOnClickListener { editProject(item) }
                setOnLongClickListener {
                    AlertDialog.Builder(this@MainActivity).setTitle(item.optString("name"))
                        .setItems(arrayOf("سمول", "شریکول", "حذفول")) { _, choice ->
                            when (choice) {
                                0 -> editProject(item)
                                1 -> shareProject(item)
                                2 -> AlertDialog.Builder(this@MainActivity).setMessage("پروژه حذف شي؟")
                                    .setNegativeButton("نه", null).setPositiveButton("حذف") { _, _ ->
                                        all.remove(i)
                                        prefs.edit().putString("project_items", all.toString()).apply()
                                        showProjects()
                                    }.show()
                            }
                        }.show()
                    true
                }
            }
            val lp = LinearLayout.LayoutParams(-1, -2); lp.setMargins(0, 6, 0, 6); root.addView(row, lp)
        }
        root.addView(bottomNav("projects"))
    }

    private fun editProject(item: JSONObject) {
        val fields = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(8, 4, 8, 0)
        }
        val nameField = EditText(this).apply {
            hint = "د پروژې نوم"
            setText(item.optString("name"))
        }
        val descriptionField = EditText(this).apply {
            hint = "د پروژې تشریح"
            minLines = 3
            gravity = Gravity.TOP
            setText(item.optString("description"))
        }
        fields.addView(nameField)
        fields.addView(descriptionField)
        AlertDialog.Builder(this).setTitle("پروژه سمول").setView(fields)
            .setNegativeButton("لغوه", null)
            .setPositiveButton("ساتل") { _, _ ->
                val name = nameField.text.toString().trim()
                if (name.isBlank()) {
                    Toast.makeText(this, "د پروژې نوم اړین دی.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val all = projectItems
                for (i in 0 until all.length()) {
                    val saved = all.optJSONObject(i) ?: continue
                    if ((item.optString("id").isNotBlank() && saved.optString("id") == item.optString("id")) ||
                        (item.optString("id").isBlank() && saved.optString("name") == item.optString("name") && saved.optLong("created") == item.optLong("created"))) {
                        saved.put("name", name)
                        saved.put("description", descriptionField.text.toString().trim())
                        saved.put("updated", System.currentTimeMillis())
                        item.put("name", name)
                        item.put("description", descriptionField.text.toString().trim())
                        item.put("updated", saved.optLong("updated"))
                        break
                    }
                }
                prefs.edit().putString("project_items", all.toString()).apply()
                showProjects()
                Toast.makeText(this, "پروژه تازه شوه.", Toast.LENGTH_SHORT).show()
            }.show()
    }

    private fun shareProject(item: JSONObject) {
        val content = buildString {
            append(item.optString("name", "HAMA AI Project")).append("\n\n")
            append(item.optString("description", "تشریح نشته")).append("\n")
            if (item.has("created")) append("جوړه شوې: ").append(item.optLong("created")).append("\n")
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, item.optString("name", "HAMA AI Project"))
            putExtra(Intent.EXTRA_TEXT, content)
        }
        startActivity(Intent.createChooser(intent, "پروژه شریکول"))
    }

    private fun welcomeMessage(): String = when (selectedLanguage) {
        "پښتو" -> "سلام! زه HAMA AI یم. څنګه مرسته درسره وکړم؟"
        "دري", "فارسی" -> "سلام! من HAMA AI هستم. چطور می‌توانم کمک کنم؟"
        "English" -> "Hello! I'm HAMA AI. How can I help you today?"
        "العربية" -> "مرحباً! أنا HAMA AI. كيف يمكنني مساعدتك؟"
        "اردو" -> "السلام علیکم! میں HAMA AI ہوں۔ میں آپ کی کیا مدد کر سکتا ہوں؟"
        "हिन्दी" -> "नमस्ते! मैं HAMA AI हूँ। मैं आपकी कैसे मदद कर सकता हूँ?"
        "বাংলা" -> "হ্যালো! আমি HAMA AI। কীভাবে সাহায্য করতে পারি?"
        "中文" -> "你好！我是 HAMA AI。有什么可以帮你？"
        "日本語" -> "こんにちは！HAMA AIです。どのようにお手伝いできますか？"
        "한국어" -> "안녕하세요! 저는 HAMA AI입니다. 무엇을 도와드릴까요?"
        "Türkçe" -> "Merhaba! Ben HAMA AI. Size nasıl yardımcı olabilirim?"
        "Русский" -> "Здравствуйте! Я HAMA AI. Чем могу помочь?"
        "Français" -> "Bonjour ! Je suis HAMA AI. Comment puis-je vous aider ?"
        "Deutsch" -> "Hallo! Ich bin HAMA AI. Wie kann ich Ihnen helfen?"
        "Español" -> "¡Hola! Soy HAMA AI. ¿Cómo puedo ayudarte?"
        "Bahasa Indonesia" -> "Halo! Saya HAMA AI. Ada yang bisa saya bantu?"
        "Bahasa Melayu" -> "Helo! Saya HAMA AI. Apa yang boleh saya bantu?"
        "Italiano" -> "Ciao! Sono HAMA AI. Come posso aiutarti?"
        "Português" -> "Olá! Sou HAMA AI. Como posso ajudar?"
        "Nederlands" -> "Hallo! Ik ben HAMA AI. Hoe kan ik je helpen?"
        "Polski" -> "Cześć! Jestem HAMA AI. W czym mogę pomóc?"
        "Українська" -> "Вітаю! Я HAMA AI. Чим можу допомогти?"
        "Ελληνικά" -> "Γεια σας! Είμαι το HAMA AI. Πώς μπορώ να βοηθήσω;"
        "עברית" -> "שלום! אני HAMA AI. איך אפשר לעזור?"
        "Svenska" -> "Hej! Jag är HAMA AI. Hur kan jag hjälpa dig?"
        "Norsk" -> "Hei! Jeg er HAMA AI. Hva kan jeg hjelpe deg med?"
        "Dansk" -> "Hej! Jeg er HAMA AI. Hvordan kan jeg hjælpe?"
        "Suomi" -> "Hei! Olen HAMA AI. Miten voin auttaa?"
        "Čeština" -> "Ahoj! Jsem HAMA AI. Jak vám mohu pomoci?"
        "Română" -> "Salut! Sunt HAMA AI. Cu ce te pot ajuta?"
        "Magyar" -> "Szia! HAMA AI vagyok. Miben segíthetek?"
        "Български" -> "Здравейте! Аз съм HAMA AI. С какво мога да помогна?"
        "Kiswahili" -> "Habari! Mimi ni HAMA AI. Ninawezaje kukusaidia?"
        "ไทย" -> "สวัสดี! ฉันคือ HAMA AI มีอะไรให้ช่วยไหม?"
        "Tiếng Việt" -> "Xin chào! Tôi là HAMA AI. Tôi có thể giúp gì cho bạn?"
        "नेपाली" -> "नमस्ते! म HAMA AI हुँ। म तपाईंलाई कसरी मद्दत गर्न सक्छु?"
        "O‘zbekcha" -> "Salom! Men HAMA AI. Sizga qanday yordam bera olaman?"
        else -> "سلام! زه HAMA AI یم. څنګه مرسته درسره وکړم؟"
    }

    private fun showChat() {
        baseScreen("HAMA AI", "ستاسو څو ژبی هوښیار مرستیال")
        root.addView(TextView(this).apply {
            text = "✦  HAMA AI  •  آنلاین څو ژبی AI"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(80, 226, 255))
            setPadding(10, 7, 10, 10)
            background = rounded(Color.rgb(10, 30, 58), 18f)
        })

        scroll = ScrollView(this).apply { clipToPadding = false }
        messages = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 8, 0, 12)
        }
        scroll.addView(messages)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        if (chatMessages.isEmpty()) addMessage(welcomeMessage(), false, false)
        else chatMessages.forEach { item -> addMessage(item.optString("text"), item.optBoolean("user"), false) }

        // Text input above a separate row of voice, attachment and send controls.
        val composer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(5, 7, 5, 5)
            background = glossy(intArrayOf(Color.rgb(13, 25, 51), Color.rgb(5, 12, 29)), 25f, Color.rgb(42, 145, 230))
            elevation = 20f * resources.displayMetrics.density
        }
        val inputPanel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(9, 3, 9, 3)
            background = glossy(intArrayOf(Color.rgb(27, 47, 83), Color.rgb(12, 23, 47)), 19f, Color.rgb(65, 157, 239))
        }
        input = EditText(this).apply {
            hint = "پیغام ولیکئ..."
            setTextColor(Color.WHITE)
            setHintTextColor(muted)
            textSize = 16f
            background = null
            setPadding(9, 10, 9, 10)
            minLines = 1
            maxLines = 6
            isSingleLine = false
            setHorizontallyScrolling(false)
            gravity = Gravity.TOP or Gravity.START
            imeOptions = EditorInfo.IME_ACTION_SEND or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEND) { sendMessage(); true } else false
            }
        }
        inputPanel.addView(input, LinearLayout.LayoutParams(0, -2, 1f))
        composer.addView(inputPanel, LinearLayout.LayoutParams(-1, -2))

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(3, 6, 3, 1)
        }
        fun actionButton(symbol: String, description: String, action: () -> Unit): TextView =
            TextView(this).apply {
                text = symbol
                contentDescription = description
                textSize = 20f
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                background = glossy(intArrayOf(Color.rgb(38, 91, 158), Color.rgb(13, 34, 70)), 15f, Color.rgb(73, 169, 245))
                elevation = 7f * resources.displayMetrics.density
                setPadding(0, 0, 0, 0)
                setOnClickListener { action() }
            }

        val attach = actionButton("＋", "فایل یا انځور ضمیمه کول") {
            val pick = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }
            startActivityForResult(pick, 603)
        }
        actions.addView(attach, LinearLayout.LayoutParams(46, 44).apply { marginEnd = 7 })

        val mic = actionButton("🎙", "غږ ثبتول او متن ته اړول") { startVoiceInput() }
        actions.addView(mic, LinearLayout.LayoutParams(48, 44).apply { marginEnd = 7 })

        val voiceReply = actionButton(if (speakReplies) "🔊" else "🔈", "د AI ځوابونه په غږ اورېدل") {
            speakReplies = !speakReplies
            prefs.edit().putBoolean("speak_replies", speakReplies).apply()
            (actions.getChildAt(2) as TextView).text = if (speakReplies) "🔊" else "🔈"
        }
        actions.addView(voiceReply, LinearLayout.LayoutParams(46, 44).apply { marginEnd = 7 })
        actions.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))

        sendButton = Button(this).apply {
            text = "↑"
            contentDescription = "پیغام لېږل"
            textSize = 22f
            isAllCaps = false
            minWidth = 0
            minimumWidth = 0
            minHeight = 0
            minimumHeight = 0
            setTextColor(Color.WHITE)
            background = glossy(intArrayOf(Color.rgb(28, 154, 255), Color.rgb(16, 75, 190)), 16f, Color.rgb(125, 218, 255))
            elevation = 10f * resources.displayMetrics.density
            setPadding(0, 0, 0, 0)
            setOnClickListener { sendMessage() }
        }
        actions.addView(sendButton, LinearLayout.LayoutParams(54, 46))
        composer.addView(actions, LinearLayout.LayoutParams(-1, -2))
        root.addView(composer, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = (7 * resources.displayMetrics.density).toInt()
            bottomMargin = (4 * resources.displayMetrics.density).toInt()
        })
        root.addView(bottomNav("chat"))
    }

    private fun smallButton(label: String, action: () -> Unit): Button = Button(this).apply {
        text = label
        textSize = 12f
        isAllCaps = false
        setTextColor(Color.WHITE)
        background = rounded(accent, 18f)
        setPadding(9, 0, 9, 0)
        setOnClickListener { action() }
    }

    private fun addMessage(text: String, fromUser: Boolean, save: Boolean): LinearLayout {
        val bubble = TextView(this).apply {
            this.text = text
            textSize = 16f
            setTextColor(Color.WHITE)
            setPadding(15, 11, 15, 11)
            background = if (fromUser)
                glossy(intArrayOf(Color.rgb(0, 224, 255), Color.rgb(0, 112, 226), Color.rgb(30, 62, 180)), 20f, Color.rgb(113, 237, 255))
            else
                glossy(intArrayOf(Color.rgb(28, 55, 99), Color.rgb(12, 25, 52), Color.rgb(7, 15, 34)), 20f, Color.rgb(55, 139, 221))
            elevation = 9f * resources.displayMetrics.density
        }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = if (fromUser) Gravity.END else Gravity.START
            setPadding(2, 7, 2, 7)
            addView(bubble, LinearLayout.LayoutParams((resources.displayMetrics.widthPixels * 0.80f).toInt(), -2))
        }
        messages.addView(row)
        bubble.setOnLongClickListener {
            val actions = if (fromUser) arrayOf("کاپي کول", "شریکول") else arrayOf("کاپي کول", "شریکول", "په غږ لوستل")
            AlertDialog.Builder(this@MainActivity)
                .setItems(actions) { _, which ->
                    when (actions[which]) {
                        "کاپي کول" -> {
                            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("HAMA AI", text))
                            Toast.makeText(this@MainActivity, "کاپي شو", Toast.LENGTH_SHORT).show()
                        }
                        "شریکول" -> {
                            val share = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            startActivity(Intent.createChooser(share, "شریکول"))
                        }
                        "په غږ لوستل" -> if (speechReady) speech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "hama-message")
                    }
                }.show()
            true
        }
        if (save) {
            chatMessages.add(JSONObject().put("text", text).put("user", fromUser))
            saveCurrentChat()
        }
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
        return row
    }

    private fun sendMessage() {
        if (requestRunning) {
            requestCancelled = true
            activeConnection?.disconnect()
            sendButton.isEnabled = false
            sendButton.text = "..."
            return
        }
        val typedMessage = input.text.toString().trim()
        if (typedMessage.isEmpty() && pendingAttachmentText.isNullOrBlank()) return
        val attachmentText = pendingAttachmentText
        val attachmentName = pendingAttachmentName
        val message = buildString {
            if (typedMessage.isNotEmpty()) append(typedMessage)
            if (!attachmentText.isNullOrBlank()) {
                if (isNotEmpty()) append("\n\n")
                append("د ضمیمه شوي فایل نوم: ").append(attachmentName ?: "فایل")
                append("\nد فایل متن:\n").append(attachmentText)
            }
        }
        addMessage(if (attachmentName != null) (if (typedMessage.isNotEmpty()) "$typedMessage\n📎 $attachmentName" else "📎 $attachmentName") else message, true, true)
        pendingAttachmentText = null
        pendingAttachmentName = null
        input.text.clear()
        requestRunning = true
        requestCancelled = false
        sendButton.isEnabled = true
        sendButton.text = "×"
        waitingRow = addMessage("ځواب ترلاسه کوم...  ·  د بندولو لپاره × کېکاږئ", false, false)
        Thread {
            val answer = try {
                val connection = URL(apiUrl).openConnection() as HttpURLConnection
                activeConnection = connection
                if (requestCancelled) {
                    connection.disconnect()
                    throw java.io.InterruptedIOException("غوښتنه بنده شوه")
                }
                connection.requestMethod = "POST"
                connection.connectTimeout = 20000
                connection.readTimeout = 90000
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                val context = JSONArray()
                chatMessages.takeLast(12).forEach { item ->
                    context.put(JSONObject()
                        .put("role", if (item.optBoolean("user")) "user" else "assistant")
                        .put("content", item.optString("text").take(4000)))
                }
                val body = JSONObject()
                    .put("message", message)
                    .put("language", selectedLanguage)
                    .put("messages", context)
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val raw = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                connection.disconnect()
                activeConnection = null
                val json = JSONObject(raw)
                if (code !in 200..299) throw IllegalStateException(json.optString("error", "د سرور تېروتنه"))
                json.optString("reply", "ځواب ترلاسه نه شو.")
            } catch (e: Exception) {
                if (requestCancelled) "" else (e.message ?: "انټرنېټ یا سرور کې ستونزه ده.")
            } finally {
                activeConnection = null
            }
            runOnUiThread {
                waitingRow?.let { messages.removeView(it) }
                waitingRow = null
                if (requestCancelled) {
                    Toast.makeText(this, "غوښتنه بنده شوه.", Toast.LENGTH_SHORT).show()
                } else {
                    addMessage(answer, false, true)
                    if (speakReplies && speechReady) speech.speak(answer, TextToSpeech.QUEUE_FLUSH, null, "hama-reply")
                }
                requestRunning = false
                requestCancelled = false
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
            override fun onResults(results: Bundle?) {                val words = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
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
        baseScreen("د خبرو تاریخچه", "پخواني چټونه ولټوئ او بېرته یې پرانیزئ")
        val list = loadChats()
        val search = EditText(this).apply {
            hint = "🔎 د خبرو په نوم یا متن کې لټون..."
            setTextColor(Color.WHITE)
            setHintTextColor(muted)
            textSize = 15f
            setSingleLine(true)
            background = rounded(panel, 16f)
            setPadding(14, 12, 14, 12)
        }
        root.addView(search, LinearLayout.LayoutParams(-1, -2))
        val results = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val resultScroll = ScrollView(this)
        resultScroll.addView(results)
        root.addView(resultScroll, LinearLayout.LayoutParams(-1, 0, 1f))

        fun renderHistory(query: String) {
            results.removeAllViews()
            val needle = query.trim().lowercase(Locale.ROOT)
            var matches = 0
            for (i in list.length() - 1 downTo 0) {
                val item = list.optJSONObject(i) ?: continue
                val title = item.optString("title", "نوی چټ")
                val messagesArray = item.optJSONArray("messages") ?: JSONArray()
                val searchable = buildString {
                    append(title).append(' ')
                    for (j in 0 until messagesArray.length()) {
                        append(messagesArray.optJSONObject(j)?.optString("text") ?: "").append(' ')
                    }
                }.lowercase(Locale.ROOT)
                if (needle.isNotEmpty() && !searchable.contains(needle)) continue
                matches++
                val row = TextView(this).apply {
                    text = "💬  $title\n" + item.optString("date", "") + "\n" +
                        messagesArray.length().toString() + " پیغامونه"
                    textSize = 15f
                    setTextColor(Color.WHITE)
                    setPadding(14, 14, 14, 14)
                    background = rounded(panel, 14f)
                    setOnClickListener { openChat(item) }
                    setOnLongClickListener {
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle(title)
                            .setItems(arrayOf("د چټ نوم بدلول", "چټ شریکول", "چټ حذفول")) { _, choice ->
                                when (choice) {
                                    0 -> renameChat(item) { renderHistory(search.text.toString()) }
                                    1 -> shareChat(item)
                                    2 -> AlertDialog.Builder(this@MainActivity).setMessage("دا چټ حذف شي؟")
                                        .setNegativeButton("نه", null).setPositiveButton("هو") { _, _ ->
                                            list.remove(i)
                                            prefs.edit().putString("chats", list.toString()).apply()
                                            renderHistory(search.text.toString())
                                        }.show()
                                }
                            }.show()
                        true
                    }
                }
                val lp = LinearLayout.LayoutParams(-1, -2)
                lp.setMargins(0, 5, 0, 5)
                results.addView(row, lp)
            }
            if (matches == 0) results.addView(TextView(this).apply {
                text = if (needle.isEmpty()) "تر اوسه کوم پخوانی چټ نشته." else "د دې لټون لپاره نتیجه پیدا نه شوه."
                setTextColor(muted)
                textSize = 15f
                setPadding(12, 24, 12, 24)
            })
        }
        search.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                renderHistory(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        renderHistory("")
        root.addView(smallButton("＋ نوی چټ") { newChat() })
        root.addView(bottomNav("history"))
    }

    private fun renameChat(item: JSONObject, onSaved: () -> Unit) {
        val field = EditText(this).apply {
            setText(item.optString("title", "نوی چټ"))
            setSelection(text.length)
            hint = "د چټ نوی نوم"
        }
        AlertDialog.Builder(this).setTitle("د چټ نوم بدلول").setView(field)
            .setNegativeButton("لغوه", null)
            .setPositiveButton("ساتل") { _, _ ->
                val title = field.text.toString().trim()
                if (title.isBlank()) {
                    Toast.makeText(this, "نوم تش نه شي پاتې کېدای.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val all = loadChats()
                for (i in 0 until all.length()) {
                    val saved = all.optJSONObject(i) ?: continue
                    if (saved.optString("id") == item.optString("id")) {
                        saved.put("title", title)
                        break
                    }
                }
                item.put("title", title)
                prefs.edit().putString("chats", all.toString()).apply()
                onSaved()
                Toast.makeText(this, "د چټ نوم بدل شو.", Toast.LENGTH_SHORT).show()
            }.show()
    }

    private fun shareChat(item: JSONObject) {
        val entries = item.optJSONArray("messages") ?: JSONArray()
        val transcript = buildString {
            append(item.optString("title", "HAMA AI چټ")).append("\n")
            append(item.optString("date", "")).append("\n\n")
            for (i in 0 until entries.length()) {
                val message = entries.optJSONObject(i) ?: continue
                append(if (message.optBoolean("user")) "تاسو" else "HAMA AI")
                append(":\n")
                append(message.optString("text"))
                append("\n\n")
            }
        }
        val share = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, item.optString("title", "HAMA AI chat"))
            putExtra(Intent.EXTRA_TEXT, transcript)
        }
        startActivity(Intent.createChooser(share, "چټ شریکول"))
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
        root.addView(smallButton("🌐  ژبه: $selectedLanguage") { showLanguageSettings() })
        root.addView(smallButton(if (speakReplies) "د ځوابونو اتومات غږ: فعال" else "د ځوابونو اتومات غږ: بند") {
            speakReplies = !speakReplies
            prefs.edit().putBoolean("speak_replies", speakReplies).apply()
            showSettings()
        })
        root.addView(smallButton("💾 د معلوماتو بیک اپ صادرول") { createBackup() })
        root.addView(smallButton("📂 د بیک اپ واردول") {
            val pick = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/json"
            }
            startActivityForResult(pick, 605)
        })
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
            text = "نسخه 1.3\nHAMA AI — څو ژبی هوښیار مرستیال"
            textSize = 12f
            setTextColor(muted)
            setPadding(0, 20, 0, 10)
        })
        root.addView(bottomNav("settings"))
    }

    private fun showLanguageSettings() {
        baseScreen("ژبه", "د HAMA AI د ځوابونو ژبه وټاکئ")
        val current = TextView(this).apply {
            text = "اوسنۍ ژبه: $selectedLanguage"
            textSize = 15f
            setTextColor(Color.WHITE)
            setPadding(4, 12, 4, 12)
        }
        root.addView(current)
        val picker = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_item, languages).also {
                it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            setSelection(languages.indexOf(selectedLanguage).coerceAtLeast(0))
            onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    selectedLanguage = languages[position]
                    prefs.edit().putString("selected_language", selectedLanguage).apply()
                    current.text = "اوسنۍ ژبه: $selectedLanguage"
                }
            }
        }
        root.addView(picker, LinearLayout.LayoutParams(-1, -2))
        root.addView(smallButton("✓  بېرته تنظیماتو ته") { showSettings() })
        root.addView(bottomNav("settings"))
    }

    private fun createBackup() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, "hama-ai-backup.json")
        }
        startActivityForResult(intent, 604)
    }

    private fun loadChats(): JSONArray = try { JSONArray(prefs.getString("chats", "[]")) } catch (_: Exception) { JSONArray() }

    private fun saveCurrentChat() {
        if (chatMessages.isEmpty()) return
        val all = loadChats()
        var existingTitle: String? = null
        for (i in 0 until all.length()) {
            val existing = all.optJSONObject(i) ?: continue
            if (existing.optString("id") == currentChatId) {
                existingTitle = existing.optString("title").takeIf { it.isNotBlank() }
                break
            }
        }
        val item = JSONObject()
            .put("id", currentChatId)
            .put("title", existingTitle ?: chatMessages.firstOrNull()?.optString("text")?.take(36) ?: "نوی چټ")
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 604 && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            try {
                val backup = JSONObject()
                    .put("format", "hama-ai-backup")
                    .put("version", 1)
                    .put("exportedAt", System.currentTimeMillis())
                    .put("chats", loadChats())
                    .put("projects", projectItems)
                    .put("library", libraryItems)
                    .put("selectedLanguage", selectedLanguage)
                    .put("speakReplies", speakReplies)
                    .put("profileName", prefs.getString("profile_name", "HAMA AI کاروونکی"))
                contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use {
                    it.write(backup.toString(2))
                } ?: throw IllegalStateException("فایل ته لیکل ممکن نه شول")
                Toast.makeText(this, "بیک اپ صادر شو.", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, e.message ?: "بیک اپ صادر نه شو.", Toast.LENGTH_LONG).show()
            }
            return
        }
        if (requestCode == 605 && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            try {
                val raw = contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                    ?: throw IllegalStateException("فایل نه لوستل کېږي")
                if (raw.length > 2_000_000) throw IllegalArgumentException("بیک اپ ډېر لوی دی.")
                val backup = JSONObject(raw)
                if (backup.optString("format") != "hama-ai-backup") throw IllegalArgumentException("دا د HAMA AI بیک اپ نه دی.")
                val incomingChats = backup.optJSONArray("chats") ?: JSONArray()
                val mergedChats = loadChats()
                val knownIds = HashSet<String>()
                for (i in 0 until mergedChats.length()) knownIds.add(mergedChats.optJSONObject(i)?.optString("id") ?: "")
                var added = 0
                for (i in 0 until incomingChats.length()) {
                    val item = incomingChats.optJSONObject(i) ?: continue
                    val chatId = item.optString("id")
                    if (chatId.isNotBlank() && knownIds.add(chatId)) {
                        mergedChats.put(item)
                        added++
                    }
                }
                prefs.edit().putString("chats", mergedChats.toString()).apply()
                val incomingProjects = backup.optJSONArray("projects") ?: JSONArray()
                val mergedProjects = projectItems
                val projectNames = HashSet<String>()
                for (i in 0 until mergedProjects.length()) projectNames.add(mergedProjects.optJSONObject(i)?.optString("name") ?: "")
                for (i in 0 until incomingProjects.length()) {
                    val item = incomingProjects.optJSONObject(i) ?: continue
                    val name = item.optString("name")
                    if (name.isNotBlank() && projectNames.add(name)) mergedProjects.put(item)
                }
                prefs.edit().putString("project_items", mergedProjects.toString()).apply()
                val incomingLibrary = backup.optJSONArray("library") ?: JSONArray()
                val mergedLibrary = libraryItems
                val knownUris = HashSet<String>()
                for (i in 0 until mergedLibrary.length()) knownUris.add(mergedLibrary.optJSONObject(i)?.optString("uri") ?: "")
                var libraryAdded = 0
                for (i in 0 until incomingLibrary.length()) {
                    val entry = incomingLibrary.optJSONObject(i) ?: continue
                    val uri = entry.optString("uri")
                    if (uri.isNotBlank() && knownUris.add(uri)) {
                        mergedLibrary.put(entry)
                        libraryAdded++
                    }
                }
                val editor = prefs.edit()
                    .putString("project_items", mergedProjects.toString())
                    .putString("library_items", mergedLibrary.toString())
                val restoredLanguage = backup.optString("selectedLanguage")
                if (restoredLanguage in languages) {
                    selectedLanguage = restoredLanguage
                    editor.putString("selected_language", restoredLanguage)
                }
                if (backup.has("speakReplies")) {
                    speakReplies = backup.optBoolean("speakReplies")
                    editor.putBoolean("speak_replies", speakReplies)
                }
                if (backup.has("profileName")) editor.putString("profile_name", backup.optString("profileName"))
                editor.apply()
                Toast.makeText(this, "$added خبرې او $libraryAdded فایلونه وارد شول. د پروژو او تنظیماتو معلومات هم یوځای شول.", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, e.message ?: "بیک اپ وارد نه شو.", Toast.LENGTH_LONG).show()
            }
            return
        }
        if (requestCode == 603 && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            try {
                val mime = contentResolver.getType(uri) ?: ""
                if (!(mime.startsWith("text/") || mime.contains("json") || mime.contains("xml") || mime.contains("javascript"))) {
                    Toast.makeText(this, "اوس یوازې د متن، JSON، XML او کوډ فایلونه ملاتړ کېږي.", Toast.LENGTH_LONG).show()
                    return
                }
                val name = contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && index >= 0) cursor.getString(index) else "متن فایل"
                } ?: "متن فایل"
                val text = contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { reader ->
                    val output = StringBuilder()
                    val buffer = CharArray(4096)
                    while (true) {
                        val remaining = 60001 - output.length
                        if (remaining <= 0) break
                        val count = reader.read(buffer, 0, minOf(buffer.size, remaining))
                        if (count < 0) break
                        output.append(buffer, 0, count)
                    }
                    output.toString()
                } ?: throw IllegalStateException("فایل نه لوستل کېږي")
                if (text.length > 60000) throw IllegalArgumentException("فایل ډېر لوی دی؛ تر ۶۰ زره تورو کوچنی فایل وټاکئ.")
                pendingAttachmentText = text
                pendingAttachmentName = name
                Toast.makeText(this, "ضمیمه شوه: $name — اوس خپل سوال ولیکئ او واستوئ.", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, e.message ?: "فایل لوستل کېدای نه شي.", Toast.LENGTH_LONG).show()
            }
            return
        }
        if (requestCode == 602 && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {}
            val name = try {
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && index >= 0) cursor.getString(index) else "فایل"
                } ?: "فایل"
            } catch (_: Exception) { "فایل" }
            val all = libraryItems
            all.put(JSONObject().put("name", name).put("uri", uri.toString()))
            prefs.edit().putString("library_items", all.toString()).apply()
            Toast.makeText(this, "فایل Library ته اضافه شو.", Toast.LENGTH_SHORT).show()
            showLibrary()
        }
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