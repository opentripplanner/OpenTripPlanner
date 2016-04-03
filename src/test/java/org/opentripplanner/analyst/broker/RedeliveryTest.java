package org.opentripplanner.analyst.broker;

import org.opentripplanner.analyst.cluster.AnalystWorker;
import org.opentripplanner.analyst.cluster.JobSimulator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * This test is not an automatic unit test. It is an integration test that must be started manually, because it takes
 * a long time to run. It will start up a broker and some local workers, then submit a large job to the broker. The
 * workers will fail to complete tasks some percentage of the time, but eventually the whole job should be finished
 * because the broker will re-send tasks.
 */
public class RedeliveryTest {

    private static final Logger LOG = LoggerFactory.getLogger(RedeliveryTest.class);
    static final int N_TASKS = 100;
    static final int N_WORKERS = 4;
    static final int FAILURE_RATE = 20; // percent

    public static void main(String[] params) {

        // Start a broker in a new thread.
        Properties brokerConfig = new Properties();
        brokerConfig.setProperty("graphs-bucket", "FAKE");
        brokerConfig.setProperty("pointsets-bucket", "FAKE");
        brokerConfig.setProperty("work-offline", "true");
        BrokerMain brokerMain = new BrokerMain(brokerConfig);
        Thread brokerThread = new Thread(brokerMain); // TODO combine broker and brokermain, set offline mode.
        brokerThread.start();

        // Start some workers.
        Properties workerConfig = new Properties();
        workerConfig.setProperty("initial-graph-id", "GRAPH");
        List<Thread> workerThreads = new ArrayList<>();
        for (int i = 0; i < N_WORKERS; i++) {
            AnalystWorker worker = new AnalystWorker(workerConfig);
            worker.dryRunFailureRate = FAILURE_RATE;
            Thread workerThread = new Thread(worker);
            workerThreads.add(workerThread);
            workerThread.start();
        }

        // Feed some work to the broker.
        JobSimulator jobSimulator = new JobSimulator();
        jobSimulator.nOrigins = N_TASKS;
        jobSimulator.graphId = "GRAPH";
        jobSimulator.sendFakeJob();

        // Wait for all tasks to be marked finished
        while (brokerMain.broker.anyJobsActive()) {
            try {
                LOG.info("Some jobs are still not complete.");
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        LOG.info("All jobs finished.");
        System.exit(0);
    }

}
