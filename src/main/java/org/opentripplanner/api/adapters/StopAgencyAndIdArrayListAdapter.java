package org.opentripplanner.api.adapters;

import java.util.ArrayList;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.opentripplanner.model.Stop;

public class StopAgencyAndIdArrayListAdapter extends
        XmlAdapter<ArrayList<AgencyAndIdType>, ArrayList<Stop>> {

    @Override
    public ArrayList<Stop> unmarshal(ArrayList<AgencyAndIdType> arg) throws Exception {
        throw new UnsupportedOperationException(
                "We presently serialize stops as FeedScopedId, and thus cannot deserialize them");
    }

    @Override
    public ArrayList<AgencyAndIdType> marshal(ArrayList<Stop> arg) throws Exception {
        if (arg == null) {
            return null;
        }
        ArrayList<AgencyAndIdType> result = new ArrayList<AgencyAndIdType>();
        for (Stop a : arg)
            result.add(new AgencyAndIdType(a.getId().getAgencyId(), a.getId().getId()));
        return result;
    }

}
