package org.opentripplanner.standalone.configure;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.Test;

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

  @Test
  void requestScopedBindingsAreCachedWithinOneRequestButNotAcrossRequests() {
    var factory = TestConstructApplicationFactoryBuilder.of().build();

    var requestOne = factory.requestScopedFactoryBuilder().build();
    assertThat(requestOne.transitService()).isSameInstanceAs(requestOne.transitService());
    assertThat(requestOne.transactionScope()).isSameInstanceAs(requestOne.transactionScope());
    assertThat(requestOne.gtfsRequestContext()).isSameInstanceAs(requestOne.gtfsRequestContext());
    assertThat(requestOne.gtfsRequestContext().transitService()).isSameInstanceAs(
      requestOne.transitService()
    );

    var requestTwo = factory.requestScopedFactoryBuilder().build();
    assertThat(requestOne.transitService()).isNotSameInstanceAs(requestTwo.transitService());
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
}
