package org.opentripplanner.apis.gtfs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;

class GraphQLUtilsTest {

  @Test
  void typeName() {
    var name = GraphQLUtils.typeName(new GraphQLTypes.GraphQLTransitFilterInput(Map.of()));
    assertEquals("TransitFilterInput", name);
  }
}
