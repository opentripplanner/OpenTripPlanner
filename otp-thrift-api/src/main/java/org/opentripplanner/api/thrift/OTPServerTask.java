/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.api.thrift;

import lombok.Data;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TNonblockingServerTransport;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TTransportException;

import org.opentripplanner.api.thrift.definition.OTPService;
import org.opentripplanner.api.thrift.impl.OTPServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Data
public class OTPServerTask implements Runnable {

    private static Logger LOG = LoggerFactory.getLogger(OTPServerTask.class);

    private OTPServiceImpl otpServiceImpl;

    private int port;

    public void run() {
        LOG.info("Run called, port {}", port);
        if (otpServiceImpl == null) {
            LOG.warn("otpServiceImpl is null, bailing");
            return;
        }

        try {
            OTPService.Processor<OTPServiceImpl> processor = new OTPService.Processor<OTPServiceImpl>(
                    otpServiceImpl);

            // TODO(flamholz): make the transport, protocol and server type be configurable?
            TServerTransport serverTransport = new TServerSocket(port);
            TProtocolFactory protocolFactory = new TBinaryProtocol.Factory(true, true);
            TThreadPoolServer.Args args = new TThreadPoolServer.Args(serverTransport)
                .processor(processor)
                .protocolFactory(protocolFactory);
            TServer server = new TThreadPoolServer(args);

            LOG.info("Starting the OTPService on port {}", port);
            server.serve();
        } catch (TTransportException e) {
            e.printStackTrace();
        }
    }
}
