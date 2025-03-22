package com.example.yumi2

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.yumi2.viewmodel.SummonerViewModel
import kotlinx.coroutines.launch

class NameSearchMainActivity : AppCompatActivity() {
    private val viewModel: SummonerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.name_search_main)

        val gameName = intent.getStringExtra("gameName") ?: ""
        val tagLine = intent.getStringExtra("tagLine") ?: ""

        val summonerNameText = findViewById<TextView>(R.id.summonerName)
        val summonerRankText = findViewById<TextView>(R.id.summonerRank)
        val summonerIcon = findViewById<ImageView>(R.id.summonerIcon)
        val refreshButton = findViewById<Button>(R.id.refreshMatchData)
        val backButton = findViewById<Button>(R.id.PageBack)

        // ✅ 랭크 관련 UI 요소 추가
        val rankCard = findViewById<View>(R.id.rankCard)
        val rankType = findViewById<TextView>(R.id.rankType)
        val rankTier = findViewById<TextView>(R.id.rankTier)
        val rankLP = findViewById<TextView>(R.id.rankLP)
        val rankWinLoss = findViewById<TextView>(R.id.rankWinLoss)
        val rankPosition = findViewById<TextView>(R.id.rankPosition)
        val rankTierImage = findViewById<ImageView>(R.id.rankTierImage)

        // 🔹 뒤로 가기 버튼 설정
        backButton.setOnClickListener {
            finish() // 현재 액티비티 종료 (이전 화면으로 돌아감)
        }

        // 🔹 닉네임 표시
        summonerNameText.text = "$gameName#$tagLine"

        // 🔹 Riot API 호출 (소환사 정보 가져오기)
        viewModel.searchSummoner(gameName, tagLine)

        lifecycleScope.launch {
            viewModel.summonerInfo.collect { summoner ->
                if (summoner != null) {
                    // 🔹 Riot API에서 반환된 대소문자 및 띄어쓰기가 적용된 닉네임 사용
                    val displayGameName = summoner.gameName ?: gameName
                    val displayTagLine = summoner.tagLine ?: tagLine

                    summonerNameText.text = "$displayGameName#$displayTagLine"
                    summonerRankText.text = "Puuid: ${summoner.puuid}"

                    val latestVersion = viewModel.getLatestLolVersion()
                    val iconUrl = "https://ddragon.leagueoflegends.com/cdn/$latestVersion/img/profileicon/${summoner.profileIconId}.png"

                    Log.d("SummonerViewModel", "아이콘 URL: $iconUrl")

                    Glide.with(this@NameSearchMainActivity)
                        .load(iconUrl)
                        .skipMemoryCache(true)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .into(summonerIcon)
                } else {
                    summonerNameText.text = "소환사 정보를 불러올 수 없습니다."
                }
            }
        }

        lifecycleScope.launch {
            viewModel.rankInfo.collect { rankInfo ->
                if (rankInfo != null) {
                    Log.d("NameSearchMainActivity", "랭크 데이터 수신: $rankInfo") // 🔥 수신한 랭크 데이터 로그

                    rankCard.visibility = View.VISIBLE
                    rankType.text = if (rankInfo.queueType == "RANKED_SOLO_5x5") "개인/2인전" else "자유 5:5 랭크"
                    rankTier.text = "${rankInfo.tier} ${rankInfo.rank}"
                    rankLP.text = "${rankInfo.leaguePoints} LP"
                    rankWinLoss.text = "${rankInfo.wins + rankInfo.losses}전 ${rankInfo.wins}승 ${rankInfo.losses}패"
                    rankPosition.text = "랭킹 -위"

                    val tierImageUrl = "https://opgg-static.akamaized.net/images/medals/${rankInfo.tier.lowercase()}.png"
                    Glide.with(this@NameSearchMainActivity)
                        .load(tierImageUrl)
                        .into(rankTierImage)
                } else {
                    Log.e("NameSearchMainActivity", "랭크 정보 없음")
                    rankCard.visibility = View.GONE
                }
            }
        }




        refreshButton.setOnClickListener {
            viewModel.searchSummoner(gameName, tagLine)
        }
    }
}
