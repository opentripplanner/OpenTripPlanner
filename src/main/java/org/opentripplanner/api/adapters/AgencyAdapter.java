package org.opentripplanner.api.adapters;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.opentripplanner.model.Agency;

public class AgencyAdapter extends XmlAdapter<AgencyType, Agency> {

    @Override
    public Agency unmarshal(AgencyType arg) throws Exception {
        if (arg == null) {
            return null;
        }
        Agency a = new Agency();
        a.setId(arg.id);
        a.setName(arg.name);
        a.setUrl(arg.url);
        a.setTimezone(arg.timezone);
        a.setLang(arg.lang);
        a.setPhone(arg.phone);
        a.setFareUrl(arg.fareUrl);
        return new Agency(a);
    }

    @Override
    public AgencyType marshal(Agency arg) throws Exception {
        if (arg == null) {
            return null;
        }
        return new AgencyType(arg);
    }

}
