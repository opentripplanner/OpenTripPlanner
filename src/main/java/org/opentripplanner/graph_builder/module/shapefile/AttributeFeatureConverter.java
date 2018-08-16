package org.opentripplanner.graph_builder.module.shapefile;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import org.opengis.feature.simple.SimpleFeature;
import org.opentripplanner.graph_builder.services.shapefile.SimpleFeatureConverter;

/** 
 * Reads a single attribute from a feature and converts it to an object */
public class AttributeFeatureConverter<T> implements SimpleFeatureConverter<T> {

    private String attributeName;
    private boolean decodeUTF8 = true;

    public AttributeFeatureConverter(String attributeName) {
        this.attributeName = attributeName;
    }
    
    public AttributeFeatureConverter() {
        
    }
    
    public void setAttributeName(String attributeName){
        this.attributeName = attributeName;
    }
    
    public String getAttributeName() {
        return attributeName;
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    @Override
    public T convert(SimpleFeature feature) {
        T value = (T) feature.getAttribute(attributeName);
        if (value instanceof String && decodeUTF8) {
            String str = (String) value;
            //decode UTF-8, irritatingly
            Charset charset = Charset.forName("UTF-8");
            byte[] bytes = new byte[str.length()];
            //we have to use a deprecated method because it's the only one that works.
            str.getBytes(0, str.length(), bytes, 0);
            ByteBuffer bb = ByteBuffer.wrap(bytes);
            value = (T) charset.decode(bb).toString();
        }
        return value;
    }

    public boolean isDecodeUTF8() {
        return decodeUTF8;
    }

    public void setDecodeUTF8(boolean decodeUTF8) {
        this.decodeUTF8 = decodeUTF8;
    }

}
