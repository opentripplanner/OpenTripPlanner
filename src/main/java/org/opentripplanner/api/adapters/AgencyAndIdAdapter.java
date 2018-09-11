package org.opentripplanner.api.adapters;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.opentripplanner.model.FeedScopedId;

public class AgencyAndIdAdapter extends XmlAdapter<AgencyAndIdType, FeedScopedId> {

    @Override
    public FeedScopedId unmarshal(AgencyAndIdType arg) throws Exception {
        if (arg == null) {
            return null;
        }
        return new FeedScopedId(arg.agency, arg.id);
    }

    @Override
    public AgencyAndIdType marshal(FeedScopedId arg) throws Exception {
        if (arg == null) {
            return null;
        }
        return new AgencyAndIdType(arg.getAgencyId(), arg.getId());
    }

}
