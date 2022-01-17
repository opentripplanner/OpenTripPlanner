package org.opentripplanner.model;

import java.util.Collection;

/**
 * A grouping of Stops referred to by the same name. No actual boarding or alighting happens at this
 * point, but rather at its underlying childStops.
 */
public interface StopCollection {

        FeedScopedId getId();

        /**
         * Implementations should go down the hierarchy and return all the underlying stops
         * recursively.
         */
        Collection<StopLocation> getChildStops();

        double getLat();

        double getLon();
}
