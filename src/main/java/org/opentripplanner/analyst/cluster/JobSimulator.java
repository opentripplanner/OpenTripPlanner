package org.opentripplanner.analyst.cluster;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.opentripplanner.profile.ProfileRequest;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * Enqueues a bunch of messages to simulate an Analyst Cluster job
 *
 * $ aws s3 ls analyst-demo-graphs
 * $ aws sqs list-queues
 */
public class JobSimulator {

    public static void main(String[] args) {

        String prefix = args[0];
        String pointSetId = args[1];
        String graphId = pointSetId.split("_")[0];
        int nOrigins = Integer.parseInt(args[2]);

        String jobId = compactUUID();

        // When creating the S3 and SQS clients use the default credentials chain.
        // This will check environment variables and ~/.aws/credentials first, then fall back on
        // the auto-assigned IAM role if this code is running on an EC2 instance.
        // http://docs.aws.amazon.com/AWSSdkDocsJava/latest/DeveloperGuide/java-dg-roles.html
        AmazonSQS sqs = new AmazonSQSClient();
        Region awsRegion = Region.getRegion(Regions.US_EAST_1);
        sqs.setRegion(awsRegion);

        // Create a queue for this job
        String taskQueueUrl = sqs.createQueue(String.join("_", prefix, "job", graphId, jobId)).getQueueUrl();
        String resultQueueUrl = sqs.createQueue(String.join("_", prefix, "job", graphId, jobId)).getQueueUrl();

        ObjectMapper objectMapper = new ObjectMapper();
        IntStream.range(0, nOrigins).forEach(i -> {
            // Enqueue one fake origin
            ProfileRequest profileRequest = new ProfileRequest();
            AnalystClusterRequest clusterRequest = new OneToManyProfileRequest(pointSetId, profileRequest, graphId);
            clusterRequest.id = Integer.toString(i);
            clusterRequest.jobId = jobId;
            clusterRequest.outputLocation = prefix + "_output";
            clusterRequest.outputQueue = resultQueueUrl;
            clusterRequest.destinationPointsetId = pointSetId;
            try {
                sqs.sendMessage(taskQueueUrl, objectMapper.writeValueAsString(clusterRequest));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        });

    }

    public static String compactUUID() {
        UUID uuid = UUID.randomUUID();
        byte[] bytes = new byte[16];
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        byteBuffer.putLong(uuid.getMostSignificantBits());
        byteBuffer.putLong(uuid.getLeastSignificantBits());

        String base64 = Base64.getUrlEncoder().encodeToString(bytes);
        base64 = base64.substring(0, base64.length() - 2); // may contain underscores!
        String hex = uuid.toString().replaceAll("-", "");

//        System.out.println("base64 " + base64);
//        System.out.println("hex    " + hex);

        return hex;
    }

}
