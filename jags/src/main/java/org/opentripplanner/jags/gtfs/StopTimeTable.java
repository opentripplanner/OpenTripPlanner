package org.opentripplanner.jags.gtfs;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;

import au.com.bytecode.opencsv.CSVReader;

public class StopTimeTable implements Iterable<StopTime>{
	TableHeader header;
	CSVReader reader;
	
	class TableIterator implements Iterator<StopTime> {
		
		StopTime nextElement;

		public boolean hasNext() {
			try {
				String[] record = reader.readNext();
				nextElement = new StopTime(header,record);
				return nextElement != null;
			} catch( Exception ex ) {
				return false;
			}
		}

		public StopTime next() {
			return nextElement;
		}

		public void remove() {}
		
	}
	
	StopTimeTable( InputStream in ) throws IOException {
		reader = new CSVReader( new InputStreamReader( in ) );
		String[] columns = reader.readNext();
		header = new TableHeader(columns);
	}
	
	public TableHeader getHeader() {
		return header;
	}

	public Iterator<StopTime> iterator() {
		return new TableIterator();
	}
}