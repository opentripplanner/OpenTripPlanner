package org.opentripplanner.updater.alert.siri.lite;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.test.support.ResourceLoader;
import org.opentripplanner.updater.spi.HttpHeaders;
import uk.org.siri.siri20.NaturalLanguageStringStructure;

class SiriLiteHttpLoaderTest {

  private static final Duration ONE_MIN = Duration.ofMinutes(1);

  @Test
  void test() {
    var uri = ResourceLoader.of(this).uri("siri-sx.xml");
    var loader = new SiriLiteHttpLoader(uri, ONE_MIN, HttpHeaders.empty());
    var siri = loader.fetchETFeed("OTP");
    var delivery = siri.get().getServiceDelivery().getSituationExchangeDeliveries().getFirst();
    var element = delivery.getSituations().getPtSituationElements().getFirst();
    assertEquals(
      List.of(
        "Hindernis auf Strecke",
        "Obstacle on the route",
        "Ostacolo sul percorso",
        "Ostacul su la via"
      ),
      element.getReasonNames().stream().map(NaturalLanguageStringStructure::getValue).toList()
    );
  }
}
