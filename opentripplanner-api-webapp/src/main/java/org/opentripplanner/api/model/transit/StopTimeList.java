package org.opentripplanner.api.model.transit;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="StopTimeList")
public class StopTimeList {

    @XmlElements(value=@XmlElement(name="stopTime"))
    public List<StopTime> stopTimes;
}
