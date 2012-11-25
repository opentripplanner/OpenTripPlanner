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
	// e.g. bearing, azimuth, accuracy.
}

struct GraphVertex {
	1: required string label;
	2: optional Location location;
	3: optional string name;
	4: optional i32 in_degree;
	5: optional i32 out_degree;
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

struct TripDurationResponse {
	1: required i32 expected_trip_duration;
}

// Request to calculate the time several trips will take.
struct BulkTripDurationRequest {
	1: required list<TripParameters> trips;
}

struct BulkTripDurationResponse {
	1: required list<i32> expected_trip_durations;
}

// Request to get vertices in the graph.
struct GraphVerticesRequest {
	// TODO(flamholz): add parameters about which graph, etc.
}

struct GraphVerticesResponse {
	1: required list<GraphVertex> vertices;
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
	 * Get the graph vertices.
	 */
	GraphVerticesResponse GetVertices(1:GraphVerticesRequest req);

	/**
	 * Calculate the duration of a trip.
	 */
	TripDurationResponse GetTripDuration(1:TripDurationRequest req)
		throws (1: NoPathFoundError path_err);
	
	/**
	 * Calculate the duration of a trip.
	 */
	BulkTripDurationResponse GetManyTripDurations(1:BulkTripDurationRequest req);
}


