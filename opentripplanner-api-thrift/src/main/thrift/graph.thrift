/**
 * @author Avi Flamholz (flamholz@gmail.com)
 * This file contains graph-related structures in the OpenTripPlanner Thrift API.
 *
 * @version draft
 */
 
namespace cpp opentripplanner.api.thrift.definition
namespace java org.opentripplanner.api.thrift.definition
namespace py opentripplanner.api.thrift.definition.graph
// NOTE(flamholz): Python Thrift compiler doesn't like multiple
// files with the same namespace

include "location.thrift"

typedef location.LatLng LatLng


// A vertex in the graph.
struct GraphVertex {
	1: required string label;
	2: optional LatLng lat_lng;
	3: optional string name;
	4: optional i32 in_degree;
	5: optional i32 out_degree;
}

// An edge in the graph.
struct GraphEdge {
	// Head and tail of the directed edge.
	1: required GraphVertex head;
	2: required GraphVertex tail;

	// TODO(flamholz): add more fields, like the street name etc.
}