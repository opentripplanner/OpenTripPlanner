package org.opentripplanner.routing.api.request.refactor.preference;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.routing.api.request.RaptorOptions;
import org.opentripplanner.transit.model.basic.TransitMode;

public class TransitPreferences {
  boolean ignoreRealtimeUpdates = false;
  boolean includePlannedCancellations = false;
  int boardSlack;
  Map<TransitMode, Integer> boardSlackForMode = new EnumMap<TransitMode, Integer>(TransitMode.class);
  int alightSlack = 0;
  Map<TransitMode, Integer> alightSlackForMode = new EnumMap<TransitMode, Integer>(TransitMode.class);
  Map<TransitMode, Double> reluctanceForMode = new HashMap<>();
  @Deprecated
  int otherThanPreferredRoutesPenalty = 300;
  @Deprecated
  int useUnpreferredRoutesPenalty = 300;
  RaptorOptions raptorOptions = new RaptorOptions();
}
