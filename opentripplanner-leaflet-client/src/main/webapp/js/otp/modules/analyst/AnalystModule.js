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

otp.namespace("otp.modules.analyst");


otp.modules.analyst.AnalystModule = 
    otp.Class(otp.modules.planner.PlannerModule, {
    
    moduleName  : "Analyst",

    analystLayer : null,

             
    initialize : function(webapp, id, options) {
        //otp.modules.planner.PlannerModule.prototype.initialize.apply(this, arguments);
        otp.modules.planner.PlannerModule.prototype.initialize.apply(this, arguments);
        
        this.analystUrl = otp.config.hostname + "/opentripplanner-api-webapp/ws/tile/{z}/{x}/{y}.png";
    },

    activate : function() {
        if(this.activated) return;
        var this_ = this;
        
        otp.modules.planner.PlannerModule.prototype.activate.apply(this);

        // set up travel options widget
        this.optionsWidget = new otp.widgets.tripoptions.TripOptionsWidget('otp-' + this.id + '-optionsWidget', this);

        /*if(this.webapp.geocoders && this.webapp.geocoders.length > 0) {
            this.optionsWidget.addControl("locations", new otp.widgets.tripoptions.LocationsSelector(this.optionsWidget, this.webapp.geocoders), true);
            this.optionsWidget.addVerticalSpace(12, true);
        }*/
                
        this.optionsWidget.addControl("time", new otp.widgets.tripoptions.TimeSelector(this.optionsWidget), true);
        this.optionsWidget.addVerticalSpace(12, true);
        
        
        var modeSelector = new otp.widgets.tripoptions.ModeSelector(this.optionsWidget);
        this.optionsWidget.addControl("mode", modeSelector, true);

        modeSelector.addModeControl(new otp.widgets.tripoptions.MaxWalkSelector(this.optionsWidget));
        modeSelector.addModeControl(new otp.widgets.tripoptions.BikeTriangle(this.optionsWidget));
        //modeSelector.addModeControl(new otp.widgets.tripoptions.PreferredRoutes(this.optionsWidget));

        modeSelector.refreshModeControls();

        var buttonRow = $('<div style="text-align: center; margin-top: 6px;"></div>')
        .appendTo(this.optionsWidget.$());
        
        $('<button>Refresh</button>').button().click(function() {
            this_.runAnalystQuery();            
        }).appendTo(buttonRow);

        this.optionsWidget.applyQueryParams(this.defaultQueryParams);


        // set up legend widget 
        this.legendWidget = new otp.modules.analyst.AnalystLegendWidget(this.id + 'legend', this, 300, 40);

        // set up location marker
        this.startLatLng = this.webapp.map.lmap.getCenter();
        
        this.locMarker = new L.Marker(this.startLatLng, {draggable: true});
        //locMarker.bindPopup('<strong>Start</strong>');
        this.locMarker.on('dragend', function() {
            this_.webapp.hideSplash();
            this_.startLatLng = this_.locMarker.getLatLng();
            this_.runAnalystQuery();
        });
        this.markerLayer.addLayer(this.locMarker);

        this_.runAnalystQuery();

    },

    addMapContextMenuItems : function() {
        var this_ = this;
        this.webapp.map.addContextMenuItem("Set as Analysis Location", function(latlng) {
            this_.locMarker.setLatLng(latlng);
            this_.startLatLng = this_.locMarker.getLatLng();
            this_.runAnalystQuery();
        });
    },    
    
    runAnalystQuery : function() {

	    var params = { 
		    batch : true,
		    layers : 'traveltime',
		    styles : 'color30',
		    
            time : (this.time) ? this.time : moment().format("h:mma"),
            date : (this.date) ? this.date : moment().format("MM-DD-YYYY"),
            mode : this.mode,
	        maxWalkDistance : this.maxWalkDistance,
	        arriveBy : this.arriveBy,
	        fromPlace : this.startLatLng.lat+','+this.startLatLng.lng,
	        toPlace : this.startLatLng.lat+','+this.startLatLng.lng
        }

        	
        var URL = this.analystUrl + this.buildQuery(params);
        console.log(params);
        console.log(URL);
        
        // is there a better way to trigger a refresh than removing and re-adding?
	    if(this.analystLayer != null)
		    this.webapp.map.lmap.removeLayer(this.analystLayer);
        else {
            this.analystLayer = new L.TileLayer(this.analystUrl, {zIndex : 100});
            this.addLayer("Analyst Accessibility", this.analystLayer)
        }
            
	    this.analystLayer._url = URL;
        this.webapp.map.lmap.addLayer(this.analystLayer);

        this.legendWidget.refresh(params);
	    //legend.src = "/opentripplanner-api-webapp/ws/legend.png?width=300&height=40&styles=" + params.styles;
	    //return false;*/
    },     

    // convert a map of query parameters into a query string, 
    // expanding Array values into multiple query parameters
    buildQuery :  function(params) {
	    ret = [];
	    for (key in params) {
		    vals = params[key];
		    // wrap scalars in array
		    if ( ! (vals instanceof Array)) {
		        var v = vals;
		        vals = new Array();
		        vals[0] = v;
		    }
		    for (i in vals) { 
			    val = vals[i]; // js iterates over indices not values!
			    // skip params that are empty or stated to be the same as previous
			    if (val == '' || val == 'same')
				    continue;
			    param = [encodeURIComponent(key), encodeURIComponent(val)].join('=');
			    ret.push(param);
		    }
	    }
	    return "?" + ret.join('&');
    },
     
});
