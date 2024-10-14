package org.opentripplanner.ext.geocoder;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.stopconsolidation.internal.DefaultStopConsolidationRepository;
import org.opentripplanner.ext.stopconsolidation.internal.DefaultStopConsolidationService;
import org.opentripplanner.ext.stopconsolidation.model.ConsolidatedStopGroup;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.StopModel;
import org.opentripplanner.transit.service.TransitModel;

class StopClusterMapperTest {

  private static final TransitModelForTest TEST_MODEL = TransitModelForTest.of();
  private static final RegularStop STOP_A = TEST_MODEL.stop("A").build();
  private static final RegularStop STOP_B = TEST_MODEL.stop("B").build();
  private static final RegularStop STOP_C = TEST_MODEL.stop("C").build();
  private static final List<RegularStop> STOPS = List.of(STOP_A, STOP_B, STOP_C);
  private static final StopModel STOP_MODEL = TEST_MODEL
    .stopModelBuilder()
    .withRegularStops(STOPS)
    .build();
  private static final TransitModel TRANSIT_MODEL = new TransitModel(
    STOP_MODEL,
    new Deduplicator()
  );
  private static final List<StopLocation> LOCATIONS = STOPS
    .stream()
    .map(StopLocation.class::cast)
    .toList();

  @Test
  void clusterConsolidatedStops() {
    var repo = new DefaultStopConsolidationRepository();
    repo.addGroups(List.of(new ConsolidatedStopGroup(STOP_A.getId(), List.of(STOP_B.getId()))));

    var service = new DefaultStopConsolidationService(repo, TRANSIT_MODEL);
    var mapper = new StopClusterMapper(new DefaultTransitService(TRANSIT_MODEL), service);

    var clusters = mapper.generateStopClusters(LOCATIONS, List.of());

    var expected = new LuceneStopCluster(
      STOP_A.getId().toString(),
      List.of(STOP_B.getId().toString()),
      List.of(STOP_A.getName(), STOP_B.getName()),
      List.of(STOP_A.getCode(), STOP_B.getCode()),
      new StopCluster.Coordinate(STOP_A.getLat(), STOP_A.getLon())
    );
    assertThat(clusters).contains(expected);
  }
}
