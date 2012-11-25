#!.usr.bin.env python
 
from optparse import OptionParser
from thrift import Thrift
from thrift.transport import TSocket
from thrift.transport import TTransport
from thrift.protocol import TBinaryProtocol
from opentripplanner.api.thrift.definition import OTPService

import time


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
    print 'GraphVertices.Request took %.6f seconds' % total_t 
    print '\tReturned %d vertices' % len(res.vertices)
    
    #for v in res.vertices:
    #    print v.label
    
    return
        
        
if __name__ == '__main__':
    Main()
