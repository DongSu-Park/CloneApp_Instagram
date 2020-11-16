package com.flore.instagramclone.util

import com.flore.instagramclone.model.PushDTO
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import okhttp3.*
import java.io.IOException

class FcmPush{
    var JSON = MediaType.parse("application/json; charset=utf-8")
    var url = "https://fcm.googleapis.com/fcm/send"
    var serverKey = "AAAAEdTH3fo:APA91bHESxJh8AW39a8QSKXkZ5bDtAAyYafULI3M71_VIZ_c5P9lsQn8vDGBj4gwwh1_IRhnaoN4VT05-cxk0zrudCUIc_JQS6knKzKPmFPsQR5a92llVmJ-_tmUBvWKWODCfAv_MyNn"
    var gson : Gson? = null
    var okHttpClient : OkHttpClient? = null

    companion object{
        var instance = FcmPush()
    }

    init {
        gson = Gson()
        okHttpClient = OkHttpClient()
    }

    fun sendMessage(destinationUid : String, title : String, message : String){
        FirebaseFirestore.getInstance().collection("pushtokens").document(destinationUid).get().addOnCompleteListener { task ->
            if(task.isSuccessful){
                val token = task?.result?.get("pushToken").toString()

                val pushDTO = PushDTO()
                pushDTO.to = token
                pushDTO.notification.title = title
                pushDTO.notification.body = message

                val body = RequestBody.create(JSON, gson?.toJson(pushDTO))
                val request = Request.Builder()
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "key=" + serverKey)
                    .url(url)
                    .post(body)
                    .build()

                okHttpClient?.newCall(request)?.enqueue(object : Callback{
                    override fun onFailure(call: Call?, e: IOException?) {
                        // 요청 실패의 경우
                        println(e?.toString())
                    }

                    override fun onResponse(call: Call?, response: Response?) {
                        // 요청 성공의 경우
                        println(response?.body()?.string())
                    }
                })
            }
        }
    }
}