/**
 * @author Avi Flamholz (flamholz@gmail.com)
 * This file contains graph-related structures in the OpenTripPlanner Thrift API.
 *
 * @version draft
 */
 
namespace cpp opentripplanner.api.thrift.definition
namespace java org.opentripplanner.api.thrift.definition
namespace py opentripplanner.api.thrift.definition.trip
// NOTE(flamholz): Python Thrift compiler doesn't like multiple
// files with the same namespace

include "graph.thrift"
include "location.thrift"

typedef graph.GraphEdge GraphEdge
typedef graph.GraphVertex GraphVertex
typedef location.Location Location


// Modes of travel. 
// TODO(flamholz): expose them all?
enum TravelMode {
	BICYCLE, WALK, CAR, TRAM, SUBWAY,
	RAIL, ANY_TRAIN, ANY_TRANSIT
}

struct TravelState {
	// Time upon arriving at this state. Seconds since the epoch.
	1: required i64 arrival_time;
	
	// Vertex associated with this state.
	2: required GraphVertex vertex;
	
	// TODO(flamholz): include the mode of travel used to reach this state.
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
