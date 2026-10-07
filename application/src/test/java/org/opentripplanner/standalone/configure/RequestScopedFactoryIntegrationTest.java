package org.opentripplanner.standalone.configure;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.transit.transfer.regular.parameters.TransferProfileType.WALK;

import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.place.api.NearbyStop;
import org.opentripplanner.transit.transfer.regular.RegularTransferBuildRepository;
import org.opentripplanner.transit.transfer.regular.configure.RegularTransferFactory;

/**
 * Verifies the real Dagger scoping added for issue #7441: bindings inside one {@link
 * RequestScopedFactory} build (one simulated HTTP request) are cached and shared, while two
 * separate builds (two requests) get independent instances. {@link
 * org.opentripplanner.apis.gtfs.GtfsGraphQLRequestContext} is one such binding, so this test needs
 * a fully wired application-level Dagger component to build a {@link RequestScopedFactory} from.
 * <p>
 * This test builds the real production {@link ConstructApplicationFactory} — the same component
 * {@link ConstructApplication} builds when OTP boots — via {@link
 * TestConstructApplicationFactoryBuilder}. That way every real module (including the ones outside
 * {@link RequestScopedModule} itself) is actually exercised through Dagger, not substituted with a
 * hand-built instance.
 */
class RequestScopedFactoryIntegrationTest {

  private static final int AWAIT_COMMIT_TIMEOUT_MS = 5_000;
  private static final int AWAIT_COMMIT_POLL_INTERVAL_MS = 20;
  private static final int FROM_STOP = 0;
  private static final int TO_STOP = 1;
  private static final NearbyStop PATH = new NearbyStop(
    FeedScopedId.of("F", "B"),
    100.0,
    List.of(),
    null
  );

  @Test
  void requestScopedBindingsAreCachedWithinOneRequestButNotAcrossRequests() {
    var factory = TestConstructApplicationFactoryBuilder.of().build();

    var requestOne = factory.requestScopedFactoryBuilder().build();
    assertThat(requestOne.transitService()).isSameInstanceAs(requestOne.transitService());
    assertThat(requestOne.regularTransferServiceFactory()).isSameInstanceAs(
      requestOne.regularTransferServiceFactory()
    );
    assertThat(requestOne.transactionScope()).isSameInstanceAs(requestOne.transactionScope());
    assertThat(requestOne.gtfsRequestContext()).isSameInstanceAs(requestOne.gtfsRequestContext());
    assertThat(requestOne.gtfsRequestContext().transitService()).isSameInstanceAs(
      requestOne.transitService()
    );

    var requestTwo = factory.requestScopedFactoryBuilder().build();
    assertThat(requestOne.transitService()).isNotSameInstanceAs(requestTwo.transitService());
    assertThat(requestOne.regularTransferServiceFactory()).isNotSameInstanceAs(
      requestTwo.regularTransferServiceFactory()
    );
    assertThat(requestOne.gtfsRequestContext()).isNotSameInstanceAs(
      requestTwo.gtfsRequestContext()
    );
    assertThat(requestOne.gtfsRequestContext().schema()).isSameInstanceAs(
      requestTwo.gtfsRequestContext().schema()
    );
    assertThat(requestOne.transmodelGraphQLSchema()).isSameInstanceAs(
      requestTwo.transmodelGraphQLSchema()
    );
  }

  /**
   * The production wiring turns the build repository into the initial repository snapshot, which
   * requests read through their scope.
   */
  @Test
  void requestsReadTheTransfersOfTheBuildRepository() {
    RegularTransferBuildRepository<NearbyStop> buildRepository =
      RegularTransferFactory.createBuildRepository();
    buildRepository.setPaths(WALK, FROM_STOP, Map.of(TO_STOP, PATH));
    var factory = TestConstructApplicationFactoryBuilder.of()
      .withRegularTransferBuildRepository(buildRepository)
      .build();

    var request = factory.requestScopedFactoryBuilder().build();

    assertThat(findPath(request)).isSameInstanceAs(PATH);
  }

  /**
   * A request reads the regular transfers of the snapshot it started with, also after an update
   * is committed. Requests started after the commit see the update.
   */
  @Test
  void regularTransferServiceFactoryReadsTheSnapshotOfItsRequest() throws Exception {
    var factory = TestConstructApplicationFactoryBuilder.of().build();
    var handle = factory.regularTransferRepositoryHandle();
    var updateManager = factory.transitUpdateManager();
    try {
      var before = factory.requestScopedFactoryBuilder().build();
      assertThat(findPath(before)).isNull();

      updateManager
        .submit(ctx -> ctx.repository(handle).setPath(WALK, FROM_STOP, TO_STOP, PATH))
        .get();
      var after = awaitRequestSeeing(factory, PATH);

      assertThat(findPath(after)).isSameInstanceAs(PATH);
      assertThat(findPath(before)).isNull();
    } finally {
      updateManager.shutdown();
    }
  }

  @Nullable
  private static NearbyStop findPath(RequestScopedFactory request) {
    return request.regularTransferServiceFactory().findPath(WALK, FROM_STOP, TO_STOP);
  }

  /** The transit domain commits periodically, so poll until a new request sees the update. */
  private static RequestScopedFactory awaitRequestSeeing(
    ConstructApplicationFactory factory,
    NearbyStop expected
  ) throws InterruptedException {
    long timeout = System.currentTimeMillis() + AWAIT_COMMIT_TIMEOUT_MS;
    while (true) {
      var request = factory.requestScopedFactoryBuilder().build();
      if (findPath(request) == expected || System.currentTimeMillis() > timeout) {
        return request;
      }
      Thread.sleep(AWAIT_COMMIT_POLL_INTERVAL_MS);
    }
  }
}
