package org.opentripplanner.apis.transmodel.model.stop;

import static com.google.common.truth.Truth.assertThat;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.api.model.transit.DefaultFeedIdMapper;
import org.opentripplanner.apis.support.graphql.OtpDataFetcherExceptionHandler;
import org.opentripplanner.apis.support.graphql.injectdoc.ApiDocumentationProfile;
import org.opentripplanner.apis.transmodel.TransmodelAPITestContextBuilder;
import org.opentripplanner.apis.transmodel.TransmodelGraphQLSchemaFactory;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitTuningParametersTestFactory;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.transit.model.TransitTestEnvironment;

class StopPlaceTypeTest {

  private static final GraphQL GRAPHQL = GraphQL.newGraphQL(
    new TransmodelGraphQLSchemaFactory(
      RouteRequest.defaultValue(),
      ZoneIds.OSLO,
      TransitTuningParametersTestFactory.forTest(),
      new DefaultFeedIdMapper(),
      ApiDocumentationProfile.DEFAULT
    ).create()
  )
    .defaultDataFetcherExceptionHandler(new OtpDataFetcherExceptionHandler())
    .build();

  /**
   * An explicit {@code startTime: null} must be treated as "now", like an omitted argument.
   */
  @Test
  void estimatedCallsAcceptsExplicitNullStartTime() {
    var builder = TransitTestEnvironment.of(LocalDate.of(2024, 5, 7), ZoneIds.OSLO);
    builder.stopAtStation("A", "S");
    var context = TransmodelAPITestContextBuilder.of(builder.build()).build();

    ExecutionResult result = GRAPHQL.execute(
      ExecutionInput.newExecutionInput()
        .query("{ stopPlace(id: \"F:S\") { estimatedCalls(startTime: null) { realtime } } }")
        .context(context)
        .build()
    );

    assertThat(result.getErrors()).isEmpty();
  }

  /**
   * A negative or explicit null {@code numberOfDepartures} is a client error, for both the stop
   * place and the quay, and must not surface as an internal exception.
   */
  @ParameterizedTest
  @ValueSource(
    strings = {
      "stopPlace(id: \"F:S\") { estimatedCalls(numberOfDepartures: -1) { realtime } }",
      "stopPlace(id: \"F:S\") { estimatedCalls(numberOfDepartures: null) { realtime } }",
      "quay(id: \"F:A\") { estimatedCalls(numberOfDepartures: -1) { realtime } }",
      "quay(id: \"F:A\") { estimatedCalls(numberOfDepartures: null) { realtime } }",
    }
  )
  void estimatedCallsRejectsInvalidNumberOfDepartures(String query) {
    var builder = TransitTestEnvironment.of(LocalDate.of(2024, 5, 7), ZoneIds.OSLO);
    builder.stopAtStation("A", "S");
    var context = TransmodelAPITestContextBuilder.of(builder.build()).build();

    ExecutionResult result = GRAPHQL.execute(
      ExecutionInput.newExecutionInput()
        .query("{ " + query + " }")
        .context(context)
        .build()
    );

    assertThat(result.getErrors()).hasSize(1);
    var error = result.getErrors().getFirst();
    assertThat(error.getErrorType().toString()).isEqualTo("BadRequestError");
    assertThat(error.getMessage()).startsWith(
      "The argument 'numberOfDepartures' should be a non-null positive value:"
    );
  }
}
