package com.example.yumi2.api

import com.example.yumi2.model.LeagueEntry
import com.example.yumi2.model.SummonerResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Header


data class ChampionRotationResponse(val champion_list: List<ChampionData>)

data class ChampionData(
    val id: Int,
    val name: String,
    val imageUrl: String
)


interface RiotApiService {

    // ✅ Riot ID 기반 소환사 정보 조회 (닉네임 + 태그라인)
    @GET("riot/account/v1/accounts/by-riot-id/{gameName}/{tagLine}")
    suspend fun getSummonerInfo(
        @Path("gameName") gameName: String,
        @Path("tagLine") tagLine: String,
        @Header("X-Riot-Token") apiKey: String
    ): SummonerResponse

    // ✅ PUUID 기반 소환사 정보 조회
    @GET("lol/summoner/v4/summoners/by-puuid/{puuid}")
    suspend fun getSummonerByPuuid(
        @Path("puuid") puuid: String,
        @Header("X-Riot-Token") apiKey: String
    ): SummonerResponse

    // ✅ 소환사 랭크 정보 가져오기
    @GET("lol/league/v4/entries/by-summoner/{summonerId}")
    suspend fun getRankBySummoner(
        @Path("summonerId") summonerId: String,
        @Header("X-Riot-Token") apiKey: String
    ): List<LeagueEntry>
}