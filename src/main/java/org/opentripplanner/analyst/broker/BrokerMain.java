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

// TODO Merge with Broker
public class BrokerMain implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(BrokerMain.class);

    private static final int DEFAULT_PORT = 9001;

    private static final String DEFAULT_BIND_ADDRESS = "0.0.0.0";

    Properties config = new Properties();

    public Broker broker;

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

        Properties brokerConfig = new Properties();
        try {
            FileInputStream is = new FileInputStream(cfg);
            brokerConfig.load(is);
            is.close();
        } catch (IOException e) {
            LOG.error("Error reading config file {}", e);
            return;
        }

        // Create instance and run in the current thread.
        new BrokerMain(brokerConfig).run();

    }

    public BrokerMain(Properties brokerConfig) {
        this.config = brokerConfig;
    }

    public void run() {
        int port = config.getProperty("port") != null ? Integer.parseInt(config.getProperty("port")) : DEFAULT_PORT;
        String addr = config.getProperty("bind-address") != null ? config.getProperty("bind-address") : DEFAULT_BIND_ADDRESS;
        LOG.info("Starting analyst broker on port {} of interface {}", port, addr);
        HttpServer httpServer = new HttpServer();
        NetworkListener networkListener = new NetworkListener("broker", addr, port);
        // We avoid blocking IO, and the following line allows us to see closed connections.
        networkListener.getTransport().setIOStrategy(SameThreadIOStrategy.getInstance());
        httpServer.addListener(networkListener);
        // Bypass Jersey etc. and add a low-level Grizzly handler.
        // As in servlets, * is needed in base path to identify the "rest" of the path.
        broker = new Broker(config, addr, port);
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
