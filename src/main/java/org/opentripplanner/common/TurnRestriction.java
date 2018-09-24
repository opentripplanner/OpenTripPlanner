package org.opentripplanner.common;

import java.io.Serializable;

import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.graph.Edge;

public class TurnRestriction implements Serializable {
    private static final long serialVersionUID = 6072427988268244536L;
    public TurnRestrictionType type;
    public Edge from;
    public Edge to;
    public RepeatingTimePeriod time;
    public TraverseModeSet modes;

    public String toString() {
        return type.name() + " from " + from + " to " + to + "(" + modes + ")";
    }
    
    public TurnRestriction () {
        time = null;
    }
    
    /**
     * Convenience constructor.
     * 
     * @param from
     * @param to
     * @param type
     */
    public TurnRestriction(Edge from, Edge to, TurnRestrictionType type,
            TraverseModeSet modes) {
        this();
        this.from = from;
        this.to = to;
        this.type = type;
        this.modes = modes;
    }
    
    /**
     * Return true if the turn restriction is in force at the time described by the long.
     * @param time
     * @return
     */
    public boolean active(long time) {
        if (this.time != null)
            return this.time.active(time);
        return true;
    }
}
