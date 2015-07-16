package org.opentripplanner.analyst.cluster;

import ch.qos.logback.core.PropertyDefinerBase;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.conveyal.geojson.GeoJsonModule;
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
import org.opentripplanner.analyst.ResultSet;
import org.opentripplanner.analyst.SampleSet;
import org.opentripplanner.api.model.AgencyAndIdSerializer;
import org.opentripplanner.api.model.JodaLocalDateSerializer;
import org.opentripplanner.api.model.QualifiedModeSetSerializer;
import org.opentripplanner.api.model.TraverseModeSetSerializer;
import org.opentripplanner.profile.RaptorWorkerData;
import org.opentripplanner.profile.RepeatedRaptorProfileRouter;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

/**
 *
 */
public class AnalystWorker implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(AnalystWorker.class);

    public static final String WORKER_ID_HEADER = "X-Worker-Id";

    public static final int POLL_TIMEOUT = 10 * 1000;

    /** should this worker shut down automatically */
    public final boolean autoShutdown;

    public static final Random random = new Random();

    private TaskStatisticsStore statsStore;

    ObjectMapper objectMapper;

    String BROKER_BASE_URL = "http://localhost:9001";

    String s3Prefix = "analyst-dev";

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

    private final String workerId = UUID.randomUUID().toString().replace("-", ""); // a unique identifier for each worker so the broker can catalog them


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

    /**
     * worker ID - just a random ID so we can differentiate machines used for computation.
     * Useful to isolate the logs from a particular machine, as well as to evaluate any
     * variation in performance coming from variation in the performance of the underlying
     * VMs.
     *
     * This needs to be static so the logger can access it; see the static member class
     * WorkerIdDefiner. A side effect is that only one worker can run in a given JVM.
     */
    public static final String machineId = UUID.randomUUID().toString().replaceAll("-", "");

    boolean isSinglePoint = false;

    public AnalystWorker(Properties config) {

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

    @Override
    public void run() {
        // Loop forever, attempting to fetch some messages from a queue and process them.
        boolean idle = false;
        while (true) {
            // Consider shutting down if enough time has passed
            if (System.currentTimeMillis() > nextShutdownCheckTime && autoShutdown) {
                if (idle) {
                    try {
                        Process process = new ProcessBuilder("sudo", "/sbin/shutdown", "-h", "now").start();
                        process.waitFor();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    } finally {
                        System.exit(0);
                    }
                }
                nextShutdownCheckTime += 60 * 60 * 1000;
            }
            LOG.info("Long-polling for work ({} second timeout).", POLL_TIMEOUT / 1000.0);
            // Long-poll (wait a few seconds for messages to become available)
            // TODO internal blocking queue feeding work threads, polls whenever queue.size() < nProcessors
            List<AnalystClusterRequest> tasks = getSomeWork();
            if (tasks == null) {
                LOG.info("Didn't get any work. Retrying.");
                idle = true;
                continue;
            }
            tasks.parallelStream().forEach(this::handleOneRequest);
            idle = false;
        }
    }

    private void handleOneRequest(AnalystClusterRequest clusterRequest) {
        try {
            long startTime = System.currentTimeMillis();
            LOG.info("Handling message {}", clusterRequest.toString());

            TaskStatistics ts = new TaskStatistics();
            ts.pointsetId = clusterRequest.destinationPointsetId;
            ts.graphId = clusterRequest.graphId;
            ts.awsInstanceType = instanceType;
            ts.jobId = clusterRequest.jobId;
            ts.workerId = machineId;
            ts.single = clusterRequest.outputLocation == null;

            long graphStartTime = System.currentTimeMillis();

            // Get the graph object for the ID given in the request, fetching inputs and building as needed.
            // All requests handled together are for the same graph, and this call is synchronized so the graph will
            // only be built once.
            Graph graph = clusterGraphBuilder.getGraph(clusterRequest.graphId);
            graphId = clusterRequest.graphId; // Record graphId so we "stick" to this same graph on subsequent polls

            ts.graphBuild = (int) (System.currentTimeMillis() - graphStartTime);

            ts.graphTripCount = graph.index.patternForTrip.size();
            ts.graphStopCount = graph.index.stopForId.size();

            // This result envelope will hold the result of the profile or single-time one-to-many search.
            ResultEnvelope envelope = new ResultEnvelope();
            if (clusterRequest.profileRequest != null) {
                ts.lon = clusterRequest.profileRequest.fromLon;
                ts.lat = clusterRequest.profileRequest.fromLat;


                RepeatedRaptorProfileRouter router;

                boolean isochrone = clusterRequest.destinationPointsetId == null;
                ts.isochrone = isochrone;
                if (!isochrone) {
                    // A pointset was specified, calculate travel times to the points in the pointset.
                    // Fetch the set of points we will use as destinations for this one-to-many search
                    PointSet pointSet = pointSetDatastore.get(clusterRequest.destinationPointsetId);
                    // TODO this breaks if graph has been rebuilt
                    SampleSet sampleSet = pointSet.getOrCreateSampleSet(graph);
                    router =
                            new RepeatedRaptorProfileRouter(graph, clusterRequest.profileRequest, sampleSet);

                    // no reason to cache single-point RaptorWorkerData
                    if (clusterRequest.outputLocation != null) {
                        router.raptorWorkerData = workerDataCache.get(clusterRequest.jobId, () -> {
                            return RepeatedRaptorProfileRouter
                                    .getRaptorWorkerData(clusterRequest.profileRequest, graph,
                                            sampleSet, ts);
                        });
                    }
                } else {
                    router = new RepeatedRaptorProfileRouter(graph, clusterRequest.profileRequest);

                    if (clusterRequest.outputLocation == null) {
                        router.raptorWorkerData = workerDataCache.get(clusterRequest.jobId, () -> {
                            return RepeatedRaptorProfileRouter
                                    .getRaptorWorkerData(clusterRequest.profileRequest, graph, null,
                                            ts);
                        });
                    }
                }

                try {
                    router.route(ts);
                    long resultSetStart = System.currentTimeMillis();

                    if (isochrone) {
                        envelope.worstCase = new ResultSet(router.timeSurfaceRangeSet.max);
                        envelope.bestCase = new ResultSet(router.timeSurfaceRangeSet.min);
                        envelope.avgCase = new ResultSet(router.timeSurfaceRangeSet.avg);
                    } else {
                        ResultSet.RangeSet results = router
                                .makeResults(clusterRequest.includeTimes, !isochrone, isochrone);
                        // put in constructor?
                        envelope.bestCase = results.min;
                        envelope.avgCase = results.avg;
                        envelope.worstCase = results.max;
                    }
                    envelope.id = clusterRequest.id;
                    envelope.destinationPointsetId = clusterRequest.destinationPointsetId;

                    ts.resultSets = (int) (System.currentTimeMillis() - resultSetStart);
                    ts.success = true;
                } catch (Exception ex) {
                    // Leave the envelope empty TODO include error information
                    ts.success = false;
                }
            } else {
                // No profile request, this must be a plain one to many routing request.
                RoutingRequest routingRequest = clusterRequest.routingRequest;
                // TODO finish the non-profile case
            }

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
                // DELETE the task from the broker, confirming it has been handled and should not be re-delivered.
                deleteRequest(clusterRequest);
            } else {
                // No output location on S3 specified, return the result via the broker and mark the task completed.
                finishPriorityTask(clusterRequest, envelope);
            }

            ts.total = (int) (System.currentTimeMillis() - startTime);
            statsStore.store(ts);
        } catch (Exception ex) {
            LOG.error("An error occurred while routing: " + ex.getMessage());
            ex.printStackTrace();
        }

    }

    public List<AnalystClusterRequest> getSomeWork() {

        // Run a POST request (long-polling for work) indicating which graph this worker prefers to work on
        String url = BROKER_BASE_URL + "/dequeue/" + graphId;
        HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader(new BasicHeader(WORKER_ID_HEADER, workerId));
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
            LOG.error("JSON processing exception while getting work: {}", e.getMessage());
        } catch (SocketTimeoutException stex) {
            LOG.error("Socket timeout while waiting to receive work.");
        } catch (HttpHostConnectException ce) {
            LOG.error("Broker refused connection. Sleeping before retry.");
            try {
                Thread.currentThread().sleep(5000);
            } catch (InterruptedException e) {
            }
        } catch (IOException e) {
            LOG.error("IO exception while getting work.");
            e.printStackTrace();
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
            e.printStackTrace();
            LOG.info("Failed to mark task {} as completed.", clusterRequest.taskId);
        }
    }

    /**
     * DELETE the given message from the broker, indicating that it has been processed by a worker.
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
            e.printStackTrace();
            LOG.info("Failed to delete task {}", clusterRequest.taskId);
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

        new AnalystWorker(config).run();
    }

    /**
     * A class that allows the logging framework to access the worker ID; with a custom logback config
     * this can be used to print the machine ID with each log message. This is useful if you have multiple
     * workers logging to the same log aggregation service.
     */
    public static class WorkerIdDefiner extends PropertyDefinerBase {
        @Override public String getPropertyValue() {
            return AnalystWorker.machineId;
        }
    }
}