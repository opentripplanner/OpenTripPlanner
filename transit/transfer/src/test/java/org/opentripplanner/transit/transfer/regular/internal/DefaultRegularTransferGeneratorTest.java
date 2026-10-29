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
   * The parent finds {@code p1}. Under the preferences of the child, {@code p1} costs 210 and
   * {@code p2} costs 200, so the child finds {@code p2}. The two paths are different instances.
   */
  private static final class ParentAndChild {

    final FakeTransferPathProvider provider = new FakeTransferPathProvider();
    final FakeTransferPathProvider.FakePath p1 = provider.addNearby(A, B, 100, 60);
    final FakeTransferPathProvider.FakePath p2 = provider.addNearby(A, B, 100, 61);
    final WalkPreferences parent = withReluctance(1.0);
    final WalkPreferences child = withReluctance(2.0);

    ParentAndChild() {
      provider.withCost(p1, 2.0, 210).withCost(p2, 2.0, 200);
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

    TransferProfile<WalkPreferences> parentProfile() {
      return new TransferProfile<>("parent", TransferProfileType.WALK, null, parent, parent);
    }

    TransferProfile<WalkPreferences> childProfile(String parentId) {
      return childProfile(parentId, TransferParameters.DEFAULT_REUSE_LIMIT);
    }

    TransferProfile<WalkPreferences> childProfile(String parentId, CostLinearFunction limit) {
      return new TransferProfile<>(
        "child",
        TransferProfileType.WALK,
        parentId,
        child,
        child,
        limit
      );
    }
  }

  @Test
  void aChildUsesThePathOfItsParentIfTheCostIsEquivalent() {
    var test = new ParentAndChild();

    var path = test.generateAndFindPath(
      List.of(test.parentProfile(), test.childProfile("parent")),
      test.child
    );

    assertThat(path).isSameInstanceAs(test.p1);
  }

  @Test
  void aChildKeepsItsOwnPathIfTheCostIsNotEquivalent() {
    var test = new ParentAndChild();

    // p1 costs 10 more for the child, the limit does not accept any difference
    var path = test.generateAndFindPath(
      List.of(
        test.parentProfile(),
        test.childProfile("parent", CostLinearFunction.of(Cost.ZERO, 0.0))
      ),
      test.child
    );

    assertThat(path).isSameInstanceAs(test.p2);
  }

  @Test
  void aProfileWithoutAParentKeepsItsOwnPath() {
    var test = new ParentAndChild();

    var path = test.generateAndFindPath(
      List.of(test.parentProfile(), test.childProfile(null)),
      test.child
    );

    assertThat(path).isSameInstanceAs(test.p2);
  }

  @Test
  void theParentKeepsItsOwnPath() {
    var test = new ParentAndChild();

    var path = test.generateAndFindPath(
      List.of(test.parentProfile(), test.childProfile("parent")),
      test.parent
    );

    assertThat(path).isSameInstanceAs(test.p1);
  }

  @Test
  void aChildDeclaredBeforeItsParentIsStillDeduplicated() {
    var test = new ParentAndChild();

    var path = test.generateAndFindPath(
      List.of(test.childProfile("parent"), test.parentProfile()),
      test.child
    );

    assertThat(path).isSameInstanceAs(test.p1);
  }

  @Test
  void theStatisticsCountTheSharedPathsOfAChild() {
    var test = new ParentAndChild();

    var statistics = test.generateStatistics(
      List.of(test.parentProfile(), test.childProfile("parent"))
    );

    assertThat(statistics.get("parent").paths()).isEqualTo(1);
    assertThat(statistics.get("parent").reused()).isEqualTo(0);
    assertThat(statistics.get("child").paths()).isEqualTo(1);
    assertThat(statistics.get("child").reused()).isEqualTo(1);
    assertThat(statistics.get("child").stored()).isEqualTo(0);
    assertThat(statistics.get("child").parentId()).isEqualTo("parent");
  }

  @Test
  void theStatisticsOfAChildWithItsOwnPathHaveNoSharedPaths() {
    var test = new ParentAndChild();

    var statistics = test.generateStatistics(
      List.of(
        test.parentProfile(),
        test.childProfile("parent", CostLinearFunction.of(Cost.ZERO, 0.0))
      )
    );

    assertThat(statistics.get("child").reused()).isEqualTo(0);
    assertThat(statistics.get("child").stored()).isEqualTo(1);
  }

  @Test
  void theStatisticsHaveTheStopsWithTransfersOfEachProfile() {
    var test = new ParentAndChild();

    var statistics = test.generateStatistics(
      List.of(test.parentProfile(), test.childProfile("parent"))
    );

    // Both profiles have a path from A to B
    assertThat(statistics.get("parent").stopsWithTransfers()).containsExactly(A, B);
    assertThat(statistics.get("child").stopsWithTransfers()).containsExactly(A, B);
  }

  @Test
  void theStatisticsHaveThePreferencesAndTheParentOfTheProfile() {
    var test = new ParentAndChild();
    var statistics = test.generateStatistics(List.of(test.parentProfile()));
    assertThat(statistics.get("parent").paths()).isEqualTo(1);
    assertThat(statistics.get("parent").preferences()).isEqualTo(test.parent.toString());
    assertThat(statistics.get("parent").parentId()).isNull();
  }
}
