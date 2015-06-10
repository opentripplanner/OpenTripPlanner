package org.opentripplanner.analyst.cluster;

import java.io.Serializable;

/**
 * Superclass for requests sent to an SPTWorker.
 * Not abstract, so we can bind JSON to it and discover the subclass to re-parse.
 * @author matthewc
 *
 */
public class AnalystClusterRequest implements Serializable {
	/** The ID of the destinations pointset */
	public String destinationPointsetId;
	
	/** The ID of the graph against which to calculate this request */
	public String graphId;
	
	/** The job ID this is associated with */
	public String jobId;

	/** The id of this particular origin */
	public String id;

	/** To where should the result be POSTed */
	public String directOutputUrl;

	/**
	 * To what queue should the notification of the result be delivered?
	 */
	public String outputQueue;

	/**
	 * Where should the job be saved?
	 */
	public String outputLocation;
	
	/** Should times be included in the results (i.e. ResultSetWithTimes rather than ResultSet) */
	public boolean includeTimes = false;
	
	/** Is this a profile request? */
	public boolean profile;
	
	public AnalystClusterRequest(String destinationPointsetId, String graphId, boolean profile) {
		this.destinationPointsetId = destinationPointsetId;
		this.graphId = graphId;
		this.profile = profile;
	}
	
	/** used for deserialization from JSON */
	public AnalystClusterRequest () { /* do nothing */ }
}
