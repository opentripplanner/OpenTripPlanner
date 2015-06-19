package org.opentripplanner.analyst.qbroker;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

/**
 * FIXME delivered tasks map is oblivious to multiple tasks having the same ID.
 * In fact we just generate numeric queue task IDs. Origin point IDs will be handled at the application layer.
 */
public class Job {

    private int nTasks = 0;

    /* Defines cache affinity group for contained tasks. TODO set this when created. */
    String graphId;

    public final String jobId;

    /* Tasks awaiting delivery. */
    Queue<Task> visibleTasks = new ArrayDeque<>();

    /* Tasks that have been delivered to a worker but are awaiting completion. */
    TIntObjectMap<Task> invisibleTasks = new TIntObjectHashMap<>();

    public Job (String jobId) {
        this.jobId = jobId;
    }

    /** Adds a task to this Job, assigning it a task ID number. */
    public int addTask (String taskBody) {
        Task task = new Task();
        task.taskId = nTasks++;
        task.payload = taskBody;
        visibleTasks.add(task);
        return task.taskId;
    }

    public void markTasksDelivered(List<Task> tasks) {
        long deliveryTime = System.currentTimeMillis();
        for (Task task : tasks) {
            task.invisibleUntil = deliveryTime + 60000;
            invisibleTasks.put(task.taskId, task);
        }
    }

}
