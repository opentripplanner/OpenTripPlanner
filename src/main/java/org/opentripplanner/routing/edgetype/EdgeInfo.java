package org.opentripplanner.routing.edgetype;

import org.opentripplanner.openstreetmap.model.OSMLevel;
import org.opentripplanner.routing.core.TraverseMode;

/**
 * Created by mabu on 24.3.2015.
 */
public interface EdgeInfo {
    public void setLevel(OSMLevel level);

    public String getNiceLevel();

    public Integer getLevel();

    public Boolean isReliableLevel();

    public TraverseMode getPublicTransitType();

    public long getOsmID();
}
