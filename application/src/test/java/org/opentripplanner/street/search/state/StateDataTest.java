package org.opentripplanner.street.search.state;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.street.search.request.StreetSearchRequest;

class StateDataTest {

  static Stream<Arguments> cases() {
    return Arrays.stream(StreetMode.values()).flatMap(mode ->
      Stream.of(Arguments.of(true, mode), Arguments.of(false, mode))
    );
  }

  @ParameterizedTest(name = "arriveBy={0}, streetMode={1}")
  @MethodSource("cases")
  void baseCases(boolean arriveBy, StreetMode streetMode) {
    var req = StreetSearchRequest.of().withArriveBy(arriveBy).withMode(streetMode).build();
    var data = StateData.getBaseCaseStateData(req);
    // no better assertion as this will throw an exception if there is more than one state
    assertNotNull(data);
  }
}
