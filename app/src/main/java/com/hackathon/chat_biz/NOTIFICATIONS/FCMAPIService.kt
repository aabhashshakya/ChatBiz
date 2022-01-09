package com.hackathon.chat_biz.NOTIFICATIONS

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

private const val FCM_SERVER_KEY: String = "AAAAk4xS464:APA91bEPvs_S8hMVHWaHvneEiwRKXDnjuSA49dfB5QqKdNTh4wsT_MaOSW_gEjvr5MgC9KLfAkBRKIBRnz6koKcp0qE7Ipt4HKanpRl3_R3gl5dVrg0hvrFcFt6n5tyW6xHrW8-YA1D-"

interface FCMAPIService {


    //You get the authorization key from Firebase Console -> Settings -> FCM -> Server key
    @Headers(
        "Content-Type:application/json",
        "Authorization:key= $FCM_SERVER_KEY"
    )


    @POST("fcm/send")
    fun sendNotifications(@Body body: SendNotification): Call<MyResponse>


}