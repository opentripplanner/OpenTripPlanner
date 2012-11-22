/**
 * @author Avi Flamholz (flamholz@gmail.com)
 * This file contains the definition of the OpenTripPlanner Thrift API.
 *
 * This API is intended to be lower-level than the webapp-api. It is designed
 * to expose some internals of the graph so that they can be carried over between
 * requests (e.g. to identify locations along an edge). It is also designed for small 
 * payloads so that it can be run with minimal network overhead.
 *
 * This API is NOT designed to be a replica of the REST API in Thrift.
 *
 * @version draft
 */
 
namespace cpp opentripplanner.api.thrift.definition
namespace java org.opentripplanner.api.thrift.definition
namespace py opentripplanner.api.thrift.definition

// Modes of travel. 
// TODO(flamholz): expose them all?
enum TravelMode {
	BICYCLE, WALK, CAR, TRAM, SUBWAY,
	RAIL, ANY_TRAIN, ANY_TRANSIT
}

struct LatLng {
	1: required double lat;
	2: required double lng;
}

struct Location {
	1: required LatLng lat_lng;
	
	// TODO(flamholz): add more parameters later.
	// e.g. bearing, azimuth, accuracy, historical data.
}

struct TripParameters {
	1: required Location origin;
	2: required Location destination;
	
	// Restrict allowed travel modes.
	3: optional set<TravelMode> allowed_modes;	
}

// Request to calculate the time a trip will take.
struct TripDurationRequest {
	1: required TripParameters trip;
}

// Request to calculate the time a trip will take.
struct TripDurationResponse {
	1: required i32 expected_trip_duration;
}

// Raised when there is no route found for the input trip
exception NoPathFoundError {
	1: required string message;
}

/**
 * Thrift service definition exposed to clients.
 */
service OTPService {

	/**
	 * Calculate the duration of a trip.
	 */
	TripDurationResponse GetTripDuration(1:TripDurationRequest req)
		throws (1: NoPathFoundError path_err);
}


