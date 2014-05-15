/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (props, at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.api.adapters;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.onebusaway.gtfs.model.Agency;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@XmlRootElement(name = "Agency")
public class AgencyType {

    public AgencyType(String id, String name, String url, String timezone, String lang,
            String phone, String fareUrl) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.timezone = timezone;
        this.lang = lang;
        this.phone = phone;
        this.fareUrl = fareUrl;
    }

    public AgencyType(Agency arg) {
        this.id = arg.getId();
        this.name = arg.getName();
        this.url = arg.getUrl();
        this.timezone = arg.getTimezone();
        this.lang = arg.getLang();
        this.phone = arg.getPhone();
        this.fareUrl = arg.getFareUrl();
    }

    public AgencyType() {
    }

    @XmlAttribute
    @JsonSerialize
    String id;

    @XmlAttribute
    @JsonSerialize
    String name;

    @XmlAttribute
    @JsonSerialize
    String url;

    @XmlAttribute
    @JsonSerialize
    String timezone;

    @XmlAttribute
    @JsonSerialize
    String lang;

    @XmlAttribute
    @JsonSerialize
    String phone;

    @XmlAttribute
    @JsonSerialize
    String fareUrl;

}