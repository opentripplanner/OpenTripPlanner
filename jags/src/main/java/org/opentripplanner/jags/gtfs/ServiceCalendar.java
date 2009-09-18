package org.opentripplanner.jags.gtfs;


import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;

import org.opentripplanner.jags.gtfs.exception.DateOutOfBoundsException;
import org.opentripplanner.jags.gtfs.types.GTFSBoolean;
import org.opentripplanner.jags.gtfs.types.GTFSDate;

public class ServiceCalendar extends Record{
	
	public String service_id;
	public GTFSBoolean monday;
	public GTFSBoolean tuesday;
	public GTFSBoolean wednesday;
	public GTFSBoolean thursday;
	public GTFSBoolean friday;
	public GTFSBoolean saturday;
	public GTFSBoolean sunday;
	public GTFSDate start_date;
	public GTFSDate end_date;
	public HashMap<GTFSDate,ServiceCalendarDate> serviceCalendarDates;

	public ServiceCalendar(TableHeader header, String[] record) throws SecurityException,
			NoSuchFieldException, IllegalArgumentException,
			IllegalAccessException, InstantiationException,
			InvocationTargetException, NoSuchMethodException {
		super(header, record);
		this.serviceCalendarDates = new HashMap<GTFSDate,ServiceCalendarDate>();
	}
	
	public ServiceCalendar(ServiceCalendarDate scd) {
		super();
		this.serviceCalendarDates = new HashMap<GTFSDate,ServiceCalendarDate>();
		this.monday=new GTFSBoolean("0");
		this.tuesday=new GTFSBoolean("0");
		this.wednesday=new GTFSBoolean("0");
		this.thursday=new GTFSBoolean("0");
		this.friday=new GTFSBoolean("0");
		this.saturday=new GTFSBoolean("0");
		this.sunday=new GTFSBoolean("0");
		this.start_date=new GTFSDate(1900,1,1);
		this.end_date=new GTFSDate(2099,1,1);
		this.addServiceCalendarDate(scd);
	}
	
	public String toString() {
		return service_id + " " + this.start_date + " " + this.end_date + " ["+this.monday+this.tuesday+this.wednesday+this.thursday+this.friday+this.saturday+this.sunday+"]";
	}

	public void addServiceCalendarDate(ServiceCalendarDate serviceCalendarDate) {
		this.serviceCalendarDates.put(serviceCalendarDate.date, serviceCalendarDate);
	}

	public ArrayList<ServiceCalendarDate> getServiceCalendarDates() {
		return new ArrayList<ServiceCalendarDate>(serviceCalendarDates.values());
	}
	
	public ServiceCalendarDate getServiceCalendarDate(GTFSDate date) {
		return serviceCalendarDates.get(date);
	}
	
	public boolean runsOn( GTFSDate date ) {
		
		//Throw an exception if the date is out of bounds
		if( date.before( this.start_date) ) {
			throw new DateOutOfBoundsException( date+" is before period start "+this.start_date );
		}
		if( date.after( this.end_date) ) {
			throw new DateOutOfBoundsException( date+" is after period end "+this.end_date );
		}
		
		//Return false if service is canceled on this day; true of it specifically runs
		ServiceCalendarDate exception = this.getServiceCalendarDate(date);
		if(exception != null) {
			if( exception.exception_type.intValue() == ServiceCalendarDateExceptionType.ADDED ) {
				return true;
			} else if( exception.exception_type.intValue() == ServiceCalendarDateExceptionType.REMOVED ) {
				return false;
			}
		}
		
		//Return true if it runs on today's DOW
		GregorianCalendar cal = new GregorianCalendar(date.year,date.month-1,date.day);
		int dow = cal.get(GregorianCalendar.DAY_OF_WEEK);
		if((dow==GregorianCalendar.MONDAY    && this.monday.val   ) ||
		   (dow==GregorianCalendar.TUESDAY   && this.tuesday.val  ) ||
		   (dow==GregorianCalendar.WEDNESDAY && this.wednesday.val) ||
		   (dow==GregorianCalendar.THURSDAY  && this.thursday.val ) ||
		   (dow==GregorianCalendar.FRIDAY    && this.friday.val   ) ||
		   (dow==GregorianCalendar.SATURDAY  && this.saturday.val ) ||
		   (dow==GregorianCalendar.SUNDAY    && this.sunday.val   )) {
			return true;
		} else {
			return false;
		}
	}

	public boolean runsOn(GregorianCalendar time) {
		return runsOn( new GTFSDate(time.get(GregorianCalendar.YEAR),
				                    time.get(GregorianCalendar.MONTH)+1,
				                    time.get(GregorianCalendar.DATE)) );
	}

}
