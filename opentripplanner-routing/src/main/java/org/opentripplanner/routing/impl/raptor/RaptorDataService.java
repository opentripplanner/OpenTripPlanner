package org.opentripplanner.routing.impl.raptor;

import java.io.Serializable;

public class RaptorDataService implements Serializable {
    private static final long serialVersionUID = -5046519968713244930L;

    private RaptorData data;

    public RaptorDataService(RaptorData data) {
        this.data = data;
    }

    public RaptorData getData() {
        return data;
    }
}
