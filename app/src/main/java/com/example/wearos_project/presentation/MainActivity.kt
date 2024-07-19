package com.example.cj_project_app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.DataSource
import com.google.android.gms.fitness.data.DataSource.TYPE_RAW
import com.google.android.gms.fitness.data.DataType.TYPE_HEART_RATE_BPM
import com.google.android.gms.fitness.data.DataType.TYPE_STRESS_LEVEL
import com.google.android.gms.fitness.data.DataSet
import com.google.android.gms.fitness.data.DataReadRequest
import com.google.android.gms.fitness.data.DataReadResponse
import com.google.android.gms.fitness.data.DataPoint
import com.google.android.gms.fitness.data.Value
import com.google.android.gms.tasks.Task
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private val googleSignInLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val account = GoogleSignIn.getLastSignedInAccount(this)
            account?.let { readFitnessData(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupGoogleSignIn()
        requestFitnessData()
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(
                FitnessOptions.builder()
                    .addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_READ)
                    .addDataType(DataType.TYPE_STRESS_LEVEL, FitnessOptions.ACCESS_READ)
                    .build()
            )
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun requestFitnessData() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun readFitnessData(account: GoogleSignInAccount) {
        val fitnessOptions = FitnessOptions.builder()
            .addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_STRESS_LEVEL, FitnessOptions.ACCESS_READ)
            .build()

        val googleAccount = GoogleSignIn.getAccountForExtension(this, fitnessOptions)

        val endTime = System.currentTimeMillis()
        val startTime = endTime - TimeUnit.DAYS.toMillis(1)

        val readRequest = DataReadRequest.Builder()
            .read(DataType.TYPE_HEART_RATE_BPM)
            .read(DataType.TYPE_STRESS_LEVEL)
            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
            .build()

        Fitness.getHistoryClient(this, googleAccount)
            .readData(readRequest)
            .addOnSuccessListener { response: DataReadResponse ->
                handleDataResponse(response)
            }
            .addOnFailureListener { e ->
                Log.e("FitnessData", "Error reading data", e)
            }
    }

    private fun handleDataResponse(response: DataReadResponse) {
        for (dataSet in response.dataSets) {
            for (dataPoint in dataSet.dataPoints) {
                for (field in dataPoint.dataType.fields) {
                    val value = dataPoint.getValue(field)
                    if (dataPoint.dataType == DataType.TYPE_HEART_RATE_BPM) {
                        Log.d("HeartRate", "Heart Rate: ${value.asFloat()}")
                    } else if (dataPoint.dataType == DataType.TYPE_STRESS_LEVEL) {
                        Log.d("StressLevel", "Stress Level: ${value.asFloat()}")
                    }
                }
            }
        }
    }
}
