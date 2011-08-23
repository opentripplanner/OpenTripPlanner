package org.opentripplanner.routing.patch;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

/**
 * Represents a period of time, in terms of seconds in [start, end)
 * @author novalis
 *
 */
@XmlType
public class TimePeriod {
    public TimePeriod(long start, long end) {
        this.startTime = start;
        this.endTime = end;
    }

    public TimePeriod() {
    }

    @XmlAttribute
    public long startTime;

    @XmlAttribute
    public long endTime;
}
