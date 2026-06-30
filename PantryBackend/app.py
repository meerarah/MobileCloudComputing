from flask import Flask, jsonify, request, render_template
from flask_cors import CORS
import random
import os
import datetime
from config import get_db_connection

app = Flask(__name__)
CORS(app)

# Global Request Log Memory Stream
HTTP_REQUEST_LOGS = []

def record_log(method, endpoint, detail, status=200):
    timestamp = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    HTTP_REQUEST_LOGS.append({
        "timestamp": timestamp,
        "method": method,
        "endpoint": endpoint,
        "detail": detail,
        "status": status
    })
    # Keep last 50 logs in queue
    if len(HTTP_REQUEST_LOGS) > 50:
        HTTP_REQUEST_LOGS.pop(0)

# Helper list of simulated food items for the ML Kit analyzer
SIMULATED_FOOD_MODELS = [
    {"keywords": ["tuna", "fish", "canned"], "name": "Canned Tuna", "category": "Proteins", "shelf_life": 365},
    {"keywords": ["cereal", "grain", "oats"], "name": "Whole Grain Cereal", "category": "Grains", "shelf_life": 120},
    {"keywords": ["soup", "can", "vegetable"], "name": "Canned Vegetable Soup", "category": "Canned Goods", "shelf_life": 270},
    {"keywords": ["milk", "dairy", "carton"], "name": "Organic Whole Milk", "category": "Dairy", "shelf_life": 10},
    {"keywords": ["apple", "fruit", "gala"], "name": "Fresh Gala Apple", "category": "Produce", "shelf_life": 14},
    {"keywords": ["beans", "legumes", "black"], "name": "Canned Black Beans", "category": "Proteins", "shelf_life": 360},
    {"keywords": ["rice", "jasmine", "bag"], "name": "Jasmine Rice (1kg)", "category": "Grains", "shelf_life": 180},
    {"keywords": ["pasta", "spaghetti", "box"], "name": "Spaghetti Pasta", "category": "Grains", "shelf_life": 240}
]

@app.route('/')
def home():
    """Index page rendering the Central Admin Dashboard console."""
    record_log("GET", "/", "Loaded Admin Operations Dashboard Console", 200)
    return render_template('index.html')

@app.route('/api/info')
def api_info():
    """Returns the In-House REST API server status in JSON."""
    record_log("GET", "/api/info", "Queried service metadata node details", 200)
    return jsonify({
        "status": "online",
        "service": "Smart Campus Pantry In-House Web Service",
        "version": "1.0.0",
        "mcc_features": [
            "Offloaded ML Food Image Analysis (/api/analyze-food)",
            "Student Eligibility & Cloud Database Claim Management (/api/claim)"
        ]
    })

@app.route('/api/students', methods=['GET'])
def get_all_students():
    """Retrieves all student profiles from SQLite database (for the Admin Portal)."""
    conn = get_db_connection()
    students = conn.execute('SELECT * FROM students').fetchall()
    conn.close()
    
    students_list = []
    for s in students:
        students_list.append({
            "studentId": s['student_id'],
            "name": s['name'],
            "phone": s['phone'],
            "eligible": bool(s['eligible']),
            "impactPoints": s['impact_points'],
            "claimsThisWeek": s['claims_this_week']
        })
        
    return jsonify({
        "success": True,
        "students": students_list
    })

@app.route('/api/student/<student_id>', methods=['GET'])
def get_student(student_id):
    """Retrieves student details, eligibility, impact points, and claim counts from SQLite."""
    conn = get_db_connection()
    student = conn.execute('SELECT * FROM students WHERE student_id = ?', (student_id,)).fetchone()
    conn.close()
    
    if student is None:
        record_log("GET", f"/api/student/{student_id}", f"Failed to fetch profile: ID not found", 404)
        return jsonify({
            "success": False,
            "message": f"Student with ID {student_id} not found."
        }), 404
        
    record_log("GET", f"/api/student/{student_id}", f"Fetched student profile: {student['name']}", 200)
    return jsonify({
        "success": True,
        "studentId": student['student_id'],
        "name": student['name'],
        "eligible": bool(student['eligible']),
        "impactPoints": student['impact_points'],
        "claimsThisWeek": student['claims_this_week'],
        "maxWeeklyClaims": 3
    })

@app.route('/api/student/toggle', methods=['POST'])
def toggle_student_eligibility():
    """Admin feature: Toggles a student's eligibility status inside SQLite database."""
    data = request.json or {}
    student_id = data.get('studentId')
    
    if not student_id:
        record_log("POST", "/api/student/toggle", "Failed toggle request: Missing studentId", 400)
        return jsonify({"success": False, "message": "Missing studentId"}), 400
        
    conn = get_db_connection()
    cursor = conn.cursor()
    
    student = cursor.execute('SELECT eligible, name FROM students WHERE student_id = ?', (student_id,)).fetchone()
    if not student:
        conn.close()
        record_log("POST", "/api/student/toggle", f"Failed toggle request: Student {student_id} not found", 404)
        return jsonify({"success": False, "message": "Student not found"}), 404
        
    new_status = 0 if student['eligible'] else 1
    cursor.execute('UPDATE students SET eligible = ? WHERE student_id = ?', (new_status, student_id))
    conn.commit()
    conn.close()
    
    record_log("POST", "/api/student/toggle", f"Toggled student eligibility: {student['name']} ({student_id}) to {'Eligible' if new_status else 'Suspended'}", 200)
    return jsonify({
        "success": True,
        "eligible": bool(new_status),
        "message": f"Successfully toggled student {student_id} eligibility."
    })

@app.route('/api/register', methods=['POST'])
def register_student():
    """Registers a new student profile in the SQLite database."""
    data = request.json or {}
    student_id = data.get('studentId')
    name = data.get('name')
    phone = data.get('phone')
    
    if not student_id or not name or not phone:
        record_log("POST", "/api/register", "Registration rejected: Missing studentId, name, or phone", 400)
        return jsonify({"success": False, "message": "Missing required fields: studentId, name, and phone"}), 400
        
    conn = get_db_connection()
    cursor = conn.cursor()
    
    # Check if student already exists
    existing = cursor.execute('SELECT student_id FROM students WHERE student_id = ?', (student_id,)).fetchone()
    if existing:
        conn.close()
        record_log("POST", "/api/register", f"Registration failed: Student {student_id} is already registered", 400)
        return jsonify({"success": False, "message": f"Student ID '{student_id}' is already registered."}), 400
        
    cursor.execute('''
        INSERT INTO students (student_id, name, phone, eligible, impact_points, claims_this_week)
        VALUES (?, ?, ?, 1, 0, 0)
    ''', (student_id, name, phone))
    conn.commit()
    conn.close()
    
    record_log("POST", "/api/register", f"Registered student: {name} ({student_id}) with phone {phone}", 200)
    return jsonify({
        "success": True,
        "message": "Student registered successfully in the system!"
    })

@app.route('/api/claim', methods=['POST'])
def process_claim():
    """
    Validates eligibility, processes student claims (max 3/week), and rewards impact points.
    Expected payload: { "studentId": "std_1001", "itemId": "item_id_123", "itemName": "Canned Tuna", "location": "Kolej Perindu Hub" }
    """
    data = request.json or {}
    student_id = data.get('studentId')
    item_id = data.get('itemId')
    item_name = data.get('itemName', 'Unknown Food Item')
    location = data.get('location', 'Kolej Perindu Hub')
    
    if not student_id or not item_id:
        record_log("POST", "/api/claim", "Claim rejected: Missing studentId or itemId", 400)
        return jsonify({
            "success": False,
            "message": "Missing required fields: studentId and itemId"
        }), 400
        
    conn = get_db_connection()
    cursor = conn.cursor()
    
    # 1. Fetch student record
    student = cursor.execute('SELECT * FROM students WHERE student_id = ?', (student_id,)).fetchone()
    
    if student is None:
        conn.close()
        record_log("POST", "/api/claim", f"Claim rejected: student ID {student_id} not registered", 404)
        return jsonify({
            "success": False,
            "message": f"Student ID '{student_id}' is not registered in the system."
        }), 404
        
    # 2. Check general eligibility (suspensions, academic hold, etc.)
    if not student['eligible']:
        conn.close()
        record_log("POST", "/api/claim", f"Claim rejected: {student['name']} ({student_id}) is suspended", 403)
        return jsonify({
            "success": False,
            "message": "Access Denied: Student account is currently not eligible for pantry claims."
        }), 403
        
    # 3. Check weekly limit (MCC business logic rule: Max 3 claims per week)
    current_claims = student['claims_this_week']
    if current_claims >= 3:
        conn.close()
        record_log("POST", "/api/claim", f"Claim rejected: {student['name']} has reached weekly quota limit (3/3)", 400)
        return jsonify({
            "success": False,
            "message": f"Claim Rejected: Student '{student['name']}' has already reached the weekly limit of 3 claims."
        }), 400
        
    # 4. Success Path: Increment claim count, award 15 impact points for proper checkout
    new_claims_count = current_claims + 1
    new_points = student['impact_points'] + 15
    
    try:
        # Update student record
        cursor.execute('''
            UPDATE students 
            SET claims_this_week = ?, impact_points = ?
            WHERE student_id = ?
        ''', (new_claims_count, new_points, student_id))
        
        # Log claim transaction
        cursor.execute('''
            INSERT INTO claims (student_id, item_id, item_name, location)
            VALUES (?, ?, ?, ?)
        ''', (student_id, item_id, item_name, location))
        
        conn.commit()
        record_log("POST", "/api/claim", f"Claim APPROVED: {student['name']} pickup '{item_name}' at {location} (Remaining: {3 - new_claims_count})", 200)
    except Exception as e:
        conn.rollback()
        conn.close()
        record_log("POST", "/api/claim", f"Claim failed: Internal SQLite Database error: {str(e)}", 500)
        return jsonify({
            "success": False,
            "message": f"Internal database error occurred: {str(e)}"
        }), 500
        
    conn.close()
    
    return jsonify({
        "success": True,
        "message": f"Claim for '{item_name}' approved successfully!",
        "pointsAwarded": 15,
        "totalImpactPoints": new_points,
        "claimsThisWeek": new_claims_count,
        "remainingClaims": 3 - new_claims_count
    })

@app.route('/api/analyze-food', methods=['POST'])
def analyze_food():
    """
    MCC Offloaded Task: Performs image recognition and expiry forecasting.
    Receives an image URL (representing upload to Cloud Storage) or filename.
    Expected payload: { "imageUrl": "https://firebase-storage/.../apple.png" }
    """
    data = request.json or {}
    image_url = data.get('imageUrl', '').lower()
    
    if not image_url:
        record_log("POST", "/api/analyze-food", "ML Analysis rejected: Missing imageUrl", 400)
        return jsonify({
            "success": False,
            "message": "Missing required field: imageUrl"
        }), 400
        
    # Simulate heavy processing delay (MCC model: cloud does the heavy lifting, saving phone CPU/battery)
    import time
    time.sleep(0.5) 
    
    detected_item = None
    for item in SIMULATED_FOOD_MODELS:
        if any(keyword in image_url for keyword in item["keywords"]):
            detected_item = item
            break
            
    if not detected_item:
        detected_item = random.choice(SIMULATED_FOOD_MODELS)
        
    confidence = round(random.uniform(0.85, 0.99), 2)
    
    record_log("POST", "/api/analyze-food", f"Offloaded ML Analysis: Classifed image as '{detected_item['name']}' ({int(confidence*100)}% confidence)", 200)
    return jsonify({
        "success": True,
        "analyzedItem": {
            "itemName": detected_item["name"],
            "category": detected_item["category"],
            "daysToExpiry": detected_item["shelf_life"],
            "confidenceScore": confidence,
            "offloadNode": "In-House Cloud Edge Node A"
        }
    })

@app.route('/api/donate-points', methods=['POST'])
def award_donation_points():
    """Awards student impact points for donating items and logs the restock transaction."""
    data = request.json or {}
    student_id = data.get('studentId')
    points = data.get('points', 50)
    item_name = data.get('itemName', 'Food Item')
    quantity = data.get('quantity', 1)
    image_url = data.get('imageUrl', 'tuna')
    location = data.get('location', 'Kolej Perindu Hub')
    
    if not student_id:
        record_log("POST", "/api/donate-points", "Donation failed: Missing studentId", 400)
        return jsonify({"success": False, "message": "Missing studentId"}), 400
        
    conn = get_db_connection()
    cursor = conn.cursor()
    
    student = cursor.execute('SELECT * FROM students WHERE student_id = ?', (student_id,)).fetchone()
    if not student:
        conn.close()
        record_log("POST", "/api/donate-points", f"Donation failed: Student {student_id} not registered", 404)
        return jsonify({"success": False, "message": "Student not found"}), 404
        
    new_points = student['impact_points'] + points
    cursor.execute('UPDATE students SET impact_points = ? WHERE student_id = ?', (new_points, student_id))
    
    # Record restock history in SQLite
    cursor.execute('''
        INSERT INTO restocks (restocker_name, item_name, quantity, image_url, location)
        VALUES (?, ?, ?, ?, ?)
    ''', (student['name'], item_name, quantity, image_url, location))
    
    conn.commit()
    conn.close()
    
    record_log("POST", "/api/donate-points", f"Restocked: {student['name']} added {quantity}x {item_name} at {location} (+{points} pts)", 200)
    return jsonify({
        "success": True,
        "message": f"Successfully awarded {points} points to {student['name']}!",
        "newPoints": new_points
    })

@app.route('/api/claims', methods=['GET'])
def get_all_claims():
    """Retrieves all claims (checkout records) with student info from SQLite database."""
    conn = get_db_connection()
    claims = conn.execute('''
        SELECT c.claim_id, c.student_id, s.name, s.phone, c.item_name, c.location, c.timestamp
        FROM claims c
        JOIN students s ON c.student_id = s.student_id
        ORDER BY c.timestamp DESC
    ''').fetchall()
    conn.close()
    
    claims_list = []
    for c in claims:
        claims_list.append({
            "claimId": c['claim_id'],
            "studentId": c['student_id'],
            "name": c['name'],
            "phone": c['phone'],
            "itemName": c['item_name'],
            "location": c['location'],
            "timestamp": c['timestamp']
        })
        
    return jsonify({
        "success": True,
        "claims": claims_list
    })

@app.route('/api/restocks', methods=['GET'])
def get_all_restocks():
    """Retrieves all restocking records from SQLite database."""
    conn = get_db_connection()
    restocks = conn.execute('SELECT * FROM restocks ORDER BY timestamp DESC').fetchall()
    conn.close()
    
    restocks_list = []
    for r in restocks:
        restocks_list.append({
            "restockId": r['restock_id'],
            "restockerName": r['restocker_name'],
            "itemName": r['item_name'],
            "quantity": r['quantity'],
            "imageUrl": r['image_url'],
            "location": r['location'],
            "timestamp": r['timestamp']
        })
        
    return jsonify({
        "success": True,
        "restocks": restocks_list
    })

@app.route('/api/report', methods=['POST'])
def submit_report():
    """Receives an issue report about a pantry item from a student and logs it in SQLite."""
    data = request.json or {}
    student_id = data.get('studentId')
    item_name = data.get('itemName')
    location = data.get('location', 'Kolej Perindu Hub')
    issue = data.get('issue')
    
    if not student_id or not item_name or not issue:
        record_log("POST", "/api/report", "Report failed: Missing studentId, itemName, or issue", 400)
        return jsonify({"success": False, "message": "Missing required fields: studentId, itemName, or issue"}), 400
        
    conn = get_db_connection()
    cursor = conn.cursor()
    
    student = cursor.execute('SELECT name FROM students WHERE student_id = ?', (student_id,)).fetchone()
    student_name = student['name'] if student else "Unknown Student"
    
    cursor.execute('''
        INSERT INTO reports (student_id, student_name, item_name, location, issue_description)
        VALUES (?, ?, ?, ?, ?)
    ''', (student_id, student_name, item_name, location, issue))
    
    conn.commit()
    conn.close()
    
    record_log("POST", "/api/report", f"Report submitted: {student_name} reported '{item_name}' at {location}: {issue}", 200)
    return jsonify({
        "success": True,
        "message": "Report submitted successfully. Thank you for your feedback!"
    })

@app.route('/api/reports', methods=['GET'])
def get_all_reports():
    """Retrieves all submitted pantry reports from SQLite database."""
    conn = get_db_connection()
    reports = conn.execute('SELECT * FROM reports ORDER BY timestamp DESC').fetchall()
    conn.close()
    
    reports_list = []
    for r in reports:
        reports_list.append({
            "reportId": r['report_id'],
            "studentId": r['student_id'],
            "studentName": r['student_name'],
            "itemName": r['item_name'],
            "location": r['location'],
            "issueDescription": r['issue_description'],
            "timestamp": r['timestamp']
        })
        
    return jsonify({
        "success": True,
        "reports": reports_list
    })

@app.route('/api/reset-claims', methods=['POST'])
def reset_claims():
    """Helper to reset weekly claim counters for testing."""
    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute('UPDATE students SET claims_this_week = 0')
    conn.commit()
    conn.close()
    
    record_log("POST", "/api/reset-claims", "Admin action: Reset weekly claim counters for all students to 0", 200)
    return jsonify({
        "success": True,
        "message": "Successfully reset all student claim counts to 0."
    })

@app.route('/api/logs', methods=['GET'])
def get_logs():
    """Exposes request logs history for real-time monitoring."""
    return jsonify({
        "success": True,
        "logs": list(reversed(HTTP_REQUEST_LOGS)) # Newest logs first
    })

if __name__ == '__main__':
    # Run on all network interfaces (port 5000) for testing access from local VM or iOS simulator
    app.run(host='0.0.0.0', port=5000, debug=True)
