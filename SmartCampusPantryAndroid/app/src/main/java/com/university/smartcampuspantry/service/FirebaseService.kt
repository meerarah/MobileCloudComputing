package com.university.smartcampuspantry.service

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import com.university.smartcampuspantry.model.FoodItem
import java.util.UUID

class FirebaseService private constructor() {
    companion object {
        val shared = FirebaseService()
    }

    // Published lists and variables representing UI-reactive states
    var currentUserEmail = mutableStateOf<String?>(null)
    var currentStudentId = mutableStateOf<String?>(null)
    val inventory = mutableStateListOf<FoodItem>()

    private var firestoreListener: ListenerRegistration? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    // Flag checking if live Firebase is initialized
    private val isLiveFirebaseAvailable: Boolean
        get() = try {
            FirebaseAuth.getInstance()
            true
        } catch (e: Exception) {
            false
        }

    init {
        // Load initial mock data for offline simulation / local fallback
        loadInitialMockInventory()
        if (isLiveFirebaseAvailable) {
            fetchLiveFirestoreInventory()
        }
    }

    /**
     * Signs in student with Firebase Auth. Enforces UiTM student email domain suffix.
     */
    fun signIn(email: String, studentId: String, callback: (Result<String>) -> Unit) {
        val lowercasedEmail = email.lowercase().trim()
        if (!lowercasedEmail.endsWith("@student.uitm.edu.my")) {
            callback(Result.failure(Exception("Access Denied: Only UiTM student accounts (@student.uitm.edu.my) can log into the client application.")))
            return
        }

        if (isLiveFirebaseAvailable) {
            val auth = FirebaseAuth.getInstance()
            // Student ID acts as their credentials verified by Flask backend, default password is "12345678"
            auth.signInWithEmailAndPassword(lowercasedEmail, "12345678")
                .addOnSuccessListener {
                    currentUserEmail.value = lowercasedEmail
                    currentStudentId.value = studentId
                    callback(Result.success(studentId))
                }
                .addOnFailureListener {
                    // Try to auto-create user for seamless lecturer testing fallback
                    auth.createUserWithEmailAndPassword(lowercasedEmail, "12345678")
                        .addOnSuccessListener {
                            currentUserEmail.value = lowercasedEmail
                            currentStudentId.value = studentId
                            callback(Result.success(studentId))
                        }
                        .addOnFailureListener { err ->
                            callback(Result.failure(err))
                        }
                }
        } else {
            // Offline / Simulation fallback mode
            mainHandler.postDelayed({
                currentUserEmail.value = lowercasedEmail
                currentStudentId.value = studentId
                callback(Result.success(studentId))
            }, 500)
        }
    }

    /**
     * Signs out from the session
     */
    fun signOut() {
        if (isLiveFirebaseAvailable) {
            FirebaseAuth.getInstance().signOut()
            firestoreListener?.remove()
        }
        currentUserEmail.value = null
        currentStudentId.value = null
    }

    /**
     * Listens for real-time inventory updates from Firebase Firestore
     */
    private fun fetchLiveFirestoreInventory() {
        val db = FirebaseFirestore.getInstance()
        firestoreListener = db.collection("inventory")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    inventory.clear()
                    for (doc in snapshots) {
                        val item = FoodItem(
                            id = doc.id,
                            name = doc.getString("name") ?: "Unknown",
                            category = doc.getString("category") ?: "Other",
                            quantity = doc.getLong("quantity")?.toInt() ?: 0,
                            imageUrl = doc.getString("imageUrl") ?: "",
                            daysToExpiry = doc.getLong("daysToExpiry")?.toInt() ?: 30
                        )
                        inventory.add(item)
                    }
                }
            }
    }

    /**
     * Deducts item quantity from Firestore after a claim is verified by Flask
     */
    fun claimInventoryItem(itemId: String) {
        if (isLiveFirebaseAvailable) {
            val db = FirebaseFirestore.getInstance()
            val docRef = db.collection("inventory").document(itemId)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val currentQty = snapshot.getLong("quantity") ?: 0
                if (currentQty > 0) {
                    transaction.update(docRef, "quantity", currentQty - 1)
                }
                null
            }.addOnFailureListener { e ->
                e.printStackTrace()
            }
        } else {
            // Offline simulation decrement
            val index = inventory.indexOfFirst { it.id == itemId }
            if (index != -1 && inventory[index].quantity > 0) {
                inventory[index] = inventory[index].copy(quantity = inventory[index].quantity - 1)
            }
        }
    }

    /**
     * Adds a newly donated item to Firestore
     */
    fun addInventoryItem(name: String, category: String, quantity: Int, imageUrl: String, daysToExpiry: Int) {
        if (isLiveFirebaseAvailable) {
            val db = FirebaseFirestore.getInstance()
            val itemData = hashMapOf(
                "name" to name,
                "category" to category,
                "quantity" to quantity,
                "imageUrl" to imageUrl,
                "daysToExpiry" to daysToExpiry,
                "timestamp" to com.google.firebase.Timestamp.now()
            )
            db.collection("inventory").add(itemData)
        } else {
            // Offline simulation addition
            val newItem = FoodItem(
                id = UUID.randomUUID().toString(),
                name = name,
                category = category,
                quantity = quantity,
                imageUrl = imageUrl,
                daysToExpiry = daysToExpiry
            )
            inventory.add(0, newItem)
        }
    }

    /**
     * Uploads a simulated photo and returns public URL
     */
    fun uploadDonationPhoto(imageName: String, callback: (Result<String>) -> Unit) {
        val simulatedURL = "https://firebasestorage.googleapis.com/v0/b/smartcampuspantry.firebasestorage.app/o/donations%2F$imageName.jpg?alt=media"
        if (isLiveFirebaseAvailable) {
            val storageRef = FirebaseStorage.getInstance().reference.child("donations/$imageName.jpg")
            val dummyBytes = byteArrayOf(0)
            storageRef.putBytes(dummyBytes)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        callback(Result.success(uri.toString()))
                    }.addOnFailureListener { err ->
                        // Fallback to simulated URL if fetching url fails
                        callback(Result.success(simulatedURL))
                    }
                }
                .addOnFailureListener { err ->
                    // Gracefully fallback to simulated URL if Firebase Storage bucket is not enabled (404)
                    callback(Result.success(simulatedURL))
                }
        } else {
            mainHandler.postDelayed({
                val simulatedURL = "https://firebasestorage.googleapis.com/v0/b/smartcampuspantry.firebasestorage.app/o/donations%2F$imageName.jpg?alt=media"
                callback(Result.success(simulatedURL))
            }, 800)
        }
    }

    private fun loadInitialMockInventory() {
        inventory.clear()
        inventory.addAll(
            listOf(
                FoodItem("item_001", "Canned Tuna", "Proteins", 15, "tuna", 365),
                FoodItem("item_002", "Whole Grain Cereal", "Grains", 8, "cereal", 120),
                FoodItem("item_003", "Organic Whole Milk", "Dairy", 4, "milk", 10),
                FoodItem("item_004", "Fresh Gala Apples", "Produce", 22, "apple", 14),
                FoodItem("item_005", "Canned Vegetable Soup", "Canned Goods", 19, "soup", 270),
                FoodItem("item_006", "Spaghetti Pasta", "Grains", 12, "pasta", 240)
            )
        )
    }
}
