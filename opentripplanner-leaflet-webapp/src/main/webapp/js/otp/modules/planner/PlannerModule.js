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

otp.namespace("otp.modules.planner");


otp.modules.planner.PlannerModule = 
    otp.Class(otp.modules.Module, {

    moduleName  : "Trip Planner",
    moduleId    : "planner",
    
    markerLayer     : null,
    pathLayer       : null,
    pathMarkerLayer : null,
    highlightLayer  : null,
    
    startMarker     : null,
    endMarker       : null,
    
    tipWidget       : null,
    noTripWidget    : null,
    tipStep         : 0,
    
    currentRequest  : null,
    currentHash : null,
    
    itinMarkers : [],

    planTripFunction : null,

    // current trip query parameters:
    startName               : null,
    endName                 : null,
    startLatLng             : null,
    endLatLng               : null,
    time                    : null,
    date                    : null,
    arriveBy                : false,
    mode                    : "TRANSIT,WALK",
    maxWalkDistance         : 804.672, // 1/2 mi.
    preferredRoutes         : null,
    bannedTrips             : null,
    optimize                : null,
    triangleTimeFactor      : 0.333,
    triangleSlopeFactor     : 0.333,
    triangleSafetyFactor    : 0.334,
    
    startTimePadding        : 0,
    
    // copy of query param set from last /plan request
    lastQueryParams : null,
    
    icons       : null,

    initialize : function(webapp) {
        otp.modules.Module.prototype.initialize.apply(this, arguments);
        this.icons = new otp.modules.planner.IconFactory();
        
        this.planTripFunction = this.planTrip;    
    },
    
    activate : function() {
        if(this.activated) return;
        var this_ = this;

        // set up layers        
        this.markerLayer = new L.LayerGroup();
        this.pathLayer = new L.LayerGroup();
        this.pathMarkerLayer = new L.LayerGroup();
        this.highlightLayer = new L.LayerGroup();
    
        this.addLayer("Highlights", this.highlightLayer);
        this.addLayer("Start/End Markers", this.markerLayer);
        this.addLayer("Paths", this.pathLayer);
        this.addLayer("Path Markers", this.pathMarkerLayer);
        
        this.activated = true;
        
        // set up primary widgets (TODO: move to bike planner module)
        /*this.tipWidget = this.createWidget("otp-tipWidget", "", this);
        this.addWidget(this.tipWidget);
        this.updateTipStep(1);
        
        this.bikestationsWidget = new otp.widgets.BikeStationsWidget('otp-bikestationsWidget', this);
        this.addWidget(this.bikestationsWidget);

        this.noTripWidget = new otp.widgets.Widget('otp-noTripWidget', this);
        this.addWidget(this.noTripWidget);*/
    },
    
    applyParameters : function() {
        // check URL params for restored trip
        if("fromPlace" in this.webapp.urlParams && "toPlace" in this.webapp.urlParams) {
            if("itinIndex" in this.webapp.urlParams) this.restoredItinIndex = this.webapp.urlParams["itinIndex"];
            this.restoreTrip(_.omit(this.webapp.urlParams, ["module", "itinIndex"]));
        }
    },
    
    addMapContextMenuItems : function() {
        var this_ = this;
        this.webapp.map.addContextMenuItem("Set as Start Location", function(latlng) {
            this_.setStartPoint(latlng, true);
        });
        this.webapp.map.addContextMenuItem("Set as End Location", function(latlng) {
            this_.setEndPoint(latlng, true);
        });
    },

    handleClick : function(event) {
        if(this.startLatLng == null) {
        	this.setStartPoint(new L.LatLng(event.latlng.lat, event.latlng.lng), true);
        }
        
        else if(this.endLatLng == null) {
        	this.setEndPoint(new L.LatLng(event.latlng.lat, event.latlng.lng), true);
        }
    },
    
    setStartPoint : function(latlng, update, name) {
        var this_ = this;
        this.startName = (typeof name !== 'undefined') ? name : null;
        this.startLatLng = latlng;
        if(this.startMarker == null) {
            this.startMarker = new L.Marker(this.startLatLng, {icon: this.icons.startFlag, draggable: true});
            this.startMarker.bindPopup('<strong>Start</strong>');
            this.startMarker.on('dragend', function() {
                this_.webapp.hideSplash();
                this_.startLatLng = this_.startMarker.getLatLng();
                if(typeof this_.userPlanTripStart == 'function') this_.userPlanTripStart();
                this_.planTripFunction.apply(this_);//planTrip();
            });
            this.markerLayer.addLayer(this.startMarker);
        }
        else { // marker already exists
            this.startMarker.setLatLng(latlng);
        }
        
        if(update) {
            this.updateTipStep(2);
            if(this.endLatLng) {
                if(typeof this.userPlanTripStart == 'function') this.userPlanTripStart();
                this_.planTripFunction.apply(this);//this.planTrip(); 
            }
        }
    },
    
    setEndPoint : function(latlng, update, name) {
        var this_ = this;
        this.endName = (typeof name !== 'undefined') ? name : null;
        this.endLatLng = latlng;    	 
        if(this.endMarker == null) {
            this.endMarker = new L.Marker(this.endLatLng, {icon: this.icons.endFlag, draggable: true}); 
            this.endMarker.bindPopup('<strong>Destination</strong>');
            this.endMarker.on('dragend', function() {
                this_.webapp.hideSplash();
                this_.endLatLng = this_.endMarker.getLatLng();
                if(typeof this_.userPlanTripStart == 'function') this_.userPlanTripStart();
                this_.planTripFunction.apply(this_);//this_.planTrip();
            });
            this.markerLayer.addLayer(this.endMarker);
        }
        else { // marker already exists
            this.endMarker.setLatLng(latlng);
        }
                 
        if(update) {
            if(this.startLatLng) {
                if(typeof this.userPlanTripStart == 'function') this.userPlanTripStart();
                this_.planTripFunction.apply(this);//this.planTrip();
            }
        }
    },
    
    getStartOTPString : function() {
        return (this.startName !== null ? this.startName + "::" : "")
                 + this.startLatLng.lat + ',' + this.startLatLng.lng;
    },

    getEndOTPString : function() {
        return (this.endName !== null ? this.endName + "::" : "")
                + this.endLatLng.lat+','+this.endLatLng.lng;
    },
        
    restoreTrip : function(queryParams) {    
        this.restoreMarkers(queryParams);
        this.planTripFunction.call(this, queryParams);
    },
    
    restoreMarkers : function(queryParams) {
      	this.startLatLng = otp.util.Geo.stringToLatLng(otp.util.Itin.getLocationPlace(queryParams.fromPlace));
    	this.setStartPoint(this.startLatLng, false);
    	
      	this.endLatLng = otp.util.Geo.stringToLatLng(otp.util.Itin.getLocationPlace(queryParams.toPlace));
    	this.setEndPoint(this.endLatLng, false);
    },
    
    planTrip : function(existingQueryParams, apiMethod) {
    
        if(typeof this.planTripStart == 'function') this.planTripStart();
        
        //this.noTripWidget.hide();
    	
    	if(this.currentRequest !== null)
        {
    		//console.log("Canceling current request.");
        	this.currentRequest.abort();
        	this.currentRequest = null;
        }
    	
    	apiMethod = apiMethod || 'plan';
        var url = otp.config.hostname + '/opentripplanner-api-webapp/ws/'+apiMethod;
        this.pathLayer.clearLayers();        
        
        var this_ = this;
        
        var queryParams = null;
        
        if(existingQueryParams) {
        	queryParams = existingQueryParams; 	        	
        }
        else
        {
            if(this.startLatLng == null || this.endLatLng == null) {
                // TODO: alert user
                return;
            }
            
            var addToStart = this.arriveBy ? 0 : this.startTimePadding;
       	    queryParams = {             
                fromPlace: this.getStartOTPString(),
                toPlace: this.getEndOTPString(),
                time : (this.time) ? this.time : moment().format("h:mma"),
                //time : (this.time) ? moment(this.time).add("s", addToStart).format("h:mma") : moment().add("s", addToStart).format("h:mma"),
                date : (this.date) ? this.date : moment().format("MM-DD-YYYY"),
                mode: this.mode,
                maxWalkDistance: this.maxWalkDistance
            };
            if(this.arriveBy !== null) _.extend(queryParams, { arriveBy : this.arriveBy } );
            if(this.preferredRoutes !== null) _.extend(queryParams, { preferredRoutes : this.preferredRoutes } );
            if(this.bannedTrips !== null) _.extend(queryParams, { bannedTrips : this.bannedTrips } );
            if(this.optimize !== null) _.extend(queryParams, { optimize : this.optimize } );
            if(this.optimize === 'TRIANGLE') {
                _.extend(queryParams, {
                    triangleTimeFactor: this_.triangleTimeFactor,
                    triangleSlopeFactor: this_.triangleSlopeFactor,
                    triangleSafetyFactor: this_.triangleSafetyFactor
                });
            } 
            _.extend(queryParams, this.getExtendedQueryParams());
            if(otp.config.routerId !== undefined) {
                queryParams.routerId = otp.config.routerId;
            }
        } 	
        $('#otp-spinner').show();
        
        this.lastQueryParams = queryParams;
        this.currentRequest = $.ajax(url, {
            data:       queryParams,
            dataType:   'jsonp',
                
            success: function(data) {
                $('#otp-spinner').hide();
                
                if(data.plan) {
                    // compare returned plan.date to sent date/time to determine timezone offset (unless set explicitly in config.js)
                    otp.config.timeOffset = (otp.config.timeOffset) ||
                        (moment(queryParams.date+" "+queryParams.time, "MM-DD-YYYY h:mma") - moment(data.plan.date))/3600000;

                    var tripPlan = new otp.modules.planner.TripPlan(data.plan, queryParams);

                    this_.processPlan(tripPlan, (existingQueryParams !== undefined));

                    this_.updateTipStep(3);
                    
                    /*if(!skipSave)
                    	this_.savePlan(queryParams);*/
                    
                }
                else {
                    this_.noTripFound(data.error);
                    //this_.noTripWidget.setContent(data.error.msg);
                    //this_.noTripWidget.show();
                }
            }
        });

    },
    
    getExtendedQueryParams : function() {
        return { };
    },
    
    processPlan : function(tripPlan, restoring) {
    },
    
    noTripFound : function(error) {
        $('<div>' + error.msg + ' (Error ' + error.id + ')</div>').dialog({
            title : "No Trip Found",
            modal: true
        });
    },
    
    drawItinerary : function(itin) {
        var this_ = this;
                
        this.pathLayer.clearLayers();
        this.pathMarkerLayer.clearLayers();
    
        var queryParams = itin.tripPlan.queryParams;
        
        console.log(itin.itinData);
        for(var i=0; i < itin.itinData.legs.length; i++) {
            var leg = itin.itinData.legs[i];

            // draw the polyline
            var polyline = new L.Polyline(otp.util.Polyline.decode(leg.legGeometry.points));
            var weight = 8;
            polyline.setStyle({ color : this.getModeColor(leg.mode), weight: weight});
            this.pathLayer.addLayer(polyline);
            polyline.leg = leg;
            polyline.bindPopup("("+leg.routeShortName+") "+leg.routeLongName);

            /* Attempt at hover functionality for trip segments on map; disabled due to "flickering" problem
               Alt. future approach: create invisible polygon buffers around polylines
            
            polyline.on('mouseover', function(e) {
                if(e.target.hover) return;
                console.log('mouseover');
                this_.highlightLeg(e.target.leg);
                this_.pathMarkerLayer.clearLayers();
                this_.drawStartBubble(e.target.leg, true);
                this_.drawEndBubble(e.target.leg, true);
                e.target.hover = true;
            });
            polyline.on('mouseout', function(e) {
                var lpt = e.layerPoint, minDist = 100;
                for(var p=0; p<e.target._parts[0].length-1; p++) {
                    var dist = L.LineUtil.pointToSegmentDistance(lpt, e.target._parts[0][p], e.target._parts[0][p+1]);
                    minDist = Math.min(minDist, dist)
                }
                console.log("minDist: "+minDist);
                if(minDist < weight/2) return;
                this_.clearHighlights();
                this_.pathMarkerLayer.clearLayers();
                this_.drawAllStartBubbles(itin);
                e.target.hover = false;
            });
            */
            
            if(otp.util.Itin.isTransit(leg.mode)) {
                this.drawStartBubble(leg, false);
            }
            else if(leg.mode === 'BICYCLE') {
                if(queryParams.mode === 'WALK,BICYCLE') { // bikeshare trip
                	polyline.bindPopup('Your '+otp.config.bikeshareName+' route');
                    //var start_and_end_stations = this.processStations(polyline.getLatLngs()[0], polyline.getLatLngs()[polyline.getLatLngs().length-1]);
                }
                else { // regular bike trip
                	polyline.bindPopup('Your bike route');
                	//this.resetStationMarkers();
                }	
            }
            else if(leg.mode === 'WALK') {
                if(queryParams.mode === 'WALK,BICYCLE') { 
                    if(i == 0) {
                    	polyline.bindPopup('Walk to the '+otp.config.bikeshareName+' dock.');
                    }
                    if(i == 2) {
                    	polyline.bindPopup('Walk from the '+otp.config.bikeshareName+' dock to your destination.');
                    }
                }
                else { // regular walking trip
                	polyline.bindPopup('Your walk route');
                	//this.resetStationMarkers();
                }
            }
        }
        
        this.webapp.map.lmap.fitBounds(itin.getBoundsArray());
    },
    
    highlightLeg : function(leg) {
        if(!leg.legGeometry) return;
        var polyline = new L.Polyline(otp.util.Polyline.decode(leg.legGeometry.points));
        polyline.setStyle({ color : "yellow", weight: 16, opacity: 0.3 });
        this.highlightLayer.addLayer(polyline);
    },
    
    clearHighlights : function() {
        this.highlightLayer.clearLayers(); 
    },
    
    drawStartBubble : function(leg, highlight) {
        var quadrant = (leg.from.lat < leg.to.lat ? 's' : 'n')+(leg.from.lon < leg.to.lon ? 'w' : 'e');
        var modeIcon = this.icons.getModeBubble(quadrant, leg.startTime, leg.mode, true, highlight);
        var marker = L.marker([leg.from.lat, leg.from.lon], {icon: modeIcon});
        this.pathMarkerLayer.addLayer(marker);
    },

    drawEndBubble : function(leg, highlight) {
        var quadrant = (leg.from.lat < leg.to.lat ? 'n' : 's')+(leg.from.lon < leg.to.lon ? 'e' : 'w');
        var modeIcon = this.icons.getModeBubble(quadrant, leg.endTime, leg.mode, false, highlight);
        var marker = L.marker([leg.to.lat, leg.to.lon], {icon: modeIcon});
        this.pathMarkerLayer.addLayer(marker);
    },
    
    drawAllStartBubbles : function(itin) {
        itin = itin.itinData;
        for(var i=0; i < itin.legs.length; i++) {
            var leg = itin.legs[i];
            if(otp.util.Itin.isTransit(leg.mode)) {
                this.drawStartBubble(leg, false);        
            }
        }
    },
    
    getModeColor : function(mode) {
        if(mode === "WALK") return '#444';
        if(mode === "BICYCLE") return '#0073e5';
        if(mode === "SUBWAY") return '#f00';
        if(mode === "BUS") return '#080';
        if(mode === "TRAM") return '#800';
        return '#aaa';
    },
        
    savePlan : function(data) {
    	
    	var data_ = {data: data, startLat: this.startLatLng.lat, startLon: this.startLatLng.lng, endLat: this.endLatLng.lat, endLon: this.endLatLng.lng, parrent : this.currentHash };
    	otp.util.DataStorage.store(data_, this );
    },
    
    // legacy -- deprecated by restoreTrip (above)
    restorePlan : function(data){
    	
    	this.startLatLng = new L.LatLng(data.startLat, data.startLon);
    	this.setStartPoint(this.startLatLng, false);
    	
    	this.endLatLng = new L.LatLng(data.endLat, data.endLon);
    	this.setEndPoint(this.endLatLng, false);
    	
    	this.webapp.setBounds(new L.LatLngBounds([this.startLatLng, this.endLatLng]));
    	
    	this.planTrip(data.data, true);
    },
        
    
    newTrip : function(hash) {
    	this.currentHash = hash;	
    	
    	window.location.hash = this.currentHash;
    	
        /*var shareRoute = $("#share-route");
        shareRoute.find(".addthis_toolbox").attr("addthis:url", otp.config.siteURL+"/#"+this.currentHash);
        addthis.toolbox(".addthis_toolbox_route");*/
    },
    
    distance : function(x1, y1, x2, y2) {
        return Math.sqrt((x1-x2)*(x1-x2) + (y1-y2)*(y1-y2));
    },
    
    updateTipStep : function(step) { // TODO: factor out to widget class
        /*if (step <= this.tipStep) return;
        if(step == 1) this.tipWidget.setContent("To Start: Click on the Map to Plan a Trip.");
        if(step == 2) this.tipWidget.setContent("Next: Click Again to Add Your Trip's End Point.");
        if(step == 3) this.tipWidget.setContent("Tip: Drag the Start or End Flags to Modify Your Trip.");
        
        this.tipStep = step;*/
    },
    
    CLASS_NAME : "otp.modules.planner.PlannerModule"
});

