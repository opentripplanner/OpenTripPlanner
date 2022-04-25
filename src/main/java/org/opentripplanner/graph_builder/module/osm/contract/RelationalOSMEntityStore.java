package org.opentripplanner.graph_builder.module.osm.contract;

import java.util.Collection;
import java.util.Set;
import org.opentripplanner.graph_builder.module.osm.Area;
import org.opentripplanner.graph_builder.module.osm.TurnRestrictionTag;
import org.opentripplanner.graph_builder.module.osm.model.OSMLevel;
import org.opentripplanner.graph_builder.module.osm.model.OSMNode;
import org.opentripplanner.graph_builder.module.osm.model.OSMWay;
import org.opentripplanner.graph_builder.module.osm.model.OSMWithTags;

public interface RelationalOSMEntityStore extends OSMEntityStore {
  boolean isAreaWay(Long wayId);

  Collection<OSMNode> getBikeParkingNodes();

  Collection<OSMNode> getCarParkingNodes();

  Collection<Area> getWalkableAreas();

  Collection<Area> getParkAndRideAreas();

  Collection<Area> getBikeParkingAreas();

  Collection<Long> getTurnRestrictionWayIds();

  Collection<TurnRestrictionTag> getFromWayTurnRestrictions(Long fromWayId);

  Collection<TurnRestrictionTag> getToWayTurnRestrictions(Long toWayId);

  Collection<OSMNode> getStopsInArea(OSMWithTags areaParent);

  OSMLevel getLevelForWay(OSMWithTags way);

  Set<OSMWay> getAreasForNode(Long nodeId);

  boolean isNodeBelongsToWay(Long nodeId);
}
