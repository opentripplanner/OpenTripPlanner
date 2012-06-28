package org.opentripplanner.api.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.opentripplanner.common.model.P2;

public class ElevationAdapter extends XmlAdapter<String, List<P2<Double>>> {
    @Override
    public String marshal(List<P2<Double>> pairs) throws Exception {
        if (pairs == null)
            return null;
        StringBuilder str = new StringBuilder();
        for (P2<Double> pair : pairs) {
            str.append(Math.round(pair.getFirst()));
            str.append(",");
            str.append(Math.round(pair.getSecond() * 10.0) / 10.0);
        }
        if (str.length() > 0) 
            str.setLength(str.length() - 1);
        return str.toString();
    }

    @Override
    public List<P2<Double>> unmarshal(String data) throws Exception {
        if (data == null)
            return null;
        String[] values = data.split(",");
        ArrayList<P2<Double>> out = new ArrayList<P2<Double>>();
        for (int i = 0; i < values.length; i += 2) {
            P2<Double> value = new P2<Double>(Double.parseDouble(values[i]),
                    Double.parseDouble(values[i + 1]));
            out.add(value);
        }
        return out;
    }
}