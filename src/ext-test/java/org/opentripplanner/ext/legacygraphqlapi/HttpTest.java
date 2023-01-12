package org.opentripplanner.ext.legacygraphqlapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opentripplanner.test.support.JsonAssertions.assertEqualJson;

import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.glassfish.jersey.message.internal.OutboundJaxrsResponse;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.test.support.FilePatternSource;

@Execution(ExecutionMode.CONCURRENT)
class HttpTest {

  static OtpServerRequestContext context = new TestServerRequestContext();
  static LegacyGraphQLAPI resource = new LegacyGraphQLAPI(context, "");

  @FilePatternSource(pattern = "src/ext-test/resources/legacygraphqlapi/queries/*.graphql")
  @ParameterizedTest(name = "Check GraphQL query in {0}")
  void pure(Path path) throws IOException {
    var query = Files.readString(path);
    var response = resource.getGraphQL(query, 2000, 10000, new TestHeaders());
    var actualJson = extracted(response);
    assertEquals(200, response.getStatus());

    var expectationFile = path
      .getParent()
      .getParent()
      .resolve("expectations")
      .resolve(path.getFileName().toString().replace(".graphql", ".json"));

    if (!expectationFile.toFile().exists()) {
      Files.writeString(expectationFile, actualJson, StandardOpenOption.CREATE_NEW);
    }

    var expectedJson = Files.readString(expectationFile);
    assertEqualJson(expectedJson, actualJson);
  }

  private static String extracted(Response response) {
    if (response instanceof OutboundJaxrsResponse outbound) {
      return (String) outbound.getContext().getEntity();
    }
    fail("expected an outbound response but got %s".formatted(response.getClass().getSimpleName()));
    return null;
  }
}
