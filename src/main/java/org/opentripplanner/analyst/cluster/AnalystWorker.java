package org.opentripplanner.analyst.cluster;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.opentripplanner.analyst.PointSet;
import org.opentripplanner.analyst.ResultSet;
import org.opentripplanner.analyst.SampleSet;
import org.opentripplanner.api.model.AgencyAndIdSerializer;
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

    DefaultHttpClient httpClient = new DefaultHttpClient();

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

    public AnalystWorker () {

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

        /* These serve as lazy-loading caches for graphs and point sets. */
        clusterGraphBuilder = new ClusterGraphBuilder(s3Prefix + "-graphs");
        pointSetDatastore = new PointSetDatastore(10, null, false, s3Prefix + "-pointsets");

        /* The HTTP Client for talking to the Analyst Broker. */
        HttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, POLL_TIMEOUT);
        HttpConnectionParams.setSoTimeout(httpParams, POLL_TIMEOUT);
        HttpConnectionParams.setSoKeepalive(httpParams, true);
        httpClient.setParams(httpParams);

    }

    @Override
    public void run() {
        // Loop forever, attempting to fetch some messages from a queue and process them.
        while (true) {
            LOG.info("Long-polling for work ({} second timeout).", POLL_TIMEOUT/1000.0);
            // Long-poll (wait a few seconds for messages to become available)
            // TODO internal blocking queue feeding work threads, polls whenever queue.size() < nProcessors
            List<AnalystClusterRequest> tasks = getSomeWork();
            if (tasks == null) {
                LOG.info("Didn't get any work. Retrying.");
                continue;
            }
            tasks.parallelStream().forEach(this::handleOneRequest);
            // Remove messages from queue so they won't be re-delivered to other workers.
            LOG.info("Removing requests from broker queue.");
            for (AnalystClusterRequest task : tasks) {
                boolean success = deleteRequest(task);
                LOG.info("deleted task {}: {}", task.taskId, success ? "SUCCESS" : "FAIL");
            }
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
                // TODO check graph and job ID against queue URL for coherency
                SampleSet sampleSet = null;
                if (clusterRequest.destinationPointsetId != null) {
                    // A pointset was specified, calculate travel times to the points in the pointset.
                    // Fetch the set of points we will use as destinations for this one-to-many search
                    PointSet pointSet = pointSetDatastore.get(clusterRequest.destinationPointsetId);
                    sampleSet = pointSet.getSampleSet(graph);
                }
                // Passing a null SampleSet parameter will properly return only isochrones in the RangeSet
                RepeatedRaptorProfileRouter router =
                        new RepeatedRaptorProfileRouter(graph, clusterRequest.profileRequest, sampleSet);
                router.route();
                ResultSet.RangeSet results = router.makeResults(clusterRequest.includeTimes);
                // put in constructor?
                envelope.bestCase  = results.min;
                envelope.avgCase   = results.avg;
                envelope.worstCase = results.max;
            } else {
                // No profile request, this must be a plain one to many routing request.
                RoutingRequest routingRequest = clusterRequest.routingRequest;
                // TODO finish the non-profile case
            }

            if (clusterRequest.outputQueue != null) {
                // TODO Enqueue a notification that the work is done
            }
            if (clusterRequest.outputLocation != null) {
                // Convert the result envelope and its contents to JSON and gzip it in this thread.
                // Transfer the results to Amazon S3 in another thread, piping between the two.
                try {
                    String s3key = String.join("/", clusterRequest.jobId, clusterRequest.id + ".json.gz");
                    PipedInputStream inPipe = new PipedInputStream();
                    PipedOutputStream outPipe = new PipedOutputStream(inPipe);
                    new Thread(() -> {
                        s3.putObject(clusterRequest.outputLocation, s3key, inPipe, null);
                    }).start();
                    OutputStream gzipOutputStream = new GZIPOutputStream(outPipe);
                    objectMapper.writeValue(gzipOutputStream, envelope);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        } catch (Exception ex) {
            LOG.error("An error occurred while routing: " + ex.getMessage());
            ex.printStackTrace();
        }

    }

    public List<AnalystClusterRequest> getSomeWork() {

        // Run a GET request (long-polling for work)
        String url = BROKER_BASE_URL + "/jobs/userA/graphA/jobA";
        HttpGet httpGet = new HttpGet(url);
        HttpResponse response = null;
        try {
            response = httpClient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() != 200) {
                return null;
            }
            HttpEntity entity = response.getEntity();
            if (entity == null) {
                return null;
            }
            return objectMapper.readValue(entity.getContent(), new TypeReference<List<AnalystClusterRequest>>(){});
        } catch (JsonProcessingException e) {
            LOG.error("JSON processing exception while getting work: {}", e.getMessage());
        } catch (SocketTimeoutException stex) {
            LOG.error("Socket timeout while waiting to receive work.");
        } catch (HttpHostConnectException ce) {
            LOG.error("Broker refused connection. Sleeping before retry.");
            try {
                Thread.currentThread().sleep(5000);
            } catch (InterruptedException e) { }
        } catch (IOException e) {
            LOG.error("IO exception while getting work.");
            e.printStackTrace();
        }
        return null;

    }

    /** DELETE the given message from the broker, indicating that it has been processed by a worker. */
    public boolean deleteRequest (AnalystClusterRequest clusterRequest) {
        String url = BROKER_BASE_URL + String.format("/jobs/%s/%s/%s/%s", clusterRequest.userId, clusterRequest.graphId, clusterRequest.jobId, clusterRequest.taskId);
        HttpDelete httpDelete = new HttpDelete(url);
        try {
            // TODO provide any parse errors etc. that occurred on the worker as the request body.
            HttpResponse response = httpClient.execute(httpDelete);
            // Signal the http client that we're done with this response, allowing connection reuse.
            EntityUtils.consumeQuietly(response.getEntity());
            return (response.getStatusLine().getStatusCode() == 200);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}