package com.example.httplab4

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.io.IOException

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val etPostId = findViewById<EditText>(R.id.etPostId)
        val btnLoad = findViewById<Button>(R.id.btnLoad)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        val tvCount = findViewById<TextView>(R.id.tvCount)
        val tvResult = findViewById<TextView>(R.id.tvResult)

        btnLoad.setOnClickListener {
            val input = etPostId.text.toString().trim()

            // Проверка поля ввода: пустое или нечисловое значение не отправляется
            if (input.isEmpty()) {
                etPostId.error = "Введите номер поста"
                return@setOnClickListener
            }

            val postId = input.toIntOrNull()
            if (postId == null || postId <= 0) {
                etPostId.error = "Введите корректное положительное число"
                return@setOnClickListener
            }

            lifecycleScope.launch {
                // Подготовка UI перед запросом
                progressBar.visibility = View.VISIBLE
                btnLoad.isEnabled = false
                tvStatus.text = "Загрузка..."
                tvCount.text = ""
                tvResult.text = ""

                try {
                    val response = RetrofitClient.api.getCommentsByPostId(postId)
                    val statusCode = response.code()
                    val statusMessage = response.message().ifEmpty {
                        if (response.isSuccessful) "OK" else "Error"
                    }

                    // Вывод HTTP-кода ответа
                    tvStatus.text = "Код ответа: $statusCode $statusMessage"

                    if (response.isSuccessful) {
                        val comments = response.body().orEmpty()
                        tvCount.text = "Количество комментариев: ${comments.size}"

                        if (comments.isEmpty()) {
                            tvResult.text = "Для поста №$postId комментариев не найдено."
                        } else {
                            // Форматирование читаемого списка: email и name
                            val formattedText = comments.mapIndexed { index, comment ->
                                "${index + 1}. Email: ${comment.email}\n   Имя (name): ${comment.name}"
                            }.joinToString(separator = "\n\n")

                            tvResult.text = formattedText
                        }
                    } else {
                        // Обработка кодов вне диапазона 200–299 (например, 404, 500)
                        tvResult.text = "Сервер вернул ошибку: HTTP $statusCode"
                    }
                } catch (e: IOException) {
                    // Обработка сетевых ошибок (отсутствие интернета, таймаут)
                    tvStatus.text = "Сетевая ошибка"
                    tvResult.text = "Не удалось выполнить запрос. Проверьте подключение к интернету.\nДетали: ${e.localizedMessage}"
                } catch (e: Exception) {
                    // Защита от прочих непредвиденных исключений
                    tvStatus.text = "Ошибка приложения"
                    tvResult.text = "Произошла ошибка: ${e.localizedMessage}"
                } finally {
                    progressBar.visibility = View.GONE
                    btnLoad.isEnabled = true
                }
            }
        }
    }
}
