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

otp.namespace("otp.core");

otp.core.Map = otp.Class({

    webapp          : null,

    lmap            : null,
    layerControl    : null,
    
    initialize : function(webapp) {
    
        this.webapp = webapp;
        
        var tileLayer = new L.TileLayer(otp.config.tileUrl, {attribution: otp.config.tileAttrib});
	    
	    if(typeof otp.config.getTileUrl != 'undefined') {
    	    tileLayer.getTileUrl = otp.config.getTileUrl;
        }
        
        this.lmap = new L.Map('map', {
            minZoom : otp.config.minZoom,
            maxZoom : otp.config.maxZoom,
            center  : otp.config.initLatLng,
            zoom    : otp.config.initZoom,
            layers  : [ tileLayer ]
        });


        var baseMaps = {
            'Base Layer' : tileLayer 
        };
        
        //this.lmap.setView(otp.config.initLatLng, otp.config.initZoom); //.addLayer(tileLayer);
        
        var overlays = { };
        
        if(typeof otp.config.overlayTileUrl != 'undefined') {
	    	var overlayTileLayer = new L.TileLayer(otp.config.overlayTileUrl);
	    	//this.lmap.addLayer(overlayTileLayer);
	    	//overlays['Overlay'] = overlayTileLayer;
        }
        
        //this.layerControl = new L.Control.Layers(baseMaps, overlays);
        //this.layerControl.addTo(this.lmap);
        
        this.lmap.on('click', function(event) {
            webapp.mapClicked(event);        
        });
    },
    
    activeModuleChanged : function(oldModule, newModule) {
        
        //console.log("actModChanged: "+oldModule+", "+newModule);
        if(oldModule != null) {
            for(var layerName in oldModule.mapLayers) {
                
                var layer = oldModule.mapLayers[layerName];
                this.lmap.removeLayer(layer);                
                //this.layerControl.removeLayer(layer);
            }
        }

        for(var layerName in newModule.mapLayers) {
        
            var layer = newModule.mapLayers[layerName];
            this.lmap.addLayer(layer);
            var this_ = this;
        }
    },
    
    setBounds : function(bounds)
    {
    	this.lmap.fitBounds(bounds);
    },
    
    CLASS_NAME : "otp.core.Map"
});

