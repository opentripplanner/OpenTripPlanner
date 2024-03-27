package org.opentripplanner.graph_builder.services.osm;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.module.osm.naming.DefaultNamer;
import org.opentripplanner.graph_builder.services.osm.EdgeNamer.EdgeNamerType;

class EdgeNamerTest {

  @Test
  void nullType() {
    assertInstanceOf(DefaultNamer.class, EdgeNamer.EdgeNamerFactory.fromConfig(null));
    assertInstanceOf(
      DefaultNamer.class,
      EdgeNamer.EdgeNamerFactory.fromConfig(EdgeNamerType.DEFAULT)
    );
  }
}
