/* 
 Copyright (C) 2016 University of South Florida.
 All rights reserved.

 This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.
 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.
 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. 
*/

package org.opentripplanner.api.resource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlRootElement;

import org.opentripplanner.api.model.transit.AgencyList;
import org.opentripplanner.updater.vehiclepositions.GtfsRealtimeHttpVehiclePositionSource;
import org.opentripplanner.updater.vehiclepositions.PollingVehiclePositionsUpdater;
import org.opentripplanner.updater.vehiclepositions.Vehicle;
import org.opentripplanner.updater.vehiclepositions.VehiclePositionSource;

import com.google.transit.realtime.GtfsRealtime.Position;
import com.google.transit.realtime.GtfsRealtime.VehiclePosition;

@Path("/vehicle_positions")
@XmlRootElement
public class VehiclePositions {
	
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    	public VehiclePositionsList getVehiclePositions(
    			@QueryParam("routeid") String routeId,
    			@QueryParam("agencyid") String agencyId){
    		
    		List<String> vehicleIds = org.opentripplanner.updater.vehiclepositions.PollingVehiclePositionsUpdater.vehicleIds;
    		Map<String,Vehicle> vehiclesById = org.opentripplanner.updater.vehiclepositions.PollingVehiclePositionsUpdater.vehiclesById;
    		
    		//if null returns new empty list:
    		if(vehicleIds == null){return new VehiclePositionsList();}
    	
    		//sets data list to vehicles
    		List<Vehicle> vehicles = new ArrayList<Vehicle>();
    		for (int x = 0; x < vehicleIds.size(); x++){
    			//getting info from the updater
    			String vehicleId = vehicleIds.get(x);
    			Vehicle v = vehiclesById.get(vehicleId);
    			if(routeId != null && agencyId != null){
    				if (v.routeId.equals(routeId) && v.agencyId.equals(agencyId)){vehicles.add(v);}
    			}
    			else if(routeId != null){
    				if(v.routeId.equals(routeId)){
    					vehicles.add(v);
    					}
    			}
    			else if(agencyId != null){
    				if (v.agencyId.equals(agencyId)){vehicles.add(v);}
    			}
    			else{vehicles.add(v);}
    		}
    		
    		VehiclePositionsList vehicleList = new VehiclePositionsList();
    		vehicleList.vehicles = vehicles;
    		return vehicleList;
    }
}
