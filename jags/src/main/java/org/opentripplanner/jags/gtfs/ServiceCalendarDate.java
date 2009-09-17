package org.opentripplanner.jags.gtfs;


import java.lang.reflect.InvocationTargetException;

import org.opentripplanner.jags.gtfs.types.GTFSDate;

public class ServiceCalendarDate extends Record{
	
	public String service_id;
	public GTFSDate date;
	public Integer exception_type;

	ServiceCalendarDate(PackagedFeed feed, TableHeader header, String[] record) throws SecurityException,
			NoSuchFieldException, IllegalArgumentException,
			IllegalAccessException, InstantiationException,
			InvocationTargetException, NoSuchMethodException {
		super(feed, header, record);
	}
	
	public String toString() {
		return service_id +" "+date+" "+exception_type;
	}

}
