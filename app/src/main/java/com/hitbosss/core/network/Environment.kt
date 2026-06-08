package com.hitbosss.core.network

import com.hitbosss.BuildConfig


object Environment {
    const val API_VERSION = "2"
    val baseUrl: String = BuildConfig.API_BASE_URL
    val name: String = BuildConfig.ENV_NAME
    val deeplinkBaseUrl: String = baseUrl + "deeplink/"
}
