import sqlite3
import os

DB_PATH = os.path.join(os.path.dirname(__file__), 'database.db')

def initialize_database():
    print(f"Initializing database at: {DB_PATH}")
    # Remove old database file if it exists to ensure schema is fresh
    if os.path.exists(DB_PATH):
        try:
            os.remove(DB_PATH)
        except Exception as e:
            print(f"Error removing old db: {e}")
            
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    
    # Create students table with phone column
    cursor.execute('''
    CREATE TABLE IF NOT EXISTS students (
        student_id TEXT PRIMARY KEY,
        name TEXT NOT NULL,
        phone TEXT NOT NULL,
        eligible INTEGER NOT NULL DEFAULT 1,
        impact_points INTEGER NOT NULL DEFAULT 0,
        claims_this_week INTEGER NOT NULL DEFAULT 0
    )
    ''')
    
    # Create claims history table
    cursor.execute('''
    CREATE TABLE IF NOT EXISTS claims (
        claim_id INTEGER PRIMARY KEY AUTOINCREMENT,
        student_id TEXT NOT NULL,
        item_id TEXT NOT NULL,
        item_name TEXT NOT NULL,
        location TEXT NOT NULL DEFAULT 'Kolej Perindu Hub',
        timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (student_id) REFERENCES students(student_id)
    )
    ''')

    # Create restocks history table
    cursor.execute('''
    CREATE TABLE IF NOT EXISTS restocks (
        restock_id INTEGER PRIMARY KEY AUTOINCREMENT,
        restocker_name TEXT NOT NULL,
        item_name TEXT NOT NULL,
        quantity INTEGER NOT NULL,
        image_url TEXT NOT NULL,
        location TEXT NOT NULL,
        timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
    )
    ''')

    # Create reports table
    cursor.execute('''
    CREATE TABLE IF NOT EXISTS reports (
        report_id INTEGER PRIMARY KEY AUTOINCREMENT,
        student_id TEXT NOT NULL,
        student_name TEXT NOT NULL,
        item_name TEXT NOT NULL,
        location TEXT NOT NULL,
        issue_description TEXT NOT NULL,
        timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
    )
    ''')
    
    # Seed mock students data
    students_data = [
        ("std_1001", "Muhammad Ammar", "+6013-4567890", 1, 120, 1),
        ("std_1002", "Nur Fatimah", "+6019-8765432", 1, 350, 3), # Reached limit
        ("std_1003", "Khairul Anuar", "+6012-3456789", 0, 0, 0), # Suspended/not eligible
        ("std_1004", "Siti Aisha", "+6017-1122334", 1, 80, 0)
    ]
    
    cursor.executemany('''
    INSERT OR REPLACE INTO students (student_id, name, phone, eligible, impact_points, claims_this_week)
    VALUES (?, ?, ?, ?, ?, ?)
    ''', students_data)

    # Seed mock claims data
    claims_data = [
        ("std_1001", "item_001", "Canned Tuna", "Kolej Perindu Hub"),
        ("std_1002", "item_002", "Whole Grain Cereal", "Kolej Mawar Hub"),
        ("std_1002", "item_003", "Organic Whole Milk", "Kolej Mawar Hub"),
        ("std_1002", "item_005", "Canned Vegetable Soup", "Kolej Mawar Hub")
    ]
    cursor.executemany('''
    INSERT INTO claims (student_id, item_id, item_name, location)
    VALUES (?, ?, ?, ?)
    ''', claims_data)

    # Seed mock restocks data
    restocks_data = [
        ("Muhammad Ammar", "Canned Tuna", 5, "tuna", "Kolej Perindu Hub"),
        ("Siti Aisha", "Whole Grain Cereal", 10, "cereal", "Kolej Mawar Hub"),
        ("Nur Fatimah", "Fresh Gala Apples", 15, "apple", "Kolej Mawar Hub 2")
    ]
    cursor.executemany('''
    INSERT INTO restocks (restocker_name, item_name, quantity, image_url, location)
    VALUES (?, ?, ?, ?, ?)
    ''', restocks_data)

    # Seed mock reports data
    reports_data = [
        ("std_1001", "Muhammad Ammar", "Organic Whole Milk", "Kolej Perindu Hub", "The item is past its expiry date"),
        ("std_1004", "Siti Aisha", "Fresh Gala Apples", "Kolej Mawar Hub 2", "Apples have soft spots and bruising")
    ]
    cursor.executemany('''
    INSERT INTO reports (student_id, student_name, item_name, location, issue_description)
    VALUES (?, ?, ?, ?, ?)
    ''', reports_data)
    
    conn.commit()
    conn.close()
    print("Database initialized successfully with restocks, reports and phone numbers!")

if __name__ == '__main__':
    initialize_database()
