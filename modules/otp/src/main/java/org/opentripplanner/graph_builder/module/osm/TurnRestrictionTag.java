package org.opentripplanner.graph_builder.module.osm;

import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.street.model.RepeatingTimePeriod;
import org.opentripplanner.street.model.TurnRestrictionType;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.search.TraverseModeSet;

/**
 * A temporary holder for turn restrictions while we have only way/node ids but not yet edge
 * objects
 */
class TurnRestrictionTag {

  long via;
  //Used only for issues so that it can be visualized in a map
  long relationOSMID;
  TurnRestrictionType type;
  Direction direction;
  RepeatingTimePeriod time;
  public List<StreetEdge> possibleFrom = new ArrayList<>();
  public List<StreetEdge> possibleTo = new ArrayList<>();
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

  enum Direction {
    LEFT,
    RIGHT,
    U,
    STRAIGHT,
  }
}
