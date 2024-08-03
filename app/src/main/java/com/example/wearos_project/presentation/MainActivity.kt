package com.example.wearos_project.presentation

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.wearos_project.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.gms.fitness.result.DataReadResponse
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val storageRef = FirebaseStorage.getInstance().reference
    private var heartRateThreshold = 150 // 테스트용 심박수 임계치

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 심박수 임계치 입력 및 버튼 설정
        val thresholdInput: EditText = findViewById(R.id.thresholdInput)
        val setThresholdTextView: TextView = findViewById(R.id.setthreshold)
        val setThresholdButton: Button = findViewById(R.id.setThresholdButton)

        setThresholdButton.setOnClickListener {
            val input = thresholdInput.text.toString()
            if (input.isNotEmpty()) {
                heartRateThreshold = input.toInt()
                setThresholdTextView.text = "심박수 임계치가 $heartRateThreshold 로 설정되었습니다."
            }
        }

        lifecycleScope.launchWhenStarted {
            while (true) {
                requestHeartRateData() // 심박수 데이터를 측정하고 Firebase Storage에 저장
                fetchHeartRateData()  // 저장된 데이터를 가져와 UI 업데이트
                delay(60000)  // 1분마다 데이터 갱신
            }
        }
    }

    private suspend fun requestHeartRateData() {
        try {
            val readRequest = DataReadRequest.Builder()
                .read(DataType.TYPE_HEART_RATE_BPM)
                .setTimeRange(1, System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .build()

            val response = Fitness.getHistoryClient(this, GoogleSignIn.getLastSignedInAccount(this)!!)
                .readData(readRequest)
                .await()

            val heartRateData = response.dataSets.flatMap { dataSet ->
                dataSet.dataPoints.map { dataPoint ->
                    dataPoint.getValue(Field.FIELD_BPM).asFloat()
                }
            }

            // 심박수 데이터를 Firebase Storage에 업로드
            uploadHeartRateDataToFirebase(heartRateData)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error fetching heart rate data", e)
        }
    }

    private fun uploadHeartRateDataToFirebase(heartRateData: List<Float>) {
        val heartRateRef = storageRef.child("heart_rate_data.txt")

        val baos = ByteArrayOutputStream()
        val writer = PrintWriter(OutputStreamWriter(baos))
        heartRateData.forEach { writer.println(it) }
        writer.flush()

        val data = baos.toByteArray()
        val uploadTask = heartRateRef.putBytes(data)

        uploadTask.addOnSuccessListener {
            Log.d("MainActivity", "Heart rate data uploaded successfully")
        }.addOnFailureListener { e ->
            Log.e("MainActivity", "Error uploading heart rate data", e)
        }
    }

    private suspend fun fetchHeartRateData() {
        try {
            val snapshot = db.collection("heartRates")
                .whereGreaterThan("heartRate", heartRateThreshold)
                .get()
                .await()

            val workers = snapshot.documents.map { document ->
                Worker(
                    name = document.getString("name") ?: "Unknown",
                    location = document.getString("location") ?: "Unknown",
                    heartRate = document.getDouble("heartRate")?.toFloat() ?: 0f,
                    timestamp = document.getString("timestamp") ?: "Unknown"
                )
            }
            updateUI(workers)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error fetching data", e)
        }
    }

    private fun updateUI(workers: List<Worker>) {
        val timeTextView: TextView = findViewById(R.id.time)
        val workerTextView: TextView = findViewById(R.id.worker)
        val workspaceTextView: TextView = findViewById(R.id.workspace)
        val alertMessageTextView: TextView = findViewById(R.id.alertMessage)

        if (workers.isNotEmpty()) {
            val worker = workers.first()
            timeTextView.text = "현재 시간: ${worker.timestamp}"
            workerTextView.text = "근무자: ${worker.name}"
            workspaceTextView.text = "위치: ${worker.location}"
            alertMessageTextView.text = "경고: 심박수 ${worker.heartRate} BPM 초과"
        } else {
            alertMessageTextView.text = "모든 근무자의 심박수가 정상입니다."
        }
    }

    data class Worker(
        val name: String,
        val location: String,
        val heartRate: Float,
        val timestamp: String
    )
}



