package com.example.rehappmobile

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.*
import java.io.Serializable

interface RehServerApi {
    @POST("/patient_login")
    fun login(@Body body: LoginClient): Call<AuthorizationReport>

    @GET("/get_patient_training")
    fun getPatientTraining(@Header("authorization") token: String): Call<GetTrainingReport>
}

class AuthorizationReport : Serializable {
    @SerializedName("status")
    @Expose
    var status: String? = null

    @SerializedName("data")
    @Expose
    var data: AuthorizationData? = null
}

class GetTrainingReport : Serializable {
    @SerializedName("status")
    @Expose
    var status: String? = null

    @SerializedName("data")
    @Expose
    var data: TrainingData? = null
}

class TrainingData: Serializable {
//    @SerializedName("")
//    @Expose
//    var
}

class AuthorizationData : Serializable {
    @SerializedName("token")
    @Expose
    var token: String? = null
}

class LoginClient(val email: String, val password: String)

