package org.opentripplanner.common;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * CTX parser for receiving Dutch realtime updates
 * 
 * @author skywave
 */
public class CTX {

    public ArrayList<HashMap<String, String>> rows = new ArrayList<HashMap<String, String>>();

    public String subscription;

    public String timestamp;

    public String type;

    public CTX(String ctx) {
        String[] lines = ctx.split("\r\n");
        String[] labels = null;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.length() > 0) {
                if (line.charAt(0) == '\\') {
                    if (line.charAt(1) == 'G') {
                        String[] header = line.split("\\|");
                        subscription = header[2];
                    } else if (line.charAt(1) == 'T') {
                        String[] table = line.split("\\|");
                        type = table[1];
                    } else if (line.charAt(1) == 'L') {
                        labels = line.substring(2).split("\\|");
                    }
                } else {
                    HashMap<String, String> row = new HashMap<String, String>();
                    String[] values = line.split("\\|");
                    for (int j = 0; j < values.length; j++) {
                        if (values[j] == "\0") {
                            values[j] = null;
                        }
                        row.put(labels[j], values[j]);
                    }
                    rows.add(row);
                }
            }
        }
    }
}
