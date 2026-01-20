package org.opentripplanner.apis.gtfs.mapping;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.apis.support.graphql.DataFetchingSupport;
import org.opentripplanner.transit.model.basic.TransitMode;

class CanceledTripsFilterMapperTest {

  @Test
  void testIncludeWithModes() {
    var mode = TransitMode.BUS;
    Map<String, Object> args = Map.of(
      "filters",
      Map.of("include", List.of(Map.of("modes", List.of(TransitModeMapper.map(mode)))))
    );
    var environment = getEnvironment(args);
    var request = CanceledTripsFilterMapper.mapToTripOnServiceDateRequest(environment);
    var modes = request.includeModes().get();
    assertThat(modes).hasSize(1);
    assertEquals(mode, modes.iterator().next());
    assertTrue(request.excludeModes().includeEverything());
  }

  @Test
  void testExcludeWithModes() {
    var mode = TransitMode.BUS;
    Map<String, Object> args = Map.of(
      "filters",
      Map.of("exclude", List.of(Map.of("modes", List.of(TransitModeMapper.map(mode)))))
    );
    var environment = getEnvironment(args);
    var request = CanceledTripsFilterMapper.mapToTripOnServiceDateRequest(environment);
    var modes = request.excludeModes().get();
    assertThat(modes).hasSize(1);
    assertEquals(mode, modes.iterator().next());
    assertTrue(request.includeModes().includeEverything());
  }

  @Test
  void testEmptyInclude() {
    Map<String, Object> args = Map.of("filters", Map.of("include", List.of()));
    var environment = getEnvironment(args);
    var exception = assertThrows(IllegalArgumentException.class, () ->
      CanceledTripsFilterMapper.mapToTripOnServiceDateRequest(environment)
    );
    assertEquals("'filters.include' must not be empty.", exception.getMessage());
  }

  @Test
  void testEmptyExclude() {
    Map<String, Object> args = Map.of("filters", Map.of("exclude", List.of()));
    var environment = getEnvironment(args);
    var exception = assertThrows(IllegalArgumentException.class, () ->
      CanceledTripsFilterMapper.mapToTripOnServiceDateRequest(environment)
    );
    assertEquals("'filters.exclude' must not be empty.", exception.getMessage());
  }

  @Test
  void testEmptyModes() {
    Map<String, Object> args = Map.of(
      "filters",
      Map.of("include", List.of(Map.of("modes", List.of())))
    );
    var environment = getEnvironment(args);
    var exception = assertThrows(IllegalArgumentException.class, () ->
      CanceledTripsFilterMapper.mapToTripOnServiceDateRequest(environment)
    );
    assertEquals(
      "Mode filter must be either null or have at least one entry.",
      exception.getMessage()
    );
  }

  private DataFetchingEnvironment getEnvironment(Map<String, Object> arguments) {
    var executionContext = DataFetchingSupport.executionContext();
    return DataFetchingEnvironmentImpl.newDataFetchingEnvironment(executionContext)
      .arguments(arguments)
      .build();
  }
}
