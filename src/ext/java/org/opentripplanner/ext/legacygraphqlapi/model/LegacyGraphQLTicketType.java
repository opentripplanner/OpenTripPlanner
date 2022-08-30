package org.opentripplanner.ext.legacygraphqlapi.model;

import java.util.Set;
import org.opentripplanner.routing.core.FareRuleSet;

public class LegacyGraphQLTicketType {

  FareRuleSet rs;

  public LegacyGraphQLTicketType(FareRuleSet rs) {
    this.rs = rs;
  }

  public String getFareId() {
    return rs.getFareAttribute().getId().toString();
  }

  public float getPrice() {
    return rs.getFareAttribute().getPrice();
  }

  public String getCurrency() {
    return rs.getFareAttribute().getCurrencyType();
  }

  public Set<String> getZones() {
    return rs.getContains();
  }
}
