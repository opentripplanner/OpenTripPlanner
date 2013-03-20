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
	
	// Direction of travel in decimal degrees from -180° to +180° relative to
    // true north.
    // 0      = heading true north.
    // +/-180 = heading south.
	3: optional double heading;
	
	// TODO(flamholz): add more parameters later.
	// e.g. azimuth, elevation, accuracy.
}