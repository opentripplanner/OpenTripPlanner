
/* This program is free software: you can redistribute it and/or
   modify it under the terms of the GNU Lesser General Public License
   as published by the Free Software Foundation, either version 3 of
   the License, or (at your option) any later version.
   
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.
   
   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>. 
*/

otp.namespace("otp.layers");
otp.layers.GeoJsonLayer = 
    otp.Class(L.LayerGroup, {
   
    module : null,
    
    initialize : function(module) {
	if(!otp.config.GeoJson || !otp.config.GeoJson.regions) return ;
        L.LayerGroup.prototype.initialize.apply(this);
        this.module = module;
        this.module.addLayer("geojson", this);
	var arrayLength = otp.config.GeoJson.regions.length;
	for (var i = 0; i < arrayLength; i++) {
		var region=otp.config.GeoJson.regions[i];
		if (otp.config.GeoJson.active === region.label) continue;
		this.fetchGeoJson(region);
	}
        //this.module.webapp.map.lmap.on('dragend zoomend', $.proxy(this.refresh, this));
    },
    
    fetchGeoJson : function(region) {
	var _this=this;
	return $.ajax({
	    url: region.geojson,
	    dataType: 'json',
	    success: function (response) {
	        region.mapObject = L.geoJson(response, {
	            style: otp.config.GeoJson.active === region.label ? activeStyle : style,
	            onEachFeature: otp.config.GeoJson.active === region.label ? onActiveEachFeature :onEachFeature,
		    name: region.label,
		    url: region.url
	        }).addTo(_this.module.webapp.map.lmap);
		console.log("setting "+region.mapObject+" to "+region.label + " as "+region.mapObject.options.name);
	    }
	 });	  
    },
});

function style(feature) {
    return {
        fillColor: 'green',
        weight: 2,
        opacity: 1,
        color: 'white',
        dashArray: '3',
        fillOpacity: 0.3
    };
}

function activeStyle(feature) {
    return {
	fillColor : null,
	fillOpacity : 0,
        weight: 3,
        opacity: 1,
	dashArray : '2',
        color: 'white'
    };
}


function highlightFeature(e) {
    var layer = e.target;

    layer.setStyle({
    	fillColor : 'purple',
        weight: 2,
        color: 'red',
        dashArray: '1',
        fillOpacity: 0.8
    });

    if (!L.Browser.ie && !L.Browser.opera) {
        layer.bringToFront();
    }
}
function highlightActiveFeature(e) {
    var layer = e.target;

    layer.setStyle({
	fillColor : null,
	fillOpacity : 0,
        weight: 3,
	dashArray : '2',
	opacity : 1,
        color: 'white'
    });

    if (!L.Browser.ie && !L.Browser.opera) {
        layer.bringToFront();
    }
}

function resetHighlight(e) {
	var arrayLength = otp.config.GeoJson.regions.length;
	for (var i = 0; i < arrayLength; i++) {
		var region=otp.config.GeoJson.regions[i];
		if (otp.config.GeoJson.active === region.label) continue;
	   	region.mapObject.resetStyle(region.mapObject);
	}
}

function onActiveEachFeature(feature, layer) {
    layer.on({
        mouseover: highlightActiveFeature,
        mouseout: resetHighlight,
//	click: this.module.webapp.mapClicked
    });
}

function onEachFeature(feature, layer) {
    layer.on({
        mouseover: highlightFeature,
        mouseout: resetHighlight,
        click: hyperlink
    });
}

function hyperlink(e){
	location.href = "/"+e.target.feature.properties.BRK_NAME;
}

