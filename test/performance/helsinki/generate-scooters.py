import json
import random
import uuid

def generate_scooter_locations(num_scooters=50):
    # Define the bounding box for the Helsinki region
    min_lat, max_lat = 59.0, 61.0  # Approximate latitude range for Helsinki
    min_lon, max_lon = 24.0, 26.0  # Approximate longitude range for Helsinki
    
    scooters = []
    for _ in range(num_scooters):
        lat = random.uniform(min_lat, max_lat)
        lon = random.uniform(min_lon, max_lon)
        scooter = {
            "lat": round(lat, 6),
            "lon": round(lon, 6),
            "current_range_meters": 28400,
            "current_fuel_percent": 100,
            "pricing_plan_id": "c63c9139-3d90-52d9-a725-6e19b05491e8",
            "vehicle_type_id": "e92dc79f-736d-5eca-9ff4-1806653b6672",
            "is_reserved": False,
            "is_disabled": False,
            "rental_uris": {
            "android": "bolt://action/rentalsSelectVehicleByRotatedUuid?rotated_uuid=13a83fce-dced-4358-b2e2-09bb3bcc034d",
            "ios": "bolt://action/rentalsSelectVehicleByRotatedUuid?rotated_uuid=13a83fce-dced-4358-b2e2-09bb3bcc034d"
            },
            "bike_id": str(uuid.uuid4())
            }
            
        
        scooters.append(scooter)
    
    return scooters


    

if __name__ == "__main__":
    num_scooters = 100000  # Change this value to generate more or fewer scooters
    scooter_data = generate_scooter_locations(num_scooters)
    file_content = {
        "last_updated": 1740491476,
        "ttl": 300,
        "version": "2.3",
        "data": {
            "bikes": scooter_data
        }
    }
    
    with open("data/free_bike_status.json", "w") as f:
        json.dump(file_content, f, indent=4)
    
    print("scooters.json file has been created.")
