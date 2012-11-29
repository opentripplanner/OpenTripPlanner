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
	1: optional LatLng lat_lng;
	
	// TODO(flamholz): add more parameters later.
	// e.g. bearing, azimuth, accuracy.
}

struct GraphVertex {
	1: required string label;
	2: optional LatLng lat_lng;
	3: optional string name;
	4: optional i32 in_degree;
	5: optional i32 out_degree;
}

struct TravelState {
	// Time upon arriving at this state. Seconds since the epoch.
	1: required i64 arrival_time;
	
	// Vertex associated with this state.
	2: required GraphVertex vertex;
	
	// TODO(flamholz): include the mode of travel used to reach this state.
}

struct GraphEdge {
	// Head and tail of the directed edge.
	1: required GraphVertex head;
	2: required GraphVertex tail;

	// TODO(flamholz): add more fields, like the street name etc.
}

struct Path {
	// Expected traversal duration, seconds.
	1: required i32 duration;
	
	// Starting and ending time, seconds since the epoch.
	2: required i64 start_time;
	3: required i64 end_time;
	
	4: optional list<TravelState> states;
	5: optional list<GraphEdge> edges;
	
	// TODO(flamholz): Add more fields like total distance, distance walked.
}

struct TripParameters {
	1: required Location origin;
	2: required Location destination;
	
	// Start time of trip, seconds since the epoch.
	3: optional i64 start_time;
	
	// Requested arrival time.
	// Never set this if start_time is set. 
	4: optional i64 arrive_by;
	
	// Restrict allowed travel modes.
	5: optional set<TravelMode> allowed_modes;	
}

struct TripPaths {
	// Echos the input trip parameters.
	1: required TripParameters trip;
	// If unset, no paths were found.
	2: optional list<Path> paths;
	
	// Set to true in the bulk API when no paths are found.
	// If true, paths list is not set.
	3: optional bool no_paths_found = false;
}

struct PathOptions {
	// The number of paths to return per trip.
	1: optional i32 num_paths = 1;
	
	// Whether to return the full path (true) or just summary info (false).
	2: optional bool return_detailed_path = true;
}

// Request to find paths for a single trip.
struct FindPathsRequest {
	1: required TripParameters trip;
	2: required PathOptions options;
}

// Response containing resulting paths.
struct FindPathsResponse {
	1: required TripPaths paths;
}

// Request to find paths for a single trip.
struct BulkPathsRequest {
	1: required list<TripParameters> trips;
	2: optional PathOptions options;
}

// Response containing paths for each trip.
struct BulkPathsResponse {
	1: required list<TripPaths> paths;
}

// Request to find the nearest vertex.
struct FindNearestVertexRequest {
	// Find vertex near this location.
	1: required Location location;
	
	// Find vertex accessible to one of these modes.
	2: optional set<TravelMode> allowed_modes;	
}

struct FindNearestVertexResponse {
	1: required GraphVertex nearest_vertex;
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
	 * Find the nearest graph vertex.
	 */
	FindNearestVertexResponse FindNearestVertex(1:FindNearestVertexRequest req);

	/**
	 * Find paths for a single trip.
	 */
	FindPathsResponse FindPaths(1:FindPathsRequest req);
		
	/**
	 * Find paths for a single trip.
	 */
	BulkPathsResponse BulkFindPaths(1:BulkPathsRequest req);
}


