package org.opentripplanner.gtfs.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.FareLegRule;
import org.onebusaway.gtfs.model.FareProduct;
import org.opentripplanner.ext.fares.model.Distance;
import org.opentripplanner.ext.fares.model.FareDistance;
import org.opentripplanner.ext.fares.model.FareDistance.LinearDistance;
import org.opentripplanner.ext.fares.model.FareDistance.Stops;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;

class FareLegRuleMapperTest {

  private record TestCase(
    Integer distanceType,
    Double minDistance,
    Double maxDistance,
    FareDistance expectedDistance
  ) {}

  private final List<TestCase> testCases = List.of(
    new TestCase(0, 1d, 10d, new Stops(1, 10)),
    new TestCase(
      1,
      5000d,
      10000d,
      new LinearDistance(Distance.ofKilometers(5), Distance.ofKilometers(10))
    ),
    new TestCase(null, null, null, null)
  );

  @TestFactory
  Stream<DynamicTest> mapDistance() {
    var mapper = new FareLegRuleMapper(new FareProductMapper(), DataImportIssueStore.NOOP);

    return testCases
      .stream()
      .map(tc ->
        dynamicTest(
          tc.toString(),
          () -> {
            var fp = new FareProduct();
            fp.setAmount(10);
            fp.setName("Day pass");
            fp.setCurrency("EUR");
            fp.setFareProductId(new AgencyAndId("1", "1"));

            var obaRule = new FareLegRule();
            obaRule.setFareProduct(fp);
            obaRule.setDistanceType(tc.distanceType);
            obaRule.setMinDistance(tc.minDistance);
            obaRule.setMaxDistance(tc.maxDistance);

            var mappedRules = List.copyOf(mapper.map(List.of(obaRule)));
            assertEquals(1, mappedRules.size());

            var otpRule = mappedRules.get(0);
            assertEquals(otpRule.fareDistance(), tc.expectedDistance);
          }
        )
      );
  }
}
