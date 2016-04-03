package org.opentripplanner.analyst.cluster;

import org.opentripplanner.common.MavenVersion;

import java.io.Serializable;

/**
 * Statistics about running a single task.
 * TODO add markField functions: "field = System.currentTimeMillis() - field;"
 * Every time it's called after the first time will record the elapsed time, assuming fields are initialized to 0.
 */
public class TaskStatistics implements Serializable{

    public static final long serialVersionUID = 1;

    /** milliseconds of compute time once the RAPTOR worker was started, exclusive of building result sets */
    public int compute = -1;

    /** number of milliseconds spent in graph build */
    public int graphBuild = -1;

    /** number of milliseconds spent in stop tree caching */
    public int stopTreeCaching = -1;

    /** milliseconds of time spent in RAPTOR algorithm (transit search) */
    public int transitSearch = -1;

    /** milliseconds of time spent in initial stop search */
    public int initialStopSearch = -1;

    /** milliseconds spent in walk search (this is not a search per se but simply using the SPT from the initial stop search to create walk times to reachable destinations */
    public int walkSearch = -1;

    /** milliseconds spent in propagating travel times from transit stops to targets */
    public int propagation = -1;

    /** time to make raptor data (milliseconds) */
    public int raptorData;

    /** number of discrete times (e.g. minutes) for which a RAPTOR search was performed */
    public int searchCount;

    /** time step between RAPTOR searches (seconds) */
    public int timeStep;

    /** milliseconds spent in preparing resultsets */
    public int resultSets = -1;

    /** was this an isochrone request */
    public boolean isochrone = false;

    /** total processing time, including fetching and pushing results, in milliseconds */
    public int total = -1;

    /** number of stops used in search (i.e. number of stops that had service on this day) */
    public int stopCount = -1;

    /** number of stops found in the initial walk search (stand-in for density/city-ness) */
    public int initialStopCount = -1;

    /** number of patterns used in the search (i.e. those in the RAPTOR worker data) */
    public int patternCount = -1;

    /** number of targets of this search */
    public int targetCount = -1;

    /** number of targets reached */
    public int targetsReached = 0;

    /** number of scheduled trips in the RAPTOR worker data */
    public int scheduledTripCount = -1;

    /** number of frequency trips (the actual number of trips - i.e. a frequency entry running every 10 minutes for an hour is 6 trips */
    public int frequencyTripCount = -1;

    /** number of frequency entries */
    public int frequencyEntryCount = -1;

    /** number of stops in the graph */
    public int graphStopCount;

    /** number of trips in the graph */
    public int graphTripCount;

    /** latitude of origin (note: potentially sensitive, should be stripped from some analyses) */
    public double lat;

    /** longitude of origins */
    public double lon;

    /** graph ID */
    public String graphId;

    /** point set ID */
    public String pointsetId;

    /** the job ID */
    public String jobId;

    /** AWS instance type, if applicable */
    public String awsInstanceType;

    /** unique ID of the worker (to control for any variation introduced by e.g. noisy neighbors in a virtualized environment) */
    public String workerId;

    /** was this request successful */
    public boolean success;

    /** was this a single-point request */
    public boolean single;

    /** OTP commit used for computation */
    public String otpCommit;

    /** UTC date/time this was computed */
    public long computeDate;

    public TaskStatistics() {
        otpCommit = MavenVersion.VERSION.commit;
        computeDate = System.currentTimeMillis();
    }
}
