package com.example.yumitsuhori.googlefitsample

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.request.DataReadRequest
import com.jakewharton.threetenabp.AndroidThreeTen
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.temporal.ChronoField
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private val GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 1
    private val TAG = "GoogleFitLog"
    private var dailyStepText: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        AndroidThreeTen.init(application)
        dailyStepText = findViewById<TextView>(R.id.daily_step)

        val fitnessOptions: FitnessOptions = FitnessOptions.builder()
                .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_STEP_COUNT_CADENCE, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_STEP_COUNT_CUMULATIVE)
                .build()

        if (!GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(this), fitnessOptions)) {
            GoogleSignIn.requestPermissions(
                    this, // your activity
                    GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                    GoogleSignIn.getLastSignedInAccount(this),
                    fitnessOptions)
        } else {
            subscribe()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == GOOGLE_FIT_PERMISSIONS_REQUEST_CODE) {
                subscribe()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.sync -> {
                readData()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    private fun subscribe() {
        Fitness.getRecordingClient(this, GoogleSignIn.getLastSignedInAccount(this))
                .subscribe(DataType.TYPE_ACTIVITY_SAMPLES)
                .addOnSuccessListener {
                    Log.d(TAG, "Successfully subscribed!")
                }
                .addOnFailureListener { e ->
                    Log.d(TAG, e.stackTrace.toString())
                }
    }

    private fun readDailyData() {
        Fitness.getHistoryClient(this, GoogleSignIn.getLastSignedInAccount(this))
                .readDailyTotal(DataType.TYPE_STEP_COUNT_DELTA)
                .addOnSuccessListener { dataSet ->
                    val total = if (dataSet.isEmpty) 0 else dataSet.dataPoints[0].getValue(Field.FIELD_STEPS).asInt()
                    dailyStepText?.text = total.toString()
                    Log.d("total: ", total.toString())
                }
                .addOnFailureListener { e -> Log.e("err", e.toString()) }
    }

    private fun readData() {
        val now = ZonedDateTime.now(ZoneId.of("Asia/Tokyo"))
        val endTime = now.getLong(ChronoField.EPOCH_DAY)
        val startTime = now.minusYears(1).getLong(ChronoField.EPOCH_DAY)
        Log.i("endTime", endTime.toString())
        Log.i("startTime", startTime.toString())

        val dataReadRequest = DataReadRequest.Builder()
                .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime, endTime, TimeUnit.DAYS)
                .build()

        Fitness.getHistoryClient(this, GoogleSignIn.getLastSignedInAccount(this))
                .readData(dataReadRequest)
                .addOnSuccessListener { response ->
                    Log.d("STEp", response.dataSets.isEmpty().toString())
                    response.buckets.map { bucket ->
                        val startTime = LocalDate.ofEpochDay(bucket.getStartTime(TimeUnit.DAYS))
                        Log.i("startEpoch", startTime.toString())
                        val endTime: LocalDate = LocalDate.ofEpochDay(bucket.getEndTime(TimeUnit.DAYS))
                        Log.i("endEpoch", endTime.toString())
                        bucket.dataSets.map { dataSet ->
                            dataSet.dataPoints.map { dataPoint ->
                                Log.i("bucket", bucket.toString())
                                Log.i("dataSet", dataSet.dataType.name)
                                Log.i("points", dataPoint.toString())
                                Log.i("value", dataPoint.getValue(Field.FIELD_STEPS).toString())
                            }
                        }
                    }
                }
                .addOnFailureListener { e -> Log.e("err", e.toString()) }
    }
}
