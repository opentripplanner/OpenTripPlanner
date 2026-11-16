package org.opentripplanner.transit.transfer.regular.internal;

import static com.google.common.truth.Truth.assertThat;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.basic.Cost;
import org.opentripplanner.core.model.basic.CostLinearFunction;
import org.opentripplanner.core.model.basic.Reluctance;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.raptor.data.stop.RaptorStopIndex;
import org.opentripplanner.transit.transfer.regular.FakeRequestMapper;
import org.opentripplanner.transit.transfer.regular.FakeTransferPathProvider;
import org.opentripplanner.transit.transfer.regular.RegularTransferProfileStatistics;
import org.opentripplanner.transit.transfer.regular.api.maxduration.MaxDurationsPreference;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferParameters;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferProfile;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferProfileType;
import org.opentripplanner.transit.transfer.regular.api.preferences.WalkPreferences;

class DefaultRegularTransferGeneratorTest {

  private static final FeedScopedId A = FeedScopedId.of("F", "A");
  private static final FeedScopedId B = FeedScopedId.of("F", "B");
  private static final FeedScopedId C = FeedScopedId.of("F", "C");

  private static RaptorStopIndex stopIndex() {
    return TestStopIndex.of(A, B, C);
  }

  private static TransferParameters<WalkPreferences> parameters(WalkPreferences walk) {
    return new TransferParameters<>(
      List.of(new TransferProfile<>("walk", TransferProfileType.WALK, walk, walk))
    );
  }

  private static TransferParameters<WalkPreferences> parameters(
    WalkPreferences walk,
    Duration maxDuration
  ) {
    return new TransferParameters<>(
      List.of(new TransferProfile<>("walk", TransferProfileType.WALK, walk, walk)),
      Map.of(TransferProfileType.WALK, MaxDurationsPreference.of(maxDuration))
    );
  }

  private static WalkPreferences withReluctance(double reluctance) {
    return WalkPreferences.of().withReluctance(Reluctance.of(reluctance)).build();
  }

  @Test
  void keepsOnlyTheCheapestCandidatePerTargetStop() {
    var provider = new FakeTransferPathProvider();
    var cheapAB = provider.addNearby(A, B, 100, 60);
    provider.addNearby(A, B, 200, 90);

    WalkPreferences userPreferences = withReluctance(1.0);
    var stopIndex = stopIndex();
    var buildRepository =
      new DefaultRegularTransferBuildRepository<FakeTransferPathProvider.FakePath>();
    var generator = new DefaultRegularTransferGenerator<>(
      stopIndex,
      parameters(userPreferences),
      provider,
      FakeRequestMapper.INSTANCE,
      buildRepository
    );

    generator.generateTransfersForAllStops();

    assertThat(
      TestLifecycles.initialSnapshot(buildRepository)
        .forRequest(userPreferences)
        .findPath(stopIndex.toStopIndex(A), stopIndex.toStopIndex(B))
    ).isSameInstanceAs(cheapAB);
  }

  @Test
  void pathsExceedingTheMaxDurationAreExcluded() {
    var provider = new FakeTransferPathProvider();
    provider.addNearby(A, B, 100, 600);

    WalkPreferences userPreferences = withReluctance(1.0);
    var stopIndex = stopIndex();
    var buildRepository =
      new DefaultRegularTransferBuildRepository<FakeTransferPathProvider.FakePath>();
    var generator = new DefaultRegularTransferGenerator<>(
      stopIndex,
      parameters(userPreferences, Duration.ofMinutes(2)),
      provider,
      FakeRequestMapper.INSTANCE,
      buildRepository
    );

    generator.generateTransfersForAllStops();

    assertThat(
      TestLifecycles.initialSnapshot(buildRepository)
        .forRequest(userPreferences)
        .findPath(stopIndex.toStopIndex(A), stopIndex.toStopIndex(B))
    ).isNull();
  }

  @Test
  void candidatesOutsideTheStopIndexAreSkipped() {
    var flexStop = FeedScopedId.of("F", "FLEX");
    var provider = new FakeTransferPathProvider();
    var pathAB = provider.addNearby(A, B, 100, 60);
    provider.addNearby(A, flexStop, 50, 30);

    WalkPreferences userPreferences = withReluctance(1.0);
    var buildRepository =
      new DefaultRegularTransferBuildRepository<FakeTransferPathProvider.FakePath>();
    var generator = new DefaultRegularTransferGenerator<>(
      stopIndex(),
      parameters(userPreferences),
      provider,
      FakeRequestMapper.INSTANCE,
      buildRepository
    );

    generator.generateTransfersForAllStops();

    assertThat(buildRepository.calculateNumberOfTransferPaths(userPreferences)).isEqualTo(1);
    assertThat(
      TestLifecycles.initialSnapshot(buildRepository).forRequest(userPreferences).findPath(0, 1)
    ).isEqualTo(pathAB);
  }

  @Test
  void pathsWithinTheMaxDurationAreKept() {
    var provider = new FakeTransferPathProvider();
    var path = provider.addNearby(A, B, 100, 120);
    provider.addNearby(A, C, 100, 121);

    WalkPreferences userPreferences = withReluctance(1.0);
    var stopIndex = stopIndex();
    var buildRepository =
      new DefaultRegularTransferBuildRepository<FakeTransferPathProvider.FakePath>();
    new DefaultRegularTransferGenerator<>(
      stopIndex,
      parameters(userPreferences, Duration.ofMinutes(2)),
      provider,
      FakeRequestMapper.INSTANCE,
      buildRepository
    ).generateTransfersForAllStops();

    var transfers = TestLifecycles.initialSnapshot(buildRepository).forRequest(userPreferences);
    assertThat(
      transfers.findPath(stopIndex.toStopIndex(A), stopIndex.toStopIndex(B))
    ).isSameInstanceAs(path);
    assertThat(transfers.findPath(stopIndex.toStopIndex(A), stopIndex.toStopIndex(C))).isNull();
  }

  @Test
  void listsTheStopsAtBothEndsOfTransfers() {
    var provider = new FakeTransferPathProvider();
    provider.addNearby(A, B, 100, 60);

    WalkPreferences userPreferences = withReluctance(1.0);
    var buildRepository =
      new DefaultRegularTransferBuildRepository<FakeTransferPathProvider.FakePath>();
    var generator = new DefaultRegularTransferGenerator<>(
      stopIndex(),
      parameters(userPreferences),
      provider,
      FakeRequestMapper.INSTANCE,
      buildRepository
    );

    generator.generateTransfersForAllStops();

    assertThat(generator.listAllStopsUsedInTransfers()).containsExactly(A, B);
  }

  /**
   * Three profiles that find the paths {@code p1} and {@code p2} from A to B. The costs for the
   * reluctance 1.0, 2.0 and 3.0 are: {@code p1} 100, 210 and 310, and {@code p2} 100, 200 and 300.
   * The first profile finds {@code p1} (the first path with the lowest cost), the second and the
   * third find {@code p2}. The two paths are different instances.
   */
  private static final class ThreeProfiles {

    final FakeTransferPathProvider provider = new FakeTransferPathProvider();
    final FakeTransferPathProvider.FakePath p1 = provider.addNearby(A, B, 100, 60);
    final FakeTransferPathProvider.FakePath p2 = provider.addNearby(A, B, 100, 61);
    final WalkPreferences first = withReluctance(1.0);
    final WalkPreferences second = withReluctance(2.0);
    final WalkPreferences third = withReluctance(3.0);

    ThreeProfiles() {
      provider
        .withCost(p1, 2.0, 210)
        .withCost(p2, 2.0, 200)
        .withCost(p1, 3.0, 310)
        .withCost(p2, 3.0, 300);
    }

    /** The path from A to B found for the preferences, after the transfers are generated. */
    FakeTransferPathProvider.FakePath generateAndFindPath(
      List<TransferProfile<WalkPreferences>> profiles,
      WalkPreferences forPreferences
    ) {
      var stopIndex = stopIndex();
      var buildRepository =
        new DefaultRegularTransferBuildRepository<FakeTransferPathProvider.FakePath>();
      new DefaultRegularTransferGenerator<>(
        stopIndex,
        new TransferParameters<>(
          profiles,
          Map.of(TransferProfileType.WALK, MaxDurationsPreference.DEFAULT)
        ),
        provider,
        FakeRequestMapper.INSTANCE,
        buildRepository
      ).generateTransfersForAllStops();
      return TestLifecycles.initialSnapshot(buildRepository)
        .forRequest(forPreferences)
        .findPath(stopIndex.toStopIndex(A), stopIndex.toStopIndex(B));
    }

    DefaultRegularTransferGenerator<FakeTransferPathProvider.FakePath, WalkPreferences> generator(
      List<TransferProfile<WalkPreferences>> profiles
    ) {
      return new DefaultRegularTransferGenerator<>(
        stopIndex(),
        new TransferParameters<>(
          profiles,
          Map.of(TransferProfileType.WALK, MaxDurationsPreference.DEFAULT)
        ),
        provider,
        FakeRequestMapper.INSTANCE,
        new DefaultRegularTransferBuildRepository<FakeTransferPathProvider.FakePath>()
      );
    }

    /** The statistics of each profile after the transfers are generated, by profile id. */
    Map<String, RegularTransferProfileStatistics> generateStatistics(
      List<TransferProfile<WalkPreferences>> profiles
    ) {
      var generator = generator(profiles);
      generator.generateTransfersForAllStops();
      var result = new HashMap<String, RegularTransferProfileStatistics>();
      generator.profileStatistics().forEach(statistics -> result.put(statistics.id(), statistics));
      return result;
    }

    TransferProfile<WalkPreferences> firstProfile() {
      return new TransferProfile<>("first", TransferProfileType.WALK, first, first);
    }

    TransferProfile<WalkPreferences> secondProfile() {
      return secondProfile(TransferParameters.DEFAULT_REUSE_LIMIT);
    }

    TransferProfile<WalkPreferences> secondProfile(CostLinearFunction limit) {
      return new TransferProfile<>("second", TransferProfileType.WALK, second, second, limit);
    }

    TransferProfile<WalkPreferences> thirdProfile() {
      return new TransferProfile<>("third", TransferProfileType.WALK, third, third);
    }
  }

  private static final CostLinearFunction NO_REUSE = CostLinearFunction.of(Cost.ZERO, 0.0);

  @Test
  void aProfileUsesThePathOfAnEarlierProfileIfTheCostIsEquivalent() {
    var test = new ThreeProfiles();

    var path = test.generateAndFindPath(
      List.of(test.firstProfile(), test.secondProfile()),
      test.second
    );

    assertThat(path).isSameInstanceAs(test.p1);
  }

  @Test
  void aProfileKeepsItsOwnPathIfTheCostIsNotEquivalent() {
    var test = new ThreeProfiles();

    // p1 costs 10 more for the second profile, the limit does not accept any difference
    var path = test.generateAndFindPath(
      List.of(test.firstProfile(), test.secondProfile(NO_REUSE)),
      test.second
    );

    assertThat(path).isSameInstanceAs(test.p2);
  }

  @Test
  void theFirstProfileKeepsItsOwnPath() {
    var test = new ThreeProfiles();

    var path = test.generateAndFindPath(
      List.of(test.firstProfile(), test.secondProfile()),
      test.first
    );

    assertThat(path).isSameInstanceAs(test.p1);
  }

  @Test
  void theOrderOfTheProfilesDecidesWhichProfileReusesThePathOfTheOther() {
    var test = new ThreeProfiles();

    // The second profile is before the first now: it keeps p2, and the first profile reuses it
    var path = test.generateAndFindPath(
      List.of(test.secondProfile(), test.firstProfile()),
      test.first
    );

    assertThat(path).isSameInstanceAs(test.p2);
  }

  @Test
  void aProfileUsesTheCheapestOfTheEquivalentPathsOfAllEarlierProfiles() {
    var test = new ThreeProfiles();

    // The first profile has p1 and the second has p2, both are within the limit of the third
    // profile, p2 is the cheapest of them for the third profile, and not the one of the first
    // profile
    var path = test.generateAndFindPath(
      List.of(test.firstProfile(), test.secondProfile(NO_REUSE), test.thirdProfile()),
      test.third
    );

    assertThat(path).isSameInstanceAs(test.p2);
  }

  @Test
  void aPathOfAnEarlierProfileIsNotReusedIfItIsLongerThanTheMaxDurationOfTheProfile() {
    var test = new ThreeProfiles();
    // The first profile finds the longer path p2, it is cheapest for the second profile too
    test.provider.withCost(test.p2, 1.0, 90);
    // The second profile only accepts paths up to 60 seconds, so p2 (61 seconds) is not an option
    test.provider.withMaxDuration(2.0, 60);

    var path = test.generateAndFindPath(
      List.of(test.firstProfile(), test.secondProfile()),
      test.second
    );

    assertThat(path).isSameInstanceAs(test.p1);
  }

  @Test
  void theStatisticsCountTheSharedPathsOfAProfile() {
    var test = new ThreeProfiles();

    var statistics = test.generateStatistics(List.of(test.firstProfile(), test.secondProfile()));

    assertThat(statistics.get("first").paths()).isEqualTo(1);
    assertThat(statistics.get("first").reused()).isEqualTo(0);
    assertThat(statistics.get("second").paths()).isEqualTo(1);
    assertThat(statistics.get("second").reused()).isEqualTo(1);
    assertThat(statistics.get("second").stored()).isEqualTo(0);
  }

  @Test
  void theStatisticsOfAProfileWithItsOwnPathHaveNoSharedPaths() {
    var test = new ThreeProfiles();

    var statistics = test.generateStatistics(
      List.of(test.firstProfile(), test.secondProfile(NO_REUSE))
    );

    assertThat(statistics.get("second").reused()).isEqualTo(0);
    assertThat(statistics.get("second").stored()).isEqualTo(1);
  }

  @Test
  void theStatisticsHaveTheStopsWithTransfersOfEachProfile() {
    var test = new ThreeProfiles();

    var statistics = test.generateStatistics(List.of(test.firstProfile(), test.secondProfile()));

    // Both profiles have a path from A to B
    assertThat(statistics.get("first").stopsWithTransfers()).containsExactly(A, B);
    assertThat(statistics.get("second").stopsWithTransfers()).containsExactly(A, B);
  }

  @Test
  void theStatisticsHaveTheReuseLimitOfTheProfile() {
    var test = new ThreeProfiles();

    var statistics = test.generateStatistics(
      List.of(test.firstProfile(), test.secondProfile(NO_REUSE))
    );

    assertThat(statistics.get("first").reuseLimit()).isEqualTo(
      TransferParameters.DEFAULT_REUSE_LIMIT
    );
    assertThat(statistics.get("second").reuseLimit()).isEqualTo(NO_REUSE);
  }
}
