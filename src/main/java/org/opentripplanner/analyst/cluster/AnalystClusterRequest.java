package org.opentripplanner.analyst.cluster;

import org.opentripplanner.profile.ProfileRequest;
import org.opentripplanner.routing.core.RoutingRequest;

import java.io.Serializable;

/**
 * A request sent to an Analyst cluster worker.
 * It has two separate fields for RoutingReqeust or ProfileReqeust to facilitate binding from JSON.
 * Only one of them should be set in a given instance, with the ProfileRequest taking precedence if both are set.
 */
public class AnalystClusterRequest implements Serializable {

	/** The ID of the destinations pointset */
	public String destinationPointsetId;

	/** The Analyst Cluster user that created this request */
	public String userId;

	/** The ID of the graph against which to calculate this request */
	public String graphId;

	/** The job ID this is associated with */
	public String jobId;

	/** The id of this particular origin */
	public String id;

	/** To where should the result be POSTed */
	public String directOutputUrl;

	/** A unique identifier for this request assigned by the queue/broker system. */
	public int taskId;

	/**
	 * To what queue should the notification of the result be delivered?
	 */
	public String outputQueue;

	/**
	 * Where should the job be saved?
	 */
	public String outputLocation;

	/**
	 * The routing parameters to use if this is a one-to-many profile request.
	 * If profileRequest is provided, it will take precedence and routingRequest will be ignored.
	 */
	public ProfileRequest profileRequest;

	/**
	 * The routing parameters to use if this is a non-profile one-to-many request.
	 * If profileRequest is not provided, we will fall back on this routingRequest.
	 */
	public RoutingRequest routingRequest;

	/** Should times be included in the results (i.e. ResultSetWithTimes rather than ResultSet) */
	public boolean includeTimes = false;
	
	private AnalystClusterRequest(String destinationPointsetId, String graphId) {
		this.destinationPointsetId = destinationPointsetId;
		this.graphId = graphId;
	}

	/** Create a cluster request that wraps a ProfileRequest, and has no RoutingRequest. */
	public AnalystClusterRequest(String destinationPointsetId, String graphId, ProfileRequest req) {
		this(destinationPointsetId, graphId);
		routingRequest = null;
		try {
			profileRequest = req.clone();
		} catch (CloneNotSupportedException e) {
			throw new AssertionError();
		}
		profileRequest.analyst = true;
		profileRequest.toLat = profileRequest.fromLat;
		profileRequest.toLon = profileRequest.fromLon;
	}

	/** Create a cluster request that wraps a RoutingRequest, and has no ProfileRequest. */
	public AnalystClusterRequest(String destinationPointsetId, String graphId, RoutingRequest req) {
		this(destinationPointsetId, graphId);
		profileRequest = null;
		routingRequest = req.clone();
		routingRequest.batch = true;
		routingRequest.rctx = null;
	}

	/** Used for deserialization from JSON */
	public AnalystClusterRequest () { /* do nothing */ }
}
