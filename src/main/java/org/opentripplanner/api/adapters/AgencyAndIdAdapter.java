package org.opentripplanner.api.adapters;

import org.opentripplanner.model.FeedScopedId;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class AgencyAndIdAdapter extends XmlAdapter<AgencyAndIdType, FeedScopedId> {

    @Override
    public FeedScopedId unmarshal(AgencyAndIdType arg) {
        if (arg == null) {
            return null;
        }
        return new FeedScopedId(arg.agency, arg.id);
    }

    @Override
    public AgencyAndIdType marshal(FeedScopedId arg) {
        if (arg == null) {
            return null;
        }
        return new AgencyAndIdType(arg.getFeedId(), arg.getId());
    }

}
