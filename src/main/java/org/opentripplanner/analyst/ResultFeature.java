package org.opentripplanner.analyst;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.opentripplanner.analyst.pointset.PropertyMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ResultFeature implements Serializable{

	private static final long serialVersionUID = -6723127825189535112L;
	
	private static final Logger LOG = LoggerFactory.getLogger(ResultFeature.class);
    
	public String id;
	public Map<String,Histogram> histograms = new HashMap<String,Histogram>();
	
	public ResultFeature() {
		
	}
	
	public ResultFeature(SampleSet samples, TimeSurface surface){
    	id = samples.pset.id + "_" + surface.id;
    	
        PointSet targets = samples.pset;
        // Evaluate the surface at all points in the pointset
        int[] times = samples.eval(surface);
        buildHistograms(times, targets);
        
	}
	
    protected void buildHistograms(int[] times, PointSet targets) {
    	for (Entry<String, int[]> cat : targets.properties.entrySet()) {
        	String catId = cat.getKey();
        	int[] mags = cat.getValue();
        	this.histograms.put(catId, new Histogram(times, mags));
        }
    }
    
    
    /**
     * Each origin will yield CSV with columns category,min,q25,q50,q75,max
     * Another column for the origin ID would allow this to extend to many-to-many.
     */
    void toCsv() {

    }
    
    public void writeJson(OutputStream output) {
    	writeJson(output, null);
    }
    
	public void writeJson(OutputStream output, PointSet ps) {
		try {
			JsonFactory jsonFactory = new JsonFactory(); 
			
			JsonGenerator jgen = jsonFactory.createGenerator(output);
			jgen.setCodec(new ObjectMapper());
			
			jgen.writeStartObject(); {	
				
				if(ps == null) {
					jgen.writeObjectFieldStart("properties"); {
						if (id != null)
							jgen.writeStringField("id", id);
					}
					jgen.writeEndObject();
				}
				else {
					ps.writeJsonProperties(jgen);
				}
								
				jgen.writeObjectFieldStart("data"); {
					for(String propertyId : histograms.keySet()) {
						
						jgen.writeObjectFieldStart(propertyId); {
							histograms.get(propertyId).writeJson(jgen);
						}
						jgen.writeEndObject();
					
					}
				}
				jgen.writeEndObject();
			}
			jgen.writeEndObject();
			
			jgen.close();
		} catch (IOException ioex) {
			LOG.info("IOException, connection may have been closed while streaming JSON.");
		}
	}
}