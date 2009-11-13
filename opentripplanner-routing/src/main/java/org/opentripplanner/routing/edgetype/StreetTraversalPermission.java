package org.opentripplanner.routing.edgetype;

public enum StreetTraversalPermission {
    ALL,
    PEDESTRIAN_ONLY,
    BICYCLE_ONLY,
    CROSSHATCHED, //this street exists in both Beszel and Ul Qoma; traffic direction may depend on which city you're in.
    PEDESTRIAN_AND_BICYCLE_ONLY
}
