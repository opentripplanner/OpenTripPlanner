package org.opentripplanner.routing.routepreferences;

import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.Graph;

/**
 * Interface for patching default preferred, unpreferred or banned routes into
 * {@link RoutingRequest} {@link RouteMatcher} based on custom logic.
 *
 * @author optionsome
 */
public interface RoutePreferencesSource {

	public void setRoutePreferences(RoutingRequest routingRequest, Graph graph);

	/**
	 * Return the given RoutePreferencesSource or throws IllegalArgumentException
	 * if an unkown type is specified
	 */
	public static RoutePreferencesSource fromConfig(String type) {

		switch (type) {
		case "HSL":
			return new HSLRoutePreferencesSource();
		default:
			throw new IllegalArgumentException(String.format(
					"Unknown RoutePreferencesSource: '%s'", type));
		}
	}

}