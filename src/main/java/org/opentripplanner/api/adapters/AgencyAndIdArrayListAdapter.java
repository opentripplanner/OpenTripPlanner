package org.opentripplanner.api.adapters;

import java.util.ArrayList;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.opentripplanner.model.FeedScopedId;

public class AgencyAndIdArrayListAdapter extends XmlAdapter<ArrayList<AgencyAndIdType>, ArrayList<FeedScopedId>> {

    @Override
    public ArrayList<FeedScopedId> unmarshal(ArrayList<AgencyAndIdType> arg) throws Exception {
        if (arg == null) {
            return null;
        }
        ArrayList<FeedScopedId> result = new ArrayList<FeedScopedId>();
        for (AgencyAndIdType a : arg)
            result.add(new FeedScopedId(a.agency, a.id));
        return result;
    }

    @Override
    public ArrayList<AgencyAndIdType> marshal(ArrayList<FeedScopedId> arg) throws Exception {
        if (arg == null) {
            return null;
        }
        ArrayList<AgencyAndIdType> result = new ArrayList<AgencyAndIdType>();
        for(FeedScopedId a:arg) result.add(new AgencyAndIdType(a.getAgencyId(), a.getId()));
        return result;
    }

}
