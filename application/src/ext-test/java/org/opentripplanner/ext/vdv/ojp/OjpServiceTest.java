package org.opentripplanner.ext.vdv.ojp;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;
import static org.opentripplanner.transit.model.basic.TransitMode.BUS;
import static org.opentripplanner.transit.model.basic.TransitMode.FERRY;

import de.vdv.ojp20.LineDirectionFilterStructure;
import de.vdv.ojp20.ModeFilterStructure;
import de.vdv.ojp20.OJPStopEventRequestStructure;
import de.vdv.ojp20.PlaceContextStructure;
import de.vdv.ojp20.StopEventParamStructure;
import de.vdv.ojp20.siri.LineDirectionStructure;
import de.vdv.ojp20.siri.LineRefStructure;
import de.vdv.ojp20.siri.VehicleModesOfTransportEnumeration;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.ext.vdv.id.UseFeedIdResolver;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.rutebanken.time.XmlDateTime;

class OjpServiceTest {

  private static final ZonedDateTime ZDT = ZonedDateTime.parse("2025-02-17T14:24:02+01:00");
  private static final UseFeedIdResolver ID_RESOLVER = new UseFeedIdResolver();
  private static final OjpService SERVICE = new OjpService(
    null,
    ID_RESOLVER,
    new StopEventResponseMapper(ZoneIds.BERLIN, ID_RESOLVER, l -> Optional.of("de")),
    ZoneIds.BERLIN
  );

  private static final FeedScopedId LINE_ID = id("line1");
  private static final FeedScopedId AGENCY_ID = id("line1");

  @Test
  void defaultCase() {
    var params = SERVICE.extractStopEventParams(stopEvent(new StopEventParamStructure()));
    assertThat(params.includedAgencies()).isEmpty();
    assertThat(params.excludedAgencies()).isEmpty();
    assertThat(params.includedRoutes()).isEmpty();
    assertThat(params.excludedAgencies()).isEmpty();
    assertThat(params.includedModes()).isEmpty();
    assertThat(params.excludedModes()).isEmpty();
  }

  @Test
  void lineFilterImplicitExclude() {
    var params = SERVICE.extractStopEventParams(
      lineFilter(
        new LineDirectionFilterStructure()
          .withLine(
            new LineDirectionStructure()
              .withLineRef(new LineRefStructure().withValue(LINE_ID.toString()))
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
    var params = SERVICE.extractStopEventParams(
      lineFilter(
        new LineDirectionFilterStructure()
          .withExclude(true)
          .withLine(
            new LineDirectionStructure()
              .withLineRef(new LineRefStructure().withValue(LINE_ID.toString()))
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
    var params = SERVICE.extractStopEventParams(
      lineFilter(
        new LineDirectionFilterStructure()
          .withExclude(false)
          .withLine(
            new LineDirectionStructure()
              .withLineRef(new LineRefStructure().withValue(LINE_ID.toString()))
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
    var params = SERVICE.extractStopEventParams(
      stopEvent(
        new StopEventParamStructure()
          .withModeFilter(
            new ModeFilterStructure()
              .withPtMode(
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
    var params = SERVICE.extractStopEventParams(
      stopEvent(
        new StopEventParamStructure()
          .withModeFilter(
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

  private static OJPStopEventRequestStructure lineFilter(LineDirectionFilterStructure value) {
    return stopEvent(new StopEventParamStructure().withLineFilter(value));
  }

  private static OJPStopEventRequestStructure stopEvent(StopEventParamStructure p) {
    return new OJPStopEventRequestStructure()
      .withLocation(new PlaceContextStructure().withDepArrTime(new XmlDateTime(ZDT)))
      .withParams(p);
  }
}
