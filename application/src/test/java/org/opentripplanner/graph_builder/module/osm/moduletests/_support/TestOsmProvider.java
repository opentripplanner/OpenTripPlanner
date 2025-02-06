package org.opentripplanner.graph_builder.module.osm.moduletests._support;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.graph_builder.module.osm.OsmDatabase;
import org.opentripplanner.osm.OsmProvider;
import org.opentripplanner.osm.model.OsmNode;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.osm.tagmapping.OsmTagMapper;
import org.opentripplanner.osm.wayproperty.WayPropertySet;

public class TestOsmProvider implements OsmProvider {

  private final List<OsmWay> ways;
  private final List<OsmNode> nodes;
  private final OsmTagMapper osmTagMapper = new OsmTagMapper();
  private final WayPropertySet wayPropertySet = new WayPropertySet();

  public TestOsmProvider(List<OsmWay> ways, List<OsmNode> nodes) {
    this.ways = ways.stream().peek(w -> w.setOsmProvider(this)).toList();
    this.nodes = List.copyOf(nodes);
  }

  public static Builder of() {
    return new Builder();
  }

  @Override
  public void readOsm(OsmDatabase osmdb) {
    ways.forEach(osmdb::addWay);

    osmdb.doneSecondPhaseWays();

    nodes.forEach(osmdb::addNode);
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

    private final List<OsmWay> ways = new ArrayList<>();
    private final List<OsmNode> nodes = new ArrayList<>();

    public TestOsmProvider build() {
      return new TestOsmProvider(ways, nodes);
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
  }
}
