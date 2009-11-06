package org.opentripplanner.jags.narrative;

import com.vividsolutions.jts.geom.Geometry;

/* A NarrativeItem represents a particular line in turn-by-turn directions */
public interface NarrativeItem {
    String getName(); // 2, Q, Red Line, R5, Rector St.

    String getDirection(); // 241 St-Wakefield Sta, Inbound, Northwest

    String getTowards();

    Geometry getGeometry();

    String getStart();

    String getEnd();

    double getDistanceKm();
}

