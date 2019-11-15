package org.opentripplanner.graph_builder.module.osm;

import org.opentripplanner.common.RepeatingTimePeriod;
import org.opentripplanner.common.TurnRestrictionType;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.StreetEdge;

import java.util.ArrayList;
import java.util.List;

/**
 * A temporary holder for turn restrictions while we have only way/node ids but not yet edge objects
 */
class TurnRestrictionTag {

    enum Direction {
        LEFT, RIGHT, U, STRAIGHT;
    }

    long via;

    //Used only for issues so that it can be visualized in a map
    long relationOSMID;

    TurnRestrictionType type;

    Direction direction;

    RepeatingTimePeriod time;

    public List<StreetEdge> possibleFrom = new ArrayList<StreetEdge>();

    public List<StreetEdge> possibleTo = new ArrayList<StreetEdge>();

    public TraverseModeSet modes;

    TurnRestrictionTag(long via, TurnRestrictionType type, Direction direction, long relationOSMID) {
        this.via = via;
        this.type = type;
        this.direction = direction;
        this.relationOSMID = relationOSMID;
    }

    @Override
    public String toString() {
        return String.format("%s turn restriction via node %d", direction, via);
    }
}
