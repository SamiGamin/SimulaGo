package com.martinez.simulago.data.remote

data class UpdateInfo(
    val latestVersionCode: Int,
    val latestVersionName: String,
    val updateUrl: String,
    val releaseNotes: List<String>
)