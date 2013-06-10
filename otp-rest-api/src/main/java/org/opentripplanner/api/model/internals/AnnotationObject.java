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

package org.opentripplanner.api.model.internals;

import javax.xml.bind.annotation.XmlAttribute;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class AnnotationObject {

    @JsonSerialize
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @XmlAttribute
    public String vertex;

    @JsonSerialize
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @XmlAttribute
    public Integer edge;

    @JsonSerialize
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @XmlAttribute
    public String message;

    @JsonSerialize
    @XmlAttribute
    public String agency;

    @JsonSerialize
    @XmlAttribute
    public String id;

}
