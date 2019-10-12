package org.opentripplanner.api.adapters;

import org.opentripplanner.model.Station;
import javax.xml.bind.annotation.adapters.XmlAdapter;

public class StationAdapter extends XmlAdapter<StopType, Station> {

    @Override
    public Station unmarshal(StopType arg) throws Exception {
        throw new UnsupportedOperationException("Cannot unmarshal stations");
    }

    @Override
    public StopType marshal(Station arg) throws Exception {
        if (arg == null) {
            return null;
        }
        return new StopType(arg);
    }

}
