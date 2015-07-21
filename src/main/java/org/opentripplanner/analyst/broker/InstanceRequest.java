package org.opentripplanner.analyst.broker;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeSpotInstanceRequestsRequest;
import com.amazonaws.services.ec2.model.DescribeSpotInstanceRequestsResult;
import com.amazonaws.services.ec2.model.IamInstanceProfileSpecification;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.LaunchSpecification;
import com.amazonaws.services.ec2.model.RequestSpotInstancesRequest;
import com.amazonaws.services.ec2.model.RequestSpotInstancesResult;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.ShutdownBehavior;
import com.amazonaws.services.ec2.model.SpotInstanceRequest;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Properties;

/**
 * Represents a single outstanding request for AWS instances that will be used as Transport Analyst workers.
 */
public class InstanceRequest implements Runnable {

    private static AmazonEC2 ec2 = new AmazonEC2Client();

    // TODO: compute desired number of workers. For now, create one worker at a time.
    int nWorkers = 1;

    /* The time in milliseconds since the epoch when we last requested some instances. */
    long requestTime = -1;

    // SpotInstanceRequest tracks the state of an outstanding request on EC2. We are requesting one of these requests.
    RequestSpotInstancesRequest spotReqReq;

    LaunchSpecification launchSpec;

    /* If this is non-null, we have given up on spot instances and are requesting on-demand instances. */
    RunInstancesRequest onDemandReq = null;

    public InstanceRequest (Properties workerConfig, String graphId, int nWorkers) {

        // Threadsafe for now because this is always called from a synchronized method, but we really should copy.
        workerConfig.setProperty("initial-graph-id", graphId);

        // Create a spot instance request.
        spotReqReq = new RequestSpotInstancesRequest();
        spotReqReq.setSpotPrice("0.50");
        spotReqReq.setInstanceCount(nWorkers);

        // Specify further details about the instances we are requesting.
        launchSpec = new LaunchSpecification();
        launchSpec.setImageId(workerConfig.getProperty("ami-id"));
        launchSpec.setInstanceType(InstanceType.valueOf(workerConfig.getProperty("worker-type")));
        launchSpec.setSubnetId(workerConfig.getProperty("subnet-id"));
        launchSpec.setUserData(propertiesToBase64(workerConfig));
        String iamRole = workerConfig.getProperty("worker-iam-role");
        if (iamRole != null) {
            launchSpec.setIamInstanceProfile(new IamInstanceProfileSpecification().withArn(iamRole));
        }
        String subnet = workerConfig.getProperty("subnet"); // Which Virtual Private Cloud, if any.
        if (subnet != null) {
            launchSpec.setSubnetId(subnet);
        }
        spotReqReq.setLaunchSpecification(launchSpec);

    }

    /**
     * Running an InstanceRequest will first request spot instances, then if that fails it will move on to request
     * on-demand instances.
     */
    @Override
    public void run () {
        RequestSpotInstancesResult spotResult = ec2.requestSpotInstances(spotReqReq);
        // Result should contain N SpotInstanceRequests if we requested N instances.
        List<String> spotInstanceRequestIds = new ArrayList<>();
        for (SpotInstanceRequest req : spotResult.getSpotInstanceRequests()) {
            spotInstanceRequestIds.add(req.getInstanceId());
        }
        int nActive;
        while (true) {
            DescribeSpotInstanceRequestsRequest describeRequest = new DescribeSpotInstanceRequestsRequest();
            describeRequest.setSpotInstanceRequestIds(spotInstanceRequestIds);
            boolean anyRequestsOpen = false;
            nActive = 0;
            try {
                DescribeSpotInstanceRequestsResult describeResult = ec2.describeSpotInstanceRequests(describeRequest);
                for (SpotInstanceRequest describeResponse : describeResult.getSpotInstanceRequests()) {
                    if (describeResponse.getState().equals("open")) {
                        anyRequestsOpen = true;
                    } else if (describeResponse.getState().equals("active")) {
                        nActive += 1;
                    }
                }
            } catch (AmazonServiceException e) {
                // Retry on exception.
                anyRequestsOpen = true;
            }
            if (anyRequestsOpen) {
                try {
                    Thread.sleep(60 * 1000);
                } catch (Exception e) {
                    // Retry, sleep was awakened early.
                }
            } else {
                break;
            }
        }
        // At this point no spot requests are open. If we acquired enough instances, we are done.
        nWorkers = nWorkers - nActive;
        if (nWorkers == 0) {
            return;
        }
        // Attempt to get on-demand instances for the balance of needed instances.
        onDemand();
        RunInstancesResult res = ec2.runInstances(onDemandReq);
        res.getReservation().getInstances();
    }

    private void onDemand() {

        // On-demand instance request
        onDemandReq = new RunInstancesRequest();
        // even if we can't get all the workers we want at least get some
        onDemandReq.setMinCount(1);
        onDemandReq.setMaxCount(nWorkers);
        onDemandReq.setUserData(launchSpec.getUserData());
        // Run instances requests do not use LaunchSpec.
        // They directly contain some information that spot requests contain indirectly via LaunchSpec.
        onDemandReq.setImageId(launchSpec.getImageId());
        onDemandReq.setInstanceType(launchSpec.getInstanceType());
        onDemandReq.setSubnetId(launchSpec.getSubnetId());
        onDemandReq.setIamInstanceProfile(launchSpec.getIamInstanceProfile());
        onDemandReq.setSubnetId(launchSpec.getSubnetId());
        // Allow machine to shut itself completely off. This option does not seem to exist in spot instances.
        onDemandReq.setInstanceInitiatedShutdownBehavior(ShutdownBehavior.Terminate);

        // allow us to retry request at will
        // req.setClientToken(clientToken);

    }

    private static String propertiesToBase64 (Properties workerConfig) {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try {
            workerConfig.store(byteStream, "Analyst Worker Config");
            byteStream.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return new String(Base64.getEncoder().encode(byteStream.toByteArray()));
    }

}
