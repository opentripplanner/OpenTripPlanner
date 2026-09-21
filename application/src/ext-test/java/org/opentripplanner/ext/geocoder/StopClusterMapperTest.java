package org.opentripplanner.ext.geocoder;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.stopconsolidation.internal.DefaultStopConsolidationRepository;
import org.opentripplanner.ext.stopconsolidation.internal.DefaultStopConsolidationService;
import org.opentripplanner.ext.stopconsolidation.model.ConsolidatedStopGroup;
import org.opentripplanner.transit.model._data.TransitRepositoryForTest;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.SiteRepository;
import org.opentripplanner.transit.service.TransitRepository;

class StopClusterMapperTest {

  private static final TransitRepositoryForTest TEST_MODEL = TransitRepositoryForTest.of();
  private static final RegularStop STOP_A = TEST_MODEL.stop("A").build();
  private static final RegularStop STOP_B = TEST_MODEL.stop("B").build();
  private static final RegularStop STOP_C = TEST_MODEL.stop("C").build();
  private static final List<RegularStop> STOPS = List.of(STOP_A, STOP_B, STOP_C);
  private static final SiteRepository SITE_REPOSITORY = TEST_MODEL.siteRepositoryBuilder()
    .withRegularStops(STOPS)
    .build();
  private static final TransitRepository TIMETABLE_REPOSITORY = new TransitRepository(
    SITE_REPOSITORY
  );
  private static final List<StopLocation> LOCATIONS = STOPS.stream()
    .map(StopLocation.class::cast)
    .toList();

  @Test
  void clusterConsolidatedStops() {
    var repo = new DefaultStopConsolidationRepository();
    repo.addGroups(List.of(new ConsolidatedStopGroup(STOP_A.getId(), List.of(STOP_B.getId()))));

    var mapper = buildMapper(repo);

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

  @Test
  void noConsolidatedStops() {
    var repo = new DefaultStopConsolidationRepository();

    var mapper = buildMapper(repo);

    var primaries = Iterables.transform(mapper.generateStopClusters(LOCATIONS, List.of()), c ->
      FeedScopedId.parseStrict(c.primaryId())
    );

    assertThat(primaries).containsExactly(STOP_A.getId(), STOP_B.getId(), STOP_C.getId());
  }

  @Test
  void unknownSecondaries() {
    var repo = new DefaultStopConsolidationRepository();
    repo.addGroups(List.of(new ConsolidatedStopGroup(STOP_A.getId(), List.of(id("unknown")))));

    var mapper = buildMapper(repo);

    var primaries = Iterables.transform(mapper.generateStopClusters(LOCATIONS, List.of()), c ->
      FeedScopedId.parseStrict(c.primaryId())
    );
    assertThat(primaries).containsExactly(STOP_A.getId(), STOP_B.getId(), STOP_C.getId());

    var secondaries = ImmutableList.copyOf(mapper.generateStopClusters(LOCATIONS, List.of()))
      .stream()
      .flatMap(c -> c.secondaryIds().stream());
    assertThat(secondaries).isEmpty();
  }

  private static StopClusterMapper buildMapper(DefaultStopConsolidationRepository repo) {
    var service = new DefaultStopConsolidationService(repo, TIMETABLE_REPOSITORY);
    return new StopClusterMapper(new DefaultTransitService(TIMETABLE_REPOSITORY), service);
  }
}
