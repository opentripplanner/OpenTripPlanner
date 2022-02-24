/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class Pathway extends TransitEntity {

    private static final long serialVersionUID = -2404871423254094109L;

    private int pathwayMode;

    private StationElement fromStop;

    private StationElement toStop;

    private String name;

    private String reversedName;

    private int traversalTime;

    private double length;

    private int stairCount;

    private double slope;

    private boolean isBidirectional;

    public Pathway(FeedScopedId id) {
        super(id);
    }

    @Override
    public String toString() {
        return "<Pathway " + getId() + ">";
    }

    public boolean isPathwayModeWheelchairAccessible() {
        return getPathwayMode() != 2 && getPathwayMode() != 4;
    }
}
