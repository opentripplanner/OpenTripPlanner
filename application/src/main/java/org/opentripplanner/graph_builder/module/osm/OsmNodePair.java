package org.opentripplanner.graph_builder.module.osm;

import java.util.Objects;
import org.opentripplanner.osm.model.OsmNode;

class OsmNodePair {

  final OsmNode first;
  final OsmNode second;

  OsmNodePair(OsmNode first, OsmNode second) {
    if (first.getId() > second.getId()) {
      this.second = first;
      this.first = second;
    } else {
      this.first = first;
      this.second = second;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    OsmNodePair that = (OsmNodePair) o;
    return Objects.equals(first, that.first) && Objects.equals(second, that.second);
  }

  @Override
  public int hashCode() {
    return Objects.hash(first, second);
  }
}
