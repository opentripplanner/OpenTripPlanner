package org.opentripplanner.analyst.broker;

/**
 *
 */
public class User {

    public final String userId;
    public String region; // only worker machines in this AWS region should handle tasks for this user
    public final CircularList<Job> jobs = new CircularList<>();

    public User(String userId) {
        this.userId = userId;
    }

    public Job findJob (String jobId, boolean create) {
        for (Job job : jobs) {
            if (job.jobId.equals(jobId)) {
                return job;
            }
        }
        if (create) {
            Job job = new Job(jobId);
            jobs.insertAtTail(job);
            return job;
        }
        return null;
    }


}
