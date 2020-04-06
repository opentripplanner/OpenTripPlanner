package org.opentripplanner.api.adapters;

import org.opentripplanner.api.model.ApiFeedScopedId;
import org.opentripplanner.model.FeedScopedId;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class AgencyAndIdAdapter extends XmlAdapter<ApiFeedScopedId, FeedScopedId> {

    @Override
    public FeedScopedId unmarshal(ApiFeedScopedId arg) {
        if (arg == null) {
            return null;
        }
        return new FeedScopedId(arg.agency, arg.id);
    }

    @Override
    public ApiFeedScopedId marshal(FeedScopedId arg) {
        if (arg == null) {
            return null;
        }
        return new ApiFeedScopedId(arg.getFeedId(), arg.getId());
    }

}
