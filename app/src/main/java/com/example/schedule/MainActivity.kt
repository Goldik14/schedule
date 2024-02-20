package com.example.schedule

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import org.jsoup.Jsoup
import java.io.FileOutputStream
import java.io.PrintWriter

class MainActivity : AppCompatActivity() {

    private val PICK_IMAGE_REQUEST = 1
    private val GROUP_PREFERENCES = "GroupPreferences"
    private val GROUP_ID_KEY = "GroupID"
    private val GROUP_NAME_KEY = "GroupName"

    private val GroupID = ArrayList<String>()
    private val GroupName = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Загрузка сохраненных данных
        loadSavedGroups()

        val GoToSchedule = findViewById<Button>(R.id.GoToSchedule)
        val GetGroupID = findViewById<Button>(R.id.GetGroupID)
        val SGroup = findViewById<Button>(R.id.SelectGroup)
        val GetGroup = findViewById<EditText>(R.id.GetGroup)

        val selectImage = findViewById<Button>(R.id.SelectImage)
        selectImage.setOnClickListener() {
            openGallery();
        }

        GetGroupID.setOnClickListener() {
            getSchedule()
        }

        SGroup.setOnClickListener(){
            if (GroupName.contains(GetGroup.text.toString())) {
                val Schedule = "Scheduleurl.txt"
                val outputStream: FileOutputStream
                val url3 = GroupID[GroupName.indexOf(GetGroup.text.toString())]
                val printWriter: PrintWriter
                try {
                    outputStream = openFileOutput(Schedule, Context.MODE_PRIVATE)
                    printWriter = PrintWriter(outputStream)
                    printWriter.print(url3)
                    printWriter.close()
                    Toast.makeText(this@MainActivity, "Группа сохранена", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                Toast.makeText(this@MainActivity, "Группа не найдена", Toast.LENGTH_SHORT).show()
            }
        }

        GoToSchedule.setOnClickListener() {
            val intent = Intent(this, NowWeek::class.java)
            startActivity(intent)
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, @Nullable data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.data != null) {
            val selectedImageUri: Uri? = data.data
            val intent = Intent(this@MainActivity, NowWeek::class.java)
            intent.data = selectedImageUri
            startActivity(intent)
        }
    }

    private fun getSchedule() {
        val scope = CoroutineScope(Job() + Dispatchers.IO)
        scope.launch {
            val url = "https://www.istu.edu/schedule/"
            try {
                // Получаем HTML-код страницы
                val doc = Jsoup.connect(url).get()

                // Находим все теги <a> внутри <ul>
                val links = doc.select("ul a")

                // Итерируемся по найденным ссылкам и извлекаем значения атрибута "subdiv"
                for (link in links) {
                    val href = link.attr("href")
                    // Разбиваем строку href на параметры
                    val params = href.split("?")
                    if (params.size == 2) {
                        val keyValuePairs = params[1].split("&")
                        for (pair in keyValuePairs) {
                            val keyValue = pair.split("=")
                            if (keyValue.size == 2 && keyValue[0] == "subdiv") {
                                val subdivValue = keyValue[1]
                                val url2 = "https://www.istu.edu/schedule/?subdiv=$subdivValue"

                                try {
                                    // Получаем HTML-код страницы
                                    val doc = Jsoup.connect(url2).get()

                                    // Находим все теги <a> внутри страницы
                                    val links = doc.select("a[href]")

                                    // Итерируемся по найденным ссылкам
                                    for (link in links) {
                                        val href = link.attr("href")
                                        val text = link.text()

                                        // Проверяем, содержит ли href атрибут "group"
                                        if (href.contains("group=")) {
                                            val groupValue = href.split("group=")[1].split("&")[0]
                                            GroupID.add(groupValue)
                                            GroupName.add(text)
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            withContext(Dispatchers.Main) {
                // Этот код выполнится после завершения doInBackground
                Toast.makeText(this@MainActivity, "Группы выгружены", Toast.LENGTH_SHORT).show()
                // Сохраняем данные после их загрузки
                saveGroups()
            }
        }
    }

    private fun saveGroups() {
        val sharedPreferences = getSharedPreferences(GROUP_PREFERENCES, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("GroupCount", GroupID.size)
        for (i in GroupID.indices) {
            editor.putString("$GROUP_ID_KEY$i", GroupID[i])
            editor.putString("$GROUP_NAME_KEY$i", GroupName[i])
        }
        editor.apply()
    }

    private fun loadSavedGroups() {
        val sharedPreferences = getSharedPreferences(GROUP_PREFERENCES, Context.MODE_PRIVATE)
        val groupCount = sharedPreferences.getInt("GroupCount", 0)
        for (i in 0 until groupCount) {
            val groupID = sharedPreferences.getString("$GROUP_ID_KEY$i", null)
            val groupName = sharedPreferences.getString("$GROUP_NAME_KEY$i", null)
            if (groupID != null && groupName != null) {
                GroupID.add(groupID)
                GroupName.add(groupName)
            }
        }
    }
}