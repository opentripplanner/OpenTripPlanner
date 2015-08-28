package org.opentripplanner.analyst.cluster;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.opentripplanner.api.model.AgencyAndIdSerializer;
import org.opentripplanner.api.model.JodaLocalDateSerializer;
import org.opentripplanner.api.model.QualifiedModeSetSerializer;
import org.opentripplanner.api.model.TraverseModeSetSerializer;
import org.opentripplanner.profile.ProfileRequest;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * Enqueues a bunch of messages to simulate an Analyst Cluster job
 *
 * $ aws s3 ls analyst-demo-graphs
 * $ aws sqs list-queues
 */
public class JobSimulator {

    public String s3prefix = "S3PREFIX";
    public String pointSetId = "census";
    public String graphId = "c4aa8cc8666788c8d51d4fc99201fa56";
    public int nOrigins = 4;

    DefaultHttpClient httpClient = new DefaultHttpClient();

    public static void main(String[] args) {

        JobSimulator js = new JobSimulator();
//        js.s3prefix = args[0];
//        js.pointSetId = args[1];
//        js.graphId = args[2];
//        js.nOrigins = Integer.parseInt(args[3]);
        js.sendFakeJob();

    }

    public void sendFakeJob() {

        String jobId = compactUUID();

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.registerModule(AgencyAndIdSerializer.makeModule());
        mapper.registerModule(QualifiedModeSetSerializer.makeModule());
        mapper.registerModule(JodaLocalDateSerializer.makeModule());
        mapper.registerModule(TraverseModeSetSerializer.makeModule());

        List<AnalystClusterRequest> requests = new ArrayList<>();

        IntStream.range(0, nOrigins).forEach(i -> {
            // Enqueue one fake origin
            ProfileRequest profileRequest = new ProfileRequest();
            profileRequest.fromLat = 45.515;
            profileRequest.fromLon = -122.643;
            profileRequest.transitModes = null; //new TraverseModeSet(TraverseMode.TRANSIT);
            // profileRequest.accessModes ...
            AnalystClusterRequest clusterRequest = new AnalystClusterRequest(pointSetId, graphId, profileRequest);
            clusterRequest.jobId = jobId;
            clusterRequest.id = Integer.toString(i);
            clusterRequest.outputLocation = s3prefix + "_output";
            clusterRequest.destinationPointsetId = pointSetId;
            requests.add(clusterRequest);
        });

//        try {
//            objectMapper.writeValue(System.out, requests);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }

        String url = String.format("http://localhost:9001/enqueue/jobs");
        HttpPost httpPost = new HttpPost(url);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            mapper.writeValue(out, requests);
            // System.out.println(out.toString());
            httpPost.setEntity(new ByteArrayEntity(out.toByteArray()));
            HttpResponse response = httpClient.execute(httpPost);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

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
