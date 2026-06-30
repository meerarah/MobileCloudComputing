# Smart Campus Pantry Backend - In-House Web Service

This Python Flask web service manages student profiles, records claims, validates eligibility criteria, and executes cloud-offloaded image analysis to satisfy Mobile Cloud Computing (MCC) requirements.

---

## Technical Features
1. **Student Eligibility System**: SQLite-backed verification enforcing weekly claim quotas (maximum 3 claims per week).
2. **Impact Points Engine**: Accrues student loyalty points for checking out pantry stocks and donations.
3. **MCC Image Recognition API**: Offloads intensive deep learning/computer vision tasks from mobile devices to cloud compute resources.

---

## Setup Instructions

### Prerequisites
- Python 3.8 or higher installed on your computer.

### Step 1: Install Dependencies
Open your command prompt or terminal in this folder and run:
```bash
pip install -r requirements.txt
```

### Step 2: Initialize Database
Seeds the local SQLite database with test students:
```bash
python init_db.py
```

### Step 3: Run the Server
Starts the local development server:
```bash
python app.py
```
By default, the server runs on `http://localhost:5000` (or `http://0.0.0.0:5000` to accept local network devices and Android emulators).

---

## Mock Student Data for Testing

| Student ID | Name | Eligibility Status | Starting Impact Points | Current Claims This Week |
| :--- | :--- | :--- | :--- | :--- |
| `std_1001` | Muhammad Ammar | Eligible | 120 | 1 / 3 |
| `std_1002` | Nur Fatimah | Eligible | 350 | 3 / 3 (Limit Reached) |
| `std_1003` | Khairul Anuar | Suspended | 0 | 0 / 3 |
| `std_1004` | Siti Aisha | Eligible | 80 | 0 / 3 |

---

## Endpoint Reference

### 1. Home Node Info
- **URL**: `GET /`
- **Response**: Details on running features and services.

### 2. Fetch Student Profile
- **URL**: `GET /api/student/<student_id>`
- **Example**: `GET http://localhost:5000/api/student/std_1001`

### 3. Claim Pantry Item
- **URL**: `POST /api/claim`
- **Body**:
  ```json
  {
    "studentId": "std_1001",
    "itemId": "mock_canned_tuna_01",
    "itemName": "Canned Tuna"
  }
  ```

### 4. MCC Offloaded Image Analyzer
- **URL**: `POST /api/analyze-food`
- **Body**:
  ```json
  {
    "imageUrl": "https://storage.google.com/.../canned_tuna_photo.jpg"
  }
  ```
