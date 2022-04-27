package com.example.safetynetapp.models

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.data.DataPoint
import com.google.android.gms.fitness.data.DataType
import com.google.firebase.Timestamp
import org.json.JSONObject
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class Weight(
    override val title: String = "Weight",
    override val units: String = "lbs",
    override var cardData: String = "-- $units",
    override var cardTimestamp: String = "--",
    override val dataType: DataType? = DataType.TYPE_WEIGHT,
    var weights: SortedMap<String, Any> = sortedMapOf<String, Any>()
) : Vital {
    override fun updateCard() {
        if (weights.isNotEmpty()) {
            val key = weights.keys.elementAt(weights.size-1)
            val weight = weights[key]
            cardData = "$weight $units"
            cardTimestamp = mapKeyToString(key)
        }
        super.updateCard()
    }

    override fun setModelData(map: SortedMap<String, Any>) {
        weights = map
    }

    override fun setDiastolicData(map: SortedMap<String, Any>) {
        Log.d("[ERROR]", "should never be here")
    }

    override fun fetchVital(
        callingActivity: AppCompatActivity,
        googleSignInAccount: GoogleSignInAccount,
        dataType: DataType?,
        defaultVal: String,
        mRequestQueue: RequestQueue,
        dashboardAdapter: DashboardViewModel
    ) {
        val url: String = "https://us-central1-safetynet-1636515641171.cloudfunctions.net/updatePatientData/AddHeatlthData"

        Fitness.getHistoryClient(callingActivity, googleSignInAccount)
            .readDailyTotal(dataType!!)
            .addOnSuccessListener { response ->
                if (response.dataPoints.isEmpty()) {
                    Log.d("[INFO]", "No data points exist")
                } else {
                    var mStringRequest = object : StringRequest(Request.Method.POST, url, Response.Listener { response ->
                        Log.i("[INFO]", "Response from upload Data: " + response)
                    }, Response.ErrorListener { error ->
                        Log.i("[INFO]", error.toString());
                    }) {
                        @Throws(AuthFailureError::class)
                        override fun getHeaders(): Map<String, String>? {
                            val params: MutableMap<String, String> = HashMap()
                            params["Content-Type"] = "application/json"
                            return params
                        }

                        @Throws(AuthFailureError::class)
                        override fun getBody(): ByteArray {
                            val rootObject= JSONObject()
                            rootObject.put("uid",googleSignInAccount.id)
                            val dataTypeNameArray = dataType.name.split(".")
                            val dataTypeName = dataTypeNameArray.get(dataTypeNameArray.size - 1)
                            val dp: DataPoint = response.dataPoints[0]
                            val dataJSON = JSONObject()
                            var secondsVal: Long = dp.getStartTime(TimeUnit.SECONDS)
                            var nanosecVal: Int = dp.getStartTime(TimeUnit.NANOSECONDS).toInt()
                            var ts: Timestamp = Timestamp(secondsVal, 0)

                            var tsString: String = timestampToMapKey(ts)
                            dataJSON.put(tsString, dp.getValue(dp.dataType.fields[0]).toString())
                            rootObject.put(dataTypeName, dataJSON)
                            return rootObject.toString().toByteArray()
                        }
                    }
                    mRequestQueue!!.add(mStringRequest!!)
                }
            }
            .addOnFailureListener { e ->
                Log.w("[ERROR]", "There was an error reading data from Google Fit", e)
            }
    }

    override fun dataPointToValueString(dp: DataPoint, defaultVal: String): String {
        var weightInKGString = dp!!.getValue(dp.dataType.fields[0]).toString()
        var weightInPoundDouble = (weightInKGString.toDouble() * 2.20462).roundToInt()
        return weightInPoundDouble.toString()
    }


}