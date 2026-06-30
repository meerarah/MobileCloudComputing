import sys
import os

# Add the parent directory of this file to sys.path
# This ensures that imports of app.py and config.py resolve correctly inside Vercel's environment
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from app import app
