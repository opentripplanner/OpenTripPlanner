package org.opentripplanner.apis.transmodel.mapping;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.apis.transmodel._support.RequestHelper.list;
import static org.opentripplanner.apis.transmodel._support.RequestHelper.map;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.api.model.transit.DefaultFeedIdMapper;
import org.opentripplanner.apis.transmodel.model.TransmodelTransportSubmode;
import org.opentripplanner.apis.transmodel.support.DataFetcherDecorator;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.request.TransitRequest;
import org.opentripplanner.routing.api.request.request.TransitRequestBuilder;
import org.opentripplanner.transit.model.basic.TransitMode;

class TransitFilterOldWayMapperTest {

  private static final TransitFilterOldWayMapper MAPPER = new TransitFilterOldWayMapper(
    new DefaultFeedIdMapper()
  );

  private TransitRequestBuilder transitBuilder = TransitRequest.of();

  @Test
  void mapEmptyFilter() {
    var env = envOf(Map.of());

    // If nothing is passed in the mapper will do nothing
    MAPPER.mapFilter(env, new DataFetcherDecorator(env), transitBuilder);

    assertEquals("()", transitBuilder.build().toString());
  }

  @Test
  void mapFilterEmptyWhiteListed() {
    // setting one of the old filters will triger the old filter mapping
    var env = envOf(map(entry("whiteListed", Map.of())));

    MAPPER.mapFilter(env, new DataFetcherDecorator(env), transitBuilder);

    assertEquals(
      "(filters: [(select: [(transportModes: ALL)])])",
      transitBuilder.build().toString()
    );
  }

  @Test
  void mapFilterEmptyBanned() {
    // setting one of the old filters will triger the old filter mapping
    var env = envOf(map(entry("banned", Map.of())));

    MAPPER.mapFilter(env, new DataFetcherDecorator(env), transitBuilder);

    assertEquals(
      "(filters: [(select: [(transportModes: ALL)])])",
      transitBuilder.build().toString()
    );
  }

  @Test
  void mapFilterWhiteListedAuthoritiesAndLines() {
    var env = envOf(
      map(entry("whiteListed", map(entry("authorities", list("A:1")), entry("lines", list("L:1")))))
    );

    MAPPER.mapFilter(env, new DataFetcherDecorator(env), transitBuilder);

    assertEquals(
      "(filters: [(select: [(transportModes: ALL, agencies: [A:1]), (transportModes: ALL, routes: [L:1])])])",
      transitBuilder.build().toString()
    );
  }

  @Test
  void mapFilterBannedAuthoritiesAndLines() {
    var env = envOf(
      map(entry("banned", map(entry("authorities", list("A:1")), entry("lines", list("L:1")))))
    );

    MAPPER.mapFilter(env, new DataFetcherDecorator(env), transitBuilder);

    assertEquals(
      "(filters: [(select: [(transportModes: ALL)], not: [(transportModes: EMPTY, agencies: [A:1]), (transportModes: EMPTY, routes: [L:1])])])",
      transitBuilder.build().toString()
    );
  }

  @Test
  void mapFilterIncludeEverythingIfModeIsNotSet() {
    var env = envOf(
      map(
        entry("modes", Map.of()),
        // This is ignored:
        entry("banned", map(entry("lines", list("L:1"))))
      )
    );

    MAPPER.mapFilter(env, new DataFetcherDecorator(env), transitBuilder);

    assertEquals(
      "(filters: [(select: [(transportModes: ALL)], not: [(transportModes: EMPTY, routes: [L:1])])])",
      transitBuilder.build().toString()
    );
  }

  @Test
  void mapFilterSkipTransitIfTransportModesIsEmpty() {
    var env = envOf(
      map(
        entry("modes", map("transportModes", list())),
        // This is ignored:
        entry("banned", map(entry("lines", list("L:1"))))
      )
    );

    MAPPER.mapFilter(env, new DataFetcherDecorator(env), transitBuilder);

    assertEquals("(filters: [ExcludeAllTransitFilter])", transitBuilder.build().toString());
  }

  @Test
  void mapFilterSelectTransportModes() {
    var env = envOf(
      map(
        entry(
          "modes",
          map(
            entry(
              "transportModes",
              list(
                map("transportMode", TransitMode.AIRPLANE),
                map("transportMode", TransitMode.COACH),
                map("transportMode", TransitMode.CABLE_CAR)
              )
            ),
            // Ignored by subject
            entry("accessMode", StreetMode.BIKE)
          )
        )
      )
    );

    MAPPER.mapFilter(env, new DataFetcherDecorator(env), transitBuilder);

    assertEquals(
      "(filters: [(select: [(transportModes: [AIRPLANE, CABLE_CAR, COACH])])])",
      transitBuilder.build().toString()
    );
  }

  @Test
  void mapFilterSelectTransportModesWithSubmodes() {
    var env = envOf(
      map(
        entry(
          "modes",
          map(
            entry(
              "transportModes",
              list(
                map(
                  entry("transportMode", TransitMode.RAIL),
                  entry(
                    "transportSubModes",
                    list(TransmodelTransportSubmode.LOCAL, TransmodelTransportSubmode.REGIONAL_RAIL)
                  )
                )
              )
            ),
            // Ignored by subject
            entry("accessMode", StreetMode.BIKE)
          )
        )
      )
    );

    MAPPER.mapFilter(env, new DataFetcherDecorator(env), transitBuilder);

    assertEquals(
      "(filters: [(select: [(transportModes: [RAIL::local, RAIL::regionalRail])])])",
      transitBuilder.build().toString()
    );
  }

  private static DataFetchingEnvironment envOf(Map<String, Object> arguments) {
    return DataFetchingEnvironmentImpl.newDataFetchingEnvironment().arguments(arguments).build();
  }
}
