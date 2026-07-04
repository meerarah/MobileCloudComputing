package com.university.smartcampuspantry.service

import android.os.Handler
import android.os.Looper
import com.university.smartcampuspantry.model.StudentProfile
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class APIService {
    companion object {
        val shared = APIService()
    }

    // Point to the live Vercel deployment!
    var baseURL: String = "https://mobile-cloud-computing-4u8u.vercel.app"

    private val mainHandler = Handler(Looper.getMainLooper())

    fun fetchStudentProfile(studentId: String, callback: (Result<StudentProfile>) -> Unit) {
        thread {
            try {
                val urlObj = URL("$baseURL/api/student/$studentId")
                val conn = urlObj.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 5000

                if (conn.responseCode == 200) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val response = reader.readText()
                    reader.close()

                    val json = JSONObject(response)
                    val profile = StudentProfile(
                        success = json.getBoolean("success"),
                        studentId = json.getString("studentId"),
                        name = json.getString("name"),
                        eligible = json.getBoolean("eligible"),
                        impactPoints = json.getInt("impactPoints"),
                        claimsThisWeek = json.getInt("claimsThisWeek"),
                        maxWeeklyClaims = json.getInt("maxWeeklyClaims")
                    )
                    mainHandler.post { callback(Result.success(profile)) }
                } else {
                    val errorReader = BufferedReader(InputStreamReader(conn.errorStream))
                    val errorText = errorReader.readText()
                    errorReader.close()
                    val json = JSONObject(errorText)
                    val msg = json.optString("message", "Error loading profile")
                    mainHandler.post { callback(Result.failure(Exception(msg))) }
                }
            } catch (e: Exception) {
                mainHandler.post { callback(Result.failure(e)) }
            }
        }
    }

    fun claimItem(studentId: String, itemId: String, itemName: String, callback: (Result<JSONObject>) -> Unit) {
        thread {
            try {
                val urlObj = URL("$baseURL/api/claim")
                val conn = urlObj.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.connectTimeout = 5000

                val payload = JSONObject().apply {
                    put("studentId", studentId)
                    put("itemId", itemId)
                    put("itemName", itemName)
                }

                val writer = OutputStreamWriter(conn.outputStream)
                writer.write(payload.toString())
                writer.flush()
                writer.close()

                val responseCode = conn.responseCode
                val stream = if (responseCode == 200) conn.inputStream else conn.errorStream
                val reader = BufferedReader(InputStreamReader(stream))
                val response = reader.readText()
                reader.close()

                val json = JSONObject(response)
                if (responseCode == 200) {
                    mainHandler.post { callback(Result.success(json)) }
                } else {
                    val msg = json.optString("message", "Claim rejected")
                    mainHandler.post { callback(Result.failure(Exception(msg))) }
                }
            } catch (e: Exception) {
                mainHandler.post { callback(Result.failure(e)) }
            }
        }
    }

    fun analyzeFoodImage(imageUrl: String, callback: (Result<JSONObject>) -> Unit) {
        thread {
            try {
                val urlObj = URL("$baseURL/api/analyze-food")
                val conn = urlObj.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.connectTimeout = 5000

                val payload = JSONObject().apply {
                    put("imageUrl", imageUrl)
                }

                val writer = OutputStreamWriter(conn.outputStream)
                writer.write(payload.toString())
                writer.flush()
                writer.close()

                val responseCode = conn.responseCode
                val stream = if (responseCode == 200) conn.inputStream else conn.errorStream
                val reader = BufferedReader(InputStreamReader(stream))
                val response = reader.readText()
                reader.close()

                val json = JSONObject(response)
                if (responseCode == 200) {
                    mainHandler.post { callback(Result.success(json)) }
                } else {
                    val msg = json.optString("message", "Analysis failed")
                    mainHandler.post { callback(Result.failure(Exception(msg))) }
                }
            } catch (e: Exception) {
                mainHandler.post { callback(Result.failure(e)) }
            }
        }
    }

    fun donatePoints(
        studentId: String,
        points: Int,
        itemName: String,
        quantity: Int,
        imageUrl: String,
        location: String,
        callback: (Result<JSONObject>) -> Unit
    ) {
        thread {
            try {
                val urlObj = URL("$baseURL/api/donate-points")
                val conn = urlObj.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.connectTimeout = 5000

                val payload = JSONObject().apply {
                    put("studentId", studentId)
                    put("points", points)
                    put("itemName", itemName)
                    put("quantity", quantity)
                    put("imageUrl", imageUrl)
                    put("location", location)
                }

                val writer = OutputStreamWriter(conn.outputStream)
                writer.write(payload.toString())
                writer.flush()
                writer.close()

                val responseCode = conn.responseCode
                val stream = if (responseCode == 200) conn.inputStream else conn.errorStream
                val reader = BufferedReader(InputStreamReader(stream))
                val response = reader.readText()
                reader.close()

                val json = JSONObject(response)
                if (responseCode == 200) {
                    mainHandler.post { callback(Result.success(json)) }
                } else {
                    val msg = json.optString("message", "Points transaction failed")
                    mainHandler.post { callback(Result.failure(Exception(msg))) }
                }
            } catch (e: Exception) {
                mainHandler.post { callback(Result.failure(e)) }
            }
        }
    }

    fun submitReport(studentId: String, itemName: String, location: String, issue: String, callback: (Result<JSONObject>) -> Unit) {
        thread {
            try {
                val urlObj = URL("$baseURL/api/report")
                val conn = urlObj.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.connectTimeout = 5000

                val payload = JSONObject().apply {
                    put("studentId", studentId)
                    put("itemName", itemName)
                    put("location", location)
                    put("issue", issue)
                }

                val writer = OutputStreamWriter(conn.outputStream)
                writer.write(payload.toString())
                writer.flush()
                writer.close()

                val responseCode = conn.responseCode
                val stream = if (responseCode == 200) conn.inputStream else conn.errorStream
                val reader = BufferedReader(InputStreamReader(stream))
                val response = reader.readText()
                reader.close()

                val json = JSONObject(response)
                if (responseCode == 200) {
                    mainHandler.post { callback(Result.success(json)) }
                } else {
                    val msg = json.optString("message", "Failed to submit report")
                    mainHandler.post { callback(Result.failure(Exception(msg))) }
                }
            } catch (e: Exception) {
                mainHandler.post { callback(Result.failure(e)) }
            }
        }
    }

    fun registerStudent(studentId: String, name: String, phone: String, callback: (Result<JSONObject>) -> Unit) {
        thread {
            try {
                val urlObj = URL("$baseURL/api/register")
                val conn = urlObj.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.connectTimeout = 5000

                val payload = JSONObject().apply {
                    put("studentId", studentId)
                    put("name", name)
                    put("phone", phone)
                }

                val writer = OutputStreamWriter(conn.outputStream)
                writer.write(payload.toString())
                writer.flush()
                writer.close()

                val responseCode = conn.responseCode
                val stream = if (responseCode == 200) conn.inputStream else conn.errorStream
                val reader = BufferedReader(InputStreamReader(stream))
                val response = reader.readText()
                reader.close()

                val json = JSONObject(response)
                if (responseCode == 200) {
                    mainHandler.post { callback(Result.success(json)) }
                } else {
                    val msg = json.optString("message", "Failed to register student details")
                    mainHandler.post { callback(Result.failure(Exception(msg))) }
                }
            } catch (e: Exception) {
                mainHandler.post { callback(Result.failure(e)) }
            }
        }
    }
}
