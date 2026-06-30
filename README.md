# Smart Campus Pantry (ITT632 Course Project)

Welcome to the **Smart Campus Pantry** codebase! This project is engineered specifically to satisfy all Mobile Cloud Computing (MCC) requirements under course code **ITT632** using a native **Android client**.

---

## 📋 1. Core Architecture & Compliance Matrix

This project demonstrates the principles of **Mobile Cloud Computing (MCC)** by combining a lightweight mobile client frontend with high-performance, real-time cloud data storage and custom web services that execute intensive processing rules.

| Core ITT632 Criteria | Project Implementation | File & Folder References |
| :--- | :--- | :--- |
| **Mobile Client (Android)** | Jetpack Compose & Kotlin application with responsive view layouts, dark mode options, and clean navigation. | `c:\SmartCampusPantry\SmartCampusPantryAndroid\` |
| **3+ Third-Party Cloud Services** | 1. **Firebase Authentication**: Secures student session login.<br>2. **Firebase Firestore Database**: Syncs live pantry quantities and stock profiles.<br>3. **Firebase Cloud Storage**: Hosts raw food item photos for donations.<br>4. **Google Maps SDK**: Location routing to the campus hub. | `c:\SmartCampusPantry\SmartCampusPantryAndroid\app\src\main\java\com\university\smartcampuspantry\service\FirebaseService.kt` |
| **1+ In-House REST API** | Custom Python Flask API exposing secure endpoints for processing pantry claims, checking eligibility logs, and awarding donor scores. | `c:\SmartCampusPantry\PantryBackend\app.py` |
| **MCC Workload Management** | Image-based classification & expiry analysis are offloaded to our cloud REST service instead of consuming local mobile processor power. | `c:\SmartCampusPantry\PantryBackend\app.py` (Line 228: `/api/analyze-food`) |

---

## 🛠️ 2. Repository Map

```text
c:\SmartCampusPantry\
├── PantryBackend\              # Custom In-House Python Flask REST API
│   ├── app.py                  # API endpoints and mock ML heuristics logic
│   ├── config.py               # SQLite connection helper
│   ├── database.db             # Local SQLite database (Generated after setup)
│   ├── init_db.py              # Script to build and seed database tables
│   ├── requirements.txt        # Backend dependencies
│   └── README.md               # Backend execution manual
│
└── SmartCampusPantryAndroid\   # Android Jetpack Compose Frontend Source
    ├── app/
    │   ├── src/main/
    │   │   ├── AndroidManifest.xml # Permissions (INTERNET) & HTTP cleartext config
    │   │   └── java/com/university/smartcampuspantry/
    │   │       ├── MainActivity.kt # Routes to login or main dashboard
    │   │       ├── model/
    │   │       │   ├── FoodItem.kt # Model mapping to Firestore stocks
    │   │       │   └── StudentProfile.kt # Model for student profile details
    │   │       ├── service/
    │   │       │   ├── APIService.kt # Connects to Flask on http://10.0.2.2:5000
    │   │       │   └── FirebaseService.kt # Controls Auth, Firestore, and Storage
    │   │       └── ui/view/
    │   │           ├── LoginScreen.kt # Student login page (zahra@student.uitm.edu.my)
    │   │           ├── MainScreen.kt # Navigation tabs bar
    │   │           ├── InventoryScreen.kt # Shelf stock list and claim checkout
    │   │           ├── DonationScreen.kt # Camera scan offloaded ML recognition
    │   │           ├── ProfileScreen.kt # Points tracker and quota reset (demo)
    │   │           └── MapScreen.kt # Directions map to Kolej Perindu Hub
    │   └── google-services.json # Firebase project configurations
    ├── build.gradle.kts
    └── settings.gradle.kts
```

---

## 🚀 3. Quickstart Guide

### Running the Backend REST API
Ensure you have Python 3.8+ installed, then:
1. Open your command prompt in the `PantryBackend` folder.
2. Install libraries:
   ```bash
   pip install -r requirements.txt
   ```
3. Initialize the database:
   ```bash
   python init_db.py
   ```
4. Start the web service:
   ```bash
   python app.py
   ```
The backend is now live at `http://localhost:5000` (allowing incoming connections from simulators and local network nodes).

### Running the Android Client on the Emulator (VS Code)
1. Open the `SmartCampusPantryAndroid` folder in VS Code.
2. Start your local Android Emulator via command line or AVD Manager:
   ```bash
   emulator -avd <your_emulator_avd_name>
   ```
3. In the VS Code terminal, build and install the debug APK onto your running emulator:
   ```bash
   ./gradlew installDebug
   ```
4. Open the installed "Smart Campus Pantry" app on your emulator and sign in with:
   * **Email**: `zahra@student.uitm.edu.my`
   * **Student ID**: `std_1001`
   * **Password**: `12345678` (configured as default in Auth)

---

## 🔄 4. MCC Interaction Workflows

### The "Smart Claim" Flow (Quota Verification)
```text
1. Student clicks [Claim Item] on the Jetpack Compose interface.
2. App sends a POST request to In-House REST API (/api/claim) on port 5000.
3. Flask backend queries the SQLite Database to check if:
   - The student ID exists
   - Student's general eligibility is active
   - Student has made LESS THAN 3 claims this week
4. If approved:
   - Flask updates claims count & adds 15 Impact Points in SQLite DB.
   - Flask returns approval response.
   - Android deducts the quantity from real-time Firebase Firestore and refreshes the student profile.
5. If denied:
   - Flask returns descriptive error JSON.
   - Android displays a Toast warning alert explaining why the request failed.
```

### The "Smart Donation" Flow (Offloaded Processing)
```text
1. Student selects a sample photo (e.g., Tuna Can) in [Donate] view.
2. Android uploads the photo to Firebase Cloud Storage and receives a storage URL.
3. Android sends the image URL to Flask API (/api/analyze-food), offloading computation.
4. Flask analyzes the filename/URL pattern (simulating Vision/ML classification), computes the food category, predicts standard expiry timelines, and returns the metadata.
5. Android shows a preview of the recognized food item, category, and confidence level.
6. Student confirms donation:
   - Item is added directly to Firebase Firestore inventory.
   - Points are awarded in the in-house DB via the /api/donate-points API.
```
