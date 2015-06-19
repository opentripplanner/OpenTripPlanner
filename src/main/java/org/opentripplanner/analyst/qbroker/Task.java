package org.opentripplanner.analyst.qbroker;

/**
 *
 */
public class Task {

    public int taskId;
    public String payload; // Requests are stored as JSON text because we don't have the model objects from OTP.
    public long invisibleUntil; // This task has been delivered or hidden, and should not be re-delivered until this time.

}
