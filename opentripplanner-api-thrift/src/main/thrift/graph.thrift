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
	// Unique label
	1: required string label;

	// Unique identifier
	2: required i32 id;
	
	// Location of the vertex
	3: optional LatLng lat_lng;
	
	// Name of the vertex
	4: optional string name;

	// Directed degree
	5: optional i32 in_degree;
	6: optional i32 out_degree;
}

// An edge in the graph.
struct GraphEdge {
	// Unique id.
	1: required i32 id;
	
	// Head and tail of the directed edge.
	2: required GraphVertex head;
	3: required GraphVertex tail;

	// Name of the dge
	4: optional string name;
}