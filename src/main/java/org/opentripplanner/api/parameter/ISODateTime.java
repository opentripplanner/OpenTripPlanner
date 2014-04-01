package org.opentripplanner.api.parameter;

import org.joda.time.LocalDateTime;

public class ISODateTime {
    LocalDateTime ldt;
    public ISODateTime (String s) {
        ldt = LocalDateTime.parse(s);        
    }
}
