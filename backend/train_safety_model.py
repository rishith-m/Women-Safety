import pandas as pd
import numpy as np
from sklearn.ensemble import RandomForestClassifier
import pickle

# Since we don't have direct Kaggle API credentials, we will create a
# high-quality synthetic dataset based on common Kaggle safety patterns
# (Time, Lighting, Crowd, Police, Incidents).

def generate_kaggle_like_data(n_samples=2000):
    np.random.seed(42)

    # hour: 0-23
    hours = np.random.randint(0, 24, n_samples)

    # lighting: 0 (dark) to 2 (bright)
    # Higher chance of being dark at night
    lighting = []
    for h in hours:
        if 6 <= h <= 18:
            lighting.append(np.random.choice([1, 2], p=[0.3, 0.7]))
        else:
            lighting.append(np.random.choice([0, 1], p=[0.8, 0.2]))
    lighting = np.array(lighting)

    # crowd: 0 (empty) to 2 (busy)
    crowd = []
    for h in hours:
        if 8 <= h <= 20:
            crowd.append(np.random.choice([1, 2], p=[0.4, 0.6]))
        else:
            crowd.append(np.random.choice([0, 1], p=[0.9, 0.1]))
    crowd = np.array(crowd)

    # police_proximity: 0 (far) to 1 (near)
    police = np.random.choice([0, 1], n_samples, p=[0.7, 0.3])

    # past_incidents: 0 (none) to 1 (high)
    incidents = np.random.choice([0, 1], n_samples, p=[0.8, 0.2])

    # Target: Safety (1 = Safe, 0 = Unsafe)
    # Logic: Safe if it's day, well-lit, crowded, or near police, AND no incidents.
    safety = []
    for i in range(n_samples):
        score = 0
        if 7 <= hours[i] <= 19: score += 2  # Daylight
        if lighting[i] == 2: score += 2     # Bright
        if crowd[i] >= 1: score += 1        # Some people
        if police[i] == 1: score += 2       # Police near
        if incidents[i] == 1: score -= 3    # Incident history (Big penalty)

        # Threshold for safety
        is_safe = 1 if score >= 2 else 0
        safety.append(is_safe)

    df = pd.DataFrame({
        'hour': hours,
        'lighting': lighting,
        'crowd': crowd,
        'police': police,
        'incidents': incidents,
        'safety': safety
    })
    return df

# Create and train
print("Generating dataset inspired by Kaggle safety patterns...")
data = generate_kaggle_like_data()

X = data[['hour', 'lighting', 'crowd', 'police', 'incidents']]
y = data['safety']

model = RandomForestClassifier(n_estimators=100, random_state=42)
model.fit(X, y)

# Save the model
with open('safety_model.pkl', 'wb') as f:
    pickle.dump(model, f)

print("Model trained and saved as safety_model.pkl")
print("Accuracy on training data:", model.score(X, y))
