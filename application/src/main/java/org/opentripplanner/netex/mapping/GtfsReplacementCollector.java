package org.opentripplanner.netex.mapping;

import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.model.impl.SubmodeMappingService;
import org.opentripplanner.netex.mapping.support.NetexMainAndSubMode;
import org.opentripplanner.transit.model.basic.TransitMode;

public class GtfsReplacementCollector {

  private final SubmodeMappingService submodeMappingService;
  private final Map<String, Map<String, Integer>> lineIdAndSubmodeToCount;
  private final Map<String, Integer> lineIdTotal;

  public GtfsReplacementCollector(SubmodeMappingService submodeMappingService) {
    lineIdAndSubmodeToCount = new HashMap<>();
    lineIdTotal = new HashMap<>();
    this.submodeMappingService = submodeMappingService;
  }

  public void collectTransitMode(String lineId, NetexMainAndSubMode transitMode) {
    if (!lineIdAndSubmodeToCount.containsKey(lineId)) {
      lineIdAndSubmodeToCount.put(lineId, new HashMap<>());
    }
    var submodeToCount = lineIdAndSubmodeToCount.get(lineId);
    submodeToCount.merge(transitMode.subMode(), 1, Integer::sum);
    lineIdTotal.merge(lineId, 1, Integer::sum);
  }

  private String findEffectiveSubmode(String lineId) {
    int total = lineIdTotal.getOrDefault(lineId, 0);
    if (lineIdAndSubmodeToCount.containsKey(lineId)) {
      for (var submode : lineIdAndSubmodeToCount.get(lineId).keySet()) {
        int count = lineIdAndSubmodeToCount.get(lineId).get(submode);
        if (count == total) {
          return submode;
        }
      }
    }
    return null;
  }

  public TransitMode findGtfsReplacementMode(String lineId) {
    String submode = findEffectiveSubmode(lineId);
    if (submode != null) {
      return submodeMappingService.findGtfsReplacementMode(submode);
    } else if (lineIdTotal.getOrDefault(lineId, 0) == 0) {
      return submodeMappingService.findGtfsReplacementMode(null);
    }
    return null;
  }

  public Integer findGtfsReplacementType(String lineId) {
    String submode = findEffectiveSubmode(lineId);
    if (submode != null) {
      return submodeMappingService.findGtfsReplacementType(submode);
    } else if (lineIdTotal.getOrDefault(lineId, 0) == 0) {
      return submodeMappingService.findGtfsReplacementType(null);
    }
    return null;
  }
}
