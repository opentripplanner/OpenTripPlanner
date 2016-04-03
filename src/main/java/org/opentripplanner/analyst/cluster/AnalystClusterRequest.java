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
	/** The type of this request, provided for compatibility with R5 polymorphic request types. */
	public final String type = "analyst";

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
	@Deprecated
	public String directOutputUrl;

	/** A unique identifier for this request assigned by the queue/broker system. */
	public int taskId;

	/**
	 * Where should the job be saved?
	 */
	public String outputLocation;

	/**
	 * The routing parameters to use for a one-to-many profile request.
	 * Non-profile one-to-many requests are represented by simply setting the time window to zero width, i.e.
	 * in profileRequest fromTime == toTime.
	 * Non-transit one-to-many requests are represented by setting profileRequest.transitModes to null or empty.
	 * In that case only the directModes will be used to reach the destinations on the street.
	 */
	public ProfileRequest profileRequest;

	/** Should times be included in the results (i.e. ResultSetWithTimes rather than ResultSet) */
	public boolean includeTimes = false;
	
	private AnalystClusterRequest(String destinationPointsetId, String graphId) {
		this.destinationPointsetId = destinationPointsetId;
		this.graphId = graphId;
	}

	/**
	 * We're now using ProfileRequests for everything (no RoutingRequests for non-transit searches).
	 * An AnalystClusterRequest is a wrapper around a ProfileRequest with some additional settings and context.
	 */
	 public AnalystClusterRequest(String destinationPointsetId, String graphId, ProfileRequest req) {
		this(destinationPointsetId, graphId);
		try {
			profileRequest = req.clone();
		} catch (CloneNotSupportedException e) {
			throw new AssertionError();
		}
		profileRequest.analyst = true;
		profileRequest.toLat = profileRequest.fromLat;
		profileRequest.toLon = profileRequest.fromLon;
	}

	/** Used for deserialization from JSON */
	public AnalystClusterRequest () { /* do nothing */ }

}
