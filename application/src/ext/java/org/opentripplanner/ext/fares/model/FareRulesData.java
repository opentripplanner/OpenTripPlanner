package org.opentripplanner.ext.fares.model;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public record FareRulesData(
  List<FareAttribute> fareAttributes,
  List<FareRule> fareRules,
  List<FareLegRule> fareLegRules,
  List<FareTransferRule> fareTransferRules,
  Multimap<FeedScopedId, FeedScopedId> stopAreas
) {
  public FareRulesData() {
    this(
      new ArrayList<>(),
      new ArrayList<>(),
      new ArrayList<>(),
      new ArrayList<>(),
      ArrayListMultimap.create()
    );
  }
}
