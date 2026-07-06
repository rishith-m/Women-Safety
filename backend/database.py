import os
from pymongo import MongoClient

_client = None
_db = None

def get_db():
    global _client, _db
    if _db is None:
        mongo_uri = os.environ.get("MONGO_URL", os.environ.get("MONGO_URI", "mongodb://localhost:27017/womensafety"))
        _client = MongoClient(mongo_uri, serverSelectionTimeoutMS=5000)
        _db = _client.get_database("womensafety")
    return _db
