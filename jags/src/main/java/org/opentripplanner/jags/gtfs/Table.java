package org.opentripplanner.jags.gtfs;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;

import au.com.bytecode.opencsv.CSVReader;

public class Table implements Iterable<String[]>{
	TableHeader header;
	CSVReader reader;
	String[] nextElement;
	
	class TableIterator implements Iterator<String[]> {
		
		String[] nextElement;

		public boolean hasNext() {
			try {
				nextElement = reader.readNext();
				return nextElement != null;
			} catch( IOException ex ) {
				return false;
			}
		}

		public String[] next() {
			return nextElement;
		}

		public void remove() {}
		
	}
	
	Table( PackagedFeed feed, InputStream in ) throws IOException {
		reader = new CSVReader( new InputStreamReader( in ) );
		String[] columns = reader.readNext();
		header = new TableHeader(columns);
	}
	
	public TableHeader getHeader() {
		return header;
	}

	public Iterator<String[]> iterator() {
		return new TableIterator();
	}
}
