package org.opentripplanner.graph_builder.services.osm;

import org.junit.jupiter.api.Test;

class EdgeNamerTest {

  @Test
  void nullType(){
    var namer = EdgeNamer.EdgeNamerFactory.fromConfig(null);
  }

}