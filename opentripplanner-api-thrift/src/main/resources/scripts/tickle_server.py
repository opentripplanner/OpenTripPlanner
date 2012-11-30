#!.usr.bin.env python
 
from optparse import OptionParser
from thrift import Thrift
from thrift.transport import TSocket
from thrift.transport import TTransport
from thrift.protocol import TBinaryProtocol
from opentripplanner.api.thrift.definition import OTPService
from opentripplanner.api.thrift.definition.location.ttypes import Location
from opentripplanner.api.thrift.definition.trip.ttypes import PathOptions
from opentripplanner.api.thrift.definition.trip.ttypes import TravelMode
from opentripplanner.api.thrift.definition.trip.ttypes import TripParameters

import time
import random


def Connect(host, port):
    try:
        # Make socket
        transport = TSocket.TSocket(host, port=port)
            
        # Buffering is critical. Raw sockets are very slow
        transport = TTransport.TBufferedTransport(transport)
        
        # Wrap in a protocol
        protocol = TBinaryProtocol.TBinaryProtocolAccelerated(transport)
        
        # Create a client to use the protocol encoder
        client = OTPService.Client(protocol)
        
        # Connect!
        transport.open()
        
        return client
    except Thrift.TException, tx:
        print "%s" % (tx.message)


def Main():
    usage = ("usage: python lt_pings.py -p port")
    parser = OptionParser(usage=usage)
    parser.add_option("-H", "--host",
                      dest="host", default="localhost",
                      help="host where server lives")
    parser.add_option("-p", "--port",
                      dest="port", type="int", default=30303,
                      help="port at which server lives")
    options, unused_args = parser.parse_args()
    assert options.port

    client = Connect(options.host, options.port)
    assert client, 'Failed to connect'
    
    req = OTPService.GraphVerticesRequest()
    start_t = time.time()
    res = client.GetVertices(req)
    total_t = time.time() - start_t
    
    vertices = res.vertices
    print 'GraphVerticesRequest took %.6f seconds' % total_t 
    print '\tReturned %d vertices' % len(vertices)
    
    # Sample an origin and a destination (deterministically)
    random.seed(12345)
    origin, dest = random.sample(vertices, 2)
    origin_ll = origin.lat_lng
    dest_ll = dest.lat_lng
    origin_loc = Location(lat_lng=origin_ll)
    dest_loc = Location(lat_lng=dest_ll)
    
    # Run a geocoding request
    req = OTPService.FindNearestVertexRequest(location=origin_loc)
    start_t = time.time()
    res = client.FindNearestVertex(req) 
    total_t = time.time() - start_t
    
    print 'FindNearestVertexRequest took %.6f seconds' % total_t
    print 'Nearest vertex: ', res.nearest_vertex
    
    # Request a walking trip between them.
    trip_params = TripParameters(
        origin=origin_loc, destination=dest_loc,
        allowed_modes=set([TravelMode.WALK]))    
    path_opts = PathOptions(num_paths=1, return_detailed_path=True)
    req = OTPService.FindPathsRequest(trip=trip_params,
                                      options=path_opts)
    start_t = time.time()
    res = client.FindPaths(req)
    total_t = time.time() - start_t
    
    print 'FindPathsRequest took %.6f seconds' % total_t
    paths = res.paths
    if paths.no_paths_found:
        print 'Found no paths'
    else:
        expected_duration = paths.paths[0].duration
        print 'Trip expected to take %d seconds' % expected_duration
    
    # Sample 10 origins and a destinations (deterministically)
    random.seed(12345)
    origins = random.sample(vertices, 100)
    dests = random.sample(vertices, 100)
    
    trip_params = []
    for origin, dest in zip(origins, dests):
        origin_loc = Location(lat_lng=origin.lat_lng)
        dest_loc = Location(lat_lng=dest.lat_lng)
        trip_params.append(TripParameters(
            origin=origin_loc, destination=dest_loc,
            allowed_modes=set([TravelMode.WALK])))

    req = OTPService.BulkPathsRequest(trips=trip_params,
                                      options=path_opts)
    
    try:
        start_t = time.time()
        res = client.BulkFindPaths(req)
        total_t = time.time() - start_t
        
        print ('BulkFindPaths took %.6f seconds '
               'for %d trips ' % (total_t, len(origins))) 
    except OTPService.NoPathFoundError, e:
        print e
        
        
if __name__ == '__main__':
    Main()
