package org.opentripplanner.analyst.cluster;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.opentripplanner.analyst.PointSet;
import org.opentripplanner.analyst.ResultSet;
import org.opentripplanner.analyst.SampleSet;
import org.opentripplanner.profile.ProfileRequest;
import org.opentripplanner.profile.RepeatedRaptorProfileRouter;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

/**
 *
 */
public class AnalystWorker implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(AnalystWorker.class);
    public static final Random random = new Random();
    public static final int nCores = Runtime.getRuntime().availableProcessors();
    public static final String QUEUE_PREFIX = "analyst_t_job_";

    public static final int MIGRATE_PERCENTAGE = 20; // X percent of the time workers will ignore their graph affinity when polling

    ObjectMapper objectMapper;
    String lastQueueUrl = null;

    // Of course this will eventually need to be shared between multiple AnalystWorker threads.
    ClusterGraphBuilder clusterGraphBuilder;

    // Of course this will eventually need to be shared between multiple AnalystWorker threads.
    PointSetDatastore pointSetDatastore;

    // Clients for communicating with Amazon web services
    AmazonSQS sqs;
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
        sqs = new AmazonSQSClient();
        sqs.setRegion(awsRegion);
        s3 = new AmazonS3Client();
        s3.setRegion(awsRegion);

        /* The ObjectMapper (de)serializes JSON. */
        objectMapper = new ObjectMapper();
        objectMapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false); // ignore JSON fields that don't match target type

        /* These serve as lazy-loading caches for graphs and point sets. */
        clusterGraphBuilder = new ClusterGraphBuilder();
        pointSetDatastore = new PointSetDatastore(10, null, false, "analyst-dev-pointsets");

    }

    @Override
    public void run() {

        // TODO requestMetricCollector, generalProgressListener

        // Loop forever, attempting to fetch some messages from a queue and process them.
        while (true) {
            List<Message> messages = new ArrayList<>();
            try {
                // sqs.listQueues().getQueueUrls().stream().forEach(q -> LOG.info(q));

                boolean affinity = random.nextInt(100) >= MIGRATE_PERCENTAGE;
                LOG.info("This polling attempt will have graph affinity: {}", affinity);

                // Attempt to get messages from the last queue URL from which we successfully received messages.
                if (affinity && lastQueueUrl != null) {
                    int retries = 0;
                    while (messages.isEmpty() && retries++ < 2) {
                        LOG.info("Polling for more messages on the same queue {}", lastQueueUrl);
                        // Long-poll (wait a few seconds for messages to become available)
                        ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(lastQueueUrl)
                                .withVisibilityTimeout(600).withMaxNumberOfMessages(nCores).withWaitTimeSeconds(5);
                        messages = sqs.receiveMessage(receiveMessageRequest).getMessages();
                    }
                }

                // Attempt to discover a new queue, "sticking" to the last graph for which we fetched messages.
                int retries = 0;
                DISCOVER: while (messages.isEmpty()) {
                    lastQueueUrl = null;
                    // For the first two retries, discover queues for the same graph. After that, all graphs.
                    String queuePrefix = QUEUE_PREFIX;
                    if (affinity && retries++ < 2 && graphId != null) {
                        LOG.info("Polling for messages on different queues for the same graph {}", graphId);
                        queuePrefix += graphId;
                    } else {
                        LOG.info("Polling for messages on all queues across all graphs.");
                    }
                    List<String> queueUrls = sqs.listQueues(queuePrefix).getQueueUrls();
                    Collections.shuffle(queueUrls);
                    for (String queueUrl : queueUrls) {
                        // Filter out all non-work queues TODO mark work queues with a prefix
                        if (Stream.of("_output_", "_results_", "_single").anyMatch(s ->  queueUrl.contains(s))) {
                            continue;
                        }
                        LOG.debug("  {}", queueUrl);
                        // Non-blocking poll would return fast if no messages are available.
                        // However we use long polling just because it polls all servers and gives less false-empties.
                        ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(queueUrl)
                                .withVisibilityTimeout(600).withMaxNumberOfMessages(nCores * 2).withWaitTimeSeconds(1);
                        messages = sqs.receiveMessage(receiveMessageRequest).getMessages();
                        if (!messages.isEmpty()) {
                            lastQueueUrl = queueUrl;
                            break DISCOVER;
                        }
                        // Delete empty queues, as there is currently no mechanism for this.
                        // LOG.info("Deleting queue that contained no messages: {}", queueUrl);
                        // sqs.deleteQueue(queueUrl);
                    }
                    int sleepSeconds = 5;
                    LOG.info("Sleeping {} seconds before discovery of new queues.", sleepSeconds);
                    Thread.sleep(sleepSeconds * 1000); // wait a while before re-scanning the queues
                }

                // At this point we are sure to have some messages to process.
                LOG.info("Received {} messages. ", messages.size());

                // All tasks are known to require the same graph object (they came from the same queue).
                // Execute all tasks in the default ForkJoinPool with as many threads as we have processor cores.
                // This will block until all tasks have completed.
                messages.parallelStream().forEach(this::handleOneMessage);
            } catch (AmazonServiceException ase) {
                LOG.error("Error message from server: " + ase.getMessage());
                continue;
            } catch (AmazonClientException ace) {
                LOG.error("Error message from client: " + ace.getMessage());
                continue;
            } catch (InterruptedException ex) {
                LOG.warn("The thread was interrupted.");
                sqs.shutdown();
                break;
            }
        }
    }

    /**
     * AnalystClusterRequest and its subtypes are in the analyst-server repo, which depends on OTP.
     * To avoid circular dependencies, for now we will read the fields of these incoming objects from a tree
     * rather than binding to classes.
     */
    private void handleOneMessage(Message message) {
        try {
            LOG.info("Handling message {}", message.getBody());
            // Parse / bind the cluster request into the shared superclass so we can decide which request subclass
            // we actually want to bind it to.
            // TODO there has to be a better way to do this. Maybe by having separate profileRequest and request fields
            // inside AnalystClusterReqeust
            AnalystClusterRequest clusterRequest = objectMapper.readValue(message.getBody(), AnalystClusterRequest.class);

            // Get the graph object for the ID given in the request, fetching inputs and building as needed.
            Graph graph = clusterGraphBuilder.getGraph(clusterRequest.graphId);
            graphId = clusterRequest.graphId; // Record graphId so we "stick" to this same graph on subsequent polls

            // This result envelope will hold the result of the profile or single-time one-to-many search.
            ResultEnvelope envelope = new ResultEnvelope();
            if (clusterRequest.profile) {
                // TODO check graph and job ID against queue URL for coherency
                ProfileRequest profileRequest = objectMapper.readValue(message.getBody(), ProfileRequest.class);
                SampleSet sampleSet = null;
                if (clusterRequest.destinationPointsetId != null) {
                    // A pointset was specified, calculate travel times to the points in the pointset.
                    // Fetch the set of points we will use as destinations for this one-to-many search
                    PointSet pointSet = pointSetDatastore.get(clusterRequest.destinationPointsetId);
                    sampleSet = pointSet.getSampleSet(graph);
                }
                // Passing a null SampleSet parameter will properly return only isochrones in the RangeSet
                RepeatedRaptorProfileRouter router = new RepeatedRaptorProfileRouter(graph, profileRequest, sampleSet);
                router.route();
                ResultSet.RangeSet results = router.makeResults(clusterRequest.includeTimes);
                // put in constructor?
                envelope.bestCase  = results.min;
                envelope.avgCase   = results.avg;
                envelope.worstCase = results.max;
            } else {
                RoutingRequest routingRequest = objectMapper.readValue(message.getBody(), RoutingRequest.class);
                // TODO finish the non-profile case
            }

            if (clusterRequest.outputQueue != null) {
                // Enqueue a notification that the work is done
                sqs.sendMessage(clusterRequest.outputQueue, String.format("{ \"jobId\" : \"%s\", \"id\" : \"%s\" }",
                        clusterRequest.jobId, clusterRequest.id));
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

        } catch (JsonProcessingException e) {
            LOG.error("JSON processing exception while parsing incoming message: {}", e);
            LOG.error("Deleting this message as it will likely confuse all workers that attempt to read it.");
            e.printStackTrace();
        } catch (IOException e) {
            LOG.error("IO exception while parsing incoming message: {}", e);
            LOG.error("Leaving this message alive for later consumption by another worker.");
            e.printStackTrace();
            return; // to skip message deletion below.
        } catch (Exception ex) {
            LOG.error("An error occurred while routing: " + ex.getMessage());
            ex.printStackTrace();
        }

        // Remove messages from queue so they won't be re-delivered to other workers.
        LOG.info("Removing message: {}", message.getBody());
        sqs.deleteMessage(lastQueueUrl, message.getReceiptHandle());

    }

}