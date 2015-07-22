package org.opentripplanner.analyst.broker;

/**
 * Describes the status of Job.
 */
public class JobStatus {
    /** number of complete tasks */
    public int complete;

    /** number of tasks remaining */
    public int remaining;

    /** number of tasks in flight (i.e. on workers now) */
    public int inFlight;

    /** number of workers on this job */
    public int nWorkers;

    /** ID of this job */
    public String jobId;

    public JobStatus (Job job) {
        this.complete = job.getCompletedTaskCount();
        this.inFlight = job.invisibleUntil.size();
        this.remaining = job.tasksAwaitingDelivery.size();
        this.jobId = job.jobId;
    }

    /** default constructor for JSON deserialization */
    public JobStatus () { /* nothing */ }
}
