package org.opentripplanner.ext.edgenaming;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.module.osm.EdgeNamer.EdgeNamerType;
import org.opentripplanner.graph_builder.module.osm.internal.naming.DefaultNamer;

class EdgeNamerTest {

  @Test
  void nullType() {
    assertInstanceOf(DefaultNamer.class, EdgeNamerFactory.fromConfig(null));
    assertInstanceOf(DefaultNamer.class, EdgeNamerFactory.fromConfig(EdgeNamerType.DEFAULT));
  }
}
