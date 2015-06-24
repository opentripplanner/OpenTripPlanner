package org.opentripplanner.analyst.broker;

import gnu.trove.map.TIntLongMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntLongHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.opentripplanner.analyst.cluster.AnalystClusterRequest;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

/**
 * FIXME delivered tasks map is oblivious to multiple tasks having the same ID.
 * In fact we just generate numeric queue task IDs. Origin point IDs will be handled at the application layer.
 */
public class Job {

    private int nTasks = 0;

    private int completed;

    public final String jobId;

    /* Defines cache affinity group for contained tasks. */
    String graphId;

    /* Tasks awaiting delivery. */
    Queue<AnalystClusterRequest> visibleTasks = new ArrayDeque<>();

    /* Tasks that have been delivered to a worker but are awaiting completion. */
    TIntObjectMap<AnalystClusterRequest> invisibleTasks = new TIntObjectHashMap<>();

    TIntLongMap invisibleUntil = new TIntLongHashMap();

    public Job (String jobId) {
        this.jobId = jobId;
    }

    /** Adds a task to this Job, assigning it a task ID number. */
    public void addTask (AnalystClusterRequest task) {
        nTasks++;
        visibleTasks.add(task);
    }

    public void markTasksDelivered(List<AnalystClusterRequest> tasks) {
        long deliveryTime = System.currentTimeMillis();
        long visibleAt = deliveryTime + 60000; // one minute
        for (AnalystClusterRequest task : tasks) {
            invisibleUntil.put(task.taskId, visibleAt);
            invisibleTasks.put(task.taskId, task);
        }
    }

    public synchronized void markTasksCompleted (AnalystClusterRequest... tasks) {
        for (AnalystClusterRequest task : tasks) {
            invisibleTasks.remove(task.taskId);
            invisibleUntil.remove(task.taskId);
        }

        completed += tasks.length;
    }

    public int getCompleted () {
        return completed;
    }

}
