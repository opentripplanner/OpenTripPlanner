package org.opentripplanner.api.adapters;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.opentripplanner.model.Stop;

public class StopAdapter extends XmlAdapter<StopType, Stop> {

    @Override
    public Stop unmarshal(StopType arg) throws Exception {
        throw new UnsupportedOperationException("Unmarshalling Stops is not supported.");
    }

    @Override
    public StopType marshal(Stop arg) throws Exception {
        if (arg == null) {
            return null;
        }
        return new StopType(arg);
    }

}
