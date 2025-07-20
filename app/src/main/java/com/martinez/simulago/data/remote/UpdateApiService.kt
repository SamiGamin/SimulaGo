package com.martinez.simulago.data.remote

import com.martinez.simulago.data.remote.UpdateInfo // Asegúrate que esta ruta sea correcta
import retrofit2.http.GET

interface UpdateApiService {
    @GET("https://raw.githubusercontent.com/SamiGamin/SimulaGo/refs/heads/master/update_config.json")
    suspend fun checkForUpdates(): UpdateInfo
}
