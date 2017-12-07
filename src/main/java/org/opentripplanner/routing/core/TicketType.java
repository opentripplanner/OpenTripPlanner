package org.opentripplanner.routing.core;

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

}
