/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.graph_builder.module.shapefile;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import org.opengis.feature.simple.SimpleFeature;
import org.opentripplanner.graph_builder.services.shapefile.SimpleFeatureConverter;

/** 
 * Reads a single attribute from a feature and converts it to an object */
public class AttributeFeatureConverter<T> implements SimpleFeatureConverter<T> {

    private String _attributeName;
    private boolean decodeUTF8 = true;

    public AttributeFeatureConverter(String attributeName) {
        _attributeName = attributeName;
    }
    
    public AttributeFeatureConverter() {
        
    }
    
    public void setAttributeName(String attributeName){
        _attributeName = attributeName;
    }
    
    public String getAttributeName() {
        return _attributeName;
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    @Override
    public T convert(SimpleFeature feature) {
        T value = (T) feature.getAttribute(_attributeName);
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
