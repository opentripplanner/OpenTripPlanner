package org.opentripplanner.api.adapters;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Stop;

public class StopAgencyAndIdAdapter extends XmlAdapter<ApiFeedScopedId, Stop> {

    @Override
    public Stop unmarshal(ApiFeedScopedId arg) throws Exception {
        throw new UnsupportedOperationException(
                "We presently serialize stops as FeedScopedId, and thus cannot deserialize them");
    }

    @Override
    public ApiFeedScopedId marshal(Stop arg) throws Exception {
        if (arg == null) {
            return null;
        }
        FeedScopedId id = arg.getId();
        return new ApiFeedScopedId(id.getFeedId(), id.getId());
    }

}
