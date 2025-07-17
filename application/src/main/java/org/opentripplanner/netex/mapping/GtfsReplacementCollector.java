package org.opentripplanner.netex.mapping;

import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.netex.mapping.support.NetexMainAndSubMode;

public class GtfsReplacementCollector {

  private final Map<String, Counters> countersMap;

  public GtfsReplacementCollector() {
    countersMap = new HashMap<>();
  }

  record Counters(int replacements, int total) {
    public Counters sum(Counters other) {
      return new Counters(replacements + other.replacements, total + other.total);
    }
  }

  public void collectTransitMode(String lineId, NetexMainAndSubMode transitMode) {
    var replacement = "replacementRailService".equals(transitMode.subMode());
    var counters = new Counters(replacement ? 1 : 0, 1);
    countersMap.merge(lineId, counters, Counters::sum);
  }

  public boolean getGtfsReplacement(String lineId) {
    if (countersMap.containsKey(lineId)) {
      var counters = countersMap.get(lineId);
      return counters.replacements == counters.total;
    } else {
      return true;
    }
  }
}
