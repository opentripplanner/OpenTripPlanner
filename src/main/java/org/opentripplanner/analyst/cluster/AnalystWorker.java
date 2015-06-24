package org.opentripplanner.analyst.cluster;

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
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.opentripplanner.analyst.PointSet;
import org.opentripplanner.analyst.ResultSet;
import org.opentripplanner.analyst.SampleSet;
import org.opentripplanner.api.model.AgencyAndIdSerializer;
import org.opentripplanner.api.model.JodaLocalDateSerializer;
import org.opentripplanner.api.model.QualifiedModeSetSerializer;
import org.opentripplanner.api.model.TraverseModeSetSerializer;
import org.opentripplanner.profile.IsochroneGenerator;
import org.opentripplanner.profile.RepeatedRaptorProfileRouter;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Random;
import java.util.zip.GZIPOutputStream;

/**
 *
 */
public class AnalystWorker implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(AnalystWorker.class);

    public static final int POLL_TIMEOUT = 10000;

    public static final Random random = new Random();

    ObjectMapper objectMapper;

    String BROKER_BASE_URL = "http://localhost:9001";

    String s3Prefix = "analyst-dev";

    static final HttpClient httpClient;

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
    long startupTime;

    // Region awsRegion = Region.getRegion(Regions.EU_CENTRAL_1);
    Region awsRegion = Region.getRegion(Regions.US_EAST_1);

    boolean isSinglePoint = false;

    public AnalystWorker() {

        startupTime = System.currentTimeMillis() / 1000; // TODO auto-shutdown

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

        /* These serve as lazy-loading caches for graphs and point sets. */
        clusterGraphBuilder = new ClusterGraphBuilder(s3Prefix + "-graphs");
        pointSetDatastore = new PointSetDatastore(10, null, false, s3Prefix + "-pointsets");

        int timeout = 10 * 1000;
    }

    @Override
    public void run() {
        // Loop forever, attempting to fetch some messages from a queue and process them.
        while (true) {
            LOG.info("Long-polling for work ({} second timeout).", POLL_TIMEOUT / 1000.0);
            // Long-poll (wait a few seconds for messages to become available)
            // TODO internal blocking queue feeding work threads, polls whenever queue.size() < nProcessors
            List<AnalystClusterRequest> tasks = getSomeWork();
            if (tasks == null) {
                LOG.info("Didn't get any work. Retrying.");
                continue;
            }
            tasks.parallelStream().forEach(this::handleOneRequest);
        }
    }

    private void handleOneRequest(AnalystClusterRequest clusterRequest) {
        try {
            LOG.info("Handling message {}", clusterRequest.toString());

            // Get the graph object for the ID given in the request, fetching inputs and building as needed.
            // All requests handled together are for the same graph, and this call is synchronized so the graph will
            // only be built once.
            Graph graph = clusterGraphBuilder.getGraph(clusterRequest.graphId);
            graphId = clusterRequest.graphId; // Record graphId so we "stick" to this same graph on subsequent polls

            // This result envelope will hold the result of the profile or single-time one-to-many search.
            ResultEnvelope envelope = new ResultEnvelope();
            if (clusterRequest.profileRequest != null) {
                SampleSet sampleSet;
                boolean isochrone = clusterRequest.destinationPointsetId == null;
                if (!isochrone) {
                    // A pointset was specified, calculate travel times to the points in the pointset.
                    // Fetch the set of points we will use as destinations for this one-to-many search
                    PointSet pointSet = pointSetDatastore.get(clusterRequest.destinationPointsetId);
                    // TODO this breaks if graph has been rebuilt
                    sampleSet = pointSet.getOrCreateSampleSet(graph);
                } else {
                    // TODO cache
                    // FIXME this is making a regular grid and then projecting it into another regular
                    // grid with the same grid size in IsochroneGenerator.
                    PointSet grid = PointSet.regularGrid(graph.getExtent(), IsochroneGenerator.GRID_SIZE_METERS);
                    sampleSet = grid.getSampleSet(graph);
                }
                RepeatedRaptorProfileRouter router =
                        new RepeatedRaptorProfileRouter(graph, clusterRequest.profileRequest, sampleSet);
                try {
                    router.route();
                    ResultSet.RangeSet results = router.makeResults(clusterRequest.includeTimes, !isochrone, isochrone);
                    // put in constructor?
                    envelope.bestCase = results.min;
                    envelope.avgCase = results.avg;
                    envelope.worstCase = results.max;
                    envelope.id = clusterRequest.id;
                    envelope.destinationPointsetId = clusterRequest.destinationPointsetId;
                } catch (Exception ex) {
                    // Leave the envelope empty TODO include error information
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
        } catch (Exception ex) {
            LOG.error("An error occurred while routing: " + ex.getMessage());
            ex.printStackTrace();
        }

    }

    public List<AnalystClusterRequest> getSomeWork() {

        // Run a POST request (long-polling for work) indicating which graph this worker prefers to work on
        String url = BROKER_BASE_URL + "/dequeue/" + graphId;
        HttpPost httpPost = new HttpPost(url);
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

    public static void main(String[] args) {
        new AnalystWorker().run();
    }

}