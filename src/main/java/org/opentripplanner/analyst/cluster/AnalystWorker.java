package org.opentripplanner.analyst.cluster;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.conveyal.geojson.GeoJsonModule;
import com.conveyal.r5.R5Main;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.opentripplanner.analyst.PointSet;
import org.opentripplanner.analyst.SampleSet;
import org.opentripplanner.api.model.AgencyAndIdSerializer;
import org.opentripplanner.api.model.JodaLocalDateSerializer;
import org.opentripplanner.api.model.QualifiedModeSetSerializer;
import org.opentripplanner.api.model.TraverseModeSetSerializer;
import org.opentripplanner.common.MavenVersion;
import org.opentripplanner.profile.RaptorWorkerData;
import org.opentripplanner.profile.RepeatedRaptorProfileRouter;
import org.opentripplanner.routing.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

/**
 *
 */
public class AnalystWorker implements Runnable {

    /**
     * worker ID - just a random ID so we can differentiate machines used for computation.
     * Useful to isolate the logs from a particular machine, as well as to evaluate any
     * variation in performance coming from variation in the performance of the underlying
     * VMs.
     *
     * This needs to be static so the logger can access it; see the static member class
     * WorkerIdDefiner. A side effect is that only one worker can run in a given JVM. It also
     * needs to be defined before the logger is defined, so that it is initialized before the
     * logger is.
     */
    public static final String machineId = UUID.randomUUID().toString().replaceAll("-", "");

    private static final Logger LOG = LoggerFactory.getLogger(AnalystWorker.class);

    public static final String WORKER_ID_HEADER = "X-Worker-Id";

    public static final int POLL_TIMEOUT = 10 * 1000;

    /**
     * If this value is non-negative, the worker will not actually do any work. It will just report all tasks
     * as completed immediately, but will fail to do so on the given percentage of tasks. This is used in testing task
     * re-delivery and overall broker sanity.
     */
    public int dryRunFailureRate = -1;

    /** How long (minimum, in milliseconds) should this worker stay alive after receiving a single point request? */
    public static final int SINGLE_POINT_KEEPALIVE = 15 * 60 * 1000;

    /** should this worker shut down automatically */
    public final boolean autoShutdown;

    public static final Random random = new Random();

    private TaskStatisticsStore statsStore;

    /** is there currently a channel open to the broker to receive single point jobs? */
    private volatile boolean sideChannelOpen = false;

    ObjectMapper objectMapper;

    String BROKER_BASE_URL = "http://localhost:9001";

    static final HttpClient httpClient;

    /** Cache RAPTOR data by Job ID */
    private Cache<String, RaptorWorkerData> workerDataCache = CacheBuilder.newBuilder()
            .maximumSize(200)
            .build();

    static {
        PoolingHttpClientConnectionManager mgr = new PoolingHttpClientConnectionManager();
        mgr.setDefaultMaxPerRoute(20);

        int timeout = 10 * 1000;
        SocketConfig cfg = SocketConfig.custom()
                .setSoTimeout(timeout)
                .build();
        mgr.setDefaultSocketConfig(cfg);

        httpClient = HttpClients.custom()
                .setConnectionManager(mgr)
                .build();
    }

    // Of course this will eventually need to be shared between multiple AnalystWorker threads.
    ClusterGraphBuilder clusterGraphBuilder;

    // Of course this will eventually need to be shared between multiple AnalystWorker threads.
    PointSetDatastore pointSetDatastore;

    // Clients for communicating with Amazon web services
    AmazonS3 s3;

    String graphId = null;
    long startupTime, nextShutdownCheckTime;

    // Region awsRegion = Region.getRegion(Regions.EU_CENTRAL_1);
    Region awsRegion = Region.getRegion(Regions.US_EAST_1);

    /** aws instance type, or null if not running on AWS */
    private String instanceType;

    long lastHighPriorityRequestProcessed = 0;

    /**
     * Queue for high-priority tasks. Should be plenty long enough to hold all that have come in -
     * we don't need to block on polling the manager.
     */
    private ThreadPoolExecutor highPriorityExecutor, batchExecutor;

    public AnalystWorker(Properties config) {
        // print out date on startup so that CloudWatch logs has a unique fingerprint
        LOG.info("Analyst worker starting at {}", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));

        // parse the configuration
        // set up the stats store
        String statsQueue = config.getProperty("statistics-queue");
        if (statsQueue != null)
            this.statsStore = new SQSTaskStatisticsStore(statsQueue);
        else
            // a stats store that does nothing.
            this.statsStore = s -> {};

        String addr = config.getProperty("broker-address");
        String port = config.getProperty("broker-port");

        if (addr != null) {
            if (port != null)
                this.BROKER_BASE_URL = String.format("http://%s:%s", addr, port);
            else
                this.BROKER_BASE_URL = String.format("http://%s", addr);
        }

        // set the initial graph affinity of this worker (if it is not in the config file it will be
        // set to null, i.e. no graph affinity)
        // we don't actually build the graph now; this is just a hint to the broker as to what
        // graph this machine was intended to analyze.
        this.graphId = config.getProperty("initial-graph-id");

        this.pointSetDatastore = new PointSetDatastore(10, null, false, config.getProperty("pointsets-bucket"));
        this.clusterGraphBuilder = new ClusterGraphBuilder(config.getProperty("graphs-bucket"));

        Boolean autoShutdown = Boolean.parseBoolean(config.getProperty("auto-shutdown"));
        this.autoShutdown = autoShutdown == null ? false : autoShutdown;

        // Consider shutting this worker down once per hour, starting 55 minutes after it started up.
        startupTime = System.currentTimeMillis();
        nextShutdownCheckTime = startupTime + 55 * 60 * 1000;

        // When creating the S3 and SQS clients use the default credentials chain.
        // This will check environment variables and ~/.aws/credentials first, then fall back on
        // the auto-assigned IAM role if this code is running on an EC2 instance.
        // http://docs.aws.amazon.com/AWSSdkDocsJava/latest/DeveloperGuide/java-dg-roles.html
        s3 = new AmazonS3Client();
        s3.setRegion(awsRegion);

        /* The ObjectMapper (de)serializes JSON. */
        objectMapper = new ObjectMapper();
        objectMapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false); // ignore JSON fields that don't match target type

        /* Tell Jackson how to (de)serialize AgencyAndIds, which appear as map keys in routing requests. */
        objectMapper.registerModule(AgencyAndIdSerializer.makeModule());

        /* serialize/deserialize qualified mode sets */
        objectMapper.registerModule(QualifiedModeSetSerializer.makeModule());

        /* serialize/deserialize Joda dates */
        objectMapper.registerModule(JodaLocalDateSerializer.makeModule());

        /* serialize/deserialize traversemodesets */
        objectMapper.registerModule(TraverseModeSetSerializer.makeModule());

        objectMapper.registerModule(new GeoJsonModule());

        instanceType = getInstanceType();
    }

    /**
     * This is the main worker event loop which fetches tasks from a broker and schedules them for execution.
     * It maintains a small local queue on the worker so that it doesn't idle while fetching new tasks.
     */
    @Override
    public void run() {
        // create executors with up to one thread per processor
        int nP = Runtime.getRuntime().availableProcessors();
        highPriorityExecutor = new ThreadPoolExecutor(1, nP, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(255));
        highPriorityExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        batchExecutor = new ThreadPoolExecutor(1, nP, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(nP * 2));
        batchExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());

        // Build a graph on startup, iff a graph ID was provided.
        if (graphId != null) {
            LOG.info("Prebuilding graph {}", graphId);
            Graph graph = clusterGraphBuilder.getGraph(graphId);
            // also prebuild the stop tree cache
            graph.index.getStopTreeCache();
            LOG.info("Done prebuilding graph {}", graphId);
        }

        // Start filling the work queues.
        boolean idle = false;
        while (true) {
            long now = System.currentTimeMillis();
            // Consider shutting down if enough time has passed
            if (now > nextShutdownCheckTime && autoShutdown) {
                if (idle && now > lastHighPriorityRequestProcessed + SINGLE_POINT_KEEPALIVE) {
                    LOG.warn("Machine is idle, shutting down.");
                    try {
                        Process process = new ProcessBuilder("sudo", "/sbin/shutdown", "-h", "now")
                                .start();
                        process.waitFor();
                    } catch (Exception ex) {
                        LOG.error("Unable to terminate worker", ex);
                    } finally {
                        System.exit(0);
                    }
                }
                nextShutdownCheckTime += 60 * 60 * 1000;
            }
            LOG.info("Long-polling for work ({} second timeout).", POLL_TIMEOUT / 1000.0);
            // Long-poll (wait a few seconds for messages to become available)
            List<AnalystClusterRequest> tasks = getSomeWork(WorkType.BATCH);
            if (tasks == null) {
                LOG.info("Didn't get any work. Retrying.");
                idle = true;
                continue;
            }

            // run through high-priority tasks first to ensure they are enqueued even if the batch
            // queue blocks.
            tasks.stream().filter(t -> t.outputLocation == null)
                    .forEach(t -> highPriorityExecutor.execute(() -> {
                        LOG.warn(
                                "Handling single point request via normal channel, side channel should open shortly.");
                        this.handleOneRequest(t);
                    }));

            logQueueStatus();

            // enqueue low-priority tasks; note that this may block anywhere in the process
            tasks.stream().filter(t -> t.outputLocation != null)
                .forEach(t -> {
                    // attempt to enqueue, waiting if the queue is full
                    while (true) {
                        try {
                            batchExecutor.execute(() -> this.handleOneRequest(t));
                            break;
                        } catch (RejectedExecutionException e) {
                            // queue is full, wait 200ms and try again
                            try {
                                Thread.sleep(200);
                            } catch (InterruptedException e1) { /* nothing */}
                        }
                    }
                });

            logQueueStatus();

            idle = false;
        }
    }

    /**
     * This is the callback that processes a single task and returns the results upon completion.
     * It may be called several times simultaneously on different executor threads.
     */
    private void handleOneRequest(AnalystClusterRequest clusterRequest) {

        if (dryRunFailureRate >= 0) {
            // This worker is running in test mode.
            // It should report all work as completed without actually doing anything,
            // but will fail a certain percentage of the time.
            if (random.nextInt(100) >= dryRunFailureRate) {
                // Pretend to succeed.
                deleteRequest(clusterRequest);
            } else {
                LOG.info("Intentionally failing on task {}", clusterRequest.taskId);
            }
            return;
        }

        try {
            long startTime = System.currentTimeMillis();
            LOG.info("Handling message {}", clusterRequest.toString());

            // We need to distinguish between and handle four different types of requests here:
            // Either vector isochrones or accessibility to a pointset,
            // as either a single-origin priority request (where the result is returned immediately)
            // or a job task (where the result is saved to output location on S3).
            boolean isochrone = (clusterRequest.destinationPointsetId == null);
            boolean singlePoint = (clusterRequest.outputLocation == null);
            boolean transit = (clusterRequest.profileRequest.transitModes != null && clusterRequest.profileRequest.transitModes.isTransit());

            if (singlePoint) {
                lastHighPriorityRequestProcessed = startTime;
                if (!sideChannelOpen) {
                    openSideChannel();
                }
            }

            TaskStatistics ts = new TaskStatistics();
            ts.pointsetId = clusterRequest.destinationPointsetId;
            ts.graphId = clusterRequest.graphId;
            ts.awsInstanceType = instanceType;
            ts.jobId = clusterRequest.jobId;
            ts.workerId = machineId;
            ts.single = singlePoint;

            // Get the graph object for the ID given in the request, fetching inputs and building as needed.
            // All requests handled together are for the same graph, and this call is synchronized so the graph will
            // only be built once.
            long graphStartTime = System.currentTimeMillis();
            Graph graph = clusterGraphBuilder.getGraph(clusterRequest.graphId);
            graphId = clusterRequest.graphId; // Record graphId so we "stick" to this same graph on subsequent polls
            ts.graphBuild = (int) (System.currentTimeMillis() - graphStartTime);
            ts.graphTripCount = graph.index.patternForTrip.size();
            ts.graphStopCount = graph.index.stopForId.size();
            ts.lon = clusterRequest.profileRequest.fromLon;
            ts.lat = clusterRequest.profileRequest.fromLat;

            final SampleSet sampleSet;

            // If this one-to-many request is for accessibility information based on travel times to a pointset,
            // fetch the set of points we will use as destinations.
            if (isochrone) {
                // This is an isochrone request, tell the RepeatedRaptorProfileRouter there are no targets.
                sampleSet = null;
            } else {
                // This is not an isochrone request. There is necessarily a destination point set supplied.
                PointSet pointSet = pointSetDatastore.get(clusterRequest.destinationPointsetId);
                sampleSet = pointSet.getOrCreateSampleSet(graph); // TODO this breaks if graph has been rebuilt
            }

            // Note that all parameters to create the Raptor worker data are passed in the constructor except ts.
            // Why not pass in ts as well since this is a throwaway calculator?
            RepeatedRaptorProfileRouter router =
                    new RepeatedRaptorProfileRouter(graph, clusterRequest.profileRequest, sampleSet);
            router.ts = ts;

            // Produce RAPTOR data tables, going through a cache where relevant.
            // This is only used for multi-point requests. Single-point requests are assumed to be continually
            // changing, so we create throw-away RAPTOR tables for them.
            // Ideally we'd want this cacheing to happen transparently inside the RepeatedRaptorProfileRouter,
            // but the RepeatedRaptorProfileRouter doesn't know the job ID or other information from the cluster request.
            // It would be possible to just supply the cache _key_ as a way of saying that the cache should be used.
            // But then we'd need to pass in both the cache and the key, which is weird.
            if (transit && !singlePoint) {
                long dataStart = System.currentTimeMillis();
                router.raptorWorkerData = workerDataCache.get(clusterRequest.jobId, () -> RepeatedRaptorProfileRouter
                        .getRaptorWorkerData(clusterRequest.profileRequest, graph, sampleSet, ts));
                ts.raptorData = (int) (System.currentTimeMillis() - dataStart);
            } else {
                // The worker will generate a one-time throw-away table.
                router.raptorWorkerData = null;
            }

            // Run the core repeated-raptor analysis.
            // This result envelope will contain the results of the one-to-many profile or single-departure-time search.
            ResultEnvelope envelope = new ResultEnvelope();
            try {
                // TODO when router runs, if there are no transit modes defined it should just skip the transit work.
                router.includeTimes = clusterRequest.includeTimes;
                envelope = router.route();
                envelope.id = clusterRequest.id;
                ts.success = true;
            } catch (Exception ex) {
                // An error occurred. Leave the envelope empty and TODO include error information.
                LOG.error("Error occurred in profile request", ex);
                ts.success = false;
            }

            // Send the ResultEnvelope back to the user.
            // The results are either stored on S3 (for multi-origin jobs) or sent back through the broker (for
            // immediate interactive display of isochrones).
            envelope.id = clusterRequest.id;
            envelope.jobId = clusterRequest.jobId;
            envelope.destinationPointsetId = clusterRequest.destinationPointsetId;
            if (clusterRequest.outputLocation != null) {
                // Convert the result envelope and its contents to JSON and gzip it in this thread.
                // Transfer the results to Amazon S3 in another thread, piping between the two.
                String s3key = String.join("/", clusterRequest.jobId, clusterRequest.id + ".json.gz");
                PipedInputStream inPipe = new PipedInputStream();
                PipedOutputStream outPipe = new PipedOutputStream(inPipe);
                new Thread(() -> {
                    s3.putObject(clusterRequest.outputLocation, s3key, inPipe, null);
                }).start();
                OutputStream gzipOutputStream = new GZIPOutputStream(outPipe);
                // We could do the writeValue() in a thread instead, in which case both the DELETE and S3 options
                // could consume it in the same way.
                objectMapper.writeValue(gzipOutputStream, envelope);
                gzipOutputStream.close();
                // Tell the broker the task has been handled and should not be re-delivered to another worker.
                deleteRequest(clusterRequest);
            } else {
                // No output location was provided. Instead of saving the result on S3,
                // return the result immediately via a connection held open by the broker and mark the task completed.
                finishPriorityTask(clusterRequest, envelope);
            }

            // Record information about the current task so we can analyze usage and efficiency over time.
            ts.total = (int) (System.currentTimeMillis() - startTime);
            statsStore.store(ts);

        } catch (Exception ex) {
            LOG.error("An error occurred while routing", ex);
        }

    }

    /** Open a single point channel to the broker to receive high-priority requests immediately */
    private synchronized void openSideChannel () {
        if (sideChannelOpen) {
            return;
        }
        LOG.info("Opening side channel for single point requests.");
        new Thread(() -> {
            sideChannelOpen = true;
            // don't keep single point connections alive forever
            while (System.currentTimeMillis() < lastHighPriorityRequestProcessed + SINGLE_POINT_KEEPALIVE) {
                LOG.info("Awaiting high-priority work");
                try {
                    List<AnalystClusterRequest> tasks = getSomeWork(WorkType.HIGH_PRIORITY);

                    if (tasks != null)
                        tasks.stream().forEach(t -> highPriorityExecutor.execute(
                                () -> this.handleOneRequest(t)));

                    logQueueStatus();
                } catch (Exception e) {
                    LOG.error("Unexpected exception getting single point work", e);
                }
            }
            sideChannelOpen = false;
        }).start();
    }

    public List<AnalystClusterRequest> getSomeWork(WorkType type) {

        // Run a POST request (long-polling for work) indicating which graph this worker prefers to work on
        String url;
        if (type == WorkType.HIGH_PRIORITY) {
            // this is a side-channel request for single point work
            url = BROKER_BASE_URL + "/single/" + graphId;
        } else {
            url = BROKER_BASE_URL + "/dequeue/" + graphId;
        }
        HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader(new BasicHeader(WORKER_ID_HEADER, machineId));
        HttpResponse response = null;
        try {
            response = httpClient.execute(httpPost);
            HttpEntity entity = response.getEntity();
            if (entity == null) {
                return null;
            }
            if (response.getStatusLine().getStatusCode() != 200) {
                EntityUtils.consumeQuietly(entity);
                return null;
            }
            return objectMapper.readValue(entity.getContent(), new TypeReference<List<AnalystClusterRequest>>() {
            });
        } catch (JsonProcessingException e) {
            LOG.error("JSON processing exception while getting work", e);
        } catch (SocketTimeoutException stex) {
            LOG.error("Socket timeout while waiting to receive work.");
        } catch (HttpHostConnectException ce) {
            LOG.error("Broker refused connection. Sleeping before retry.");
            try {
                Thread.currentThread().sleep(5000);
            } catch (InterruptedException e) {
            }
        } catch (IOException e) {
            LOG.error("IO exception while getting work", e);
        }
        return null;

    }

    /**
     * Signal the broker that the given high-priority task is completed, providing a result.
     */
    public void finishPriorityTask(AnalystClusterRequest clusterRequest, Object result) {
        String url = BROKER_BASE_URL + String.format("/complete/priority/%s", clusterRequest.taskId);
        HttpPost httpPost = new HttpPost(url);
        try {
            // TODO reveal any errors etc. that occurred on the worker.
            // Really this should probably be done with an InputStreamEntity and a JSON writer thread.
            byte[] serializedResult = objectMapper.writeValueAsBytes(result);
            httpPost.setEntity(new ByteArrayEntity(serializedResult));
            HttpResponse response = httpClient.execute(httpPost);
            // Signal the http client library that we're done with this response object, allowing connection reuse.
            EntityUtils.consumeQuietly(response.getEntity());
            if (response.getStatusLine().getStatusCode() == 200) {
                LOG.info("Successfully marked task {} as completed.", clusterRequest.taskId);
            } else if (response.getStatusLine().getStatusCode() == 404) {
                LOG.info("Task {} was not marked as completed because it doesn't exist.", clusterRequest.taskId);
            } else {
                LOG.info("Failed to mark task {} as completed, ({}).", clusterRequest.taskId,
                        response.getStatusLine());
            }
        } catch (Exception e) {
            LOG.warn("Failed to mark task {} as completed.", clusterRequest.taskId, e);
        }
    }

    /**
     * Tell the broker that the given message has been successfully processed by a worker (HTTP DELETE).
     */
    public void deleteRequest(AnalystClusterRequest clusterRequest) {
        String url = BROKER_BASE_URL + String.format("/tasks/%s", clusterRequest.taskId);
        HttpDelete httpDelete = new HttpDelete(url);
        try {
            HttpResponse response = httpClient.execute(httpDelete);
            // Signal the http client library that we're done with this response object, allowing connection reuse.
            EntityUtils.consumeQuietly(response.getEntity());
            if (response.getStatusLine().getStatusCode() == 200) {
                LOG.info("Successfully deleted task {}.", clusterRequest.taskId);
            } else {
                LOG.info("Failed to delete task {} ({}).", clusterRequest.taskId, response.getStatusLine());
            }
        } catch (Exception e) {
            LOG.warn("Failed to delete task {}", clusterRequest.taskId, e);
        }
    }

    /** Get the AWS instance type if applicable */
    public String getInstanceType () {
        try {
            HttpGet get = new HttpGet();
            // see http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-instance-metadata.html
            // This seems very much not EC2-like to hardwire an IP address for getting instance metadata,
            // but that's how it's done.
            get.setURI(new URI("http://169.254.169.254/latest/meta-data/instance-type"));
            get.setConfig(RequestConfig.custom()
                    .setConnectTimeout(2000)
                    .setSocketTimeout(2000)
                    .build()
            );

            HttpResponse res = httpClient.execute(get);

            InputStream is = res.getEntity().getContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String type = reader.readLine().trim();
            reader.close();
            return type;
        } catch (Exception e) {
            LOG.info("could not retrieve EC2 instance type, you may be running outside of EC2.");
            return null;
        }
    }

    /** log queue status */
    private void logQueueStatus() {
        LOG.info("Waiting tasks: high priority: {}, batch: {}", highPriorityExecutor.getQueue().size(), batchExecutor.getQueue().size());
    }

    /**
     * Requires a worker configuration, which is a Java Properties file with the following
     * attributes.
     *
     * broker-address               address of the broker, without protocol or port
     * broker port                  port broker is running on, default 80.
     * graphs-bucket                S3 bucket in which graphs are stored.
     * pointsets-bucket             S3 bucket in which pointsets are stored
     * auto-shutdown                Should this worker shut down its machine if it is idle (e.g. on throwaway cloud instances)
     * statistics-queue             SQS queue to which to send statistics (optional)
     * initial-graph-id             The graph ID for this worker to start on
     */
    public static void main(String[] args) {
        LOG.info("Starting analyst worker");
        LOG.info("OTP commit is {}", MavenVersion.VERSION.commit);

        Properties config = new Properties();

        try {
            File cfg;
            if (args.length > 0)
                cfg = new File(args[0]);
            else
                cfg = new File("worker.conf");

            InputStream cfgis = new FileInputStream(cfg);
            config.load(cfgis);
            cfgis.close();
        } catch (Exception e) {
            LOG.info("Error loading worker configuration", e);
            return;
        }

        if (Boolean.parseBoolean(config.getProperty("use-transport-networks", "false"))) {
            // start R5 to work with transport networks
            LOG.info("Transport network support enabled, deferring computation to R5");
            com.conveyal.r5.analyst.cluster.AnalystWorker.main(args);
        }
        else {
            try {
                new AnalystWorker(config).run();
            } catch (Exception e) {
                LOG.error("Error in analyst worker", e);
                return;
            }
        }
    }

    public static enum WorkType {
        HIGH_PRIORITY, BATCH;
    }
}