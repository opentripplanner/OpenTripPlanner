package org.opentripplanner.analyst.broker;

import gnu.trove.iterator.TIntLongIterator;
import gnu.trove.map.TIntLongMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntLongHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.opentripplanner.analyst.cluster.AnalystClusterRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

/**
 * FIXME delivered tasks map is oblivious to multiple tasks having the same ID.
 * In fact we just generate numeric queue task IDs. Origin point IDs will be handled at the application layer.
 */
public class Job {

    private static final Logger LOG = LoggerFactory.getLogger(Job.class);

    /* How long until tasks are eligible for re-delivery. */
    private static final int INVISIBLE_DURATION_SEC = 30;

    /* A unique identifier for this job, usually a random UUID. */
    public final String jobId;

    /* The graph needed to handle all tasks contained in this job. */
    String graphId;

    /* Tasks in this job that have yet to be delivered, or that will be re-delivered due to completion timeout. */
    // maybe this should only be a list of IDs.
    Queue<AnalystClusterRequest> tasksAwaitingDelivery = new ArrayDeque<>();

    /* The tasks in this job keyed on their task ID. */
    TIntObjectMap<AnalystClusterRequest> tasksById = new TIntObjectHashMap<>();

    /*
     * Completion timeouts for tasks that have been delivered.
     * A task whose ID is in this map has been delivered, has not been reported completed,
     * and is not awaiting re-delivery.
     */
    TIntLongMap invisibleUntil = new TIntLongHashMap();

    /* The IDs of all tasks that have been marked completed. */
    TIntSet completedTasks = new TIntHashSet();

    public Job (String jobId) {
        this.jobId = jobId;
    }

    /** Adds a task to this Job, assigning it a task ID number. */
    public void addTask (AnalystClusterRequest task) {
        tasksById.put(task.taskId, task);
        tasksAwaitingDelivery.add(task);
    }

    public void markTasksDelivered(List<AnalystClusterRequest> tasks) {
        long deliveryTime = System.currentTimeMillis();
        long visibleAt = deliveryTime + INVISIBLE_DURATION_SEC * 1000;
        for (AnalystClusterRequest task : tasks) {
            invisibleUntil.put(task.taskId, visibleAt);
        }
    }

    /**
     * Find all tasks that are currently invisible but have passed their invisibility timeout without being marked
     * completed, and make all these tasks visible again for delivery.
     * TODO maybe this should only be triggered when the awaiting delivery queue is empty to reduce double-delivery.
     */
    public int redeliver () {
        long now = System.currentTimeMillis();
        TIntLongIterator invisibleIterator = invisibleUntil.iterator();
        int nRedelivered = 0;
        while (invisibleIterator.hasNext()) {
            invisibleIterator.advance();
            int taskId = invisibleIterator.key();
            long timeout = invisibleIterator.value();
            if (now > timeout) {
                invisibleIterator.remove();
                tasksAwaitingDelivery.add(tasksById.get(taskId));
                LOG.warn("Task {} of job {} was not completed in time, queueing it for re-delivery.", taskId, jobId);
                nRedelivered += 1;
            }
        }
        return nRedelivered;
    }

    public void markTaskCompleted (int taskId) {
        if (tasksById.get(taskId) == null) {
            LOG.error("Tried to mark task {} completed, but it was not in job {}.", taskId, jobId);
            return;
        }
        if (invisibleUntil.remove(taskId) != 0) {
            // If the taskId was found in the invisibleUntil map, the task was delivered and has not been slated for
            // re-delivery.
            completedTasks.add(taskId);
        } else {
            // If the taskId was not found in the invisibleUntil map, the task was never delivered, or timed out and was
            // slated for redelivery. We should ignore the completion message and let the re-delivery proceed to avoid
            // problems with redelivered tasks overwriting results in S3 after the job is considered finished.
            // TODO verify that there are no race conditions here.
            LOG.warn("Ignoring late task completion message, task {} was queued for re-delivery.");
        }
    }

    public int getTotalTaskCount() {
        return tasksById.size();
    }

    public int getCompletedTaskCount() {
        return completedTasks.size();
    }

    public boolean isComplete() {
        return completedTasks.size() == tasksById.size();
    }

    public boolean containsTask (int taskId) {
        AnalystClusterRequest req = tasksById.get(taskId);
        if (req != null) {
            if (!req.jobId.equals(this.jobId)) {
                LOG.error("Task {} has a job ID that does not match the job in which it was discovered.");
            }
            return true;
        }
        return false;
    }

}
