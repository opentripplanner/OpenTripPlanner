package org.opentripplanner.routing.patch;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class MapAdaptor extends XmlAdapter<MapType, Map<String, String>> {

    @Override
    public MapType marshal(Map<String, String> v) throws Exception {
        MapType MapType = new MapType();
        List<Entry> aList = MapType.getEntry();
        for (Map.Entry<String, String> e : v.entrySet()) {
            aList.add(new Entry(e.getKey(), e.getValue()));
        }
        return MapType;
    }

    @Override
    public Map<String, String> unmarshal(MapType v) throws Exception {
        Map<String, String> map = new TreeMap<String, String>();
        for (Entry e : v.getEntry()) {
            map.put(e.key, e.value);
        }
        return map;
    }
}