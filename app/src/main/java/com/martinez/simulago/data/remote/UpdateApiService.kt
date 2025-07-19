package com.martinez.simulago.data.remote

import retrofit2.http.GET

interface UpdateApiService {
    @GET("https://raw.githubusercontent.com/SamiGamin/SimulaGo/refs/heads/master/update_config.json?token=GHSAT0AAAAAADFUKMILYIABEOZNFYG744YS2D4D2AQ") // <-- ¡IMPORTANTE! Pega aquí la parte final de tu URL raw.
    suspend fun checkForUpdates(): UpdateInfo
}