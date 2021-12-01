package org.opentripplanner.ext.legacygraphqlapi.model;

import org.opentripplanner.model.Agency;

/**
 * Class for route types. If agency is defined, the object is the route for the specific agency.
 */
public class LegacyGraphQLRouteTypeModel {

    /**
     * If defined, this is the route type is only relevant for the agency.
     */
    private final Agency agency;

    /**
     * Route type (GTFS).
     */
    private final int routeType;

    public LegacyGraphQLRouteTypeModel(
            Agency agency,
            int routeType
    ) {
        this.agency = agency;
        this.routeType = routeType;
    }

    public Agency getAgency() {
        return agency;
    }

    public int getRouteType() {
        return routeType;
    }
}
