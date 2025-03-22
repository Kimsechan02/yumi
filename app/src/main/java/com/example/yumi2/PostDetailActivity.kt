package com.example.yumi2

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PostDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.community_post_detail)

        val titleTextView = findViewById<TextView>(R.id.detail_post_title)
        val contentTextView = findViewById<TextView>(R.id.detail_post_content)
        val timestampTextView = findViewById<TextView>(R.id.detail_post_timestamp)
        val categoryTextView = findViewById<TextView>(R.id.detail_post_category)

        // ✅ Intent에서 데이터 가져오기
        val title = intent.getStringExtra("title") ?: "제목 없음"
        val content = intent.getStringExtra("content") ?: "내용 없음"
        val category = intent.getStringExtra("category") ?: "카테고리 없음"
        val timestamp = intent.getLongExtra("timestamp", 0L)

        // ✅ 날짜 포맷 변환
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(timestamp))

        // ✅ UI에 데이터 설정
        titleTextView.text = title
        contentTextView.text = content
        categoryTextView.text = category
        timestampTextView.text = formattedDate
    }
}