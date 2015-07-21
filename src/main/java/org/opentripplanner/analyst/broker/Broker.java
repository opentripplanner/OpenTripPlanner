package org.opentripplanner.analyst.broker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.opentripplanner.analyst.cluster.AnalystClusterRequest;
import org.opentripplanner.analyst.cluster.AnalystWorker;
import org.opentripplanner.api.model.AgencyAndIdSerializer;
import org.opentripplanner.api.model.JodaLocalDateSerializer;
import org.opentripplanner.api.model.QualifiedModeSetSerializer;
import org.opentripplanner.api.model.TraverseModeSetSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

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
 * some of them to accept tasks on graphs other than the one they have declared affinity for.
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

    private static final int REDELIVERY_INTERVAL_SEC = 30;

    public final CircularList<Job> jobs = new CircularList<>();

    /** the most tasks to deliver to a worker at a time */
    public final int MAX_TASKS_PER_WORKER = 8;

    /**
     * How long to give workers to start up (in ms) before assuming that they have started (and starting more
     * on a given graph if they haven't.
     */
    public static final long WORKER_STARTUP_TIME = 5 * 60 * 1000;

    private int nUndeliveredTasks = 0; // Including normal priority jobs and high-priority tasks.

    private int nWaitingConsumers = 0; // including some that might be closed

    private int nextTaskId = 0;

    /** Maximum number of workers allowed */
    private int maxWorkers;

    private static final ObjectMapper mapper = new ObjectMapper();

    private long nextRedeliveryCheckTime = System.currentTimeMillis();

    static {
        mapper.registerModule(AgencyAndIdSerializer.makeModule());
        mapper.registerModule(QualifiedModeSetSerializer.makeModule());
        mapper.registerModule(JodaLocalDateSerializer.makeModule());
        mapper.registerModule(TraverseModeSetSerializer.makeModule());
    }

    /** broker configuration */
    private final Properties config;

    private WorkerCatalog workerCatalog = new WorkerCatalog();

    private InstanceRequestTracker instanceRequestTracker;

    /** The time at which each task was delivered to a worker, to allow re-delivery. */
    TIntIntMap deliveryTimes = new TIntIntHashMap();

    /** Requests that are not part of a job and can "cut in line" in front of jobs for immediate execution. */
    private ArrayListMultimap<String, AnalystClusterRequest> highPriorityTasks = ArrayListMultimap.create();

    /** Priority requests that have already been farmed out to workers, and are awaiting a response. */
    private TIntObjectMap<Response> highPriorityResponses = new TIntObjectHashMap<>();

    /** Outstanding requests from workers for tasks, grouped by worker graph affinity. */
    Map<String, Deque<Response>> consumersByGraph = new HashMap<>();

    /** should we work offline */
    private boolean workOffline;

    // Queue of tasks to complete Delete, Enqueue etc. to avoid synchronizing all the functions ?

    public Broker (Properties brokerConfig, String addr, int port) {

        this.config = brokerConfig;
        Boolean workOffline = Boolean.parseBoolean(brokerConfig.getProperty("work-offline"));
        if (workOffline == null) {
            workOffline = true;
        }
        this.workOffline = workOffline;
        this.maxWorkers = brokerConfig.getProperty("max-workers") != null ?
                Integer.parseInt(brokerConfig.getProperty("max-workers")) : 20;

        instanceRequestTracker = new InstanceRequestTracker(brokerConfig, addr, port);

    }

    /**
     * Enqueue a task for execution ASAP, planning to return the response over the same HTTP connection.
     * Low-reliability, no re-delivery.
     */
    public synchronized void enqueuePriorityTask (AnalystClusterRequest task, Response response) {
        task.taskId = nextTaskId++;
        highPriorityTasks.put(task.graphId, task);
        highPriorityResponses.put(task.taskId, response);
        nUndeliveredTasks += 1;
        createWorkersForGraph(task.graphId);
        notify();
    }

    /** Enqueue some tasks for queued execution possibly much later. Results will be saved to S3. */
    public synchronized void enqueueTasks (List<AnalystClusterRequest> tasks) {
        Job job = findJob(tasks.get(0)); // creates one if it doesn't exist
        createWorkersForGraph(job.graphId);
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

    /** Create additional workers for a given job, if there are not already enough running. */
    public void createWorkersForGraph(String graphId) {

        if (workOffline) {
            LOG.info("Work offline enabled, not creating workers for graph {}", graphId);
            return;
        }

        // Clear workers out of the catalog that have not been heard from in a while.
        // We don't want to assume work will be carried out by dead workers.
        workerCatalog.purgeDeadWorkers();

        if (workerCatalog.graphHasNoWorkers(graphId) &&
            workerCatalog.nActiveWorkers() < maxWorkers &&
            instanceRequestTracker.noOutstandingRequests(graphId)) {
            instanceRequestTracker.requestWorkersForGraph(graphId);
        }
        else if (workerCatalog.observationsByWorkerId.size() >= maxWorkers) {
            LOG.warn("{} workers already started, not starting more; jobs on graph {} will not complete", maxWorkers, graphId);
            // TODO retry later
        }
        else {
            LOG.info("Workers exist on graph {}, not starting new workers", graphId);
        }
    }

    /** Consumer long-poll operations are enqueued here. */
    public synchronized void registerSuspendedResponse(String graphId, Response response) {
        // Add this worker to our catalog, tracking its graph affinity and the last time it was seen.
        String workerId = response.getRequest().getHeader(AnalystWorker.WORKER_ID_HEADER);
        if (workerId != null && !workerId.isEmpty()) {
            workerCatalog.catalog(workerId, graphId);
        } else {
            LOG.error("Worker did not supply a unique ID for itself . Ignoring it.");
            return;
        }
        // Shelf this suspended response in a queue grouped by graph affinity.
        Deque<Response> deque = consumersByGraph.get(graphId);
        if (deque == null) {
            deque = new ArrayDeque<>();
            consumersByGraph.put(graphId, deque);
        }
        deque.addLast(response);
        nWaitingConsumers += 1;
        // Wake up the delivery thread if it's waiting on consumers.
        // This is whatever thread called wait() while holding the monitor for this Broker object.
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
        LOG.info("{} total active workers", workerCatalog.nActiveWorkers());
    }

    /**
     *  Check whether there are any delivered tasks that have reached their invisibility timeout but have not yet been
     *  marked complete. Enqueue those tasks for redelivery.
     */
    private void redeliver () {
        if (System.currentTimeMillis() > nextRedeliveryCheckTime) {
            nextRedeliveryCheckTime += REDELIVERY_INTERVAL_SEC * 1000;
            for (Job job : jobs) {
                nUndeliveredTasks += job.redeliver();
            }
        }
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
            redeliver();
        }
        LOG.debug("Task delivery thread is awake and there are some undelivered tasks.");
        logQueueStatus();

        while (nWaitingConsumers == 0) {
            LOG.debug("Task delivery thread is going to sleep, there are no consumers waiting.");
            // Thread will be notified when there are new incoming consumer connections.
            wait();
        }

        LOG.debug("Task delivery thread awake; consumers are waiting and tasks are available");

        // Loop over all jobs and send them to consumers
        // This makes for an as-fair-as-possible allocation: jobs are fairly allocated between
        // workers on their graph.

        // start with high-priority tasks
        HIGHPRIORITY:
        for (Map.Entry<String, Collection<AnalystClusterRequest>> e : highPriorityTasks.asMap().entrySet()) {
            // the collection is an arraylist with the most recently added at the end
            String graphId = e.getKey();
            Collection<AnalystClusterRequest> tasks = e.getValue();

            // see if there are any consumers for this
            // don't respect graph affinity when working offline; we can't arbitrarily start more workers
            Deque<Response> consumers;
            if (!workOffline)
                consumers = consumersByGraph.get(graphId);
            else {
                Optional<Deque<Response>> opt = consumersByGraph.values().stream().filter(c -> !c.isEmpty()).findFirst();
                if (opt.isPresent()) consumers = opt.get();
                else consumers = null;
            }

            if (consumers == null || consumers.isEmpty()) {
                LOG.warn("No consumer found for graph {}, needed for {} high-priority tasks", graphId, tasks.size());
                continue HIGHPRIORITY;
            }

            Iterator<AnalystClusterRequest> taskIt = tasks.iterator();
            while (taskIt.hasNext() && !consumers.isEmpty()) {
                Response consumer = consumers.pop();

                // package tasks into a job
                Job job = new Job("HIGH PRIORITY");
                job.graphId = graphId;
                for (int i = 0; i < MAX_TASKS_PER_WORKER && taskIt.hasNext(); i++) {
                    job.addTask(taskIt.next());
                    taskIt.remove();
                }

                // TODO inefficiency here: we should mix single point and multipoint in the same response
                deliver(job, consumer);
                nWaitingConsumers--;
            }
        }

        // deliver low priority tasks
        while (nWaitingConsumers > 0) {
            // ensure we advance at least one; advanceToElement will not advance if the predicate passes
            // for the first element.
            jobs.advance();

            // find a job that both has visible tasks and has available workers
            // We don't respect graph affinity when working offline, because we can't start more workers
            Job current;
            if (!workOffline) {
                current = jobs.advanceToElement(e -> !e.visibleTasks.isEmpty() &&
                        consumersByGraph.containsKey(e.graphId) &&
                        !consumersByGraph.get(e.graphId).isEmpty());
            }
            else {
                current = jobs.advanceToElement(e -> !e.visibleTasks.isEmpty());
            }

            // nothing to see here
            if (current == null) break;

            Deque<Response> consumers;
            if (!workOffline)
                consumers = consumersByGraph.get(current.graphId);
            else {
                Optional<Deque<Response>> opt = consumersByGraph.values().stream().filter(c -> !c.isEmpty()).findFirst();
                if (opt.isPresent()) consumers = opt.get();
                else consumers = null;
            }
            // deliver this job to only one consumer
            // This way if there are multiple workers and multiple jobs the jobs will be fairly distributed, more or less
            deliver(current, consumers.pop());
            nWaitingConsumers--;
        }

        // TODO: graph switching

        // we've delivered everything we can, prevent anything else from happening until something changes
        wait();
    }

    /**
     * This uses a linear search through jobs, which should not be problematic unless there are thousands of
     * simultaneous jobs.
     * @return a Job object that contains the given task ID.
     */
    public Job getJobForTask (int taskId) {
        for (Job job : jobs) {
            if (job.containsTask(taskId)) {
                return job;
            }
        }
        return null;
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
        while (tasks.size() < MAX_TASKS_PER_WORKER && !job.visibleTasks.isEmpty()) {
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
     * TODO maybe use unique delivery receipts instead of task IDs to handle redelivered tasks independently
     * @return whether the task was found and removed.
     */
    public synchronized boolean markTaskCompleted (int taskId) {
        Job job = getJobForTask(taskId);
        if (job == null) {
            LOG.error("Could not find a job containing task {}, and therefore could not mark the task as completed.");
            return false;
        }
        job.markTaskCompleted(taskId);
        return true;
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
                LOG.info("Task pump thread was interrupted.");
                return;
            }
        }
    }

    /** find the job for a task, creating it if it does not exist */
    public Job findJob (AnalystClusterRequest task) {
        Job job = findJob(task.jobId);

        if (job != null)
            return job;

        job = new Job(task.jobId);
        job.graphId = task.graphId;
        jobs.insertAtTail(job);
        return job;
    }

    /** find the job for a jobId, or null if it does not exist */
    public Job findJob (String jobId) {
        for (Job job : jobs) {
            if (job.jobId.equals(jobId)) {
                return job;
            }
        }
        return null;
    }

    /** delete a job */
    public synchronized boolean deleteJob (String jobId) {
        Job job = findJob(jobId);
        if (job == null) return false;
        nUndeliveredTasks -= job.visibleTasks.size();
        return jobs.remove(job);
    }

    private Multimap<String, String> activeJobsPerGraph = HashMultimap.create();

    void activateJob (Job job) {
        activeJobsPerGraph.put(job.graphId, job.jobId);
    }

    void deactivateJob (Job job) {
        activeJobsPerGraph.remove(job.graphId, job.jobId);
    }
}
