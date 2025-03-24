package org.opentripplanner.graph_builder.module.osm.moduletests._support;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.graph_builder.module.osm.OsmDatabase;
import org.opentripplanner.osm.OsmProvider;
import org.opentripplanner.osm.model.OsmEntity;
import org.opentripplanner.osm.model.OsmNode;
import org.opentripplanner.osm.model.OsmRelation;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.osm.tagmapping.OsmTagMapper;
import org.opentripplanner.osm.wayproperty.WayPropertySet;

public class TestOsmProvider implements OsmProvider {

  private final List<OsmWay> ways;
  private final List<OsmNode> nodes;
  private final List<OsmRelation> relations;
  private final OsmTagMapper osmTagMapper = new OsmTagMapper();
  private final WayPropertySet wayPropertySet = new WayPropertySet();

  public TestOsmProvider(List<OsmRelation> relations, List<OsmWay> ways, List<OsmNode> nodes) {
    // this was originally peek() but Joel insisted that it's "for debugging"
    for (OsmRelation relation : relations) {
      relation.setOsmProvider(this);
    }
    this.relations = List.copyOf(relations);
    for (OsmWay way : ways) {
      way.setOsmProvider(this);
    }
    this.ways = List.copyOf(ways);
    this.nodes = List.copyOf(nodes);
  }

  public static Builder of() {
    return new Builder();
  }

  @Override
  public void readOsm(OsmDatabase osmdb) {
    relations.forEach(osmdb::addRelation);
    osmdb.doneFirstPhaseRelations();
    ways.forEach(osmdb::addWay);
    osmdb.doneSecondPhaseWays();
    nodes.forEach(osmdb::addNode);
    osmdb.doneThirdPhaseNodes();
  }

  @Override
  public OsmTagMapper getOsmTagMapper() {
    return osmTagMapper;
  }

  @Override
  public void checkInputs() {}

  @Override
  public WayPropertySet getWayPropertySet() {
    return wayPropertySet;
  }

  @Override
  public ZoneId getZoneId() {
    return ZoneIds.LONDON;
  }

  public static class Builder {

    private final AtomicLong counter = new AtomicLong();
    private final List<OsmNode> nodes = new ArrayList<>();
    private final List<OsmWay> ways = new ArrayList<>();
    private final List<OsmRelation> relations = new ArrayList<>();

    public TestOsmProvider build() {
      return new TestOsmProvider(relations, ways, nodes);
    }

    /**
     * Add a way and create nodes for the from and to coordinates.
     */
    public Builder addWay(OsmWay way) {
      var from = new OsmNode(1, 1);
      from.setId(1);
      var to = new OsmNode(1.1, 1.1);
      to.setId(2);
      way.getNodeRefs().add(from.getId());
      way.getNodeRefs().add(to.getId());

      ways.add(way);
      nodes.addAll(List.of(from, to));
      return this;
    }

    public Builder addAreaFromNodes(List<OsmNode> areaNodes) {
      return addAreaFromNodes(counter.incrementAndGet(), areaNodes);
    }

    public Builder addAreaFromNodes(long id, List<OsmNode> areaNodes) {
      this.nodes.addAll(areaNodes);
      var nodeIds = areaNodes.stream().map(OsmEntity::getId).toList();

      var area = new OsmWay();
      area.setId(id);
      area.addTag("area", "yes");
      area.addTag("highway", "pedestrian");
      area.getNodeRefs().addAll(nodeIds);
      area.getNodeRefs().add(nodeIds.getFirst());

      this.ways.add(area);
      return this;
    }

    public Builder addWayFromNodes(OsmNode... nodes) {
      return addWayFromNodes(counter.incrementAndGet(), Arrays.stream(nodes).toList());
    }

    public Builder addWayFromNodes(long id, List<OsmNode> nodes) {
      this.nodes.addAll(nodes);
      var nodeIds = nodes.stream().map(OsmEntity::getId).toList();

      var way = new OsmWay();
      way.setId(id);
      way.addTag("highway", "pedestrian");
      way.getNodeRefs().addAll(nodeIds);

      this.ways.add(way);
      return this;
    }

    public Builder addRelation(OsmRelation relation) {
      this.relations.add(relation);
      return this;
    }
  }
}
