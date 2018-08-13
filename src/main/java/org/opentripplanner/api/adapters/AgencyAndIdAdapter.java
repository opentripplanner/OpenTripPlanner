package org.opentripplanner.api.adapters;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.opentripplanner.model.FeedId;

public class AgencyAndIdAdapter extends XmlAdapter<AgencyAndIdType, FeedId> {

    @Override
    public FeedId unmarshal(AgencyAndIdType arg) throws Exception {
        if (arg == null) {
            return null;
        }
        return new FeedId(arg.agency, arg.id);
    }

    @Override
    public AgencyAndIdType marshal(FeedId arg) throws Exception {
        if (arg == null) {
            return null;
        }
        return new AgencyAndIdType(arg.getAgencyId(), arg.getId());
    }

}
