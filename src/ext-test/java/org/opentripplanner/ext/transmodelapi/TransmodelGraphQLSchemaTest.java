package org.opentripplanner.ext.transmodelapi;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.ZoneId;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.transmodelapi.support.GqlUtil;
import org.opentripplanner.routing.api.request.RoutingRequest;

class TransmodelGraphQLSchemaTest {

  @Test
  void testSchemaBuild() {
    GqlUtil gqlUtil = new GqlUtil(TimeZone.getTimeZone(ZoneId.of("Europe/Oslo")));
    var schema = TransmodelGraphQLSchema.create(new RoutingRequest(), gqlUtil);
    assertNotNull(schema);
  }
}
