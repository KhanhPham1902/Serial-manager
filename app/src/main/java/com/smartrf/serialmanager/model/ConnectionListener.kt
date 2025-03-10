package com.smartrf.serialmanager.model

interface ConnectionListener {
    fun onConnect(host: String, port: Int)
}
