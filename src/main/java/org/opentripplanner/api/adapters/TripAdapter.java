package org.opentripplanner.api.adapters;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.opentripplanner.model.Trip;

public class TripAdapter extends XmlAdapter<TripType, Trip> {

    @Override
    public Trip unmarshal(TripType arg) throws Exception {
        throw new UnsupportedOperationException("We presently serialize Trip as TripType, and thus cannot deserialize them");
    }

    @Override
    public TripType marshal(Trip arg) throws Exception {
        if (arg == null) {
            return null;
        }
        return new TripType(arg);
    }

}
