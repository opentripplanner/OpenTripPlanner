package org.opentripplanner.routing.routepreferences;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Route;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default route preference rules for HSL.
 * Currently only used for adding default unpreferred routes.
 * 
 * @author optionsome
 * @see RoutePreferencesSource
 */
public class HSLRoutePreferencesSource implements RoutePreferencesSource {

    // Routes which start with HSL:7
    private static final String U_ROUTE_REGEX = "^HSL:7.*$";
    // Routes between HSL:2143 - HSL:2165
    // that include character 'A' within 2 characters after the 4 digits
    private static final String ESPOO_FAST_ROUTES_REGEX 
            = "^HSL:21(4[3-9]|5[0-9]|6[0-5])(A|[B-Z]A).*$";

    private static final Logger LOG = LoggerFactory.getLogger(HSLRoutePreferencesSource.class);

    /**
	 * Adds default values into unpreferredRoutes based on regex patterns
	 */
    @Override
    public void setRoutePreferences(RoutingRequest routingRequest, Graph graph) {
        // Either U_ROUTE_REGEX or ESPOO_FAST_ROUTES_REGEX
        Pattern patternsCombined = Pattern.compile(String.format(
                "(%s|%s)", U_ROUTE_REGEX, ESPOO_FAST_ROUTES_REGEX));
        for (Route route : graph.index.routeForId.values()) {
            FeedScopedId routeId = route.getId();
            Matcher matcher = patternsCombined.matcher(routeId.toString());
            if (matcher.matches()) {
                routingRequest.unpreferredRoutes.addToAgencyAndRouteIds(routeId);
                LOG.info("Added {} into unpreferred routes", routeId.toString());
            }
        }
    }
}
