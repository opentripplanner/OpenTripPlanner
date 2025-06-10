package org.opentripplanner.apis.gtfs;

import static org.opentripplanner.framework.io.FileUtils.assertFileEquals;
import static org.opentripplanner.framework.io.FileUtils.readFile;
import static org.opentripplanner.framework.io.FileUtils.writeFile;

import graphql.schema.idl.SchemaPrinter;
import java.io.File;
import org.junit.jupiter.api.Test;
import org.opentripplanner.test.support.ResourceLoader;

public class GraphQLFormattingTest {

  public static final File SCHEMA_FILE = ResourceLoader.of(
    GraphQLFormattingTest.class
  ).mainResourceFile("schema.graphqls");

  @Test
  public void format() {
    String original = readFile(SCHEMA_FILE);
    var schema = SchemaFactory.createSchema();
    writeFile(SCHEMA_FILE, new SchemaPrinter().print(schema));
    assertFileEquals(original, SCHEMA_FILE);
  }
}
