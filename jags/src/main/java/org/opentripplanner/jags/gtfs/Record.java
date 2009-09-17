package org.opentripplanner.jags.gtfs;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

public class Record {
	Table table;
	
	Record() {
	}
	
	Record(Table table, String[] record) throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
		this.table = table;
		
		Field[] fields = this.getClass().getFields();
		for(int i=0; i<fields.length; i++) {
			Field field = fields[i];
			int ix = table.getHeader().index( field.getName() );
			if( ix != -1 ) {
				try {
					Constructor<?> stringConstructor = field.getType().getConstructor(String.class);
					if( record[ix].length()==0 ) {
						field.set( this, null );
					} else {
						field.set( this, stringConstructor.newInstance(record[ix]) );
					}
				} catch( NoSuchMethodException ex) {
					throw new NoSuchMethodException( "Class: "+field.getType().getName()+" does not have constructor for ("+String.class+")" );
				}
				
			} else {
				field.set( this, null );
			}
		}
	}
}
