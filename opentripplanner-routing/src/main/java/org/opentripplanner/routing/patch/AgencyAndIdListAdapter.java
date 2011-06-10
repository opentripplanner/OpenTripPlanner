package org.opentripplanner.routing.patch;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.onebusaway.gtfs.model.AgencyAndId;

public class AgencyAndIdListAdapter extends XmlAdapter<ArrayList<AgencyAndIdType>, List<AgencyAndId>> {

	static final AgencyAndIdAdapter itemAdapter = new AgencyAndIdAdapter();
	
	@Override
	public List<AgencyAndId> unmarshal(ArrayList<AgencyAndIdType> arg) throws Exception {
		if (arg == null) {
			return null;
		}
		List<AgencyAndId> result = new ArrayList<AgencyAndId>(arg.size());
		for (AgencyAndIdType item: arg) {
			result.add(itemAdapter.unmarshal(item));					
		}
		return result;
	}

	@Override
	public ArrayList<AgencyAndIdType> marshal(List<AgencyAndId> arg) throws Exception {
		if (arg == null) {
			return null;
		}
		ArrayList<AgencyAndIdType> result = new ArrayList<AgencyAndIdType>(arg.size());
		for (AgencyAndId item: arg) {
			result.add(itemAdapter.marshal(item));					
		}
		return result;
	}

	
}
