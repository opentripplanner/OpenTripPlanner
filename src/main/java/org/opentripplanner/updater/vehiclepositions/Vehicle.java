/**
 * Copyright (C) 2012 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opentripplanner.updater.vehiclepositions;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class Vehicle implements Serializable{
	private static final long serialVersionUID = 3333460609708083333L;

    @XmlAttribute
    @JsonSerialize	
	public String id;
    @XmlElement
    @JsonSerialize	
    public String agencyId;
    @XmlElement
    @JsonSerialize	
    public double lat;
    @XmlElement
    @JsonSerialize	
    public double lon;
    @XmlElement
    @JsonSerialize	
    public float bearing;
    @XmlElement
    @JsonSerialize	
    public String routeId;
    @XmlElement
    @JsonSerialize	
    public long lastUpdate;  

}