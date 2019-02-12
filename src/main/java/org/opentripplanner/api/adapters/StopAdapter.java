package org.opentripplanner.api.adapters;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.opentripplanner.model.Stop;

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
        a.setDesc(arg.stopDesc);
        a.setLat(arg.stopLat);
        a.setLon(arg.stopLon);
        a.setZoneId(arg.zoneId);
        a.setUrl(arg.stopUrl);
        a.setLocationType(arg.locationType);
        a.setParentStation(arg.parentStation);
        a.setWheelchairBoarding(arg.wheelchairBoarding);
        a.setDirection(arg.direction);
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
