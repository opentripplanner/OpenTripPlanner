package org.opentripplanner.routing.algorithm.transferoptimization.model.passthrough;

import java.util.List;
import org.opentripplanner.raptor.api.request.RaptorViaLocation;
import org.opentripplanner.raptorlegacy._data.RaptorTestConstants;

record TestCase(
  String description,
  int stopIndexA,
  int stopIndexB,
  boolean fromAToB,
  List<RaptorViaLocation> points
)
  implements RaptorTestConstants {
  static TestCaseBuilder testCase(String description) {
    return new TestCaseBuilder(description);
  }

  static TestCaseBuilder testCase() {
    return new TestCaseBuilder(null);
  }

  @Override
  public String toString() {
    var buf = new StringBuilder();
    buf.append(
      switch (points.size()) {
        case 0 -> "No pass-through-points";
        case 1 -> "One pass-through-point ";
        case 2 -> "Two pass-through-points ";
        default -> points.size() + " pass-through-points ";
      }
    );
    if (description != null) {
      buf.append(description);
    } else {
      if (points.size() == 1) {
        buf.append("at ");
        appendPoint(buf, 0);
      }
      if (points.size() > 1) {
        buf.append("at (");
        appendPoint(buf, 0);
        for (int i = 1; i < points.size(); ++i) {
          buf.append(") and (");
          appendPoint(buf, i);
        }
        buf.append(")");
      }
    }

    if (stopIndexA > 0 || stopIndexB > 0) {
      buf
        .append(". Expects transfer" + (fromAToB ? "" : "s") + " from ")
        .append(stopIndexToName(stopIndexA))
        .append(fromAToB ? " to " : " and ")
        .append(stopIndexToName(stopIndexB));
    }
    buf.append(". ").append(points.stream().map(p -> p.toString(this::stopIndexToName)).toList());
    return buf.toString();
  }

  private void appendPoint(StringBuilder buf, int passThroughPointIndex) {
    buf.append(points.get(passThroughPointIndex).toString(this::stopIndexToName));
  }

  boolean contains(int stopIndex) {
    return points.stream().anyMatch(it -> it.asBitSet().get(stopIndex));
  }
}
