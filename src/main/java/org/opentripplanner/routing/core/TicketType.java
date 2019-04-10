package org.opentripplanner.routing.core;

import java.util.Set;

public class TicketType {
    FareRuleSet rs;

    public TicketType(FareRuleSet rs) {
        this.rs = rs;
            }
    
    public String getId() {
        return rs.getFareAttribute().getId().toString();
    }
    
    public float getPrice() {
        return rs.getFareAttribute().getPrice();
    }

    public String getCurrency() { return rs.getFareAttribute().getCurrencyType(); }

    public Set<String> getZones() {
        return rs.getContains();
    }
}
