package org.opentripplanner.ext.fares.model;

import java.util.ArrayList;
import java.util.List;

public record FareRulesData(
  List<FareAttribute> fareAttributes,
  List<FareRule> fareRules,
  List<FareLegRule> fareLegRules,
  List<FareTransferRule> fareTransferRules
) {
  public FareRulesData() {
    this(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
  }
}
