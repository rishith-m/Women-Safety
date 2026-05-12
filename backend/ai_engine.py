class EmergencyClassifier:
    def __init__(self):
        # Using a pure-Python keyword-based fallback to avoid C++ Build errors on Windows 
        self.critical_keywords = [
            "help", "save me", "police", "danger", "sos", 
            "attack", "following me", "knife", "emergency"
        ]

    def is_emergency(self, text):
        """
        Classify text and return True if it indicates an emergency, False otherwise.
        """
        if not text:
            return False
            
        text_lower = text.lower()
        for kw in self.critical_keywords:
            if kw in text_lower:
                return True
                
        return False

# Instantiate a global classifier when module is loaded
classifier = EmergencyClassifier()
