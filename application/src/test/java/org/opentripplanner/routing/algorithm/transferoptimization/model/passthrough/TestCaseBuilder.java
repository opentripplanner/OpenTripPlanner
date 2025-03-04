package org.opentripplanner.routing.algorithm.transferoptimization.model.passthrough;

import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.raptor.api.model.RaptorConstants;
import org.opentripplanner.raptor.api.request.RaptorViaLocation;

class TestCaseBuilder {

  final String description;
  int stopIndexA = RaptorConstants.NOT_SET;
  int stopIndexB = RaptorConstants.NOT_SET;
  boolean txFromAToB = false;
  final List<RaptorViaLocation> points = new ArrayList<>();

  TestCaseBuilder(String description) {
    this.description = description;
  }

  TestCaseBuilder points(int... stops) {
    points.add(RaptorViaLocation.passThrough("PT").addPassThroughStops(stops).build());
    return this;
  }

  TestCase expectTransfer(int fromStopIndex, int toStopIndex) {
    this.stopIndexA = fromStopIndex;
    this.stopIndexB = toStopIndex;
    this.txFromAToB = true;
    return build();
  }

  TestCase expectTransfersFrom(int fromStopIndexA, int fromStopIndexB) {
    this.stopIndexA = fromStopIndexA;
    this.stopIndexB = fromStopIndexB;
    this.txFromAToB = false;
    return build();
  }

  TestCase build() {
    return new TestCase(description, stopIndexA, stopIndexB, txFromAToB, points);
  }
}
