package org.opentripplanner.api.model;

import java.util.Date;

import javax.xml.bind.annotation.XmlAttribute;

/**
 *
 */

public class Leg {
    public long duration = 0;
    public Date start = null;
    public Date end = null;

    public Double distance = null;

    @XmlAttribute
    public String mode = "Walk";
    
    @XmlAttribute
    public String route = "";

    public Place from = null;

    public Place to = null;

    public EncodedPolylineBean legGeometry;
}
