package org.opentripplanner.ext.transmodelapi;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.ext.transmodelapi.support.GqlUtil;
import org.opentripplanner.routing.api.request.RouteRequest;

class TransmodelGraphQLSchemaTest {

  @Test
  void testSchemaBuild() {
    GqlUtil gqlUtil = new GqlUtil(ZoneIds.OSLO);
    var schema = TransmodelGraphQLSchema.create(new RouteRequest(), gqlUtil);
    assertNotNull(schema);
  }
}
