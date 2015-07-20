package org.opentripplanner.analyst.cluster;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * A queue of worker tasks that refills automatically
 */
public class WorkerTaskDeque extends LinkedBlockingDeque<AnalystClusterRequest> {
    private static Logger LOG = LoggerFactory.getLogger(WorkerTaskDeque.class);

    private AnalystWorker parent;

    private int minSize;

    private boolean sideChannelOpen = false;

    public WorkerTaskDeque (AnalystWorker worker, int minSize) {
        parent = worker;
        this.minSize = minSize;

        // populate
        getSomeWork();

        // when the queue gets too small, we call notify and this thread wakes up
        new Thread(() -> {
            synchronized (this) {
                // poll forever
                while (true) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                    }

                    if (this.size() < minSize)
                        getSomeWork();
                }
            }
        }).start();
    }

    public AnalystClusterRequest removeFirst () {
        if (this.size() < minSize) {
            synchronized (this) {
                notify();
            }
        }

        return super.removeFirst();
    }

    public AnalystClusterRequest removeLast () {
        if (this.size() < minSize) {
            synchronized (this) {
                notify();
            }
        }

        return super.removeLast();
    }

    private boolean getSomeWork() {
        // Run a POST request (long-polling for work) indicating which graph this worker prefers to work on
        String url = parent.BROKER_BASE_URL + "/dequeue/" + parent.graphId;
        HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader(new BasicHeader(AnalystWorker.WORKER_ID_HEADER, parent.machineId));
        HttpResponse response = null;
        try {
            response = parent.httpClient.execute(httpPost);
            HttpEntity entity = response.getEntity();
            if (entity == null) {
                try {
                    Thread.currentThread().sleep(5000);
                } catch (InterruptedException e) {
                }
                return false;
            }
            if (response.getStatusLine().getStatusCode() != 200) {
                EntityUtils.consumeQuietly(entity);
                try {
                    Thread.currentThread().sleep(5000);
                } catch (InterruptedException e) {
                }
                return false;
            }

            List<AnalystClusterRequest> requests = parent.objectMapper.readValue(entity.getContent(), new TypeReference<List<AnalystClusterRequest>>() {});
            requests.stream().forEach(r -> {
                if (r.outputLocation == null)
                    // high priority
                    this.addFirst(r);

                else
                    this.addLast(r);
            });

            // if there are single point requests among them, open a side channel to receive any future single point requests
            if (requests.stream().filter(r -> r.outputLocation == null).findFirst().isPresent() && !sideChannelOpen)
                openSideChannel();

        } catch (JsonProcessingException e) {
            LOG.error("JSON processing exception while getting work: {}. Sleeping before retry.", e.getMessage());
            try {
                Thread.currentThread().sleep(5000);
            } catch (InterruptedException e1) {
            }
        } catch (SocketTimeoutException stex) {
            LOG.error("Socket timeout while waiting to receive work.");
            // do not sleep as we've already waited a long time on the socket
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
        return false;
    }

    /** open a side channel connection to the broker to receive single point requests */
    private void openSideChannel () {
        sideChannelOpen = true;

        // the side channel has its own thread
        new Thread(() -> {
            String url = parent.BROKER_BASE_URL + "/single/" + parent.graphId;
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader(new BasicHeader(AnalystWorker.WORKER_ID_HEADER, parent.machineId));
            HttpResponse response = null;
            try {
                response = parent.httpClient.execute(httpPost);
                HttpEntity entity = response.getEntity();
                if (entity == null) {
                    try {
                        Thread.currentThread().sleep(5000);
                    } catch (InterruptedException e) {
                    }
                    return;
                }
                if (response.getStatusLine().getStatusCode() != 200) {
                    EntityUtils.consumeQuietly(entity);
                    try {
                        Thread.currentThread().sleep(5000);
                    } catch (InterruptedException e) {
                    }
                    return;
                }

                List<AnalystClusterRequest> requests = parent.objectMapper
                        .readValue(entity.getContent(),
                                new TypeReference<List<AnalystClusterRequest>>() {
                                });

                // add them at the head of the queue
                requests.stream().forEach(this::addFirst);
            } catch (JsonProcessingException e) {
                LOG.error("JSON processing exception while getting work: {}. Sleeping before retry.",
                        e);
                try {
                    // short sleep just to prevent accidentally DoSing a server.
                    Thread.currentThread().sleep(500);
                } catch (InterruptedException e1) {
                }
            } catch (SocketTimeoutException stex) {
                LOG.error("Socket timeout while waiting to receive work.");
                // do not sleep as we've already waited a long time on the socket
            } catch (HttpHostConnectException ce) {
                LOG.error("Broker refused connection. Sleeping before retry.");
                try {
                    Thread.currentThread().sleep(5000);
                } catch (InterruptedException e) {
                }
            } catch (IOException e) {
                LOG.error("IO exception while getting work.", e);
            } catch (Exception e) {
                LOG.error("Unexpected exception retrieving side-channel work", e);
            }

            // open another side channel to replace this one.
            // NB: this does not cause a problem with tail recursion as we're not inside the
            // function anymore but rather in a thread.
            openSideChannel();
        }).start();
    }
}
