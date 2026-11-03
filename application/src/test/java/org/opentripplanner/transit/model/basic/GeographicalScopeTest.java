package org.opentripplanner.transit.model.basic;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.transit.model.basic.GeographicalScope.LONG_DISTANCE;
import static org.opentripplanner.transit.model.basic.GeographicalScope.REGIONAL;
import static org.opentripplanner.transit.model.basic.GeographicalScope.SUBURBAN;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.opentripplanner.gtfs.mapping.TransitModeMapper;
import org.opentripplanner.transit.service.ReplacementHelper;
import org.rutebanken.netex.model.AirSubmodeEnumeration;
import org.rutebanken.netex.model.BusSubmodeEnumeration;
import org.rutebanken.netex.model.CoachSubmodeEnumeration;
import org.rutebanken.netex.model.FunicularSubmodeEnumeration;
import org.rutebanken.netex.model.MetroSubmodeEnumeration;
import org.rutebanken.netex.model.RailSubmodeEnumeration;
import org.rutebanken.netex.model.SnowAndIceSubmodeEnumeration;
import org.rutebanken.netex.model.TaxiSubmodeEnumeration;
import org.rutebanken.netex.model.TelecabinSubmodeEnumeration;
import org.rutebanken.netex.model.TramSubmodeEnumeration;
import org.rutebanken.netex.model.WaterSubmodeEnumeration;

class GeographicalScopeTest {

  @ParameterizedTest
  @CsvSource({
    "101, LONG_DISTANCE",
    "102, LONG_DISTANCE",
    "103, REGIONAL",
    "105, LONG_DISTANCE",
    "106, REGIONAL",
    "109, SUBURBAN",
    "114, LONG_DISTANCE",
    "201, LONG_DISTANCE",
    "202, LONG_DISTANCE",
    "204, REGIONAL",
    "208, SUBURBAN",
  })
  void ofGtfsExtendedType(int gtfsType, GeographicalScope expected) {
    var mode = TransitModeMapper.mapMode(gtfsType);
    assertThat(GeographicalScope.of(mode, SubMode.UNKNOWN, gtfsType)).isEqualTo(expected);
  }

  @ParameterizedTest
  @ValueSource(ints = { 2, 3, 100, 104, 107, 110, 200, 300, 400, 403, 404, 700, 714 })
  void ofUnclassifiedGtfsExtendedType(int gtfsType) {
    var mode = TransitModeMapper.mapMode(gtfsType);
    assertThat(GeographicalScope.of(mode, SubMode.UNKNOWN, gtfsType)).isNull();
  }

  @ParameterizedTest
  @CsvSource({
    "highSpeedRail, RAIL, LONG_DISTANCE",
    "longDistance, RAIL, LONG_DISTANCE",
    "international, RAIL, LONG_DISTANCE",
    "sleeperRailService, RAIL, LONG_DISTANCE",
    "nightRail, RAIL, LONG_DISTANCE",
    "crossCountryRail, RAIL, LONG_DISTANCE",
    "interregionalRail, RAIL, REGIONAL",
    "regionalRail, RAIL, REGIONAL",
    "suburbanRailway, RAIL, SUBURBAN",
    "internationalCoach, COACH, LONG_DISTANCE",
    "nationalCoach, COACH, LONG_DISTANCE",
    "regionalCoach, COACH, REGIONAL",
    "commuterCoach, COACH, SUBURBAN",
  })
  void ofNetexSubmode(String submode, TransitMode mode, GeographicalScope expected) {
    assertThat(GeographicalScope.of(mode, SubMode.of(submode), null)).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({
    "unknown, RAIL",
    "local, RAIL",
    "replacementRailService, RAIL",
    "carTransportRailService, RAIL",
    "regionalBus, BUS",
  })
  void ofUnclassifiedNetexSubmode(String submode, TransitMode mode) {
    assertThat(GeographicalScope.of(mode, SubMode.of(submode), null)).isNull();
  }

  /**
   * A trip can inherit the submode or route type of its route while having another main mode, e.g.
   * a bus ServiceJourney without a submode on a rail line.
   */
  @Test
  void submodeOrRouteTypeOfOtherModeGivesNoScope() {
    assertThat(GeographicalScope.of(TransitMode.BUS, SubMode.of("regionalRail"), null)).isNull();
    assertThat(GeographicalScope.of(TransitMode.COACH, SubMode.of("regionalRail"), null)).isNull();
    assertThat(GeographicalScope.of(TransitMode.RAIL, SubMode.of("regionalCoach"), null)).isNull();
    assertThat(GeographicalScope.of(TransitMode.BUS, SubMode.UNKNOWN, 106)).isNull();
    assertThat(GeographicalScope.of(TransitMode.RAIL, SubMode.UNKNOWN, 204)).isNull();
  }

  @ParameterizedTest
  @ValueSource(ints = { 101, 106, 109 })
  void netexSubmodeTakesPrecedence(int gtfsType) {
    var rail = TransitMode.RAIL;
    assertThat(GeographicalScope.of(rail, SubMode.of("regionalRail"), gtfsType)).isEqualTo(
      REGIONAL
    );
    assertThat(GeographicalScope.of(rail, SubMode.of("suburbanRailway"), gtfsType)).isEqualTo(
      SUBURBAN
    );
    assertThat(GeographicalScope.of(rail, SubMode.of("longDistance"), gtfsType)).isEqualTo(
      LONG_DISTANCE
    );
  }

  /**
   * The API rejects scope filters on other modes and together with required replacement, as they
   * could never match. This guards that assumption for all GTFS route types, combined with any
   * main mode a trip could have.
   */
  @Test
  void gtfsScopeOnlyForDerivedModesAndNeverForReplacements() {
    for (var mode : TransitMode.values()) {
      IntStream.range(0, 1800)
        .filter(type -> GeographicalScope.of(mode, SubMode.UNKNOWN, type) != null)
        .forEach(type -> {
          assertThat(GeographicalScope.derivedForModes()).contains(mode);
          assertThat(TransitModeMapper.mapMode(type)).isEqualTo(mode);
          assertThat(ReplacementHelper.isReplacement(SubMode.UNKNOWN, type)).isFalse();
        });
    }
  }

  /**
   * Same as {@link #gtfsScopeOnlyForDerivedModesAndNeverForReplacements()} for all NeTEx
   * submodes. {@code submodeMode} is the main mode the NeTEx import assigns to the submode, but a
   * trip inheriting the submode from its route can have any main mode.
   */
  @ParameterizedTest
  @MethodSource("netexSubmodes")
  void netexScopeOnlyForDerivedModesAndNeverForReplacements(
    TransitMode submodeMode,
    String submode
  ) {
    for (var tripMode : TransitMode.values()) {
      if (GeographicalScope.of(tripMode, SubMode.of(submode), null) != null) {
        assertThat(tripMode).isEqualTo(submodeMode);
        assertThat(GeographicalScope.derivedForModes()).contains(tripMode);
        assertThat(ReplacementHelper.isReplacement(SubMode.of(submode), null)).isFalse();
      }
    }
  }

  private static Stream<Arguments> netexSubmodes() {
    return Stream.of(
      submodes(TransitMode.AIRPLANE, AirSubmodeEnumeration.values(), AirSubmodeEnumeration::value),
      submodes(TransitMode.BUS, BusSubmodeEnumeration.values(), BusSubmodeEnumeration::value),
      submodes(
        TransitMode.GONDOLA,
        TelecabinSubmodeEnumeration.values(),
        TelecabinSubmodeEnumeration::value
      ),
      submodes(TransitMode.COACH, CoachSubmodeEnumeration.values(), CoachSubmodeEnumeration::value),
      submodes(
        TransitMode.FUNICULAR,
        FunicularSubmodeEnumeration.values(),
        FunicularSubmodeEnumeration::value
      ),
      submodes(
        TransitMode.SUBWAY,
        MetroSubmodeEnumeration.values(),
        MetroSubmodeEnumeration::value
      ),
      submodes(TransitMode.RAIL, RailSubmodeEnumeration.values(), RailSubmodeEnumeration::value),
      submodes(TransitMode.TRAM, TramSubmodeEnumeration.values(), TramSubmodeEnumeration::value),
      submodes(TransitMode.FERRY, WaterSubmodeEnumeration.values(), WaterSubmodeEnumeration::value),
      submodes(TransitMode.TAXI, TaxiSubmodeEnumeration.values(), TaxiSubmodeEnumeration::value),
      submodes(
        TransitMode.SNOW_AND_ICE,
        SnowAndIceSubmodeEnumeration.values(),
        SnowAndIceSubmodeEnumeration::value
      )
    ).flatMap(Function.identity());
  }

  private static <T> Stream<Arguments> submodes(
    TransitMode mode,
    T[] values,
    Function<T, String> name
  ) {
    return Arrays.stream(values).map(value -> Arguments.of(mode, name.apply(value)));
  }
}
