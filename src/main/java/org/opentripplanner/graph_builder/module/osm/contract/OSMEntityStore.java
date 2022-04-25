package org.opentripplanner.graph_builder.module.osm.contract;

import java.util.Collection;
import org.opentripplanner.openstreetmap.model.OSMNode;
import org.opentripplanner.openstreetmap.model.OSMRelation;
import org.opentripplanner.openstreetmap.model.OSMWay;

public interface OSMEntityStore {
  void addNode(OSMNode node);

  OSMNode getNode(Long nodeId);

  Collection<OSMNode> getNodes();

  void addWay(OSMWay way);

  OSMWay getWay(Long wayId);

  Collection<OSMWay> getWays();

  void addRelation(OSMRelation relation);

  Collection<OSMRelation> getRelations();

  int nodeCount();

  int wayCount();

  int relationCount();
}
