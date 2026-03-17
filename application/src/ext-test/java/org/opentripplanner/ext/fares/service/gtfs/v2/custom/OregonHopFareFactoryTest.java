package org.opentripplanner.ext.fares.service.gtfs.v2.custom;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.ext.fares.service.gtfs.v2.custom.OregonHopFareFactory.ADULT_REGIONAL_SINGLE_RIDE;
import static org.opentripplanner.ext.fares.service.gtfs.v2.custom.OregonHopFareFactory.CATEGORY_ADULT;
import static org.opentripplanner.ext.fares.service.gtfs.v2.custom.OregonHopFareFactory.HOP_FASTPASS;
import static org.opentripplanner.ext.fares.service.gtfs.v2.custom.OregonHopFareFactory.LG_CTRAN_REGIONAL;
import static org.opentripplanner.ext.fares.service.gtfs.v2.custom.OregonHopFareFactory.LG_TRIMET_TRIMET;
import static org.opentripplanner.ext.fares.service.gtfs.v2.custom.OregonHopFareFactory.TRIMET_ADULT_SINGLE_RIDE;
import static org.opentripplanner.transit.model._data.FeedScopedIdForTestFactory.id;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.ext.fares.model.FareRulesData;
import org.opentripplanner.ext.fares.model.FareTestConstants;
import org.opentripplanner.model.fare.FareOffer;
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.model.plan.TestItinerary;
import org.opentripplanner.model.plan.TestTransitLeg;
import org.opentripplanner.routing.fares.FareService;
import org.opentripplanner.transit.model.basic.Money;

class OregonHopFareFactoryTest implements FareTestConstants {

  private static final FeedScopedId NETWORK_TRIMET = id("network-trimet");
  private static final FeedScopedId NETWORK_CTRAN = id("network-ctran");

  private static final FareProduct FP_TRIMET_REGULAR = FareProduct.of(
    TRIMET_ADULT_SINGLE_RIDE,
    "regular",
    Money.usDollars(10)
  ).build();

  private static final FareProduct FP_CTRAN_REGIONAL = FareProduct.of(
    ADULT_REGIONAL_SINGLE_RIDE,
    "regular",
    Money.usDollars(5)
  ).build();

  private static final FareProduct FP_TRIMET_TO_CTRAN_TRANSFER = FareProduct.of(
    OregonHopFareFactory.TRIMET_TO_CTRAN_ADULT_TRANSFER,
    "TriMet to C-TRAN",
    FP_CTRAN_REGIONAL.price()
  )
    .withCategory(CATEGORY_ADULT)
    .withMedium(HOP_FASTPASS)
    .build();

  @Test
  void trimetToCtranTransfer() {
    var service = hopService();

    var trimetLeg = TestTransitLeg.of().withNetwork(NETWORK_TRIMET).build();
    var ctranLeg = TestTransitLeg.of().withNetwork(NETWORK_CTRAN).build();

    var results = service.calculateFares(TestItinerary.of(trimetLeg, ctranLeg).build());

    assertThat(results.getItineraryProducts()).containsExactly(FP_TRIMET_REGULAR);
  }

  @Test
  void ctranToTrimetTransfer() {
    var service = hopService();

    var ctranLeg = TestTransitLeg.of().withStartTime("10:00").withNetwork(NETWORK_CTRAN).build();
    var trimetLeg = TestTransitLeg.of().withStartTime("11:00").withNetwork(NETWORK_TRIMET).build();

    var results = service.calculateFares(TestItinerary.of(ctranLeg, trimetLeg).build());

    assertThat(results.getItineraryProducts()).isEmpty();

    assertThat(results.getLegProducts().get(ctranLeg)).containsExactly(
      FareOffer.of(ctranLeg.startTime(), FP_CTRAN_REGIONAL)
    );
    assertThat(results.getLegProducts().get(trimetLeg)).contains(
      FareOffer.of(ctranLeg.startTime(), FP_TRIMET_TO_CTRAN_TRANSFER, Set.of(FP_CTRAN_REGIONAL))
    );
  }

  private static FareService hopService() {
    var factory = new OregonHopFareFactory();

    var data = new FareRulesData();
    data
      .fareLegRules()
      .addAll(
        List.of(
          FareLegRule.of(id("trimet-local"), FP_TRIMET_REGULAR)
            .withLegGroupId(LG_TRIMET_TRIMET)
            .withNetworkId(NETWORK_TRIMET)
            .build(),
          FareLegRule.of(id("ctran-regional"), FP_CTRAN_REGIONAL)
            .withLegGroupId(LG_CTRAN_REGIONAL)
            .withNetworkId(NETWORK_CTRAN)
            .build()
        )
      );

    factory.processGtfs(data);
    return factory.makeFareService();
  }
}
