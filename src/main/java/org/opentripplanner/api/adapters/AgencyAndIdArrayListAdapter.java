package org.opentripplanner.api.adapters;

import java.util.ArrayList;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.opentripplanner.model.FeedId;

public class AgencyAndIdArrayListAdapter extends XmlAdapter<ArrayList<AgencyAndIdType>, ArrayList<FeedId>> {

    @Override
    public ArrayList<FeedId> unmarshal(ArrayList<AgencyAndIdType> arg) throws Exception {
        if (arg == null) {
            return null;
        }
        ArrayList<FeedId> result = new ArrayList<FeedId>();
        for (AgencyAndIdType a : arg)
            result.add(new FeedId(a.agency, a.id));
        return result;
    }

    @Override
    public ArrayList<AgencyAndIdType> marshal(ArrayList<FeedId> arg) throws Exception {
        if (arg == null) {
            return null;
        }
        ArrayList<AgencyAndIdType> result = new ArrayList<AgencyAndIdType>();
        for(FeedId a:arg) result.add(new AgencyAndIdType(a.getAgencyId(), a.getId()));
        return result;
    }

}
