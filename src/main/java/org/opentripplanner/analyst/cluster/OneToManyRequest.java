package org.opentripplanner.analyst.cluster;

import org.opentripplanner.routing.core.RoutingRequest;

import java.io.Serializable;

public class OneToManyRequest extends AnalystClusterRequest implements Serializable {
	public RoutingRequest options;

	/** used for single point requests with from specified by options */
	public OneToManyRequest(String to, RoutingRequest options, String graphId) {
		super(to, graphId, false);

		this.options = options.clone();
		this.options.batch = true;
		this.options.rctx = null;
	}
	
	/** used for deserialization from JSON */
	public OneToManyRequest () { /* nothing */ }
}
