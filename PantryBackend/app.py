from flask import Flask, jsonify, request, render_template
from flask_cors import CORS
from google.cloud import firestore
import random
import os
from config import get_firestore_client
import datetime
import uuid

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

@app.after_request
def add_header(response):
    # Prevent caching for API responses
    if request.path.startswith('/api/'):
        response.headers['Cache-Control'] = 'no-store, no-cache, must-revalidate, max-age=0'
        response.headers['Pragma'] = 'no-cache'
        response.headers['Expires'] = '0'
    return response

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
    """Retrieves all student profiles from Firestore database (for the Admin Portal)."""
    try:
        db = get_firestore_client()
        docs = db.collection('students').stream()
        
        students_list = []
        for doc in docs:
            s = doc.to_dict()
            students_list.append({
                "studentId": s.get('student_id', doc.id),
                "name": s.get('name', ''),
                "phone": s.get('phone', ''),
                "eligible": bool(s.get('eligible', 1)),
                "impactPoints": s.get('impact_points', 0),
                "claimsThisWeek": s.get('claims_this_week', 0)
            })
            
        return jsonify({
            "success": True,
            "students": students_list
        })
    except Exception as e:
        return jsonify({"success": False, "message": str(e)}), 500

@app.route('/api/student/<student_id>', methods=['GET'])
def get_student(student_id):
    """Retrieves student details, eligibility, impact points, and claim counts from Firestore."""
    db = get_firestore_client()
    doc_ref = db.collection('students').document(student_id)
    doc = doc_ref.get()
    
    if not doc.exists:
        record_log("GET", f"/api/student/{student_id}", f"Failed to fetch profile: ID not found", 404)
        return jsonify({
            "success": False,
            "message": f"Student with ID {student_id} not found."
        }), 404
        
    student = doc.to_dict()
    record_log("GET", f"/api/student/{student_id}", f"Fetched student profile: {student.get('name')}", 200)
    return jsonify({
        "success": True,
        "studentId": student.get('student_id', student_id),
        "name": student.get('name', ''),
        "eligible": bool(student.get('eligible', 1)),
        "impactPoints": student.get('impact_points', 0),
        "claimsThisWeek": student.get('claims_this_week', 0),
        "maxWeeklyClaims": 3
    })

@app.route('/api/student/<student_id>', methods=['DELETE'])
def delete_student(student_id):
    """Deletes a student profile from Firestore."""
    db = get_firestore_client()
    db.collection('students').document(student_id).delete()
    
    record_log("DELETE", f"/api/student/{student_id}", f"Deleted student profile", 200)
    return jsonify({
        "success": True,
        "message": f"Student with ID {student_id} successfully deleted."
    })

@app.route('/api/student/<student_id>', methods=['PUT'])
def update_student(student_id):
    """Updates a student's profile details in Firestore."""
    data = request.json or {}
    
    db = get_firestore_client()
    doc_ref = db.collection('students').document(student_id)
    doc = doc_ref.get()
    
    if not doc.exists:
        return jsonify({"success": False, "message": "Student not found"}), 404
        
    student = doc.to_dict()
    new_name = data.get('name', student.get('name'))
    new_phone = data.get('phone', student.get('phone'))
    new_points = data.get('impactPoints', student.get('impact_points'))
    
    doc_ref.update({
        "name": new_name,
        "phone": new_phone,
        "impact_points": new_points
    })
    
    record_log("PUT", f"/api/student/{student_id}", f"Updated student profile", 200)
    return jsonify({
        "success": True,
        "message": "Student successfully updated."
    })

@app.route('/api/student/toggle', methods=['POST'])
def toggle_student_eligibility():
    """Admin feature: Toggles a student's eligibility status inside Firestore database."""
    data = request.json or {}
    student_id = data.get('studentId')
    
    if not student_id:
        record_log("POST", "/api/student/toggle", "Failed toggle request: Missing studentId", 400)
        return jsonify({"success": False, "message": "Missing studentId"}), 400
        
    db = get_firestore_client()
    doc_ref = db.collection('students').document(student_id)
    doc = doc_ref.get()
    
    if not doc.exists:
        record_log("POST", "/api/student/toggle", f"Failed toggle request: Student {student_id} not found", 404)
        return jsonify({"success": False, "message": "Student not found"}), 404
        
    student = doc.to_dict()
    new_status = 0 if student.get('eligible', 1) else 1
    
    doc_ref.update({"eligible": new_status})
    
    record_log("POST", "/api/student/toggle", f"Toggled student eligibility: {student.get('name')} ({student_id}) to {'Eligible' if new_status else 'Suspended'}", 200)
    return jsonify({
        "success": True,
        "eligible": bool(new_status),
        "message": f"Successfully toggled student {student_id} eligibility."
    })

@app.route('/api/register', methods=['POST'])
def register_student():
    """Registers a new student profile in the Firestore database."""
    data = request.json or {}
    student_id = data.get('studentId')
    name = data.get('name')
    phone = data.get('phone')
    
    if not student_id or not name or not phone:
        record_log("POST", "/api/register", "Registration rejected: Missing studentId, name, or phone", 400)
        return jsonify({"success": False, "message": "Missing required fields: studentId, name, and phone"}), 400
        
    db = get_firestore_client()
    doc_ref = db.collection('students').document(student_id)
    doc = doc_ref.get()
    
    # Check if student already exists
    if doc.exists:
        record_log("POST", "/api/register", f"Registration failed: Student {student_id} is already registered", 400)
        return jsonify({"success": False, "message": f"Student ID '{student_id}' is already registered."}), 400
        
    doc_ref.set({
        "student_id": student_id,
        "name": name,
        "phone": phone,
        "eligible": 1,
        "impact_points": 0,
        "claims_this_week": 0
    })
    
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
    location = data.get('location', 'Kolej Mawar Hub')
    
    if not student_id or not item_id:
        record_log("POST", "/api/claim", "Claim rejected: Missing studentId or itemId", 400)
        return jsonify({
            "success": False,
            "message": "Missing required fields: studentId and itemId"
        }), 400
        
    db = get_firestore_client()
    student_ref = db.collection('students').document(student_id)
    student_doc = student_ref.get()
    
    # 1. Fetch student record
    if not student_doc.exists:
        record_log("POST", "/api/claim", f"Claim rejected: student ID {student_id} not registered", 404)
        return jsonify({
            "success": False,
            "message": f"Student ID '{student_id}' is not registered in the system."
        }), 404
        
    student = student_doc.to_dict()
    
    # 2. Check general eligibility (suspensions, academic hold, etc.)
    if not student.get('eligible', 1):
        record_log("POST", "/api/claim", f"Claim rejected: {student.get('name')} ({student_id}) is suspended", 403)
        return jsonify({
            "success": False,
            "message": "Access Denied: Student account is currently not eligible for pantry claims."
        }), 403
        
    # 3. Check weekly limit (MCC business logic rule: Max 3 claims per week)
    current_claims = student.get('claims_this_week', 0)
    if current_claims >= 3:
        record_log("POST", "/api/claim", f"Claim rejected: {student.get('name')} has reached weekly quota limit (3/3)", 400)
        return jsonify({
            "success": False,
            "message": f"Claim Rejected: Student '{student.get('name')}' has already reached the weekly limit of 3 claims."
        }), 400
        
    # 4. Success Path: Increment claim count, award 15 impact points for proper checkout
    new_claims_count = current_claims + 1
    new_points = student.get('impact_points', 0) + 15
    
    try:
        # Update student record
        student_ref.update({
            "claims_this_week": new_claims_count,
            "impact_points": new_points
        })
        
        # Log claim transaction
        claim_id = str(uuid.uuid4())
        db.collection('claims').document(claim_id).set({
            "claim_id": claim_id,
            "student_id": student_id,
            "item_name": item_name,
            "location": location,
            "timestamp": datetime.datetime.now().isoformat()
        })
        
        record_log("POST", "/api/claim", f"Claim APPROVED: {student.get('name')} pickup '{item_name}' at {location} (Remaining: {3 - new_claims_count})", 200)
    except Exception as e:
        record_log("POST", "/api/claim", f"Claim failed: Internal Firestore Database error: {str(e)}", 500)
        return jsonify({
            "success": False,
            "message": f"Internal database error occurred: {str(e)}"
        }), 500
        
    return jsonify({
        "success": True,
        "message": f"Claim for '{item_name}' approved successfully!",
        "pointsAwarded": 15,
        "totalImpactPoints": new_points,
        "claimsThisWeek": new_claims_count,
        "remainingClaims": 3 - new_claims_count
    })

@app.route('/api/redeem-coupon', methods=['POST'])
def redeem_coupon():
    """Redeems 200 impact points for a reward coupon."""
    data = request.json or {}
    student_id = data.get('studentId')
    
    if not student_id:
        record_log("POST", "/api/redeem-coupon", "Redemption failed: Missing studentId", 400)
        return jsonify({"success": False, "message": "Missing studentId"}), 400
        
    db = get_firestore_client()
    student_ref = db.collection('students').document(student_id)
    student_doc = student_ref.get()
    
    if not student_doc.exists:
        record_log("POST", "/api/redeem-coupon", f"Redemption failed: student ID {student_id} not found", 404)
        return jsonify({"success": False, "message": "Student not found."}), 404
        
    student = student_doc.to_dict()
    current_points = student.get('impact_points', 0)
    
    if current_points < 200:
        record_log("POST", "/api/redeem-coupon", f"Redemption failed: {student.get('name')} only has {current_points} pts", 400)
        return jsonify({"success": False, "message": "Not enough points. You need at least 200 points."}), 400
        
    new_points = current_points - 200
    student_ref.update({"impact_points": new_points})
    
    import string
    coupon_code = "UITM-CARES-" + ''.join(random.choices(string.ascii_uppercase + string.digits, k=4))
    
    record_log("POST", "/api/redeem-coupon", f"Coupon Redeemed: {student.get('name')} spent 200 pts (Code: {coupon_code})", 200)
    return jsonify({
        "success": True,
        "message": "Coupon redeemed successfully!",
        "couponCode": coupon_code,
        "newPoints": new_points
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
        
    db = get_firestore_client()
    student_ref = db.collection('students').document(student_id)
    doc = student_ref.get()
    
    if not doc.exists:
        record_log("POST", "/api/donate-points", f"Donation failed: Student {student_id} not registered", 404)
        return jsonify({"success": False, "message": "Student not found"}), 404
        
    student = doc.to_dict()
    new_points = student.get('impact_points', 0) + points
    student_ref.update({"impact_points": new_points})
    
    # Record restock history in Firestore
    restock_id = str(uuid.uuid4())
    db.collection('restocks').document(restock_id).set({
        "restock_id": restock_id,
        "restocker_name": student.get('name', 'Unknown'),
        "item_name": item_name,
        "quantity": quantity,
        "image_url": image_url,
        "location": location,
        "timestamp": datetime.datetime.now().isoformat()
    })
    
    record_log("POST", "/api/donate-points", f"Restocked: {student.get('name')} added {quantity}x {item_name} at {location} (+{points} pts)", 200)
    return jsonify({
        "success": True,
        "message": f"Successfully awarded {points} points to {student.get('name')}!",
        "newPoints": new_points
    })

@app.route('/api/claims', methods=['GET'])
def get_all_claims():
    """Retrieves all claims (checkout records) from Firestore database."""
    db = get_firestore_client()
    # In Firestore, we store student name in claims, or we fetch it. 
    # To keep it simple, we'll fetch claims and students and join them in memory, or just return claim data.
    # We will fetch students first to map studentId -> student data
    students_dict = {}
    for doc in db.collection('students').stream():
        students_dict[doc.id] = doc.to_dict()
        
    claims_list = []
    # Fetch claims ordered by timestamp descending
    claims_query = db.collection('claims').order_by('timestamp', direction=firestore.Query.DESCENDING).stream()
    for doc in claims_query:
        c = doc.to_dict()
        student_id = c.get('student_id')
        student = students_dict.get(student_id, {})
        
        claims_list.append({
            "claimId": c.get('claim_id', doc.id),
            "studentId": student_id,
            "name": student.get('name', 'Unknown'),
            "phone": student.get('phone', 'Unknown'),
            "itemName": c.get('item_name'),
            "location": c.get('location'),
            "timestamp": c.get('timestamp')
        })
        
    return jsonify({
        "success": True,
        "claims": claims_list
    })

@app.route('/api/restocks', methods=['GET'])
def get_all_restocks():
    """Retrieves all restocking records from Firestore database."""
    db = get_firestore_client()
    restocks_query = db.collection('restocks').order_by('timestamp', direction=firestore.Query.DESCENDING).stream()
    
    restocks_list = []
    for doc in restocks_query:
        r = doc.to_dict()
        restocks_list.append({
            "restockId": r.get('restock_id', doc.id),
            "restockerName": r.get('restocker_name'),
            "itemName": r.get('item_name'),
            "quantity": r.get('quantity'),
            "imageUrl": r.get('image_url'),
            "location": r.get('location'),
            "timestamp": r.get('timestamp')
        })
        
    return jsonify({
        "success": True,
        "restocks": restocks_list
    })

@app.route('/api/report', methods=['POST'])
def submit_report():
    """Receives an issue report about a pantry item from a student and logs it in Firestore."""
    data = request.json or {}
    student_id = data.get('studentId')
    item_name = data.get('itemName')
    location = data.get('location', 'Kolej Perindu Hub')
    issue = data.get('issue')
    
    if not student_id or not item_name or not issue:
        record_log("POST", "/api/report", "Report failed: Missing studentId, itemName, or issue", 400)
        return jsonify({"success": False, "message": "Missing required fields: studentId, itemName, or issue"}), 400
        
    db = get_firestore_client()
    student_ref = db.collection('students').document(student_id)
    doc = student_ref.get()
    
    student_name = "Unknown Student"
    if doc.exists:
        student_name = doc.to_dict().get('name', 'Unknown Student')
        
    report_id = str(uuid.uuid4())
    db.collection('reports').document(report_id).set({
        "report_id": report_id,
        "student_id": student_id,
        "student_name": student_name,
        "item_name": item_name,
        "location": location,
        "issue_description": issue,
        "timestamp": datetime.datetime.now().isoformat()
    })
    
    record_log("POST", "/api/report", f"Report submitted: {student_name} reported '{item_name}' at {location}: {issue}", 200)
    return jsonify({
        "success": True,
        "message": "Report submitted successfully. Thank you for your feedback!"
    })

@app.route('/api/reports', methods=['GET'])
def get_all_reports():
    """Retrieves all submitted pantry reports from Firestore database."""
    db = get_firestore_client()
    reports_query = db.collection('reports').order_by('timestamp', direction=firestore.Query.DESCENDING).stream()
    
    reports_list = []
    for doc in reports_query:
        r = doc.to_dict()
        reports_list.append({
            "reportId": r.get('report_id', doc.id),
            "studentId": r.get('student_id'),
            "studentName": r.get('student_name'),
            "itemName": r.get('item_name'),
            "location": r.get('location'),
            "issueDescription": r.get('issue_description'),
            "timestamp": r.get('timestamp')
        })
        
    return jsonify({
        "success": True,
        "reports": reports_list
    })

@app.route('/api/student/<student_id>/reset-quota', methods=['PUT'])
def reset_student_quota(student_id):
    """Resets the weekly claim counter for a specific student."""
    db = get_firestore_client()
    db.collection('students').document(student_id).update({"claims_this_week": 0})
    
    record_log("PUT", f"/api/student/{student_id}/reset-quota", f"Admin action: Reset weekly claim quota for {student_id}", 200)
    return jsonify({"success": True, "message": f"Quota for student {student_id} reset successfully."})

@app.route('/api/reset-claims', methods=['POST'])
def reset_claims():
    """Helper to reset weekly claim counters for all students."""
    db = get_firestore_client()
    students = db.collection('students').stream()
    
    # Firestore doesn't have a single "update all" query, we must batch it or loop
    batch = db.batch()
    for doc in students:
        batch.update(doc.reference, {"claims_this_week": 0})
    batch.commit()
    
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
