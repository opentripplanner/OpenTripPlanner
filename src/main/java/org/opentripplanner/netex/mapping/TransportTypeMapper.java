package org.opentripplanner.netex.mapping;

public class TransportTypeMapper {
    public int mapTransportType(String type){
        if("bus".equals(type)){
            return 3;
        }else if("tram".equals(type)){
            return 0;
        }else if("rail".equals(type)){
            return 2;
        }else if("metro".equals(type)){
            return 1;
        }else if("water".equals(type)){
            return 4;
        }else if("cabelway".equals(type)){
            return 5;
        }else if("funicular".equals(type)){
            return 7;
        }else if("air".equals(type)){
            return 1100; //extended GTFS traverse mode
        }
        else {
            //LOG.warn("Unknown transport type. Transport type will be set to bus. (" + type + ")");
            return 3;
        }
    }
}
