package org.opentripplanner.osm;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.graph_builder.module.osm.OsmDatabase;
import org.opentripplanner.osm.model.OsmEntity;
import org.opentripplanner.osm.model.OsmNode;
import org.opentripplanner.osm.model.OsmRelation;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.osm.model.OsmWayBuilder;
import org.opentripplanner.osm.tagmapping.OsmTagMapper;
import org.opentripplanner.osm.wayproperty.WayPropertySet;

public class TestOsmProvider implements OsmProvider {

  public static final TestOsmProvider EMPTY = new TestOsmProvider(List.of(), List.of(), List.of());
  private final List<OsmWay> ways;
  private final List<OsmNode> nodes;
  private final List<OsmRelation> relations;
  private final OsmTagMapper osmTagMapper = new OsmTagMapper();
  private final WayPropertySet wayPropertySet = osmTagMapper.buildWayPropertySet();

  public TestOsmProvider(List<OsmRelation> relations, List<OsmWay> ways, List<OsmNode> nodes) {
    // this was originally peek() but Joel insisted that it's "for debugging"
    this.relations = List.copyOf(
      relations
        .stream()
        .map(relation -> relation.copy().withOsmProvider(this).build())
        .toList()
    );
    this.ways = List.copyOf(
      ways
        .stream()
        .map(way -> way.copy().withOsmProvider(this).build())
        .toList()
    );
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
      var from = OsmNode.of().withId(1).withLatLon(1, 1).build();
      var to = OsmNode.of().withId(2).withLatLon(1.1, 1.1).build();
      var wayWithNodes = way.copy().addNodeRef(from.getId(), to.getId()).build();

      ways.add(wayWithNodes);
      nodes.addAll(List.of(from, to));
      return this;
    }

    public Builder addAreaFromNodes(List<OsmNode> areaNodes) {
      return addAreaFromNodes(counter.incrementAndGet(), areaNodes);
    }

    public Builder addAreaFromNodes(long id, List<OsmNode> areaNodes) {
      this.nodes.addAll(areaNodes);
      var nodeIds = areaNodes.stream().map(OsmEntity::getId).toList();

      var areaBuilder = OsmWay.of()
        .withId(id)
        .withTag("area", "yes")
        .withTag("highway", "pedestrian");
      nodeIds.forEach(areaBuilder::addNodeRef);
      areaBuilder.addNodeRef(nodeIds.getFirst());
      var area = areaBuilder.build();

      this.ways.add(area);
      return this;
    }

    public Builder addWayFromNodes(OsmNode... nodes) {
      return addWayFromNodes(counter.incrementAndGet(), Arrays.stream(nodes).toList());
    }

    public Builder addWayFromNodes(long id, List<OsmNode> nodes) {
      return addWayFromNodes(way -> {}, id, nodes);
    }

    public Builder addWayFromNodes(Consumer<OsmWayBuilder> wayBuilderConsumer, OsmNode... nodes) {
      return addWayFromNodes(wayBuilderConsumer, counter.incrementAndGet(), List.of(nodes));
    }

    public Builder addRelation(OsmRelation relation) {
      this.relations.add(relation);
      return this;
    }

    private Builder addWayFromNodes(
      Consumer<OsmWayBuilder> wayBuilderConsumer,
      long id,
      List<OsmNode> nodes
    ) {
      this.nodes.addAll(nodes);
      var nodeIds = nodes.stream().map(OsmEntity::getId).toList();
      var wayBuilder = OsmWay.of().withId(id).withTag("highway", "pedestrian");
      nodeIds.forEach(wayBuilder::addNodeRef);
      wayBuilderConsumer.accept(wayBuilder);
      var way = wayBuilder.build();
      this.ways.add(way);
      return this;
    }
  }
}
