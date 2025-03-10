package com.smartrf.serialmanager.model

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

private val gson = Gson()

data class TcpResponse(
    @SerializedName("M15") val M15: Int,
    @SerializedName("H4") val H4: Int,
    @SerializedName("PNOW") val PNOW: Long,
    @SerializedName("D15") val D15: Int,
    @SerializedName("D4") val D4: Int,
    @SerializedName("L15A") val L15A: Int,
    @SerializedName("L15B") val L15B: Int,
    @SerializedName("L15C") val L15C: Int,
    @SerializedName("L4A") val L4A: Int,
    @SerializedName("L4B") val L4B: Int,
    @SerializedName("L4C") val L4C: Int,
    @SerializedName("ACC") val ACC: Int,
    @SerializedName("EP") val EP: Int,
    @SerializedName("PO") val PO: Int
)

fun parseJson(response: String): TcpResponse? {
    return try {
        gson.fromJson(response, TcpResponse::class.java)
    } catch (e: Exception) {
        e.printStackTrace()
        null  // Trả về null nếu có lỗi
    }
}
