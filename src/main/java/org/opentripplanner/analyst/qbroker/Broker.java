package org.opentripplanner.analyst.qbroker;

import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class watches for incoming requests for work tasks, and attempts to match them to enqueued tasks.
 * It draws tasks fairly from all users, and fairly from all jobs within each user, while attempting to respect the
 * cache affinity of each worker (give it tasks on the same graph it has been working on recently).
 *
 * When no work is available, the polling functions return immediately. Workers are expected to sleep and re-poll
 * after a few tens of seconds.
 *
 * TODO if there is a backlog of work (the usual case when jobs are lined up) workers will constantly change graphs
 * We need a queue of deferred work: (job, timestamp) when a job would have fairly had its work consumed  if a worker was available.
 * Anything that survives at the head of that queue for more than e.g. one minute gets forced on a non-affinity worker.
 * Any new workers without an affinity preferentially pull work off the deferred queue.
 * Polling worker connections scan the deferred queue before ever going to the main circular queue.
 * When the deferred queue exceeds a certain size, that's when we must start more workers.
 *
 * We should distinguish between two cases:
 * 1. we were waiting for work and woke up because work became available.
 * 2. we were waiting for a consumer and woke up when one arrived.
 *
 * The first case implies that many workers should migrate toward the new work.
 *
 * Two key ideas are:
 * 1. Least recently serviced queue of jobs
 * 2. Affinity Homeostasis
 *
 * If we can constantly keep track of the ideal proportion of workers by graph (based on active queues),
 * and the true proportion of consumers by graph (based on incoming requests) then we can decide when a worker's graph
 * affinity should be ignored.
 *
 * It may also be helpful to mark jobs every time they are skipped in the LRU queue. Each time a job is serviced,
 * it is taken out of the queue and put at its end. Jobs that have not been serviced float to the top.
 */
public class Broker implements Runnable {

    // TODO catalog of recently seen consumers by affinity with IP: response.getRequest().getRemoteAddr();

    private static final Logger LOG = LoggerFactory.getLogger(Broker.class);

    private CircularList<User> users = new CircularList<>();

    private int nUndeliveredTasks = 0;

    private int nWaitingConsumers = 0; // including some that might be closed

    /** Outstanding requests from workers for tasks, grouped by worker graph affinity. */
    Map<String, Deque<Response>> connectionsForGraph = new HashMap<>();

    // Queue of tasks to complete Delete, Enqueue etc. to avoid synchronizing all the functions

    public synchronized void enqueueTasks (QueuePath queuePath, Collection<String> taskBodies) {
        User user = findUser(queuePath.userId, true);
        Job job = user.findJob(queuePath.jobId, true);
        if (job.graphId == null) {
            // New job, set its graph ID
            job.graphId = queuePath.graphId;
        } else {
            // Existing job, check its graph ID
            if (!job.graphId.equals(queuePath.graphId)) {
                LOG.warn("Job associated with a different graphId");
            }
        }
        LOG.debug("Queue {}", queuePath);
        for (String taskBody : taskBodies) {
            int taskId = job.addTask(taskBody);
            nUndeliveredTasks += 1;
            LOG.debug("Enqueued task id {} with body {}", taskId, taskBody);
        }
        // Wake up the delivery thread if it's waiting on input.
        // This is whatever thread called wait() while holding the monitor for this QBroker object.
        notify();
    }

    /** Long poll operations are enqueued here. */
    public synchronized void registerSuspendedResponse(String graphId, Response response) {
        // The workers are not allowed to request a specific job or task, just a specific graph and queue type.
        Deque<Response> deque = connectionsForGraph.get(graphId);
        if (deque == null) {
            deque = new ArrayDeque<>();
            connectionsForGraph.put(graphId, deque);
        }
        deque.addLast(response);
        nWaitingConsumers += 1;
        // Wake up the delivery thread if it's waiting on consumers.
        // This is whatever thread called wait() while holding the monitor for this QBroker object.
        notify();
    }

    /** When we notice that a long poll connection has closed, we remove it here. */
    public synchronized boolean removeSuspendedResponse(String graphId, Response response) {
        Deque<Response> deque = connectionsForGraph.get(graphId);
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
        LOG.info("Status {} undelivered, {} consumers waiting.", nUndeliveredTasks, nWaitingConsumers);
    }

    /**
     * Pull the next job queue with undelivered work fairly from users and jobs.
     * Pass some of that work to a worker, blocking if necessary until there are workers available.
     */
    public synchronized void deliverTasksForOneJob () throws InterruptedException {

        // Wait until there are some undelivered tasks.
        while (nUndeliveredTasks == 0) {
            LOG.debug("Task delivery thread is going to sleep, there are no tasks waiting for delivery.");
            logQueueStatus();
            wait();
        }
        LOG.debug("Task delivery thread is awake and there are some undelivered tasks.");
        logQueueStatus();

        // Circular lists retain iteration state via their head pointers.
        Job job = null;
        while (job == null) {
            User user = users.advance();
            if (user == null) {
                LOG.error("There should always be at least one user here, because there is an undelivered task.");
            }
            job = user.jobs.advanceToElement(e -> e.visibleTasks.size() > 0);
        }

        // We have found job with some undelivered tasks. Give them to a consumer,
        // waiting until one is available even if this means ignoring graph affinity.
        LOG.debug("Task delivery thread has found undelivered tasks in job {}.", job.jobId);
        while (true) {
            while (nWaitingConsumers == 0) {
                LOG.debug("Task delivery thread is going to sleep, there are no consumers waiting.");
                // Thread will be notified when there are new incoming consumer connections.
                wait();
            }
            LOG.debug("Task delivery thread is awake, and some consumers are waiting.");
            logQueueStatus();

            // Here, we know there are some consumer connections waiting, but we don't know if they're still open.
            // First try to get a consumer with affinity for this graph
            LOG.debug("Looking for an eligible consumer, respecting graph affinity.");
            Deque<Response> deque = connectionsForGraph.get(job.graphId);
            while (deque != null && !deque.isEmpty()) {
                Response response = deque.pop();
                nWaitingConsumers -= 1;
                if (deliver(job, response)) {
                    return;
                }
            }

            // Then try to get a consumer from the graph with the most workers
            LOG.debug("No consumers with the right affinity. Looking for any consumer.");
            List<Deque<Response>> deques = new ArrayList<>(connectionsForGraph.values());
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

            // No workers were available to accept the tasks. The thread should wait on the next iteration.
            LOG.debug("No consumer was available. They all must have closed their connections.");
            if (nWaitingConsumers != 0) {
                throw new AssertionError("There should be no waiting consumers here, something is wrong.");
            }
        }

    }

    /**
     * Attempt to hand some tasks from the given job to the given waiting consumer connection.
     * This might fail because the consumer has closed the connection.
     * @return whether the handoff succeeded.
     */
    public synchronized boolean deliver (Job job, Response response) {

        // Check up-front whether the connection is still open.
        if (!response.getRequest().getRequest().getConnection().isOpen()) {
            LOG.debug("Consumer connection was closed. It will be removed.");
            return false;
        }

        // Get up to N tasks from the visibleTasks deque
        List<Task> tasks = new ArrayList<>();
        while (tasks.size() < 4 && !job.visibleTasks.isEmpty()) {
            tasks.add(job.visibleTasks.poll());
        }
        // Attempt to deliver the tasks to the given consumer.
        try {
            response.setStatus(HttpStatus.OK_200);
            OutputStream out = response.getOutputStream();
            // This is a JSON object of the form {taskId1: request1, taskId2: request2}
            int n = 0;
            out.write('{');
            for (Task task : tasks) {
                // FIXME we should really not be assembling JSON one character at a time
                // use tree model
                if (n++ > 0) {
                    out.write(',');
                    out.write('\n');
                }
                out.write('"');
                out.write(Integer.toString(task.taskId).getBytes());
                out.write('"');
                out.write(':');
                out.write(task.payload.getBytes());
            }
            out.write('\n');
            out.write('}');
            response.resume();
        } catch (IOException e) {
            // Connection was probably closed, but treat it as a server error.
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

    /** @return whether the task was found and removed. */
    public synchronized boolean deleteTask (QueuePath queuePath) {
        User user = findUser(queuePath.userId, false);
        if (user == null) {
            return false;
        }
        Job job = user.findJob(queuePath.jobId, false);
        if (job == null) {
            return false;
        }
        // There could be thousands of invisible (delivered) tasks, so we use a hash map.
        // We only allow removal of invisible tasks for now.
        // Return whether removal call discovered an existing task.
        return job.invisibleTasks.remove(queuePath.taskId) != null;
    }

    // Todo: occasionally purge closed connections from connectionsForGraph

    @Override
    public void run() {
        while (true) {
            try {
                deliverTasksForOneJob();
            } catch (InterruptedException e) {
                LOG.warn("Task pump thread was interrupted.");
                return;
            }
        }
    }

    /** Search through the users to find one with the given ID, without advancing the head of the circular list. */
    public User findUser (String userId, boolean create) {
        for (User user : users) {
            if (user.userId.equals(userId)) {
                return user;
            }
        }
        if (create) {
            User user = new User(userId);
            users.insertAtTail(user);
            return user;
        }
        return null;
    }


}
