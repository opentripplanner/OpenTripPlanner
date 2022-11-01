package org.opentripplanner.util.model;

import java.io.Serializable;

/**
 * A list of coordinates encoded as a string.
 * <p>
 * See <a href="http://code.google.com/apis/maps/documentation/polylinealgorithm.html">Encoded
 * polyline algorithm format</a>
 */

public record EncodedPolyline(String points, int length) implements Serializable {}
