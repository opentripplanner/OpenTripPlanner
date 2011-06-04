package org.opentripplanner.routing.patch;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.onebusaway.gtfs.model.AgencyAndId;

public class AgencyAndIdAdapter extends XmlAdapter<AgencyAndIdType, AgencyAndId>{

	@Override
	public AgencyAndId unmarshal(AgencyAndIdType arg) throws Exception {
		return new AgencyAndId(arg.agency, arg.id);
	}

	@Override
	public AgencyAndIdType marshal(AgencyAndId arg) throws Exception {
		return new AgencyAndIdType(arg.getAgencyId(), arg.getId());
	}

	
}
