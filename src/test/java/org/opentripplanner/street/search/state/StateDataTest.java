package org.opentripplanner.street.search.state;

import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.test.support.VariableSource;

class StateDataTest {

  static Stream<Arguments> cases = Arrays
    .stream(StreetMode.values())
    .flatMap(mode -> Stream.of(Arguments.of(true, mode), Arguments.of(false, mode)));

  @ParameterizedTest(name = "arriveBy={0}, streetMode={1}")
  @VariableSource("cases")
  void baseCases(boolean arriveBy, StreetMode streetMode) {
    var req = StreetSearchRequest.of().withArriveBy(arriveBy).withMode(streetMode).build();
    // no assertion as this will throw an exception if there is more than one state
    StateData.getBaseCaseStateData(req);
  }
}
