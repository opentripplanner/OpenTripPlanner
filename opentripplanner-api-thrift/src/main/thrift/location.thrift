/**
 * @author Avi Flamholz (flamholz@gmail.com)
 * This file contains graph-related structures in the OpenTripPlanner Thrift API.
 *
 * @version draft
 */
 
namespace cpp opentripplanner.api.thrift.definition
namespace java org.opentripplanner.api.thrift.definition
namespace py opentripplanner.api.thrift.definition.location
// NOTE(flamholz): Python Thrift compiler doesn't like multiple
// files with the same namespace

struct LatLng {
	1: required double lat;
	2: required double lng;
}

struct Location {
	1: optional LatLng lat_lng;
	
	// TODO(flamholz): add more parameters later.
	// e.g. bearing, azimuth, accuracy.
}