package org.opentripplanner.api.adapters;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.opentripplanner.model.Stop;

import static org.opentripplanner.api.adapters.WheelChairCodeMapper.mapFromWheelChairCode;

// TODO What is this used for? This needs to map either Stop or Station depending on location type

public class StopAdapter extends XmlAdapter<StopType, Stop> {

    @Override
    public Stop unmarshal(StopType arg) throws Exception {
        if (arg == null) {
            return null;
        }
        Stop a = new Stop();
        a.setId(arg.id);
        a.setName(arg.stopName);
        a.setCode(arg.stopCode);
        a.setDescription(arg.stopDesc);
        a.setLat(arg.stopLat);
        a.setLon(arg.stopLon);
        a.setZone(arg.zoneId);
        a.setUrl(arg.stopUrl);
        //a.setParentStation(arg.parentStation);
        a.setWheelchairBoarding(mapFromWheelChairCode(arg.wheelchairBoarding));
        return new Stop(a);
    }

    @Override
    public StopType marshal(Stop arg) throws Exception {
        if (arg == null) {
            return null;
        }
        return new StopType(arg);
    }

}
