package com.app.virtualsoundbox.model

import com.google.gson.annotations.SerializedName

data class NotificationRule(
    @SerializedName("id") val id: Int,
    @SerializedName("package_name") val packageName: String,
    @SerializedName("app_name") val appName: String,
    @SerializedName("regex_pattern") val regexPattern: String,
    @SerializedName("tts_format") val ttsFormat: String,
    @SerializedName("is_active") val isActive: Boolean
)