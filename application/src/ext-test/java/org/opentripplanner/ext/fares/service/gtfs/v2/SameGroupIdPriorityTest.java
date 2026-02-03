package org.opentripplanner.ext.fares.service.gtfs.v2;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.transit.model._data.FeedScopedIdForTestFactory.id;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.ext.fares.model.FareLegRuleBuilder;
import org.opentripplanner.ext.fares.model.FareTestConstants;
import org.opentripplanner.model.plan.TestTransitLeg;
import org.opentripplanner.model.plan.TransitLeg;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.network.Route;

class SameGroupIdPriorityTest implements FareTestConstants {

  private static final FeedScopedId A_1 = id("a1");
  private static final Route ROUTE = TimetableRepositoryForTest.route("route1")
    .withGroupOfRoutes(List.of(NETWORK_A))
    .build();
  private static final ImmutableMultimap<FeedScopedId, FeedScopedId> EMPTY_STOP_AREAS =
    ImmutableMultimap.of();
  private static final FeedScopedId STOP1_ID = id("stop1");
  private static final FeedScopedId STOP2_ID = id("stop2");
  private static final Multimap<FeedScopedId, FeedScopedId> STOP_AREAS = ImmutableMultimap.of(
    STOP1_ID,
    A_1,
    STOP2_ID,
    A_1
  );
  private static final FeedScopedId R_1 = id("r1");
  private static final FeedScopedId R_2 = id("r2");
  private static final FeedScopedId GROUP_ID = id("group");

  private record TestCase(
    String name,
    Function<FareLegRuleBuilder, FareLegRuleBuilder> rule1Customiser,
    Function<FareLegRuleBuilder, FareLegRuleBuilder> rule2Customiser,
    Multimap<FeedScopedId, FeedScopedId> stopAreas,
    Collection<FeedScopedId> expectedResults
  ) implements Named {
    @Override
    public String getName() {
      return name;
    }

    @Override
    public Object getPayload() {
      return this;
    }
  }

  private static List<TestCase> cases() {
    return List.of(
      new TestCase(
        "mixed area and network without priority leads to zero results",
        r1 -> r1.withNetworkId(NETWORK_A.getId()),
        r2 -> r2.withFromAreaId(A_1).withToAreaId(A_1),
        STOP_AREAS,
        Set.of()
      ),
      new TestCase(
        "adding a priority leads to some results",
        r1 -> r1.withNetworkId(NETWORK_A.getId()).withPriority(0),
        r2 -> r2.withFromAreaId(A_1).withToAreaId(A_1).withPriority(2),
        STOP_AREAS,
        Set.of(R_2)
      ),
      new TestCase(
        "network wildcard",
        r1 -> r1.withFromAreaId(A_1).withToAreaId(A_1),
        r2 -> r2.withFromAreaId(A_1).withToAreaId(A_1),
        STOP_AREAS,
        Set.of(R_1, R_2)
      ),
      new TestCase(
        "conflicting results resolved by priority",
        r1 -> r1.withNetworkId(NETWORK_A.getId()).withPriority(1),
        r2 -> r2.withNetworkId(NETWORK_A.getId()).withPriority(2),
        STOP_AREAS,
        Set.of(R_2)
      ),
      new TestCase(
        "no priority returns both",
        r1 -> r1.withNetworkId(NETWORK_A.getId()),
        r2 -> r2.withNetworkId(NETWORK_A.getId()),
        EMPTY_STOP_AREAS,
        Set.of(R_1, R_2)
      ),
      new TestCase(
        "multiple rules with equal priority",
        r1 -> r1.withNetworkId(NETWORK_A.getId()).withPriority(10),
        r2 -> r2.withNetworkId(NETWORK_A.getId()).withPriority(10),
        EMPTY_STOP_AREAS,
        Set.of(R_1, R_2)
      )
    );
  }

  @ParameterizedTest
  @MethodSource("cases")
  void priority(TestCase tc) {
    var r1 = tc.rule1Customiser.apply(rule1()).build();
    var r2 = tc.rule2Customiser.apply(rule2()).build();
    var service = new FareLookupService(
      List.of(r1, r2),
      List.of(),
      tc.stopAreas,
      ImmutableMultimap.of()
    );

    assertThat(service.legRules(leg()).stream().map(FareLegRule::id)).containsExactlyElementsIn(
      tc.expectedResults
    );
  }

  private static FareLegRuleBuilder rule1() {
    return FareLegRule.of(R_1, List.of(FARE_PRODUCT_A)).withLegGroupId(GROUP_ID);
  }

  private static FareLegRuleBuilder rule2() {
    return FareLegRule.of(R_2, List.of(FARE_PRODUCT_B)).withLegGroupId(GROUP_ID);
  }

  private TransitLeg leg() {
    return TestTransitLeg.of().withFrom(STOP1_ID).withTo(STOP2_ID).withRoute(ROUTE).build();
  }
}
