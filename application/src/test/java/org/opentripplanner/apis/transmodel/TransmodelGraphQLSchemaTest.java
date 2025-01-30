package org.opentripplanner.apis.transmodel;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.opentripplanner.framework.io.FileUtils.assertFileEquals;
import static org.opentripplanner.framework.io.FileUtils.readFile;
import static org.opentripplanner.framework.io.FileUtils.writeFile;

import graphql.schema.idl.SchemaPrinter;
import java.io.File;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.apis.support.graphql.injectdoc.ApiDocumentationProfile;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitTuningParameters;
import org.opentripplanner.routing.api.request.RouteRequest;

class TransmodelGraphQLSchemaTest {

  public static final File SCHEMA_FILE = new File(
    "src/main/resources/org/opentripplanner/apis/transmodel/schema.graphql"
  );

  @Test
  void testSchemaBuild() {
    var schema = TransmodelGraphQLSchema.create(
      new RouteRequest(),
      ZoneIds.OSLO,
      ApiDocumentationProfile.DEFAULT,
      TransitTuningParameters.FOR_TEST
    );
    assertNotNull(schema);

    String original = readFile(SCHEMA_FILE);

    writeFile(SCHEMA_FILE, new SchemaPrinter().print(schema));

    // TODO: We could use graphql.schema.diff.SchemaDiff here
    assertFileEquals(original, SCHEMA_FILE);
  }
}
