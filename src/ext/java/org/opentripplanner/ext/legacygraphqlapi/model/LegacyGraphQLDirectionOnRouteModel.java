package org.opentripplanner.ext.legacygraphqlapi.model;

import org.opentripplanner.model.Route;

/**
 * Class that contains a directionId on a {@link Route}.
 */
public class LegacyGraphQLDirectionOnRouteModel {

    /**
     * Direction on a route, i.e.trips/patterns on a route that have this directionId.
     */
    private final int directionId;

    /**
     * Route that should contain trips/patterns that have the defined directionId.
     */
    private final Route route;

    public LegacyGraphQLDirectionOnRouteModel(int directionId, Route route) {
        this.directionId = directionId;
        this.route = route;
    }

    public int getDirectionId() {
        return directionId;
    }

    public Route getRoute() {
        return route;
    }
}
