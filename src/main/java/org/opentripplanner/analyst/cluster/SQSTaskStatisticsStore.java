package org.opentripplanner.analyst.cluster;

import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClient;
import com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Task Statistics Store that throws things into SQS.
 */
public class SQSTaskStatisticsStore implements TaskStatisticsStore {
    private static final Logger LOG = LoggerFactory.getLogger(SQSTaskStatisticsStore.class);

    private String queueUrl;

    private ObjectMapper objectMapper = new ObjectMapper();

    private AmazonSQSAsync sqs = new AmazonSQSBufferedAsyncClient(new AmazonSQSAsyncClient());

    /** create a task statistics store for the given queue name */
    public SQSTaskStatisticsStore(String queueName) {
        queueUrl = sqs.getQueueUrl(queueName).getQueueUrl();
        LOG.info("Sending statistics to SQS queue {}", queueName);
    }

    public void store (TaskStatistics ts) {
        try {
            String json = objectMapper.writeValueAsString(ts);

            SendMessageRequest req = new SendMessageRequest();
            req.setMessageBody(json);
            req.setQueueUrl(queueUrl);

            sqs.sendMessageAsync(req, new AsyncHandler<SendMessageRequest, SendMessageResult>() {
                @Override public void onError(Exception e) {
                    LOG.error("Error saving stats to SQS", e);
                }

                @Override public void onSuccess(SendMessageRequest request,
                        SendMessageResult sendMessageResult) {
                    /* do nothing */
                }
            });
        } catch (Exception e) {
            LOG.error("Error saving stats to SQS", e);
        }
    }
}
