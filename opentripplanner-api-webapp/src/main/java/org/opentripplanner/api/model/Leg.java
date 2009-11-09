package org.opentripplanner.api.model;

import org.opentripplanner.api.ws.RequestInf.ModeType;

/**
 *
 */
public class Leg {
    public ModeType mode;

    public Leg() {
        mode = ModeType.bus;
    }
}
