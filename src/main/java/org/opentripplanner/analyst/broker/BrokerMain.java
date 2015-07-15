package org.opentripplanner.analyst.broker;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.strategies.SameThreadIOStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.BindException;
import java.util.Properties;

// benchmark: $ ab -n 2000 -k -c 100 http://localhost:9001/

public class BrokerMain {

    private static final Logger LOG = LoggerFactory.getLogger(BrokerMain.class);

    private static final int DEFAULT_PORT = 9001;

    private static final String DEFAULT_BIND_ADDRESS = "0.0.0.0";

    public static final Properties config = new Properties();

    public static void main(String[] args) {
        File cfg;
        if (args.length > 0)
            cfg = new File(args[0]);
        else
            cfg = new File("broker.conf");

        if (!cfg.exists()) {
            LOG.error("Broker configuration file {} not found", cfg);
            return;
        }

        try {
            FileInputStream is = new FileInputStream(cfg);
            config.load(is);
            is.close();
        } catch (IOException e) {
            LOG.error("Error reading config file {}", e);
            return;
        }

        int port = config.getProperty("port") != null ? Integer.parseInt(config.getProperty("port")) : DEFAULT_PORT;
        String addr = config.getProperty("bindAddress") != null ? config.getProperty("bindAddress") : DEFAULT_BIND_ADDRESS;

        LOG.info("Starting qbroker on port {} of interface {}", port, addr);

        HttpServer httpServer = new HttpServer();
        NetworkListener networkListener = new NetworkListener("qbroker", addr, port);
        networkListener.getTransport().setIOStrategy(SameThreadIOStrategy.getInstance()); // we avoid blocking IO, and this allows us to see closed connections.
        httpServer.addListener(networkListener);
        // Bypass Jersey etc. and add a low-level Grizzly handler.
        // As in servlets, * is needed in base path to identify the "rest" of the path.
        Broker broker = new Broker(config, addr, port);
        httpServer.getServerConfiguration().addHttpHandler(new BrokerHttpHandler(broker), "/*");
        try {
            httpServer.start();
            LOG.info("Broker running.");
            broker.run(); // run queue broker task pump in this thread
            Thread.currentThread().join();
        } catch (BindException be) {
            LOG.error("Cannot bind to port {}. Is it already in use?", port);
        } catch (IOException ioe) {
            LOG.error("IO exception while starting server.");
        } catch (InterruptedException ie) {
            LOG.info("Interrupted, shutting down.");
        }
        httpServer.shutdown();

    }


}
