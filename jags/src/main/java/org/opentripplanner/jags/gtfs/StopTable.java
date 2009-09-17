package org.opentripplanner.jags.gtfs;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;

import au.com.bytecode.opencsv.CSVReader;

public class StopTable implements Iterable<Stop>{
	TableHeader header;
	CSVReader reader;
	String[] nextElement;
	
	class TableIterator implements Iterator<Stop> {
		
		Stop nextElement;

		public boolean hasNext() {
			try {
				String[] record = reader.readNext();
				nextElement = new Stop(null,header,record);
				return nextElement != null;
			} catch( Exception ex ) {
				return false;
			}
		}

		public Stop next() {
			return nextElement;
		}

		public void remove() {}
		
	}
	
	StopTable( InputStream in ) throws IOException {
		reader = new CSVReader( new InputStreamReader( in ) );
		String[] columns = reader.readNext();
		header = new TableHeader(columns);
	}
	
	public TableHeader getHeader() {
		return header;
	}

	public Iterator<Stop> iterator() {
		return new TableIterator();
	}
}