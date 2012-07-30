package org.opentripplanner.routing.core;

import java.io.Serializable;
import java.util.HashMap;

import org.onebusaway.gtfs.model.AgencyAndId;

public class ServiceIdToNumberService implements Serializable {
    private static final long serialVersionUID = -8447673406675368532L;
    
    HashMap<AgencyAndId, Integer> numberForServiceId;
    
    
    public ServiceIdToNumberService(HashMap<AgencyAndId, Integer> serviceIds) {
        this.numberForServiceId = serviceIds;
    }


    public int getNumber(AgencyAndId serviceId) {
        Integer number = numberForServiceId.get(serviceId);
        if (number == null) {
            return numberForServiceId.size() + 1;
        }
        return number;
    }

}
