package com.martinez.simulago.data.remote

import retrofit2.http.GET

interface UpdateApiService {
    @GET("https://raw.githubusercontent.com/SamiGamin/SimulaGo/refs/tags/v1.0.2/update_config.json") // <-- ¡IMPORTANTE! Pega aquí la parte final de tu URL raw.
    suspend fun checkForUpdates(): UpdateInfo
}