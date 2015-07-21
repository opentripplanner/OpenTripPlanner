package org.opentripplanner.analyst.broker;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.IamInstanceProfileSpecification;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.ShutdownBehavior;
import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;


/**
 *
 */
public class InstanceRequestTracker {

    private static final Logger LOG = LoggerFactory.getLogger(InstanceRequestTracker.class);

    Properties workerConfig;

    /* All graphs for which we have an outstanding request for new workers. */
    private Map<String, InstanceRequest> requestForGraph = new HashMap<>();

    public InstanceRequestTracker (Properties brokerConfig, String addr, int port) {

        /** Create a configuration for AWS workers launched by this broker. */
        workerConfig = new Properties();
        workerConfig.setProperty("broker-address", addr);
        workerConfig.setProperty("broker-port", "" + port);
        if (brokerConfig.getProperty("statistics-queue") != null) {
            workerConfig.setProperty("statistics-queue", brokerConfig.getProperty("statistics-queue"));
        }
        workerConfig.setProperty("graphs-bucket", brokerConfig.getProperty("graphs-bucket"));
        workerConfig.setProperty("pointsets-bucket", brokerConfig.getProperty("pointsets-bucket"));
        /* Tell the workers to shut themselves down automatically. */
        workerConfig.setProperty("auto-shutdown", "true");

    }

    public boolean noOutstandingRequests(String graphId) {
        // We have either never requested a worker on this graph,
        // or we requested it long enough ago that it's ok to request another.
        // !recentlyRequestedWorkers.containsKey(graphId)
        // || recentlyRequestedWorkers.get(graphId) < System.currentTimeMillis() - WORKER_STARTUP_TIME))
        return false;
    }

    public void requestWorkersForGraph (String graphId) {
        LOG.info("Starting {} workers as there are none on graph {}", nWorkers, graphId);
        LOG.info("Requesting {} workers", nWorkers);
    }

}
