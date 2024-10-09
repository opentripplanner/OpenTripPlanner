package org.opentripplanner.framework.geometry;

import org.locationtech.jts.geom.LineString;

public record SplitLineString(LineString beginning, LineString ending) {}
