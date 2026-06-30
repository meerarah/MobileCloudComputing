import os
import shutil
import sqlite3

BASE_DIR = os.path.dirname(os.path.abspath(__file__))

# Check if running in a serverless environment (like Vercel)
if os.environ.get('VERCEL') or os.environ.get('NOW_REGION'):
    DB_PATH = '/tmp/database.db'
    source_db = os.path.join(BASE_DIR, 'database.db')
    # Copy pre-seeded SQLite database to writable /tmp directory if it doesn't exist yet
    if not os.path.exists(DB_PATH) and os.path.exists(source_db):
        shutil.copy2(source_db, DB_PATH)
else:
    DB_PATH = os.path.join(BASE_DIR, 'database.db')

def get_db_connection():
    """Establishes a connection to the SQLite database with row factory enabled."""
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn
