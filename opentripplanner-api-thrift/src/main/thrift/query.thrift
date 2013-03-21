/**
 * @author Avi Flamholz (flamholz@gmail.com)
 * This file contains structures used for querying the graph.
 *
 * @version draft
 */
 
namespace cpp opentripplanner.api.thrift.definition
namespace java org.opentripplanner.api.thrift.definition
namespace py opentripplanner.api.thrift.definition.query
// NOTE(flamholz): Python Thrift compiler doesn't like multiple
// files with the same namespace

include "graph.thrift"
include "location.thrift"
include "trip.thrift"

typedef graph.GraphEdge GraphEdge
typedef graph.GraphVertex GraphVertex 
typedef location.LatLng LatLng
typedef location.Location Location
typedef trip.TravelMode TravelMode

// Query for a nearby vertex.
struct VertexQuery {
	// Find vertex near this location.
	1: required Location location;
	
	// Find vertex accessible by one of these modes.
	2: optional set<TravelMode> allowed_modes;	
}

// Result for a vertex query.
struct VertexResult {
	1: optional GraphVertex nearest_vertex;
}

// Query for nearby edges.
struct NearestEdgesQuery {
	// Find edges near this location.
	1: required Location location;
	
	// Find edges accessible by one of these modes.
	2: optional set<TravelMode> allowed_modes;
	
	// Maximum number of edges to return.
	10: optional i32 max_edges = 10;
}

// An edge that matches a geographic search.
struct EdgeMatch {
	// The edge itself.
	1: required GraphEdge edge;
	
	// The closest point along the edge.
	// TODO(flamholz): this could be a Location.
	2: required LatLng closest_point;
	
	// Score of the match. Lower is better.
	3: required double score;
	
	// The distance in meters from the query point.
	4: optional double distance_from_query;
	
	// The heading of the edge geometry at the match point.
	5: optional double heading_at_closest_point;
}

struct NearestEdgesResult {
	// The list of nearby edges if any.
	1: optional list<EdgeMatch> nearest_edges;
}