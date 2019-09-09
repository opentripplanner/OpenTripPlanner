package org.opentripplanner.model;

import java.util.Collection;

/**
 * A grouping of Stops referred to by the same name. No actual boarding or alighting happens at this
 * point, but rather at its underlying childStops.
 */
public interface StopCollection {

        FeedScopedId getId();

        Collection<Stop> getChildStops();

        double getLat();

        double getLon();
}
