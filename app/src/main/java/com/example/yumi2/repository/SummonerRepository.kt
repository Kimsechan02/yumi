package com.example.yumi2.repository

import android.util.Log
import com.example.yumi2.api.RiotApiService
import com.example.yumi2.model.LeagueEntry
import com.example.yumi2.model.SummonerResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class SummonerRepository {
    private val apiKey = "RGAPI-975e82d9-5b93-41d7-8be3-76b2dbdf8531" // 🔥 새 API 키 입력 (만료된 키 사용 금지!)
    private var lastApiCallTime: Long = 0

    // ✅ Riot 계정 API (소환사 정보 조회 - Riot ID 기반)
    private val riotAccountApi = Retrofit.Builder()
        .baseUrl("https://asia.api.riotgames.com/") // 🔥 계정 정보는 'asia.api' 사용
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(RiotApiService::class.java)

    // ✅ Riot 게임 API (PUUID 기반 소환사 정보 조회)
    private val riotGameApi = Retrofit.Builder()
        .baseUrl("https://kr.api.riotgames.com/") // 🔥 게임 데이터는 'kr.api' 사용
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(RiotApiService::class.java)

    suspend fun getSummonerInfo(gameName: String, tagLine: String): SummonerResponse? {
        return try {

            val currentTime = System.currentTimeMillis()

            // 🚨 API 호출 제한 (5초 간격)
            if (currentTime - lastApiCallTime < 5000) {
                Log.w("SummonerRepository", "API 호출 제한 - 5초 대기 필요")
                return null
            }
            lastApiCallTime = currentTime // 마지막 호출 시간 갱신

            val response = riotAccountApi.getSummonerInfo(gameName, tagLine, apiKey)
            Log.d("SummonerRepository", "소환사 정보 응답 (닉네임 기반): $response")

            if (response.puuid.isNotEmpty()) {
                val puuidResponse = riotGameApi.getSummonerByPuuid(response.puuid, apiKey)
                Log.d("SummonerRepository", "소환사 정보 응답 (PUUID 기반): $puuidResponse")

                return SummonerResponse(
                    puuid = puuidResponse.puuid,
                    gameName = response.gameName, // 닉네임은 Riot API에서 받은 값 유지
                    tagLine = response.tagLine,  // 태그라인도 유지
                    profileIconId = puuidResponse.profileIconId // 아이콘은 최신 데이터 사용
                )
            } else {
                Log.e("SummonerRepository", "PUUID가 비어 있음. 닉네임 기반 응답 사용.")
                response
            }
        } catch (e: retrofit2.HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Log.e("SummonerRepository", "소환사 정보 가져오기 실패: HTTP ${e.code()} - $errorBody")
            null
        } catch (e: Exception) {
            Log.e("SummonerRepository", "소환사 정보 가져오기 실패: ${e.message}")
            null
        }
    }

    // ✅ PUUID 기반 소환사 정보 조회
    suspend fun getSummonerByPuuid(puuid: String): SummonerResponse? {
        return try {
            val response = riotGameApi.getSummonerByPuuid(puuid, apiKey)
            response
        } catch (e: Exception) {
            Log.e("SummonerRepository", "PUUID로 소환사 정보 가져오기 실패: ${e.message}")
            null
        }
    }

    suspend fun getRankBySummoner(summonerId: String): List<LeagueEntry> {
        return try {
            Log.d("SummonerRepository", "랭크 정보 API 호출: summonerId=$summonerId") // 🔥 API 호출 로그 추가

            val response = riotGameApi.getRankBySummoner(summonerId, apiKey)

            Log.d("SummonerRepository", "랭크 정보 API 응답: $response") // 🔥 응답 로그 추가

            if (response.isNotEmpty()) {
                response
            } else {
                Log.e("SummonerRepository", "⚠️ 랭크 정보 없음 (빈 리스트 반환)")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("SummonerRepository", "❌ 랭크 정보 가져오기 실패: ${e.message}", e)
            emptyList()
        }
    }
}
