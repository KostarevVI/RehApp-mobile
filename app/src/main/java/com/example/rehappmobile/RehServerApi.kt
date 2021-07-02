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
    @SerializedName("")
    @Expose

    var training: Training? = null
    var exercises: List<Exercise>? = null

    class Training {
        var patient_id: Int? = null
        var assigned_by: String? = null
        var training_description: String? = null
        var training_date: String? = null
        var exercises_amount: Int? = null
        var training_duration: String? = null
        var execution_date: String? = null
        var exercise_done_count: Int? = null
        var stop_reason: String? = null
        var spasms_total: Int? = null
    }

    class Exercise {
        var training_id: String? = null
        var order_in_training: Int? = null
        var type: String? = null
        var speed: Int? = null
        var angle_limit_from: Int? = null
        var angle_limit_to: Int? = null
        var repetitions: Int? = null
        var spasms_stop_value: Int? = null
        var involvement_threshold: Int? = null
        var repetition_timeout: String? = null
        var duration: String? = null
        var spasms_count: Int? = null
    }
}

class AuthorizationData : Serializable {
    @SerializedName("token")
    @Expose
    var token: String? = null
}

class LoginClient(val email: String, val password: String)

