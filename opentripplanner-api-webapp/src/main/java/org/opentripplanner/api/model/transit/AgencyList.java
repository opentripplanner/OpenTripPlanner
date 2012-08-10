package org.opentripplanner.api.model.transit;

import java.util.Collection;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.onebusaway.gtfs.model.Agency;
import org.opentripplanner.routing.transit_index.adapters.AgencyAdapter;

@XmlRootElement
public class AgencyList {

    @XmlJavaTypeAdapter(value = AgencyAdapter.class)
    @XmlElement(name = "Agency")
    public Collection<Agency> agencies;
}
