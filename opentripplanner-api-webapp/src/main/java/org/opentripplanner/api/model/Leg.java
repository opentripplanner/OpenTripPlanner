package org.opentripplanner.api.model;

import javax.xml.bind.annotation.XmlAttribute;

/**
 *
 */

public class Leg {

    @XmlAttribute
    public String mode = "Walk";

    public Place from = new Place();

    public Place to = new Place();

    public EncodedPolylineBean legGeometry;
}
