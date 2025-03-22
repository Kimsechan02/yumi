package com.example.yumi2.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yumi2.model.LeagueEntry
import com.example.yumi2.model.SummonerResponse
import com.example.yumi2.repository.SummonerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

class SummonerViewModel : ViewModel() {
    private val repository = SummonerRepository()

    // ✅ Riot API에서 가져온 소환사 정보를 저장하는 StateFlow
    private val _summonerInfo = MutableStateFlow<SummonerResponse?>(null)
    val summonerInfo: StateFlow<SummonerResponse?> = _summonerInfo

    private val _rankInfo = MutableStateFlow<LeagueEntry?>(null)
    val rankInfo: StateFlow<LeagueEntry?> = _rankInfo

    // ✅ 최신 LOL 버전 가져오기 (Data Dragon API 사용)
    private suspend fun fetchLatestVersion(): String {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("https://ddragon.leagueoflegends.com/api/versions.json")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val versions = JSONArray(response)
                    val latestVersion = versions.getString(0) // 최신 버전 반환

                    Log.d("SummonerViewModel", "최신 LOL 버전: $latestVersion")
                    latestVersion
                } else {
                    Log.e("SummonerViewModel", "최신 LOL 버전 가져오기 실패")
                    "14.1.1" // 기본값 (API 실패 시 대체)
                }
            } catch (e: Exception) {
                Log.e("SummonerViewModel", "최신 버전 가져오기 실패: ${e.message}")
                "14.1.1" // 기본값
            }
        }
    }

    // ✅ 최신 LOL 버전 가져오는 함수 (UI에서 사용 가능)
    suspend fun getLatestLolVersion(): String {
        return fetchLatestVersion()
    }

    // 🔹 소환사 정보 가져오기 (게임 닉네임 + 태그라인 기반)
    fun searchSummoner(gameName: String, tagLine: String) {
        viewModelScope.launch {
            val result = repository.getSummonerInfo(gameName, tagLine)

            if (result?.profileIconId == 0) {
                // 🔥 profileIconId가 0이면 PUUID 기반으로 다시 조회
                fetchSummonerInfo(result.puuid)
            } else {
                _summonerInfo.value = result
            }
        }
    }

    // 🔹 소환사 PUUID 기반 정보 조회 (profileIconId 정확한 데이터 가져오기)
    fun fetchSummonerInfo(puuid: String) {
        viewModelScope.launch {
            val result = repository.getSummonerByPuuid(puuid)
            _summonerInfo.value = result
        }
    }

    // ✅ UI에서 사용할 아이콘 URL 생성 (최신 LOL 버전 적용)
    suspend fun getSummonerIconUrl(profileIconId: Int): String {
        val latestVersion = fetchLatestVersion()
        return "https://ddragon.leagueoflegends.com/cdn/$latestVersion/img/profileicon/$profileIconId.png"
    }

    fun fetchSummonerRank(summonerId: String) {
        viewModelScope.launch {
            val rankData = repository.getRankBySummoner(summonerId)
            _rankInfo.value = rankData.find { it.queueType == "RANKED_SOLO_5x5" } // 🔹 개인/2인전 랭크 정보만 가져옴
        }
    }

    fun fetchRankInfo(summonerId: String) {
        viewModelScope.launch {
            Log.d("SummonerViewModel", "랭크 정보 요청 시작: $summonerId") // 🔥 랭크 정보 요청 시작 로그

            val result = repository.getRankBySummoner(summonerId)

            if (result.isNotEmpty()) {
                _rankInfo.value = result[0] // 🔥 가장 첫 번째 랭크 데이터 사용
                Log.d("SummonerViewModel", "랭크 정보 응답: $result")
            } else {
                _rankInfo.value = null
                Log.e("SummonerViewModel", "랭크 정보가 없음")
            }
        }
    }


}
