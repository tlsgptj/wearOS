package com.example.wearos_project

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class MainActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private var heartRateThreshold = 150 //테스트 용 심박수 임계치임

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 심박수 임계치 입력 및 버튼 설정
        val thresholdInput: EditText = findViewById(R.id.thresholdInput)
        val setthreshold: TextView = findViewById(R.id.setthreshold)
        val setThresholdButton: Button = findViewById(R.id.setThresholdButton)

        setThresholdButton.setOnClickListener {
            val input = thresholdInput.text.toString()
            if (input.isNotEmpty()) {
                heartRateThreshold = input.toInt()
                setthreshold.text = "심박수 임계치가 $heartRateThreshold 로 설정되었습니다."
            }
        }

        lifecycleScope.launchWhenStarted {
            while (true) {
                fetchHeartRateData()
                kotlinx.coroutines.delay(60000)  // 1분마다 데이터 갱신
            }
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


