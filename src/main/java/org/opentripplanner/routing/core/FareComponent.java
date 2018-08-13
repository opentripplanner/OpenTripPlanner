package org.opentripplanner.routing.core;

import org.opentripplanner.model.FeedId;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * FareComponent is a sequence of routes for a particular fare.
 * </p>
 */
public class FareComponent {

    public FeedId fareId;
    public Money price;
    public List<FeedId> routes;

    public FareComponent(FeedId fareId, Money amount) {
        this.fareId = fareId;
        price = amount;
        routes = new ArrayList<FeedId>();
    }

    public void addRoute(FeedId routeId) {
        routes.add(routeId);
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer("FareComponent(");
        buffer.append(fareId.toString());
        buffer.append(", ");
        buffer.append(price.toString());
        buffer.append(", ");
        for (FeedId routeId : routes) {
            buffer.append(routeId.toString());
            buffer.append(", ");
        }
        buffer.append(")");
        return buffer.toString();
    }
}

