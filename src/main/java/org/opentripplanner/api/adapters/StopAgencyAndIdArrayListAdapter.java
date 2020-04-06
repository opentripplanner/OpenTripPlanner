package org.opentripplanner.api.adapters;

import java.util.ArrayList;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.opentripplanner.model.Stop;

public class StopAgencyAndIdArrayListAdapter extends
        XmlAdapter<ArrayList<ApiFeedScopedId>, ArrayList<Stop>> {

    @Override
    public ArrayList<Stop> unmarshal(ArrayList<ApiFeedScopedId> arg) throws Exception {
        throw new UnsupportedOperationException(
                "We presently serialize stops as FeedScopedId, and thus cannot deserialize them");
    }

    @Override
    public ArrayList<ApiFeedScopedId> marshal(ArrayList<Stop> arg) throws Exception {
        if (arg == null) {
            return null;
        }
        ArrayList<ApiFeedScopedId> result = new ArrayList<ApiFeedScopedId>();
        for (Stop a : arg)
            result.add(new ApiFeedScopedId(a.getId().getFeedId(), a.getId().getId()));
        return result;
    }

}
