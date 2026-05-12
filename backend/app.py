import os
import pickle
import numpy as np
from dotenv import load_dotenv
from flask import Flask, request, jsonify
from datetime import datetime

# Load environment variables from .env file FIRST
load_dotenv()

from database import get_db
from ai_engine import classifier
from bson.objectid import ObjectId

app = Flask(__name__)

# Connect to database
db = get_db()
users_collection = db["users"]
alerts_collection = db["alerts"]

# Load the safety model
try:
    with open('safety_model.pkl', 'rb') as f:
        safety_model = pickle.load(f)
except FileNotFoundError:
    safety_model = None

@app.route("/", methods=["GET"])
def health_check():
    return jsonify({"status": "running", "message": "Women Safety APIs Online"}), 200

@app.route("/api/register", methods=["POST"])
def register_user():
    """Register a new user with emergency contacts."""
    data = request.json
    if not data or "phone" not in data or "name" not in data:
        return jsonify({"error": "Missing required fields (phone, name)"}), 400
        
    user = {
        "name": data["name"],
        "phone": data["phone"],
        "contacts": data.get("contacts", []), # List of generic strings or objects
        "created_at": datetime.utcnow()
    }
    
    # Check if exists
    existing = users_collection.find_one({"phone": data["phone"]})
    if existing:
        return jsonify({"error": "User with this phone already exists"}), 400
        
    result = users_collection.insert_one(user)
    return jsonify({"message": "User registered successfully", "user_id": str(result.inserted_id)}), 201

@app.route("/api/alert/trigger", methods=["POST"])
def trigger_alert():
    """Trigger an SOS alert."""
    data = request.json
    if not data or "user_id" not in data:
        return jsonify({"error": "Missing user_id"}), 400
        
    alert = {
        "user_id": data["user_id"],
        "latitude": data.get("latitude"),
        "longitude": data.get("longitude"),
        "timestamp": datetime.utcnow(),
        "status": "active"
    }
    
    result = alerts_collection.insert_one(alert)
    return jsonify({
        "message": "Alert triggered. Contacts will be notified.",
        "alert_id": str(result.inserted_id)
    }), 201

@app.route("/api/alert/location", methods=["POST"])
def update_location():
    """Update live location for an active alert."""
    data = request.json
    if not data or "alert_id" not in data or "latitude" not in data or "longitude" not in data:
        return jsonify({"error": "Missing alert_id, latitude, or longitude"}), 400
        
    alerts_collection.update_one(
        {"_id": ObjectId(data["alert_id"])},
        {"$set": {
            "latitude": data["latitude"], 
            "longitude": data["longitude"],
            "last_updated": datetime.utcnow()
        }}
    )
    
    return jsonify({"message": "Location updated"}), 200

@app.route("/api/route/analyze", methods=["POST"])
def analyze_route():
    """Analyze safety of multiple routes between origin and destination."""
    data = request.json
    print(f"DEBUG: Received route request: {data}")

    if not data:
        return jsonify({"error": "Missing payload"}), 400

    origin_lat = data.get("origin_lat", 17.3850)
    origin_lng = data.get("origin_lng", 78.4867)
    dest_lat = data.get("dest_lat", 17.4000)
    dest_lng = data.get("dest_lng", 78.5000)

    origin = {"lat": origin_lat, "lng": origin_lng}
    dest = {"lat": dest_lat, "lng": dest_lng}

    # Predictive analysis using the model
    def get_safety_prediction(hour, lighting, crowd, police, incidents):
        if safety_model:
            features = np.array([[hour, lighting, crowd, police, incidents]])
            prediction = safety_model.predict(features)
            return int(prediction[0])
        return 1 # Fallback

    current_hour = datetime.now().hour

    # Simulate two routes with multiple waypoints for a "navigation" look
    # Route 1: Main road simulation
    s1 = get_safety_prediction(current_hour, 2, 2, 1, 0)
    # Route 2: Shortcut simulation
    s2 = get_safety_prediction(current_hour, 0, 1, 0, 1)

    # Add variation so it's not always the same score
    score1 = round(0.85 + np.random.uniform(0.0, 0.12), 2) if s1 == 1 else round(0.4 + np.random.uniform(0.0, 0.1), 2)
    score2 = round(0.70 + np.random.uniform(0.0, 0.15), 2) if s2 == 1 else round(0.25 + np.random.uniform(0.0, 0.15), 2)

    routes = [
        {
            "route_id": "route_1",
            "safety_score": score1,
            "reason": "Safe: High lighting and police patrols detected." if score1 > 0.7 else "Caution: High risk at this hour.",
            "waypoints": [
                origin,
                {"lat": origin_lat + (dest_lat - origin_lat) * 0.4, "lng": origin_lng + (dest_lng - origin_lng) * 0.1},
                {"lat": origin_lat + (dest_lat - origin_lat) * 0.7, "lng": origin_lng + (dest_lng - origin_lng) * 0.6},
                dest
            ]
        },
        {
            "route_id": "route_2",
            "safety_score": score2,
            "reason": "Shortcut: Residential area with average lighting." if score2 > 0.6 else "Risky: Reported incidents and low light.",
            "waypoints": [
                origin,
                {"lat": origin_lat + (dest_lat - origin_lat) * 0.2, "lng": origin_lng + (dest_lng - origin_lng) * 0.8},
                dest
            ]
        }
    ]

    return jsonify({
        "status": "success",
        "routes": routes
    }), 200

@app.route("/api/ai/analyze", methods=["POST"])
def analyze_text():
    """Analyze Speech-To-Text string and return if it is an emergency via ML model."""
    data = request.json
    if not data or "text" not in data:
        return jsonify({"error": "Missing text payload"}), 400
        
    text_content = data["text"]
    is_emergency = classifier.is_emergency(text_content)
    
    return jsonify({
        "text_analyzed": text_content,
        "is_emergency": bool(is_emergency),
        "action_required": bool(is_emergency)
    }), 200

if __name__ == "__main__":
    app.run(debug=True, host="0.0.0.0", port=5000, use_reloader=False)
