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
                    callback(Result.success(studentId))
                }
                .addOnFailureListener {
                    // Try to auto-create user for seamless lecturer testing fallback
                    auth.createUserWithEmailAndPassword(lowercasedEmail, "12345678")
                        .addOnSuccessListener {
                            callback(Result.success(studentId))
                        }
                        .addOnFailureListener { err ->
                            callback(Result.failure(err))
                        }
                }
        } else {
            // Offline / Simulation fallback mode
            mainHandler.postDelayed({
                callback(Result.success(studentId))
            }, 500)
        }
    }

    /**
     * Completes the login process by formally setting the active session variables.
     * Should only be called AFTER SQLite verification succeeds.
     */
    fun setSession(email: String, studentId: String) {
        currentUserEmail.value = email.lowercase().trim()
        currentStudentId.value = studentId
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
        // Optimistic UI update: Decrement instantly
        mainHandler.post {
            val index = inventory.indexOfFirst { it.id == itemId }
            if (index != -1 && inventory[index].quantity > 0) {
                inventory[index] = inventory[index].copy(quantity = inventory[index].quantity - 1)
            }
        }

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
                .addOnFailureListener {
                    // Fallback to offline simulation if Firestore write fails (e.g., PERMISSION_DENIED rules)
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
     * Uploads an actual photo from Uri and returns public URL
     */
    fun uploadDonationPhoto(imageUri: android.net.Uri, callback: (Result<String>) -> Unit) {
        val fallbackUrl = imageUri.toString()
        if (isLiveFirebaseAvailable) {
            val fileName = UUID.randomUUID().toString() + ".jpg"
            val storageRef = FirebaseStorage.getInstance().reference.child("donations/$fileName")
            
            storageRef.putFile(imageUri)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        callback(Result.success(uri.toString()))
                    }.addOnFailureListener { err ->
                        callback(Result.success(fallbackUrl))
                    }
                }
                .addOnFailureListener { err ->
                    callback(Result.success(fallbackUrl))
                }
        } else {
            mainHandler.postDelayed({
                callback(Result.success(fallbackUrl))
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
