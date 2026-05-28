package org.opentripplanner.apis.gtfs.mapping;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.apis.support.InvalidInputException;
import org.opentripplanner.apis.support.graphql.DataFetchingSupport;
import org.opentripplanner.transit.model.basic.MainAndSubMode;
import org.opentripplanner.transit.model.basic.TransitMode;

class CanceledTripsFilterMapperTest {

  @Test
  void testEmptyFilter() {
    Map<String, Object> args = Map.of("filters", List.of());
    var environment = getEnvironment(args);
    var request = CanceledTripsFilterMapper.mapToTripOnServiceDateRequest(environment);
    assertThat(request.filters()).isEmpty();
  }

  @Test
  void testNullFilter() {
    Map<String, Object> args = new HashMap<>();
    var environment = getEnvironment(args);
    var request = CanceledTripsFilterMapper.mapToTripOnServiceDateRequest(environment);
    assertThat(request.filters()).isEmpty();
  }

  @Test
  void testNullExclude() {
    // When no exclude is given, exclude will be null, which should not impact the filtering
    var mode = TransitMode.BUS;
    Map<String, Object> args = Map.of(
      "filters",
      List.of(Map.of("include", List.of(Map.of("modes", List.of(TransitModeMapper.map(mode))))))
    );
    var environment = getEnvironment(args);
    var request = CanceledTripsFilterMapper.mapToTripOnServiceDateRequest(environment);
    assertThat(request.filters()).hasSize(1);
    assertNull(request.filters().getFirst().not());
  }

  @Test
  void testNullInclude() {
    // When no include is given, include will be null which should not impact the filtering
    var mode = TransitMode.BUS;
    Map<String, Object> args = Map.of(
      "filters",
      List.of(Map.of("exclude", List.of(Map.of("modes", List.of(TransitModeMapper.map(mode))))))
    );
    var environment = getEnvironment(args);
    var request = CanceledTripsFilterMapper.mapToTripOnServiceDateRequest(environment);
    assertThat(request.filters()).hasSize(1);
    assertNull(request.filters().getFirst().select());
  }

  @Test
  void testIncludeWithModes() {
    var mode = TransitMode.BUS;
    Map<String, Object> args = Map.of(
      "filters",
      List.of(Map.of("include", List.of(Map.of("modes", List.of(TransitModeMapper.map(mode))))))
    );
    var environment = getEnvironment(args);
    var request = CanceledTripsFilterMapper.mapToTripOnServiceDateRequest(environment);
    assertThat(request.filters()).hasSize(1);
    var selectModes = request.filters().getFirst().select().getFirst().transportModes().get();
    assertThat(selectModes).containsExactly(new MainAndSubMode(mode));
    assertNull(request.filters().getFirst().not());
  }

  @Test
  void testExcludeWithModes() {
    var mode = TransitMode.BUS;
    Map<String, Object> args = Map.of(
      "filters",
      List.of(Map.of("exclude", List.of(Map.of("modes", List.of(TransitModeMapper.map(mode))))))
    );
    var environment = getEnvironment(args);
    var request = CanceledTripsFilterMapper.mapToTripOnServiceDateRequest(environment);
    assertThat(request.filters()).hasSize(1);
    var notModes = request.filters().getFirst().not().getFirst().transportModes().get();
    assertThat(notModes).containsExactly(new MainAndSubMode(mode));
    assertNull(request.filters().getFirst().select());
  }

  @Test
  void testMultipleFilters() {
    Map<String, Object> args = Map.of(
      "filters",
      List.of(Map.of("include", List.of()), Map.of("include", List.of()))
    );
    var environment = getEnvironment(args);
    var exception = assertThrows(InvalidInputException.class, () ->
      CanceledTripsFilterMapper.mapToTripOnServiceDateRequest(environment)
    );
    assertEquals("Only one filter is allowed for now.", exception.getMessage());
  }

  @Test
  void testEmptyInclude() {
    Map<String, Object> args = Map.of("filters", List.of(Map.of("include", List.of())));
    var environment = getEnvironment(args);
    var exception = assertThrows(IllegalArgumentException.class, () ->
      CanceledTripsFilterMapper.mapToTripOnServiceDateRequest(environment)
    );
    assertEquals("'filters.include' must not be empty.", exception.getMessage());
  }

  @Test
  void testEmptyExclude() {
    Map<String, Object> args = Map.of("filters", List.of(Map.of("exclude", List.of())));
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
      List.of(Map.of("include", List.of(Map.of("modes", List.of()))))
    );
    var environment = getEnvironment(args);
    var exception = assertThrows(InvalidInputException.class, () ->
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
