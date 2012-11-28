package org.opentripplanner.api.thrift.util;

import org.opentripplanner.api.thrift.definition.Path;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.spt.GraphPath;

/**
 * Extends the Thrift Path for convenient construction.
 * 
 * @author avi
 * 
 */
public class PathExtension extends Path {

	/**
	 * Required for serialization.
	 */
	private static final long serialVersionUID = 6666801480159263902L;

	/**
	 * Construct from a GraphPath.
	 * 
	 * @param path
	 * @param include_path
	 *            If true, include edges and states. Otherwise only summary
	 *            stats.
	 */
	public PathExtension(GraphPath path, boolean includePathDetails) {
		super();

		// Set summary information.
		setDuration(path.getDuration());
		setStart_time(path.getStartTime());
		setEnd_time(path.getEndTime());

		// Optionall include path details
		if (includePathDetails) {
			for (State state : path.states) {
				addToStates(new TravelStateExtension(state));
			}

			for (Edge e : path.edges) {
				addToEdges(new GraphEdgeExtension(e));
			}
		}
	}

	/**
	 * Convenience constructor to initialize from GraphPath and include all path
	 * information.
	 * 
	 * 
	 * @param path
	 */
	public PathExtension(GraphPath path) {
		this(path, true);
	}
}