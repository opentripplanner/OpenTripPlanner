package org.opentripplanner.apis.gtfs.mapping;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.apis.support.InvalidInputException;
import org.opentripplanner.apis.support.graphql.DataFetchingSupport;
import org.opentripplanner.core.model.time.LocalDateRange;
import org.opentripplanner.transit.model.basic.MainAndSubMode;
import org.opentripplanner.transit.model.basic.TransitMode;

class CanceledTripsFilterMapperTest {

  @Test
  void testEmptyFilter() {
    Map<String, Object> args = Map.of("filters", List.of());
    var environment = getEnvironment(args);
    var request = CanceledTripsFilterMapper.mapToTripOnServiceDateRequest(environment);
    assertTrue(request.includeModes().includeEverything());
    assertTrue(request.excludeModes().includeEverything());
  }

  @Test
  void testNullFilter() {
    Map<String, Object> args = new HashMap<>();
    var environment = getEnvironment(args);
    var request = CanceledTripsFilterMapper.mapToTripOnServiceDateRequest(environment);
    assertTrue(request.includeModes().includeEverything());
    assertTrue(request.excludeModes().includeEverything());
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
      List.of(Map.of("exclude", List.of(Map.of("modes", List.of(TransitModeMapper.map(mode))))))
    );
    var environment = getEnvironment(args);
    var request = CanceledTripsFilterMapper.mapToTripOnServiceDateRequest(environment);
    var modes = request.excludeModes().get();
    assertThat(modes).hasSize(1);
    assertEquals(mode, modes.iterator().next());
    assertTrue(request.includeModes().includeEverything());
  }

  @Test
  void testMultipleFilters() {
    var bus = TransitMode.BUS;
    var tram = TransitMode.TRAM;
    Map<String, Object> args = Map.of(
      "filters",
      List.of(
        Map.of("include", List.of(Map.of("modes", List.of(TransitModeMapper.map(bus))))),
        Map.of("include", List.of(Map.of("modes", List.of(TransitModeMapper.map(tram)))))
      )
    );

    var request = CanceledTripsFilterMapper.mapToTripOnServiceDateRequest(getEnvironment(args));
    assertThat(request.filters()).hasSize(2);
    var firstFilterModes = request.filters().get(0).select().getFirst().transportModes().get();
    var secondFilterModes = request.filters().get(1).select().getFirst().transportModes().get();

    assertThat(firstFilterModes).containsExactlyElementsIn(
      MainAndSubMode.ofTransitModes(Set.of(bus))
    );
    assertThat(secondFilterModes).containsExactlyElementsIn(
      MainAndSubMode.ofTransitModes(Set.of(tram))
    );
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

  @Test
  void testIncludeWithServiceDateRanges() {
    var start = LocalDate.parse("2026-06-01");
    var end = LocalDate.parse("2026-06-10");
    Map<String, Object> args = Map.of(
      "filters",
      List.of(
        Map.of(
          "include",
          List.of(Map.of("serviceDateRanges", List.of(Map.of("start", start, "end", end))))
        )
      )
    );

    var request = CanceledTripsFilterMapper.mapToTripOnServiceDateRequest(getEnvironment(args));
    assertThat(request.filters()).hasSize(1);
    var include = request.filters().getFirst().select().getFirst();
    assertThat(include.serviceDateRanges().get()).hasSize(1);
    var includeRange = include.serviceDateRanges().get().iterator().next();
    assertEquals(start, includeRange.getStartInclusive());
    assertEquals(end, includeRange.getEndExclusive());
  }

  @Test
  void testExcludeWithServiceDateRanges() {
    var start = LocalDate.parse("2026-07-01");
    Map<String, Object> args = Map.of(
      "filters",
      List.of(
        Map.of("exclude", List.of(Map.of("serviceDateRanges", List.of(Map.of("start", start)))))
      )
    );

    var request = CanceledTripsFilterMapper.mapToTripOnServiceDateRequest(getEnvironment(args));
    assertThat(request.filters()).hasSize(1);
    var exclude = request.filters().getFirst().not().getFirst();
    assertThat(exclude.serviceDateRanges().get()).hasSize(1);
    var excludeRange = exclude.serviceDateRanges().get().iterator().next();
    assertEquals(start, excludeRange.getStartInclusive());
    var unboundedRange = LocalDateRange.ofUnbounded();
    assertEquals(unboundedRange.getEndExclusive(), excludeRange.getEndExclusive());
  }

  @Test
  void testEmptyServiceDateRanges() {
    Map<String, Object> args = Map.of(
      "filters",
      List.of(Map.of("include", List.of(Map.of("serviceDateRanges", List.of()))))
    );
    var environment = getEnvironment(args);
    var exception = assertThrows(InvalidInputException.class, () ->
      CanceledTripsFilterMapper.mapToTripOnServiceDateRequest(environment)
    );
    assertEquals(
      "Service date range filter must be either null or have at least one entry.",
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
