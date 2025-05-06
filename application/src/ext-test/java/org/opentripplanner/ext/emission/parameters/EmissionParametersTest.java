package org.opentripplanner.ext.emission.parameters;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.asserts.AssertEqualsAndHashCode;
import org.opentripplanner._support.net.URIUtils;

class EmissionParametersTest {

  private final EmissionViechleParameters car = new EmissionViechleParameters(90, 7.4);
  private final List<EmissionFeedParameters> feeds = List.of(
    new EmissionFeedParameters("my_feed_id", URIUtils.uri("http://host/emsissions"))
  );
  private final EmissionParameters subject = new EmissionParameters(car, feeds);

  @Test
  void car() {
    assertEquals(car, subject.car());
  }

  @Test
  void feeds() {
    assertEquals(feeds, subject.feeds());
  }

  @Test
  void testToString() {
    assertEquals(
      "EmissionParameters{" +
      "car: EmissionViechleParameters{carAvgCo2PerKm: 90, carAvgOccupancy: 7.4}, " +
      "fedds: [EmissionFeedParameters{feedId: 'my_feed_id', source: http://host/emsissions}]" +
      "}",
      subject.toString()
    );
  }

  @Test
  void testEqualsAndHashCode() {
    AssertEqualsAndHashCode.verify(subject)
      .sameAs(new EmissionParameters(car, feeds))
      .differentFrom(
        new EmissionParameters(car, List.of()),
        new EmissionParameters(EmissionViechleParameters.CAR_DEFAULTS, List.of())
      );
  }
}
