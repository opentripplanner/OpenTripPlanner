package org.opentripplanner.ext.ojp.service;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;
import static org.opentripplanner.ext.ojp.mapping.StopEventResponseMapper.OptionalFeature.ONWARD_CALLS;
import static org.opentripplanner.ext.ojp.mapping.StopEventResponseMapper.OptionalFeature.PREVIOUS_CALLS;
import static org.opentripplanner.ext.ojp.mapping.StopEventResponseMapper.OptionalFeature.REALTIME_DATA;
import static org.opentripplanner.transit.model.basic.TransitMode.BUS;
import static org.opentripplanner.transit.model.basic.TransitMode.FERRY;

import de.vdv.ojp20.IndividualTransportOptionStructure;
import de.vdv.ojp20.ItModesStructure;
import de.vdv.ojp20.LineDirectionFilterStructure;
import de.vdv.ojp20.ModeFilterStructure;
import de.vdv.ojp20.OJPStopEventRequestStructure;
import de.vdv.ojp20.PersonalModesEnumeration;
import de.vdv.ojp20.PlaceContextStructure;
import de.vdv.ojp20.StopEventParamStructure;
import de.vdv.ojp20.UseRealtimeDataEnumeration;
import de.vdv.ojp20.siri.LineDirectionStructure;
import de.vdv.ojp20.siri.LineRefStructure;
import de.vdv.ojp20.siri.VehicleModesOfTransportEnumeration;
import java.time.ZonedDateTime;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.api.model.transit.DefaultFeedIdMapper;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.ojp.mapping.StopEventParamsMapper;
import org.opentripplanner.ojp.time.XmlDateTime;

class StopEventParamsMapperTest {

  private static final ZonedDateTime ZDT = ZonedDateTime.parse("2025-02-17T14:24:02+01:00");
  private static final DefaultFeedIdMapper FEED_ID_MAPPER = new DefaultFeedIdMapper();
  private static final StopEventParamsMapper MAPPER = new StopEventParamsMapper(
    ZoneIds.BERLIN,
    FEED_ID_MAPPER
  );

  private static final FeedScopedId LINE_ID = id("line1");

  @Test
  void defaultCase() {
    var params = MAPPER.extractStopEventParams(stopEvent(new StopEventParamStructure()));
    assertThat(params.includedAgencies()).isEmpty();
    assertThat(params.excludedAgencies()).isEmpty();
    assertThat(params.includedRoutes()).isEmpty();
    assertThat(params.excludedAgencies()).isEmpty();
    assertThat(params.includedModes()).isEmpty();
    assertThat(params.excludedModes()).isEmpty();
    assertEquals(StopEventParamsMapper.DEFAULT_RADIUS_METERS, params.maximumWalkDistance());
    assertEquals(StopEventParamsMapper.DEFAULT_NUM_DEPARTURES, params.numDepartures());
  }

  @Test
  void maxDistance() {
    var params = MAPPER.extractStopEventParams(
      new OJPStopEventRequestStructure().withLocation(
        new PlaceContextStructure()
          .withDepArrTime(new XmlDateTime(ZDT))
          .withIndividualTransportOption(
            new IndividualTransportOptionStructure()
              .withItModeAndModeOfOperation(
                new ItModesStructure().withPersonalMode(PersonalModesEnumeration.FOOT)
              )
              .withMaxDistance(10)
          )
      )
    );
    assertEquals(10, params.maximumWalkDistance());
  }

  @Test
  void numDepartures() {
    var params = MAPPER.extractStopEventParams(
      stopEvent(new StopEventParamStructure().withNumberOfResults(2))
    );
    assertEquals(2, params.numDepartures());
  }

  @Test
  void lineFilterImplicitExclude() {
    var params = MAPPER.extractStopEventParams(
      lineFilter(
        new LineDirectionFilterStructure().withLine(
          new LineDirectionStructure().withLineRef(
            new LineRefStructure().withValue(LINE_ID.toString())
          )
        )
      )
    );
    assertThat(params.includedAgencies()).isEmpty();
    assertThat(params.excludedAgencies()).isEmpty();
    assertThat(params.includedRoutes()).isEmpty();
    assertEquals(Set.of(LINE_ID), params.excludedRoutes());
    assertThat(params.includedModes()).isEmpty();
    assertThat(params.excludedModes()).isEmpty();
  }

  @Test
  void lineFilterInclude() {
    var params = MAPPER.extractStopEventParams(
      lineFilter(
        new LineDirectionFilterStructure()
          .withExclude(true)
          .withLine(
            new LineDirectionStructure().withLineRef(
              new LineRefStructure().withValue(LINE_ID.toString())
            )
          )
      )
    );
    assertThat(params.includedAgencies()).isEmpty();
    assertThat(params.excludedAgencies()).isEmpty();
    assertThat(params.includedRoutes()).isEmpty();
    assertEquals(Set.of(LINE_ID), params.excludedRoutes());
    assertThat(params.includedModes()).isEmpty();
    assertThat(params.excludedModes()).isEmpty();
  }

  @Test
  void lineFilterExclude() {
    var params = MAPPER.extractStopEventParams(
      lineFilter(
        new LineDirectionFilterStructure()
          .withExclude(false)
          .withLine(
            new LineDirectionStructure().withLineRef(
              new LineRefStructure().withValue(LINE_ID.toString())
            )
          )
      )
    );
    assertThat(params.includedAgencies()).isEmpty();
    assertThat(params.excludedAgencies()).isEmpty();
    assertEquals(Set.of(LINE_ID), params.includedRoutes());
    assertThat(params.excludedRoutes()).isEmpty();
    assertThat(params.includedModes()).isEmpty();
    assertThat(params.excludedModes()).isEmpty();
  }

  @Test
  void modeFilter() {
    var params = MAPPER.extractStopEventParams(
      stopEvent(
        new StopEventParamStructure().withModeFilter(
          new ModeFilterStructure().withPtMode(
            VehicleModesOfTransportEnumeration.BUS,
            VehicleModesOfTransportEnumeration.FERRY
          )
        )
      )
    );
    assertThat(params.includedAgencies()).isEmpty();
    assertThat(params.excludedAgencies()).isEmpty();
    assertThat(params.includedRoutes()).isEmpty();
    assertThat(params.excludedRoutes()).isEmpty();
    assertThat(params.includedModes()).isEmpty();
    assertEquals(Set.of(BUS, FERRY), params.excludedModes());
  }

  @Test
  void modeFilterExclude() {
    var params = MAPPER.extractStopEventParams(
      stopEvent(
        new StopEventParamStructure().withModeFilter(
          new ModeFilterStructure()
            .withExclude(false)
            .withPtMode(VehicleModesOfTransportEnumeration.BUS)
        )
      )
    );
    assertThat(params.includedAgencies()).isEmpty();
    assertThat(params.excludedAgencies()).isEmpty();
    assertThat(params.includedRoutes()).isEmpty();
    assertThat(params.excludedRoutes()).isEmpty();
    assertEquals(Set.of(BUS), params.includedModes());
    assertThat(params.excludedModes()).isEmpty();
  }

  @Test
  void optionalFeaturesDefault() {
    var params = MAPPER.extractStopEventParams(stopEvent(new StopEventParamStructure()));
    assertThat(params.optionalFeatures()).containsExactly(REALTIME_DATA);
  }

  @Test
  void optionalFeaturesWithoutParams() {
    var ser = new OJPStopEventRequestStructure().withLocation(
      new PlaceContextStructure().withDepArrTime(new XmlDateTime(ZDT))
    );
    var params = MAPPER.extractStopEventParams(ser);
    assertThat(params.optionalFeatures()).containsExactly(REALTIME_DATA);
  }

  @Test
  void optionalFeaturesPreviousCalls() {
    var params = MAPPER.extractStopEventParams(
      stopEvent(
        new StopEventParamStructure()
          .withIncludePreviousCalls(true)
          .withUseRealtimeData(UseRealtimeDataEnumeration.NONE)
      )
    );
    assertThat(params.optionalFeatures()).containsExactly(PREVIOUS_CALLS);
  }

  @Test
  void optionalFeaturesOnwardCalls() {
    var params = MAPPER.extractStopEventParams(
      stopEvent(
        new StopEventParamStructure()
          .withIncludeOnwardCalls(true)
          .withUseRealtimeData(UseRealtimeDataEnumeration.NONE)
      )
    );
    assertThat(params.optionalFeatures()).containsExactly(ONWARD_CALLS);
  }

  @Test
  void optionalFeaturesAll() {
    var params = MAPPER.extractStopEventParams(
      stopEvent(
        new StopEventParamStructure()
          .withIncludePreviousCalls(true)
          .withIncludeOnwardCalls(true)
          .withUseRealtimeData(UseRealtimeDataEnumeration.FULL)
      )
    );
    assertThat(params.optionalFeatures()).containsExactly(
      PREVIOUS_CALLS,
      ONWARD_CALLS,
      REALTIME_DATA
    );
  }

  @Test
  void optionalFeaturesExplicitlyDisabled() {
    var params = MAPPER.extractStopEventParams(
      stopEvent(
        new StopEventParamStructure()
          .withIncludePreviousCalls(false)
          .withIncludeOnwardCalls(false)
          .withUseRealtimeData(UseRealtimeDataEnumeration.NONE)
      )
    );
    assertThat(params.optionalFeatures()).isEmpty();
  }

  /**
   * When a depArrTime is not specified, the current time should be used.
   */
  @Test
  void noDateTime() {
    var ser = new OJPStopEventRequestStructure().withLocation(new PlaceContextStructure());

    var params = MAPPER.extractStopEventParams(ser);
    assertNotNull(params.time());
  }

  private static OJPStopEventRequestStructure lineFilter(LineDirectionFilterStructure value) {
    return stopEvent(new StopEventParamStructure().withLineFilter(value));
  }

  private static OJPStopEventRequestStructure stopEvent(StopEventParamStructure p) {
    return new OJPStopEventRequestStructure()
      .withLocation(new PlaceContextStructure().withDepArrTime(new XmlDateTime(ZDT)))
      .withParams(p);
  }
}
