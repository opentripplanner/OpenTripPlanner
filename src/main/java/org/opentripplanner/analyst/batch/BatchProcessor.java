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

package org.opentripplanner.analyst.batch;

import java.util.TimeZone;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.opentripplanner.analyst.batch.aggregator.Aggregator;
import org.opentripplanner.analyst.core.Sample;
import org.opentripplanner.analyst.request.SampleFactory;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.error.VertexNotFoundException;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BatchProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(BatchProcessor.class);
    private static final String EXAMPLE_CONTEXT = "batch-context.xml";
    
    private GraphService graphService;
    private SampleFactory sampleFactory;

    private Population origins;
    private Population destinations;
    private RoutingRequest prototypeRoutingRequest;

    private Aggregator aggregator;
    private Accumulator accumulator;
    private int logThrottleSeconds = 4;    
    private int searchCutoffSeconds = -1;
    
    /**
     * Empirical results for a 4-core processor (with 8 fake hyperthreading cores):
     * Throughput increases linearly with nThreads, up to the number of physical cores. 
     * Diminishing returns beyond 4 threads, but some improvement is seen up to 8 threads.
     * The default value includes the hyperthreading cores, so you may want to set nThreads 
     * manually in your IoC XML. 
     */
    private int nThreads = Runtime.getRuntime().availableProcessors(); 

    private String date = "2011-02-04";
    private String time = "08:00 AM";
    private TimeZone timeZone = TimeZone.getDefault();
    private String outputPath = "/tmp/analystOutput";
    private float checkpointIntervalMinutes = -1;
    
    enum Mode { BASIC, AGGREGATE, ACCUMULATE };
    private Mode mode;
    private long startTime = -1;
    private long lastLogTime = 0;
    private long lastCheckpointTime = 0;
    private ResultSet aggregateResultSet = null;
    
    /** Cut off the search instead of building a full path tree. Can greatly improve run times. */
    public void setSearchCutoffMinutes(int minutes) {
        this.searchCutoffSeconds = minutes * 60;
    }

    /*
    public static void main(String[] args) throws IOException {
        org.springframework.core.io.Resource appContextResource;
        if( args.length == 0) {
            LOG.warn("no configuration XML file specified; using example on classpath");
            appContextResource = new ClassPathResource(EXAMPLE_CONTEXT);
        } else {
            String configFile = args[0];
            appContextResource = new FileSystemResource(configFile);
        }
        GenericApplicationContext ctx = new GenericApplicationContext();
        XmlBeanDefinitionReader xmlReader = new XmlBeanDefinitionReader(ctx);
        xmlReader.loadBeanDefinitions(appContextResource);
        ctx.refresh();
        ctx.registerShutdownHook();
        BatchProcessor processor = ctx.getBean(BatchProcessor.class);
        if (processor == null)
            LOG.error("No BatchProcessor bean was defined.");
        else
            processor.run();
    }
    */

    private void run() {
        origins.setup();
        destinations.setup();
        linkIntoGraph(destinations);
        // Set up a thread pool to execute searches in parallel
        LOG.info("Number of threads: {}", nThreads);
        ExecutorService threadPool = Executors.newFixedThreadPool(nThreads);
        // ECS enqueues results in the order they complete (unlike invokeAll, which blocks)
        CompletionService<Void> ecs = new ExecutorCompletionService<Void>(threadPool);
        if (aggregator != null) {
            /* aggregate over destinations and save one value per origin */
            mode = Mode.AGGREGATE;
            aggregateResultSet = new ResultSet(origins); // results shaped like origins
        } else if (accumulator != null) { 
            /* accumulate data for each origin into all destinations */
            mode = Mode.ACCUMULATE;
            aggregateResultSet = new ResultSet(destinations); // results shaped like destinations
        } else { 
            /* neither aggregator nor accumulator, save a bunch of results */
            mode = Mode.BASIC;
            aggregateResultSet = null;
            if (!outputPath.contains("{}")) {
                LOG.error("output filename must contain origin placeholder.");
                System.exit(-1);
            }
        }
        startTime = System.currentTimeMillis();
        int nTasks = 0;
        for (Individual oi : origins) { // using filtered iterator
            ecs.submit(new BatchAnalystTask(nTasks, oi), null);
            ++nTasks;
        }
        LOG.info("created {} tasks.", nTasks);
        int nCompleted = 0;
        try { // pull Futures off the queue as tasks are finished
            while (nCompleted < nTasks) {
                try {
                    ecs.take().get(); // call get to check for exceptions in the completed task
                    LOG.debug("got result {}/{}", nCompleted, nTasks);
                    if (checkpoint()) {
                        LOG.info("checkpoint written.");
                    }
                } catch (ExecutionException e) {
                    LOG.error("exception in thread task: {}", e);
                }
                ++nCompleted;
                projectRunTime(nCompleted, nTasks);
            }
        } catch (InterruptedException e) {
            LOG.warn("run was interrupted after {} tasks", nCompleted);
        }
        threadPool.shutdown();
        if (accumulator != null)
            accumulator.finish();
        if (aggregateResultSet != null)
            aggregateResultSet.writeAppropriateFormat(outputPath);
        LOG.info("DONE.");
    }

    private void projectRunTime(int current, int total) {
        long currentTime = System.currentTimeMillis();
        // not threadsafe, but the worst thing that will happen is a double log message 
        // anyway we are using this in the controller thread now
        if (currentTime > lastLogTime + logThrottleSeconds * 1000) {
            lastLogTime = currentTime;
            double runTimeMin = (currentTime - startTime) / 1000.0 / 60.0;
            double projectedMin = (total - current) * (runTimeMin / current);
            LOG.info("received {} results out of {}", current, total);
            LOG.info("running {} min, {} min remaining (projected)", (int)runTimeMin, (int)projectedMin);
        }
    }
    
    private boolean checkpoint() {
        if (checkpointIntervalMinutes < 0 || aggregateResultSet == null)
            return false;
        long currentTime = System.currentTimeMillis();
        // not threadsafe, but the worst thing that will happen is a double checkpoint
        // anyway, this is being called in the controller thread now
        if (currentTime > lastCheckpointTime + checkpointIntervalMinutes * 60 * 1000) {
            lastCheckpointTime = currentTime;
            aggregateResultSet.writeAppropriateFormat(outputPath);
            return true;
        }
        return false;
    }
    
    private RoutingRequest buildRequest(Individual i) {
        RoutingRequest req = prototypeRoutingRequest.clone();
        req.setDateTime(date, time, timeZone);
        if (searchCutoffSeconds > 0) {
            req.worstTime = req.dateTime + (req.arriveBy ? -searchCutoffSeconds : searchCutoffSeconds);
        }
        GenericLocation latLon = new GenericLocation(i.lat, i.lon);
        req.batch = true;
        if (req.arriveBy)
            req.to = latLon;
        else
            req.from = latLon;
        try {
            req.setRoutingContext(graphService.getRouter(req.routerId).graph);
            return req;
        } catch (VertexNotFoundException vnfe) {
            LOG.debug("no vertex could be created near the origin point");
            return null;
        }
    }
    
    /** 
     * Generate samples for (i.e. non-invasively link into the Graph) only those individuals that 
     * were not rejected by filters. Other Individuals will have null samples, indicating that they 
     * should be skipped.
     */
    private void linkIntoGraph(Population p) {
        LOG.info("linking population {} to the graph...", p);
        int n = 0, nonNull = 0;
        for (Individual i : p) {
            Sample s = sampleFactory.getSample(i.lon, i.lat);
            i.sample = s;
            n += 1;
            if (s != null)
                nonNull += 1;
        }
        LOG.info("successfully linked {} individuals out of {}", nonNull, n);
    }
        
    /** 
     * A single computation to perform for a single origin.
     * Runnable, not Callable. We want accumulation to happen in the worker thread. 
     * Handling all accumulation in the controller thread risks amassing a queue of large 
     * result sets. 
     */
    private class BatchAnalystTask implements Runnable {
        
        protected final int i;
        protected final Individual oi;
        
        public BatchAnalystTask(int i, Individual oi) {
            this.i = i;
            this.oi = oi;
        }
        
        @Override
        public void run() {
            LOG.debug("calling origin : {}", oi);
            RoutingRequest req = buildRequest(oi);
            if (req != null) {
                ShortestPathTree spt = new AStar().getShortestPathTree(req);
                // ResultSet should be a local to avoid memory leak
                ResultSet results = ResultSet.forTravelTimes(destinations, spt);
                req.cleanup();
                switch (mode) {
                case ACCUMULATE:
                    synchronized (aggregateResultSet) {
                        accumulator.accumulate(oi.input, results, aggregateResultSet);
                    }
                    break;
                case AGGREGATE:
                    aggregateResultSet.results[i] = aggregator.computeAggregate(results);
                    break;
                default:
                    String subName = outputPath.replace("{}", String.format("%d_%s", i, oi.label));
                    results.writeAppropriateFormat(subName);
                }
                    
            }
        }        
    }    
    
}

