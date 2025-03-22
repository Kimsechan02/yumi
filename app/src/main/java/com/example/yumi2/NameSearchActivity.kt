package com.example.yumi2

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.yumi2.viewmodel.SummonerViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NameSearchActivity : AppCompatActivity() {
    private val viewModel: SummonerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.name_search)

        val pageBack: Button = findViewById(R.id.PageBack)
        val searchInput = findViewById<EditText>(R.id.NameSearch)
        val searchIcon = findViewById<ImageView>(R.id.Search_icon)
        val searchButton = findViewById<Button>(R.id.search)
        val errorText = findViewById<TextView>(R.id.errorText) // 🔹 오류 메시지 표시용 TextView

        // 🔹 뒤로 가기 버튼 클릭 시 메인 페이지로 이동
        pageBack.setOnClickListener {
            val intent = Intent(this, MainpageActivity::class.java)
            startActivity(intent)
        }

        // 🔹 검색창 입력 감지하여 아이콘 숨김 처리 + 오류 메시지 초기화
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchIcon.visibility = if (s.isNullOrEmpty()) ImageView.VISIBLE else ImageView.GONE
                errorText.text = "" // 🔥 입력 중이면 에러 메시지 초기화
                errorText.visibility = TextView.GONE
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // 🔹 검색 버튼 클릭 이벤트
        searchButton.setOnClickListener {
            val inputText = searchInput.text.toString().trim()

            // 🔸 닉네임에 #태그가 없는 경우
            if (!inputText.contains("#")) {
                errorText.text = "올바른 형식으로 입력하세요" // 🔥 오류 메시지 표시
                errorText.visibility = TextView.VISIBLE
                return@setOnClickListener
            }

            val parts = inputText.split("#")
            val gameName = parts[0].trim()
            val tagLine = parts.getOrNull(1)?.trim() ?: ""

            // 🔹 Riot API 호출
            viewModel.searchSummoner(gameName, tagLine)

            // 🔹 StateFlow를 수집하여 UI 업데이트 (존재하는 닉네임만 이동)
            lifecycleScope.launch {
                viewModel.summonerInfo.collectLatest { summoner ->
                    if (summoner == null) {
                        errorText.text = "닉네임이 없습니다" // 🔥 검색 실패 시 오류 메시지 표시
                        errorText.visibility = TextView.VISIBLE
                    } else {
                        errorText.visibility = TextView.GONE // 🔥 성공하면 오류 메시지 숨김
                        val intent = Intent(this@NameSearchActivity, NameSearchMainActivity::class.java)
                        intent.putExtra("gameName", gameName)
                        intent.putExtra("tagLine", tagLine)
                        startActivity(intent)
                    }
                }
            }
        }
    }
}
