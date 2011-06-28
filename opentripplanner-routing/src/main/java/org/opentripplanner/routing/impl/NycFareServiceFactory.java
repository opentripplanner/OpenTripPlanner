package org.opentripplanner.routing.impl;

import org.onebusaway.gtfs.services.GtfsRelationalDao;
import org.opentripplanner.routing.services.FareService;
import org.opentripplanner.routing.services.FareServiceFactory;

public class NycFareServiceFactory implements FareServiceFactory {
    
	public FareService makeFareService() {
		return new NycFareServiceImpl(); 
	}

        
	@Override
	public void setDao(GtfsRelationalDao dao) {
	}
}
