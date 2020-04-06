package org.opentripplanner.api.adapters;

import java.util.ArrayList;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.opentripplanner.model.FeedScopedId;

public class AgencyAndIdArrayListAdapter extends XmlAdapter<ArrayList<ApiFeedScopedId>, ArrayList<FeedScopedId>> {

    @Override
    public ArrayList<FeedScopedId> unmarshal(ArrayList<ApiFeedScopedId> arg) throws Exception {
        if (arg == null) {
            return null;
        }
        ArrayList<FeedScopedId> result = new ArrayList<FeedScopedId>();
        for (ApiFeedScopedId a : arg)
            result.add(new FeedScopedId(a.agency, a.id));
        return result;
    }

    @Override
    public ArrayList<ApiFeedScopedId> marshal(ArrayList<FeedScopedId> arg) throws Exception {
        if (arg == null) {
            return null;
        }
        ArrayList<ApiFeedScopedId> result = new ArrayList<ApiFeedScopedId>();
        for(FeedScopedId a:arg) result.add(new ApiFeedScopedId(a.getFeedId(), a.getId()));
        return result;
    }

}
