import os
from pymongo import MongoClient

def get_db():
    # Use mongodb://localhost:27017/womensafety by default if not specified
    mongo_uri = os.environ.get("MONGO_URL", os.environ.get("MONGO_URI", "mongodb://localhost:27017/womensafety"))
    client = MongoClient(mongo_uri)
    return client.get_database("womensafety")
