package org.opentripplanner.analyst.broker;

import com.fasterxml.jackson.databind.ObjectMapper;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.opentripplanner.analyst.cluster.AnalystClusterRequest;
import org.opentripplanner.api.model.AgencyAndIdSerializer;
import org.opentripplanner.api.model.QualifiedModeSetSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * This class tracks incoming requests from workers to consume Analyst tasks, and attempts to match those
 * requests to enqueued tasks. It aims to draw tasks fairly from all users, and fairly from all jobs within each user,
 * while attempting to respect the graph affinity of each worker (give it tasks that require the same graph it has been
 * working on recently).
 *
 * When no work is available or no workers are available, the polling functions return immediately, avoiding spin-wait.
 * When they are receiving no work, workers are expected to disconnect and re-poll occasionally, on the order of 30
 * seconds. This serves as a signal to the broker that they are still alive and waiting.
 *
 * TODO if there is a backlog of work (the usual case when jobs are lined up) workers will constantly change graphs.
 * Because (at least currently) two users never share the same graph, we can get by with pulling tasks cyclically or
 * randomly from all the jobs, and just actively shaping the number of workers with affinity for each graph by forcing
 * some of them to accept tasks on graphs other than the one they have declared afffinity for.
 *
 * This could be thought of as "affinity homeostasis". We  will constantly keep track of the ideal proportion of workers
 * by graph (based on active queues), and the true proportion of consumers by graph (based on incoming requests) then
 * we can decide when a worker's graph affinity should be ignored and what it should be forced to.
 *
 * It may also be helpful to mark jobs every time they are skipped in the LRU queue. Each time a job is serviced,
 * it is taken out of the queue and put at its end. Jobs that have not been serviced float to the top.
 */
public class Broker implements Runnable {

    // TODO catalog of recently seen consumers by affinity with IP: response.getRequest().getRemoteAddr();

    private static final Logger LOG = LoggerFactory.getLogger(Broker.class);

    public final CircularList<Job> jobs = new CircularList<>();

    private int nUndeliveredTasks = 0; // Including normal priority jobs and high-priority tasks.

    private int nWaitingConsumers = 0; // including some that might be closed

    private int nextTaskId = 0;

    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.registerModule(AgencyAndIdSerializer.makeModule());
        mapper.registerModule(QualifiedModeSetSerializer.makeModule());
    }

    /** The messages that have already been delivered to a worker. */
    TIntObjectMap<AnalystClusterRequest> deliveredTasks = new TIntObjectHashMap<>();

    /** The time at which each task was delivered to a worker, to allow re-delivery. */
    TIntIntMap deliveryTimes = new TIntIntHashMap();

    /** Requests that are not part of a job and can "cut in line" in front of jobs for immediate execution. */
    private Queue<AnalystClusterRequest> highPriorityTasks = new ArrayDeque<>();

    /** Priority requests that have already been farmed out to workers, and are awaiting a response. */
    private TIntObjectMap<Response> highPriorityResponses = new TIntObjectHashMap<>();

    /** Outstanding requests from workers for tasks, grouped by worker graph affinity. */
    Map<String, Deque<Response>> consumersByGraph = new HashMap<>();

    // Queue of tasks to complete Delete, Enqueue etc. to avoid synchronizing all the functions ?

    /**
     * Enqueue a task for execution ASAP, planning to return the response over the same HTTP connection.
     * Low-reliability, no re-delivery.
     */
    public synchronized void enqueuePriorityTask (AnalystClusterRequest task, Response response) {
        task.taskId = nextTaskId++;
        highPriorityTasks.add(task);
        highPriorityResponses.put(task.taskId, response);
        nUndeliveredTasks += 1;
        notify();
    }

    /** Enqueue some tasks for queued execution possibly much later. Results will be saved to S3. */
    public synchronized void enqueueTasks (List<AnalystClusterRequest> tasks) {
        Job job = findJob(tasks.get(0)); // creates one if it doesn't exist
        for (AnalystClusterRequest task : tasks) {
            task.taskId = nextTaskId++;
            job.addTask(task);
            nUndeliveredTasks += 1;
            LOG.debug("Enqueued task id {} in job {}", task.taskId, job.jobId);
            if ( ! task.graphId.equals(job.graphId)) {
                LOG.warn("Task graph ID {} does not match job graph ID {}.", task.graphId, job.graphId);
            }
        }
        // Wake up the delivery thread if it's waiting on input.
        // This wakes whatever thread called wait() while holding the monitor for this Broker object.
        notify();
    }

    /** Consumer long-poll operations are enqueued here. */
    public synchronized void registerSuspendedResponse(String graphId, Response response) {
        // The workers are not allowed to request a specific job or task, just a specific graph and queue type.
        Deque<Response> deque = consumersByGraph.get(graphId);
        if (deque == null) {
            deque = new ArrayDeque<>();
            consumersByGraph.put(graphId, deque);
        }
        deque.addLast(response);
        nWaitingConsumers += 1;
        // Wake up the delivery thread if it's waiting on consumers.
        // This is whatever thread called wait() while holding the monitor for this QBroker object.
        notify();
    }

    /** When we notice that a long poll connection has closed, we remove it here. */
    public synchronized boolean removeSuspendedResponse(String graphId, Response response) {
        Deque<Response> deque = consumersByGraph.get(graphId);
        if (deque == null) {
            return false;
        }
        if (deque.remove(response)) {
            nWaitingConsumers -= 1;
            LOG.debug("Removed closed connection from queue.");
            logQueueStatus();
            return true;
        }
        return false;
    }

    private void logQueueStatus() {
        LOG.info("{} undelivered, of which {} high-priority", nUndeliveredTasks, highPriorityTasks.size());
        LOG.info("{} producers waiting, {} consumers waiting", highPriorityResponses.size(), nWaitingConsumers);
    }

    /**
     * This method checks whether there are any high-priority tasks or normal job tasks and attempts to match them with
     * waiting workers. It blocks until there are tasks or workers available.
     */
    public synchronized void deliverTasks() throws InterruptedException {

        // Wait until there are some undelivered tasks.
        while (nUndeliveredTasks == 0) {
            LOG.debug("Task delivery thread is going to sleep, there are no tasks waiting for delivery.");
            logQueueStatus();
            wait();
        }
        LOG.debug("Task delivery thread is awake and there are some undelivered tasks.");
        logQueueStatus();

        // A reference to the job that will be drawn from in this iteration.
        Job job;

        // Service all high-priority tasks before handling any normal priority jobs.
        // These tasks are wrapped in a trivial Job so we can re-use delivery code in both high and low priority cases.
        if (highPriorityTasks.size() > 0) {
            AnalystClusterRequest task = highPriorityTasks.remove();
            job = new Job("HIGH PRIORITY");
            job.graphId = task.graphId;
            job.addTask(task);
        } else {
            // Circular lists retain iteration state via their head pointers.
            // We know we will find a task here because nUndeliveredTasks > 0.
            job = jobs.advanceToElement(e -> e.visibleTasks.size() > 0);
        }

        // We have found job with some undelivered tasks. Give them to a consumer,
        // waiting until one is available, possibly defying graph affinity.
        LOG.debug("Task delivery thread has found undelivered tasks in job {}.", job.jobId);
        while (true) {

            while (nWaitingConsumers == 0) {
                LOG.debug("Task delivery thread is going to sleep, there are no consumers waiting.");
                // Thread will be notified when there are new incoming consumer connections.
                wait();
            }
            LOG.debug("Task delivery thread is awake, and some consumers are waiting.");
            logQueueStatus();

            // Here, we know there are some consumer connections waiting, but we're not sure they're still open.
            // First try to get a consumer with affinity for this graph
            LOG.debug("Looking for an eligible consumer, respecting graph affinity.");
            Deque<Response> deque = consumersByGraph.get(job.graphId);
            while (deque != null && !deque.isEmpty()) {
                Response response = deque.pop();
                nWaitingConsumers -= 1;
                if (deliver(job, response)) {
                    return;
                }
            }

            // Then try to get a consumer from the graph with the most workers
            LOG.debug("No consumers with the right affinity. Looking for any consumer.");
            List<Deque<Response>> deques = new ArrayList<>(consumersByGraph.values());
            deques.sort((d1, d2) -> Integer.compare(d2.size(), d1.size()));
            for (Deque<Response> d : deques) {
                while (!d.isEmpty()) {
                    Response response = d.pop();
                    nWaitingConsumers -= 1;
                    if (deliver(job, response)) {
                        return;
                    }
                }
            }

            // No workers were available to accept the tasks.
            // Loop back, waiting for a consumer for the tasks in this job (thread should wait on the next iteration).
            LOG.debug("No consumer was available. They all must have closed their connections.");
            if (nWaitingConsumers != 0) {
                throw new AssertionError("There should be no waiting consumers here, something is wrong.");
            }

        }

    }

    /**
     * Attempt to hand some tasks from the given job to a waiting consumer connection.
     * The write will fail if the consumer has closed the connection but it hasn't been removed from the connection
     * queue yet. This can happen because the Broker methods are synchronized, and the removal action may be waiting
     * to get the monitor while we are trying to distribute tasks here.
     * @return whether the handoff succeeded.
     */
    public synchronized boolean deliver (Job job, Response response) {

        // Check up-front whether the connection is still open.
        if (!response.getRequest().getRequest().getConnection().isOpen()) {
            LOG.debug("Consumer connection was closed. It will be removed.");
            return false;
        }

        // Get up to N tasks from the visibleTasks deque
        List<AnalystClusterRequest> tasks = new ArrayList<>();
        while (tasks.size() < 4 && !job.visibleTasks.isEmpty()) {
            tasks.add(job.visibleTasks.poll());
        }

        // Attempt to deliver the tasks to the given consumer.
        try {
            response.setStatus(HttpStatus.OK_200);
            OutputStream out = response.getOutputStream();
            mapper.writeValue(out, tasks);
            response.resume();
        } catch (IOException e) {
            // The connection was probably closed by the consumer, but treat it as a server error.
            LOG.debug("Consumer connection caused IO error, it will be removed.");
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
            response.resume();
            // Delivery failed, put tasks back on (the end of) the queue.
            job.visibleTasks.addAll(tasks);
            return false;
        }

        // Delivery succeeded, move tasks from undelivered to delivered status
        LOG.debug("Delivery of {} tasks succeeded.", tasks.size());
        nUndeliveredTasks -= tasks.size();
        job.markTasksDelivered(tasks);
        return true;

    }

    /**
     * Take a normal (non-priority) task out of a job queue, marking it as completed so it will not be re-delivered.
     * @return whether the task was found and removed.
     */
    public synchronized boolean deleteJobTask (int taskId) {
        // There could be thousands of invisible (delivered) tasks, so we use a hash map.
        // We only allow removal of delivered, invisible tasks for now (not undelivered tasks).
        // Return whether removal call discovered an existing task.
        return deliveredTasks.remove(taskId) != null;
    }

    /**
     * Marks the specified priority request as completed, and returns the suspended Response object for the connection
     * that submitted the priority request (the UI), which probably still waiting to receive a result back over the
     * same connection. A HttpHandler thread can then pump data from the DELETE body back to the origin of the request,
     * without blocking the broker thread.
     * TODO rename to "deregisterSuspendedProducer" and "deregisterSuspendedConsumer" ?
     */
    public synchronized Response deletePriorityTask (int taskId) {
        return highPriorityResponses.remove(taskId);
    }

    // TODO: occasionally purge closed connections from consumersByGraph
    // TODO: worker catalog and graph affinity homeostasis

    @Override
    public void run() {
        while (true) {
            try {
                deliverTasks();
            } catch (InterruptedException e) {
                LOG.warn("Task pump thread was interrupted.");
                return;
            }
        }
    }

    public Job findJob (AnalystClusterRequest task) {
        for (Job job : jobs) {
            if (job.jobId.equals(task.jobId)) {
                return job;
            }
        }
        Job job = new Job(task.jobId);
        job.graphId = task.graphId;
        jobs.insertAtTail(job);
        return job;
    }

}
