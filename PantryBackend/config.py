import os
import json
import firebase_admin
from firebase_admin import credentials, firestore

def get_firestore_client():
    """Initializes Firebase Admin SDK and returns a Firestore client."""
    if not firebase_admin._apps:
        service_account_str = os.environ.get('FIREBASE_SERVICE_ACCOUNT')
        if service_account_str:
            cred_dict = json.loads(service_account_str)
            cred = credentials.Certificate(cred_dict)
            firebase_admin.initialize_app(cred)
        else:
            # Fallback to local json for development if it exists
            local_key_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'serviceAccountKey.json')
            if os.path.exists(local_key_path):
                cred = credentials.Certificate(local_key_path)
                firebase_admin.initialize_app(cred)
            else:
                raise Exception("Missing FIREBASE_SERVICE_ACCOUNT environment variable and no local serviceAccountKey.json found.")
    
    return firestore.client()
