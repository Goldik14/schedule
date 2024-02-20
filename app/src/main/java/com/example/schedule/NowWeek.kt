package com.example.schedule

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.*
import java.nio.charset.Charset
import java.time.LocalDate

class NowWeek : AppCompatActivity() {

    var SelectWeek=0
    var SelectGroup=3
    var week=""
    private lateinit var sharedPref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_now_week)

        sharedPref = getSharedPreferences("my_prefs", Context.MODE_PRIVATE)
        SelectGroup = sharedPref.getInt("SELECTED_GROUP", 3)
        var backgroundImage = findViewById<ImageView>(R.id.Background)

        // Получение сохраненного изображения и его установка в качестве фона
        val bitmap = loadImageFromInternalStorage("background_image.jpg")
        if (bitmap != null) {
            backgroundImage.setImageBitmap(bitmap)
        }
        val selectedImageUri: Uri? = intent.data
        if (selectedImageUri != null) {
            try {
                val bitmap = getBitmapFromUri(selectedImageUri)
                saveImageToInternalStorage(bitmap, "background_image.jpg")
                backgroundImage.setImageBitmap(bitmap)
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }
        }


        GlobalScope.launch(Dispatchers.IO) {
            getSchedule()
        }
        val menu = findViewById<Button>(R.id.GoMenu)
        val Nextweek = findViewById<Button>(R.id.NextActivity)
        val Backweek = findViewById<Button>(R.id.BackActivity)
        val OneGroup = findViewById<Button>(R.id.OneGroup)
        val TwoGroup = findViewById<Button>(R.id.TwoGroup)
        val AllWeek = findViewById<Button>(R.id.AllWeek)
        val displayMetrics = resources.displayMetrics
        val dpWidth = displayMetrics.widthPixels/3
        menu.layoutParams.width = dpWidth
        Nextweek.layoutParams.width = dpWidth
        Backweek.layoutParams.width = dpWidth
        OneGroup.layoutParams.width = dpWidth
        TwoGroup.layoutParams.width = dpWidth
        AllWeek.layoutParams.width = dpWidth
        var redColor = Color.parseColor("#FF5858")
        var greenColor = Color.parseColor("#92FF58")
        when(SelectGroup){
            1 ->{
                OneGroup.backgroundTintList = ColorStateList.valueOf(redColor)
                TwoGroup.backgroundTintList = ColorStateList.valueOf(greenColor)
            }
            2 ->{
                TwoGroup.backgroundTintList = ColorStateList.valueOf(redColor)
                OneGroup.backgroundTintList = ColorStateList.valueOf(greenColor)
            }
            3 ->{
                OneGroup.backgroundTintList = ColorStateList.valueOf(redColor)
                TwoGroup.backgroundTintList = ColorStateList.valueOf(redColor)
            }
        }

        menu.setOnClickListener(){
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        Nextweek.setOnClickListener(){
            SelectWeek++
            GlobalScope.launch(Dispatchers.IO) {
                getSchedule()
            }
        }

        Backweek.setOnClickListener(){
            SelectWeek--
            GlobalScope.launch(Dispatchers.IO) {
                getSchedule()
            }
        }

        OneGroup.setOnClickListener(){
            if(SelectGroup!=2)this.SelectGroup=2
            else this.SelectGroup=3
            when(SelectGroup){
                1 ->{
                    OneGroup.backgroundTintList = ColorStateList.valueOf(redColor)
                    TwoGroup.backgroundTintList = ColorStateList.valueOf(greenColor)
                }
                2 ->{
                    TwoGroup.backgroundTintList = ColorStateList.valueOf(redColor)
                    OneGroup.backgroundTintList = ColorStateList.valueOf(greenColor)
                }
                3 ->{
                    OneGroup.backgroundTintList = ColorStateList.valueOf(redColor)
                    TwoGroup.backgroundTintList = ColorStateList.valueOf(redColor)
                }
            }
            saveSelectedGroup()
            GlobalScope.launch(Dispatchers.IO) {
                getSchedule()
            }
        }

        TwoGroup.setOnClickListener(){
            if(SelectGroup!=1)this.SelectGroup=1
            else this.SelectGroup=3
            when(SelectGroup){
                1 ->{
                    OneGroup.backgroundTintList = ColorStateList.valueOf(redColor)
                    TwoGroup.backgroundTintList = ColorStateList.valueOf(greenColor)
                }
                2 ->{
                    TwoGroup.backgroundTintList = ColorStateList.valueOf(redColor)
                    OneGroup.backgroundTintList = ColorStateList.valueOf(greenColor)
                }
                3 ->{
                    OneGroup.backgroundTintList = ColorStateList.valueOf(redColor)
                    TwoGroup.backgroundTintList = ColorStateList.valueOf(redColor)
                }
            }
            saveSelectedGroup()
            GlobalScope.launch(Dispatchers.IO) {
                getSchedule()
            }
        }

    }

    private fun saveSelectedGroup() {
        val editor = sharedPref.edit()
        editor.putInt("SELECTED_GROUP", SelectGroup)
        editor.apply()
    }

    private fun loadImageFromInternalStorage(filename: String): Bitmap? {
        return try {
            val inputStream = openFileInput(filename)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            null
        }
    }

    private fun getBitmapFromUri(uri: Uri): Bitmap {
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        return BitmapFactory.decodeStream(inputStream)
    }

    private fun saveImageToInternalStorage(bitmap: Bitmap, filename: String) {
        var fileOutputStream: FileOutputStream? = null
        try {
            fileOutputStream = openFileOutput(filename, Context.MODE_PRIVATE)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                fileOutputStream?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun getSchedule() {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetworkInfo
        val isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting
        if (isConnected) {
            clearEmptyScheduleFiles()
            val today = LocalDate.now().plusWeeks(SelectWeek.toLong()).toString()
            val url = "https://www.istu.edu/schedule/?group=" + getScheduleUrl() + "&date=" + today
            val document = Jsoup.connect(url).get()
            week = document.selectFirst("div.alert-info > p:nth-child(3) > b")?.text()?.trim().toString()
            val scheduleDivs = document.select("div.full-odd-week, div.full-even-week")
            val dayHeaders = document.select("h3.day-heading")
            val classLines = document.select("div.class-lines")
            for (i in dayHeaders.indices) {
                val date = dayHeaders[i].text().trim()
                val daySchedule = getDaySchedule(date)
                val classLineItems = classLines[i].select("div.class-line-item")
                var textSchedule = ""
                for (classLineItem in classLineItems) {
                    val classTime = classLineItem.selectFirst("div.class-time")?.text()?.trim() ?: ""
                    val classTails = classLineItem.select("div.class-tail")
                    for (classTail in classTails) {
                        if (scheduleDivs[0].hasClass("full-odd-week") && classTail.hasClass("class-even-week")) {
                            continue
                        } else if (scheduleDivs[0].hasClass("full-even-week") && classTail.hasClass("class-odd-week")) {
                            continue
                        }
                        try {
                            if(!(classTail.select("div.class-info").get(1)?.text()?.trim())?.contains("подгруппа $SelectGroup")!!) {
                                val classTailText = classTail?.text()?.trim() ?: ""
                                var unicode:Int
                                var unicodeS:String
                                if (!classTailText.equals("свободно")) {
                                    unicode = 0x1F553
                                    unicodeS=String(Character.toChars(unicode))
                                    textSchedule += "\n" + unicodeS + "$classTime\n"
                                }
                                var classInfo = classTail.selectFirst("div.class-info")?.ownText()?.trim() ?: ""
                                classInfo = classInfo.replace(",","")
                                if (!TextUtils.isEmpty(classInfo)) textSchedule += "$classInfo\n"
                                var professor = classTail.selectFirst("div.class-info")?.text()?.trim() ?: ""
                                professor = professor.replace(classInfo,"").trim()
                                val classPred = classTail.selectFirst("div.class-pred")?.text()?.trim() ?: ""
                                if (!TextUtils.isEmpty(classPred)) textSchedule += "$classPred\n"
                                val classInfo2 = classTail.select("div.class-info").get(1)?.text()?.trim() ?: ""
                                if (!TextUtils.isEmpty(classInfo2)){
                                    if(!TextUtils.isEmpty(professor)) if (!classInfo2.contains(professor)) textSchedule+="$professor\n"
                                    if(classInfo2.contains("подгруппа")) textSchedule += "$classInfo2\n"
                                }
                                val classAud = classTail.selectFirst("div.class-aud")?.text()?.trim() ?: ""
                                unicode = 0x1F3E2
                                unicodeS=String(Character.toChars(unicode))
                                if (!TextUtils.isEmpty(classAud)) textSchedule += unicodeS + "Аудитория: $classAud\n"
                            }
                        } catch (e: IndexOutOfBoundsException) {
                        }
                    }
                }
                if (!textSchedule.isEmpty()) {
                    textSchedule = "$date\n" + textSchedule
                }
                saveScheduleToFile(daySchedule, textSchedule)
            }
        }
        updateUI()
    }

    private suspend fun updateUI() {
        withContext(Dispatchers.Main) {
            val Pon = findViewById<TextView>(R.id.pn)
            val Vtr = findViewById<TextView>(R.id.vt)
            val Srd = findViewById<TextView>(R.id.sr)
            val Chtv = findViewById<TextView>(R.id.cht)
            val Ptn = findViewById<TextView>(R.id.pt)
            val Sbb = findViewById<TextView>(R.id.sb)
            val Pond = findViewById<Button>(R.id.Pond)
            val Vtor = findViewById<Button>(R.id.Vtor)
            val Sred = findViewById<Button>(R.id.Sred)
            val Chet = findViewById<Button>(R.id.Chet)
            val Pyat = findViewById<Button>(R.id.Pyat)
            val Subb = findViewById<Button>(R.id.Subb)
            val AllWeek = findViewById<Button>(R.id.AllWeek)
            val displayMetrics = resources.displayMetrics
            val dpWidth = displayMetrics.widthPixels
            var visabilitytb=0

            var inputStream: FileInputStream

            val Pn = "Pn.txt"
            var pn = ""
            try {
                inputStream = openFileInput(Pn)
                val size: Int = inputStream.available()
                val buffer = ByteArray(size)
                inputStream.read(buffer)
                inputStream.close()
                pn = String(buffer, Charset.forName("UTF-8"))
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if(TextUtils.isEmpty(pn)){
                Pon.setVisibility(View.GONE)
                Pond.setVisibility(View.GONE)
            } else{
                Pon.setVisibility(View.VISIBLE)
                Pond.setVisibility(View.VISIBLE)
                pn=pn.substringBeforeLast("\n")
                Pon.setText(pn)
                visabilitytb++
            }

            val Vt = "Vt.txt"
            var vt = ""
            try {
                inputStream = openFileInput(Vt)
                val size: Int = inputStream.available()
                val buffer = ByteArray(size)
                inputStream.read(buffer)
                inputStream.close()
                vt = String(buffer, Charset.forName("UTF-8"))
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if(TextUtils.isEmpty(vt)){
                Vtr.setVisibility(View.GONE)
                Vtor.setVisibility(View.GONE)
            } else{
                Vtr.setVisibility(View.VISIBLE)
                Vtor.setVisibility(View.VISIBLE)
                vt=vt.substringBeforeLast("\n")
                Vtr.setText(vt)
                visabilitytb++
            }

            val Sr = "Sr.txt"
            var sr = ""
            try {
                inputStream = openFileInput(Sr)
                val size: Int = inputStream.available()
                val buffer = ByteArray(size)
                inputStream.read(buffer)
                inputStream.close()
                sr = String(buffer, Charset.forName("UTF-8"))
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if(TextUtils.isEmpty(sr)){
                Srd.setVisibility(View.GONE)
                Sred.setVisibility(View.GONE)
            } else{
                Srd.setVisibility(View.VISIBLE)
                Sred.setVisibility(View.VISIBLE)
                sr=sr.substringBeforeLast("\n")
                Srd.setText(sr)
                visabilitytb++
            }

            val Cht = "Cht.txt"
            var cht = ""
            try {
                inputStream = openFileInput(Cht)
                val size: Int = inputStream.available()
                val buffer = ByteArray(size)
                inputStream.read(buffer)
                inputStream.close()
                cht = String(buffer, Charset.forName("UTF-8"))
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if(TextUtils.isEmpty(cht)){
                Chtv.setVisibility(View.GONE)
                Chet.setVisibility(View.GONE)
            } else{
                Chtv.setVisibility(View.VISIBLE)
                Chet.setVisibility(View.VISIBLE)
                cht=cht.substringBeforeLast("\n")
                Chtv.setText(cht)
                visabilitytb++
            }

            val Pt = "Pt.txt"
            var pt = ""
            try {
                inputStream = openFileInput(Pt)
                val size: Int = inputStream.available()
                val buffer = ByteArray(size)
                inputStream.read(buffer)
                inputStream.close()
                pt = String(buffer, Charset.forName("UTF-8"))
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if(TextUtils.isEmpty(pt)){
                Ptn.setVisibility(View.GONE)
                Pyat.setVisibility(View.GONE)
            } else{
                Ptn.setVisibility(View.VISIBLE)
                Pyat.setVisibility(View.VISIBLE)
                pt=pt.substringBeforeLast("\n")
                Ptn.setText(pt)
                visabilitytb++
            }

            val Sb = "Sb.txt"
            var sb = ""
            try {
                inputStream = openFileInput(Sb)
                val size: Int = inputStream.available()
                val buffer = ByteArray(size)
                inputStream.read(buffer)
                inputStream.close()
                sb = String(buffer, Charset.forName("UTF-8"))
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if(TextUtils.isEmpty(sb)){
                Sbb.setVisibility(View.GONE)
                Subb.setVisibility(View.GONE)
            } else{
                Sbb.setVisibility(View.VISIBLE)
                Subb.setVisibility(View.VISIBLE)
                sb=sb.substringBeforeLast("\n")
                Sbb.setText(sb)
                visabilitytb++
            }

            Pond.setOnClickListener(){
                Pon.setVisibility(View.VISIBLE)
                Vtr.setVisibility(View.GONE)
                Srd.setVisibility(View.GONE)
                Chtv.setVisibility(View.GONE)
                Ptn.setVisibility(View.GONE)
                Sbb.setVisibility(View.GONE)
            }

            Vtor.setOnClickListener(){
                Pon.setVisibility(View.GONE)
                Vtr.setVisibility(View.VISIBLE)
                Srd.setVisibility(View.GONE)
                Chtv.setVisibility(View.GONE)
                Ptn.setVisibility(View.GONE)
                Sbb.setVisibility(View.GONE)
            }

            Sred.setOnClickListener(){
                Pon.setVisibility(View.GONE)
                Vtr.setVisibility(View.GONE)
                Srd.setVisibility(View.VISIBLE)
                Chtv.setVisibility(View.GONE)
                Ptn.setVisibility(View.GONE)
                Sbb.setVisibility(View.GONE)
            }

            Chet.setOnClickListener(){
                Pon.setVisibility(View.GONE)
                Vtr.setVisibility(View.GONE)
                Srd.setVisibility(View.GONE)
                Chtv.setVisibility(View.VISIBLE)
                Ptn.setVisibility(View.GONE)
                Sbb.setVisibility(View.GONE)
            }

            Pyat.setOnClickListener(){
                Pon.setVisibility(View.GONE)
                Vtr.setVisibility(View.GONE)
                Srd.setVisibility(View.GONE)
                Chtv.setVisibility(View.GONE)
                Ptn.setVisibility(View.VISIBLE)
                Sbb.setVisibility(View.GONE)
            }

            Subb.setOnClickListener(){
                Pon.setVisibility(View.GONE)
                Vtr.setVisibility(View.GONE)
                Srd.setVisibility(View.GONE)
                Chtv.setVisibility(View.GONE)
                Ptn.setVisibility(View.GONE)
                Sbb.setVisibility(View.VISIBLE)
            }

            AllWeek.setOnClickListener(){
                if(TextUtils.isEmpty(Pon.text)){
                    Pon.setVisibility(View.GONE)
                } else{
                    Pon.setVisibility(View.VISIBLE)
                }
                if(TextUtils.isEmpty(vt)){
                    Vtr.setVisibility(View.GONE)
                } else{
                    Vtr.setVisibility(View.VISIBLE)
                }
                if(TextUtils.isEmpty(sr)){
                    Srd.setVisibility(View.GONE)
                } else{
                    Srd.setVisibility(View.VISIBLE)
                }
                if(TextUtils.isEmpty(cht)){
                    Chtv.setVisibility(View.GONE)
                } else{
                    Chtv.setVisibility(View.VISIBLE)
                }
                if(TextUtils.isEmpty(pt)){
                    Ptn.setVisibility(View.GONE)
                } else{
                    Ptn.setVisibility(View.VISIBLE)
                }
                if(TextUtils.isEmpty(sb)){
                    Sbb.setVisibility(View.GONE)
                } else{
                    Sbb.setVisibility(View.VISIBLE)
                }
            }

            if(visabilitytb!=0) {
                val elementWidth = dpWidth / visabilitytb

                if (TextUtils.isEmpty(pn)) {
                    Pond.setVisibility(View.GONE)
                } else {
                    Pond.layoutParams.width = elementWidth
                    Pond.setVisibility(View.VISIBLE)
                }
                if (TextUtils.isEmpty(vt)) {
                    Vtor.setVisibility(View.GONE)
                } else {
                    Vtor.layoutParams.width = elementWidth
                    Vtor.setVisibility(View.VISIBLE)
                }
                if (TextUtils.isEmpty(sr)) {
                    Sred.setVisibility(View.GONE)
                } else {
                    Sred.layoutParams.width = elementWidth
                    Sred.setVisibility(View.VISIBLE)
                }
                if (TextUtils.isEmpty(cht)) {
                    Chet.setVisibility(View.GONE)
                } else {
                    Chet.layoutParams.width = elementWidth
                    Chet.setVisibility(View.VISIBLE)
                }
                if (TextUtils.isEmpty(pt)) {
                    Pyat.setVisibility(View.GONE)
                } else {
                    Pyat.layoutParams.width = elementWidth
                    Pyat.setVisibility(View.VISIBLE)
                }
                if (TextUtils.isEmpty(sb)) {
                    Subb.setVisibility(View.GONE)
                } else {
                    Subb.layoutParams.width = elementWidth
                    Subb.setVisibility(View.VISIBLE)
                }
                AllWeek.setText(week)
                AllWeek.requestLayout()
                Pond.requestLayout()
                Vtor.requestLayout()
                Sred.requestLayout()
                Chet.requestLayout()
                Pyat.requestLayout()
                Subb.requestLayout()
            }
            if (visabilitytb == 0) {
                Pon.setVisibility(View.VISIBLE)
                Pon.setText("Расписанию на эту неделю отсутствует")
                AllWeek.setText("")
            }

        }
    }

    private fun getScheduleUrl(): String {
        val fileName = "Scheduleurl.txt"
        return try {
            val inputStream = openFileInput(fileName)
            val size: Int = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            String(buffer, Charset.forName("UTF-8"))
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    private fun getDaySchedule(date: String): Int {
        // Determine the schedule for the given date
        // Return the day schedule index
        return when {
            date.contains("понедельник") -> 0
            date.contains("вторник") -> 1
            date.contains("среда") -> 2
            date.contains("четверг") -> 3
            date.contains("пятница") -> 4
            date.contains("суббота") -> 5
            else -> -1
        }
    }

    private fun saveScheduleToFile(daySchedule: Int, textSchedule: String) {
        val fileName = when (daySchedule) {
            0 -> "Pn.txt"
            1 -> "Vt.txt"
            2 -> "Sr.txt"
            3 -> "Cht.txt"
            4 -> "Pt.txt"
            5 -> "Sb.txt"
            else -> ""
        }
        try {
            openFileOutput(fileName, Context.MODE_PRIVATE).use {
                it.write(textSchedule.toByteArray())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun clearEmptyScheduleFiles() {
        val filesToDelete = listOf("Pn.txt", "Vt.txt", "Sr.txt", "Cht.txt", "Pt.txt", "Sb.txt")
        filesToDelete.forEach { fileName ->
            try {
                openFileOutput(fileName, Context.MODE_PRIVATE).use {
                    it.write("".toByteArray())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
