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
 * NOTE(flamholz): as this is a draft API it is likely to change a number of times
 * before it reaches a stable build of OTP. You have been warned.
 *
 * @version draft
 */
 
namespace cpp opentripplanner.api.thrift.definition
namespace java org.opentripplanner.api.thrift.definition
namespace py opentripplanner.api.thrift.definition

include "graph.thrift"
include "location.thrift"
include "trip.thrift"

typedef graph.GraphVertex GraphVertex
typedef graph.GraphEdge GraphEdge
typedef graph.EdgeMatch EdgeMatch
typedef location.Location Location
typedef trip.TravelMode TravelMode
typedef trip.PathOptions PathOptions
typedef trip.TripParameters TripParameters
typedef trip.TripPaths TripPaths


// Request to find paths for a single trip.
struct FindPathsRequest {
	1: required TripParameters trip;
	2: required PathOptions options;
}

// Response containing resulting paths.
struct FindPathsResponse {
	1: required TripPaths paths;

	// The computation time in milliseconds.
	10: optional i64 compute_time_millis;
}

// Request to find paths for a single trip.
struct BulkPathsRequest {
	// Trips to compute paths for.
	1: required list<TripParameters> trips;
	
	// Options for how to compute those paths.
	2: optional PathOptions options;
}

struct BulkPathsResponse {
	// Paths for each trip given.
	1: required list<TripPaths> paths;

	// The computation time in milliseconds.
	10: optional i64 compute_time_millis;
}

// Request to find the nearest vertex.
struct FindNearestVertexRequest {
	// Find vertex near this location.
	1: required Location location;
	
	// Find vertex accessible to one of these modes.
	2: optional set<TravelMode> allowed_modes;	
}

struct FindNearestVertexResponse {
	// If vertex not set, none found.
	1: optional GraphVertex nearest_vertex;
	
	// The computation time in milliseconds.
	10: optional i64 compute_time_millis;
}

// Request to find nearby edges
struct FindNearestEdgesRequest {
	// Find edges near this location.
	// TODO(flamholz): allow input of historical location info.
	1: required Location location;
	
	// Find vertex accessible to one of these modes.
	2: optional set<TravelMode> allowed_modes;
	
	// Maximum number of edges to return.
	10: optional i32 max_edges = 10;
}

struct FindNearestEdgesResponse {
	// The list of nearby edges if any.
	1: optional list<EdgeMatch> nearest_edges;

	// The computation time in milliseconds.
	10: optional i64 compute_time_millis;
}

// Request to get vertices in the graph.
struct GraphVerticesRequest {
	// TODO(flamholz): add parameters about which graph, etc.
}

struct GraphVerticesResponse {
	1: required list<GraphVertex> vertices;

	// The computation time in milliseconds.
	10: optional i64 compute_time_millis;
}

// Request to get vertices in the graph.
struct GraphEdgesRequest {
	1: optional bool street_edges_only = false;
	
	// If street_edges_only, you can specify the modes which
	// must be able to traverse those street edges.
	2: optional set<TravelMode> can_be_traversed_by;

	// TODO(flamholz): add parameters about which graph, etc.
}

struct GraphEdgesResponse {
	1: required list<GraphEdge> edges;

	// The computation time in milliseconds.
	10: optional i64 compute_time_millis;
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
	 * Get the graph edges.
	 */
	GraphEdgesResponse GetEdges(1:GraphEdgesRequest req);

	/**
	 * Find the nearest graph vertex.
	 */
	FindNearestVertexResponse FindNearestVertex(1:FindNearestVertexRequest req);
	
	/**
	 * Find the nearest graph edges.
	 */
	FindNearestEdgesResponse FindNearestEdges(1:FindNearestEdgesRequest req);

	/**
	 * Find paths for a single trip.
	 */
	FindPathsResponse FindPaths(1:FindPathsRequest req);
		
	/**
	 * Find paths for a single trip.
	 */
	BulkPathsResponse BulkFindPaths(1:BulkPathsRequest req);
}


