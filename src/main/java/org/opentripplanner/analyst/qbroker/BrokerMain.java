package org.opentripplanner.analyst.qbroker;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.strategies.SameThreadIOStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.BindException;

// benchmark: $ ab -n 2000 -k -c 100 http://localhost:9001/

public class BrokerMain {

    private static final Logger LOG = LoggerFactory.getLogger(BrokerMain.class);

    private static final int PORT = 9001;

    private static final String BIND_ADDRESS = "0.0.0.0";

    public static void main(String[] args) {

        LOG.info("Starting qbroker on port {} of interface {}", PORT, BIND_ADDRESS);
        HttpServer httpServer = new HttpServer();
        NetworkListener networkListener = new NetworkListener("qbroker", BIND_ADDRESS, PORT);
        networkListener.getTransport().setIOStrategy(SameThreadIOStrategy.getInstance()); // we avoid blocking IO, and this allows us to see closed connections.
        httpServer.addListener(networkListener);
        // Bypass Jersey etc. and add a low-level Grizzly handler.
        // As in servlets, * is needed in base path to identify the "rest" of the path.
        Broker broker = new Broker();
        httpServer.getServerConfiguration().addHttpHandler(new BrokerHttpHandler(broker), "/*");
        try {
            httpServer.start();
            LOG.info("Broker running.");
            broker.run(); // run queue broker task pump in this thread
            Thread.currentThread().join();
        } catch (BindException be) {
            LOG.error("Cannot bind to port {}. Is it already in use?", PORT);
        } catch (IOException ioe) {
            LOG.error("IO exception while starting server.");
        } catch (InterruptedException ie) {
            LOG.info("Interrupted, shutting down.");
        }
        httpServer.shutdown();

    }


}
