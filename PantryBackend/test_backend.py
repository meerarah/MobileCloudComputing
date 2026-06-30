import unittest
import json
import os
import sqlite3
from app import app
from init_db import initialize_database

class SmartCampusPantryTestCase(unittest.TestCase):
    
    @classmethod
    def setUpClass(cls):
        # Force fresh database initialization
        initialize_database()
        cls.client = app.test_client()
        cls.client.testing = True

    def test_01_home_endpoint(self):
        """Test the home page routing and welcome message."""
        response = self.client.get('/api/info')
        self.assertEqual(response.status_code, 200)
        data = json.loads(response.data)
        self.assertEqual(data["status"], "online")
        self.assertIn("mcc_features", data)

    def test_02_get_student_profile(self):
        """Test fetching a registered student profile."""
        response = self.client.get('/api/student/std_1001')
        self.assertEqual(response.status_code, 200)
        data = json.loads(response.data)
        self.assertTrue(data["success"])
        self.assertEqual(data["name"], "Muhammad Ammar")
        self.assertTrue(data["eligible"])
        self.assertEqual(data["maxWeeklyClaims"], 3)

    def test_03_get_student_not_found(self):
        """Test fetching a non-existent student profile yields 404."""
        response = self.client.get('/api/student/std_9999')
        self.assertEqual(response.status_code, 404)
        data = json.loads(response.data)
        self.assertFalse(data["success"])

    def test_04_successful_claim(self):
        """Test that an eligible student with remaining quota can successfully make a claim."""
        payload = {
            "studentId": "std_1001",
            "itemId": "item_002",
            "itemName": "Whole Grain Cereal"
        }
        response = self.client.post('/api/claim', 
                                    data=json.dumps(payload),
                                    content_type='application/json')
        self.assertEqual(response.status_code, 200)
        data = json.loads(response.data)
        self.assertTrue(data["success"])
        self.assertIn("approved successfully", data["message"])
        self.assertEqual(data["claimsThisWeek"], 2) # Alex started with 1 claim, now has 2
        self.assertEqual(data["remainingClaims"], 1)

    def test_05_quota_limit_exceeded(self):
        """Test that student who has reached 3 weekly claims gets rejected by Flask API business logic."""
        payload = {
            "studentId": "std_1002", # Jane Doe is seeded with 3 claims
            "itemId": "item_001",
            "itemName": "Canned Tuna"
        }
        response = self.client.post('/api/claim', 
                                    data=json.dumps(payload),
                                    content_type='application/json')
        self.assertEqual(response.status_code, 400)
        data = json.loads(response.data)
        self.assertFalse(data["success"])
        self.assertIn("reached the weekly limit", data["message"])

    def test_06_suspended_student_claim_denied(self):
        """Test that suspended or non-eligible student gets rejected immediately."""
        payload = {
            "studentId": "std_1003", # John Peterson is seeded as not eligible (eligible=0)
            "itemId": "item_001",
            "itemName": "Canned Tuna"
        }
        response = self.client.post('/api/claim', 
                                    data=json.dumps(payload),
                                    content_type='application/json')
        self.assertEqual(response.status_code, 403)
        data = json.loads(response.data)
        self.assertFalse(data["success"])
        self.assertIn("account is currently not eligible", data["message"])

    def test_07_mcc_food_analysis(self):
        """Test the cloud-offloaded image analysis endpoint."""
        payload = {
            "imageUrl": "https://storage.google.com/pantry/donations/tuna_can_05.jpg"
        }
        response = self.client.post('/api/analyze-food', 
                                    data=json.dumps(payload),
                                    content_type='application/json')
        self.assertEqual(response.status_code, 200)
        data = json.loads(response.data)
        self.assertTrue(data["success"])
        # Check that classification accurately identified canned tuna from URL keyword
        self.assertEqual(data["analyzedItem"]["itemName"], "Canned Tuna")
        self.assertEqual(data["analyzedItem"]["category"], "Proteins")
        self.assertGreater(data["analyzedItem"]["confidenceScore"], 0.8)

    def test_08_reset_claims_utility(self):
        """Test resetting claims quota back to 0."""
        # Confirm std_1002 has 3 claims
        conn = sqlite3.connect(os.path.join(os.path.dirname(__file__), 'database.db'))
        claims_before = conn.execute('SELECT claims_this_week FROM students WHERE student_id = ?', ("std_1002",)).fetchone()[0]
        self.assertEqual(claims_before, 3)
        conn.close()

        # Call reset
        response = self.client.post('/api/reset-claims')
        self.assertEqual(response.status_code, 200)

        # Confirm std_1002 has 0 claims now
        conn = sqlite3.connect(os.path.join(os.path.dirname(__file__), 'database.db'))
        claims_after = conn.execute('SELECT claims_this_week FROM students WHERE student_id = ?', ("std_1002",)).fetchone()[0]
        self.assertEqual(claims_after, 0)
        conn.close()

if __name__ == '__main__':
    unittest.main()
