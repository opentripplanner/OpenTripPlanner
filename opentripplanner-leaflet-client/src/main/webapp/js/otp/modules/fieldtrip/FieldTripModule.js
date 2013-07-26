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

otp.namespace("otp.modules.fieldtrip");


otp.modules.fieldtrip.FieldTripModule = 
    otp.Class(otp.modules.multimodal.MultimodalPlannerModule, {

    moduleName  : "Field Trip Planner",
    
    itinWidget  : null,
    
    requestWidgets : null, // object, maps request ids to widget instances
    
    groupSize   : 100,
    bannedSegments : null,
    //itineraries : null,
    groupPlan : null,
    
    datastoreUrl : otp.config.datastoreUrl,
    
    geocoderWidgets : null, 
    geocodedOrigins : null, 
    geocodedDestinations : null, 
    
    sessionManager : null,
    
    showIntermediateStops : true,
    
    templateFile : 'otp/modules/fieldtrip/fieldtrip-templates.html',

    
    initialize : function(webapp, id, options) {
        otp.modules.multimodal.MultimodalPlannerModule.prototype.initialize.apply(this, arguments);
        
        this.planTripFunction = this.ftPlanTrip;
        this.requestWidgets = {};
        this.geocoderWidgets = {};
        this.geocodedOrigins = {};
        this.geocodedDestinations = {};
    },

    activate : function() {
        if(this.activated) return;
        console.log("activate "+this.id);
        otp.modules.multimodal.MultimodalPlannerModule.prototype.activate.apply(this);

        /*this.optionsWidget = new otp.widgets.tripoptions.TripOptionsWidget('otp-'+this.id+'-optionsWidget', this);
        
        if(this.webapp.geocoders && this.webapp.geocoders.length > 0) {
            this.optionsWidget.addControl("locations", new otp.widgets.tripoptions.LocationsSelector(this.optionsWidget, this.webapp.geocoders), true);
            this.optionsWidget.addVerticalSpace(12, true);
        }

        this.optionsWidget.addControl("time", new otp.widgets.tripoptions.TimeSelector(this.optionsWidget), true);
        this.optionsWidget.addVerticalSpace(12, true);
        
        
        var modeSelector = new otp.widgets.tripoptions.ModeSelector(this.optionsWidget);
        this.optionsWidget.addControl("mode", modeSelector, true);

        modeSelector.addModeControl(new otp.widgets.tripoptions.MaxWalkSelector(this.optionsWidget));
        modeSelector.addModeControl(new otp.widgets.tripoptions.GroupTripOptions(this.optionsWidget, "Group Size: "));
        //modeSelector.addModeControl(new otp.widgets.tripoptions.BikeTriangle(this.optionsWidget));
        modeSelector.addModeControl(new otp.widgets.tripoptions.PreferredRoutes(this.optionsWidget));
        modeSelector.addModeControl(new otp.widgets.tripoptions.BannedRoutes(this.optionsWidget));

        modeSelector.refreshModeControls();

        this.optionsWidget.addSeparator();
        //this.optionsWidget.addControl("submit", new otp.widgets.tripoptions.GroupTripSubmit(this.optionsWidget));
        this.optionsWidget.addControl("submit", new otp.widgets.tripoptions.Submit(this.optionsWidget));*/

        var modeSelector = this.optionsWidget.controls['mode'];
        modeSelector.addModeControl(new otp.widgets.tripoptions.GroupTripOptions(this.optionsWidget, "Group Size: "));
        modeSelector.refreshModeControls();
        
        //this.fieldTripManager = new otp.modules.fieldtrip.FieldTripManagerWidget('otp-'+this.id+'-fieldTripWidget', this);
        this.sessionManager = new otp.core.TrinetSessionManager(this, $.proxy(function() {
            this.fieldTripManager = new otp.modules.fieldtrip.FieldTripManagerWidget('otp-'+this.id+'-fieldTripWidget', this);
        }, this));

        //this.requestsWidget = new otp.modules.fieldtrip.FieldTripRequestsWidget('otp-'+this.moduleId+'-requestsWidget', this);


        //this.refreshTrips();
    },
    
    restore : function() {
        if(_.has(this.webapp.urlParams, 'groupSize')) {
            this.groupSize = parseInt(this.webapp.urlParams['groupSize']);
        }        
        if("fromPlace" in this.webapp.urlParams && "toPlace" in this.webapp.urlParams) {
            this.optionsWidget.restorePlan({queryParams : this.webapp.urlParams});
        }
        otp.modules.multimodal.MultimodalPlannerModule.prototype.restore.apply(this);
    },    
    
    getExtendedQueryParams : function() {
        return { 
            numItineraries : 1,
        };
    },

    getAdditionalUrlParams : function() {
        return { groupSize : this.groupSize };
    },
        
    ftPlanTrip : function(queryParams) {

        if(this.updateActiveOnly) { // single itin modified by first/last/previous/next

            if(this.groupPlan) {
                var bannedTripArr = [];
                for(var i = 0; i < this.groupPlan.itineraries.length; i++) {
                    bannedTripArr = bannedTripArr.concat(this.groupPlan.itineraries[i].getTripIds());
                }
                queryParams.bannedTrips = bannedTripArr.join(',');
            }
            
            this.planTrip(queryParams);
            return;
        }
        
        var planDate = moment(this.optionsWidget.controls['time'].getDate()).format("MM/DD/YYYY");
        this.currentGroupSize = this.groupSize;
        this.bannedSegments = [];
        //console.log("RESET SEGMENTS");
        this.groupPlan = null;

        var this_ = this;
        
        // query for trips in use by other field trip itineraries in the DB
        $.ajax(this.datastoreUrl+'/fieldtrip/getTrips', {
            data: {
                sessionId : this.sessionManager.sessionId,
                date : planDate,
                //limit : 100,
            },
                
            success: function(data) {
                if((typeof data) == "string") data = jQuery.parseJSON(data);
                
                // store the trips in use for reference by the checkTripValidity() method
                this_.tripsInUse = [];
                
                for(var t = 0; t < data.length; t++) {
                    var fieldTrip = data[t];
                    for(var i = 0; i < fieldTrip.groupItineraries.length; i++) {
                        var grpItin = fieldTrip.groupItineraries[i];
                        for(var gt =0 ; gt < grpItin.trips.length; gt++) {
                            var gtfsTrip = grpItin.trips[gt];
                            // (gtfsTrip already includes fields agencyAndId, fromStopIndex, and toStopIndex)
                            gtfsTrip.passengers = grpItin.passengers; 
                            this_.tripsInUse.push(gtfsTrip);
                        }
                    }
                }
                
                // kick off the main planTrip request
                this.itinCapacity = null;
                this_.planTrip();
            },
            
            error: function(data) {
                console.log("error retrieving trips");
                console.log(data);
            }
        });
        this.setBannedTrips();
    },
    
    processPlan : function(tripPlan, restoring) {
        if(this.updateActiveOnly) {
            var itinIndex = this.itinWidget.activeIndex;
            tripPlan.itineraries[0].groupSize = this.groupPlan.itineraries[itinIndex].groupSize;
            this.itinWidget.updateItineraries(tripPlan.itineraries);
            this.updateActiveOnly = false;    
            this.drawItinerary(tripPlan.itineraries[0]);        
            return;
        }
    
        if(this.groupPlan == null)
            this.groupPlan = new otp.modules.planner.TripPlan(null, _.extend(tripPlan.queryParams, { groupSize : this.groupSize }));

        if(this.itinWidget == null) this.createItinerariesWidget();
        /*if(restoring && this.restoredItinIndex) {
            this.itinWidget.updateItineraries(tripPlan, this.restoredItinIndex);
            this.restoredItinIndex = null;
        } else  {
            this.itinWidget.updateItineraries(tripPlan);
        }*/
        //this.itinWidget.updateItineraries(tripPlan);
        //this.itinWidget.show();
        
        /*if(restoring) {
            this.optionsWidget.restorePlan(tripPlan);
        }*/
        //this.drawItinerary(tripPlan.itineraries[0]);
        
        //var itin = new otp.modules.planner.Itinerary(tripPlan.itineraries[0], tripPlan);
        var itin = tripPlan.itineraries[0];
        var capacity = itin.getGroupTripCapacity();
        
        // if this itin shares a vehicle trip with another one already in use, only use the remainingCapacity (as set in checkTripValidity())
        if(this.itinCapacity) capacity = Math.min(capacity, this.itinCapacity);

        //console.log("cur grp size:"+this.currentGroupSize+", cap="+capacity);
        
        //console.log("FT returned trip:");
        //console.log(itin);        
        //this.itineraries.push(itin);
        this.groupPlan.addItinerary(itin);
        
        var transitLegs = itin.getTransitLegs();
        for(var i = 0; i < transitLegs.length; i++) {
            var leg = transitLegs[i];
            this.bannedSegments.push({
                agencyAndId : leg.agencyId + "_" + leg.tripId,
                fromStopIndex : leg.from.stopIndex,
                toStopIndex : leg.to.stopIndex,
            });
        }

        this.setBannedTrips();// = this.bannedSegments.join(',');     
        //console.log("added "+transitLegs.length+" banned segments, total="+this.bannedSegments.length);
        
        if(this.currentGroupSize > capacity) { // group members remain; plan another trip
            this.currentGroupSize -= capacity;
            itin.groupSize = capacity;
            //console.log("remaining: "+this.currentGroupSize);
            this.itinCapacity = null;
            this.planTrip();
        }
        else { // we're done; show the results
            itin.groupSize = this.currentGroupSize;
            this.showResults();
        }
        
    },

    showResults : function() {
        //this.itinWidget.updateItineraries(this.itineraries, tripPlan.queryParams);
        this.itinWidget.show();
        this.itinWidget.bringToFront();
        this.itinWidget.updatePlan(this.groupPlan);
        this.drawItinerary(this.groupPlan.itineraries[0]);
        
        var requestWidgets = _.values(this.requestWidgets);
        //console.log("rWidgets:")
        for(var i = 0; i <= requestWidgets.length; i++) {
            if(requestWidgets[i]) {
                //console.log(requestWidgets[i])
                requestWidgets[i].tripPlanned();
            }
        }
    },
    
        
    createItinerariesWidget : function() {
        this.itinWidget = new otp.widgets.ItinerariesWidget(this.id+"-itinWidget", this);
        //this.itinWidget.showButtonRow = false;
        this.itinWidget.showItineraryLink = false;
        //this.itinWidget.showSearchLink = true;
    },
    
    setBannedTrips : function() {
        var tripIds = [];
        for(var i=0; i<this.bannedSegments.length; i++) {
            tripIds.push(this.bannedSegments[i].agencyAndId);
        }
    
        this.bannedTrips = tripIds.length > 0 ? tripIds.join(',') : null;     
        //console.log("set bannedTrips: "+this.bannedTrips);
    },

    refreshTrips : function(date) {
        var this_ = this;
        $.ajax(this.datastoreUrl+'/fieldtrip/getTrips', {
            data: {
                sessionId : this.sessionManager.sessionId,
                //date : this.fieldTripManager.selectedDate,
                limit : 100,
            },
                
            success: function(data) {
                if((typeof data) == "string") data = jQuery.parseJSON(data);
                //this_.fieldTripManager.updateTrips(data);
            },
            
            error: function(data) {
                console.log("error retrieving trips");
                console.log(data);
            }
        });
    },
    
    showSaveWidget : function() {
        new otp.modules.fieldtrip.SaveFieldTripWidget('otp-'+this.id+'-saveFieldTripWidget', this);
    },
        
    saveTrip : function(request, requestOrder, successCallback) {
        var this_ = this;
        //console.log("saving trip: "+desc);
        
        var data = {
            sessionId : this.sessionManager.sessionId,
            requestId : request.id,
            'trip.requestOrder' : requestOrder,
            'trip.origin' : this.getStartOTPString(),
            'trip.destination' : this.getEndOTPString(),
            //'trip.description' : desc,
            'trip.createdBy' : this.userName,
            'trip.passengers' : this.groupSize,            
            'trip.departure' : moment(this.groupPlan.earliestStartTime).add("hours", otp.config.timeOffset).format("YYYY-MM-DDTHH:mm:ss"),
            'trip.queryParams' : JSON.stringify(this.groupPlan.queryParams),
        };
        
        //console.log(data);
        $.ajax(this.datastoreUrl+'/fieldtrip/newTrip', {
            type: 'POST',
            
            data: data,
                
            success: function(data) {
                if((typeof data) == "string") data = jQuery.parseJSON(data);
                //console.log("successfully saved trip, now doing itins");
                this_.saveItineraries(data, successCallback);
            },
            
            error: function(data) {
                console.log("error saving trip");
                console.log(data);
            }
        });
    },

    checkTripValidity : function(tripId, fromStopIndex, toStopIndex, itin) {
        for(var i = 0; i < this.tripsInUse.length; i++) {
            var tripInUse  = this.tripsInUse[i];
            
            // first test: are these the same vehicle trip? if not, we're ok
            if(tripId !== tripInUse.agencyAndId) continue;
            
            // second test: do the stop ranges overlap? if not, we're ok
            if(fromStopIndex > tripInUse.toStopIndex || toStopIndex < tripInUse.fromStopIndex) continue;
            
            // if ranges overlap, calculate remaining capacity
            var remainingCapacity = itin.getGroupTripCapacity() - tripInUse.passengers;
            if(remainingCapacity < 10) return false; // consider trip 'full' if < 10 spots remain
            this.itinCapacity = this.itinCapacity ? Math.min(this.itinCapacity, remainingCapacity) : remainingCapacity;
        }
        return true;
    },
        
    saveItineraries : function(tripId, successCallback) {
        var this_ = this;
        this.itinsSaved = 0;
        for(var i = 0; i < this.groupPlan.itineraries.length; i++) {
            var itin = this.groupPlan.itineraries[i];
            console.log("saving itin for trip "+tripId);
            console.log(itin.itinData);

            var data = {
                sessionId : this.sessionManager.sessionId,
                fieldTripId : tripId,
                'itinerary.passengers' : itin.groupSize,
                'itinerary.itinData' : otp.util.Text.lzwEncode(JSON.stringify(itin.itinData)),
                'itinerary.timeOffset' : otp.config.timeOffset || 0, 
            };
            
            var legs = itin.getTransitLegs();
            
            for(var l = 0; l < legs.length; l++) {
                var leg = legs[l];
                var routeName = (leg.routeShortName !== null ? ('(' + leg.routeShortName + ') ') : '') + (leg.routeLongName || ""); 
                //console.log('routeName='+routeName); 
                data['trips['+l+'].depart'] = moment(leg.startTime).format("HH:mm:ss"); 
                data['trips['+l+'].arrive'] = moment(leg.endTime).format("HH:mm:ss"); 
                data['trips['+l+'].agencyAndId'] = leg.agencyId + "_" + leg.tripId;
                data['trips['+l+'].routeName'] = routeName;
                data['trips['+l+'].fromStopIndex'] = leg.from.stopIndex;
                data['trips['+l+'].toStopIndex'] = leg.to.stopIndex;
                data['trips['+l+'].fromStopName'] = leg.from.name;
                data['trips['+l+'].toStopName'] = leg.to.name;
                data['trips['+l+'].headsign'] = leg.headsign;
                if(leg.tripBlockId) data['trips['+l+'].blockId'] = leg.tripBlockId;
            }
            //console.log(data);
            
            $.ajax(this.datastoreUrl+'/fieldtrip/addItinerary', {
                type: 'POST',
                
                data: data,
                    
                success: function(data) {
                    if((typeof data) == "string") data = jQuery.parseJSON(data);
                    this_.itinsSaved++;
                    if(this_.itinsSaved == this_.groupPlan.itineraries.length) {
                        //console.log("all itins saved");
                        //this_.refreshTrips();
                        successCallback.call(this_, tripId);
                    }
                                 
                },
                
                error: function(data) {
                    console.log("error saving itinerary");
                    console.log(data);
                }
            });
                    
        }
    },
    
    deleteTrip : function(trip) {
        var this_ = this;
        
        var data = {
                sessionId : this.sessionManager.sessionId,
                id : trip.id
        };
        
        console.log("delete");
        console.log(data);
        $.ajax(this.datastoreUrl+'/fieldtrip/deleteTrip', {
            type: 'POST',
            
            data: data,
                  
            success: function(data) {
                this_.refreshTrips();
            },
            
            error: function(data) {
                console.log("error deleting trip:");
                console.log(data);
            }
        });
    },
    
    renderTripFromId : function(tripId) {
        var this_ = this;
        $.ajax(this.datastoreUrl+'/fieldTrip', {
            data: {
                sessionId : this.sessionManager.sessionId,
                id : trip.id
            },
                
            success: function(data) {
                if((typeof data) == "string") data = jQuery.parseJSON(data);
                this_.renderTrip(data);
                /*this_.groupPlan = new otp.modules.planner.TripPlan(null, JSON.parse(data.queryParams));
                for(var i = 0; i < data.groupItineraries.length; i++) {
                    var itinData = JSON.parse(otp.util.Text.lzwDecode(data.groupItineraries[i].itinData));
                    this_.groupPlan.addItinerary(new otp.modules.planner.Itinerary(itinData, this_.groupPlan));
                }
                if(this_.itinWidget == null) this_.createItinerariesWidget();
                this_.showResults();
                var queryParams = JSON.parse(data.queryParams);
                this_.restoreMarkers(queryParams);
                this_.optionsWidget.restorePlan({ queryParams : queryParams });*/
            },
            
            error: function(data) {
                console.log("error retrieving trip "+trip.id);
                console.log(data);
            }
        });

    },

    renderTrip : function(tripData) {
        var queryParams = JSON.parse(tripData.queryParams);
        this.groupPlan = new otp.modules.planner.TripPlan(null, queryParams);//_.extend(queryParams, { groupSize : this.groupSize }));
        for(var i = 0; i < tripData.groupItineraries.length; i++) {
            var itinData = otp.util.FieldTrip.readItinData(tripData.groupItineraries[i]);
            var itin = new otp.modules.planner.Itinerary(itinData, this.groupPlan);
            itin.groupSize = tripData.groupItineraries[i].passengers;
            this.groupPlan.addItinerary(itin);
        }
        if(this.itinWidget == null) this.createItinerariesWidget();
        this.showResults();
        this.restoreMarkers(queryParams);
        this.optionsWidget.restorePlan({ queryParams : queryParams });
    },
    
    //** requests functions **//
    
    showRequests : function() {
        if(!this.requestsWidget) {
            this.requestsWidget = new otp.modules.fieldtrip.FieldTripRequestsWidget('otp-'+this.id+'-requestsWidget', this);
        }
        if(this.requestsWidget.minimized) this.requestsWidget.unminimize();
        this.requestsWidget.bringToFront();
    },
    
    loadRequests : function() {
        var this_ = this;
        $.ajax(this.datastoreUrl+'/fieldtrip/getRequests', {
            data: {
                sessionId : this.sessionManager.sessionId,
                limit : 100,
            },
                
            success: function(data) {
                if((typeof data) == "string") data = jQuery.parseJSON(data);
                //this_.requestsWidget.updateRequests(data);
                this_.fieldTripManager.updateRequests(data);
                
                for(var i = 0; i < data.length; i++) {
                    var req = data[i];
                    if(_.has(this_.requestWidgets, req.id)) {
                        var widget = this_.requestWidgets[req.id];
                        widget.request = req;
                        widget.render();
                    }
                }
                /*var widgets = _.values(this_.requestWidgets);
                for(var i =0; i < widgets.length; i++) {
                    console.log("re-rendering:")
                    console.log(widgets[i])
                    widgets[i].render();
                }*/
                
            },
            
            error: function(data) {
                console.log("error retrieving requests");
                console.log(data);
            }
        });
    },

    setRequestStatus : function(request, status) {
        var this_ = this;
        
        var data = {
            sessionId : this.sessionManager.sessionId,
            requestId : request.id,
            status : status
        };
        
        $.ajax(this.datastoreUrl+'/fieldtrip/setRequestStatus', {
            type: 'POST',
            
            data: data,
                  
            success: function(data) {
                this_.loadRequests();
            },
            
            error: function(data) {
                console.log("error setting trip status:");
                console.log(data);
            }
        });
    },
    
    cancelRequest : function(request) {
        this.setRequestStatus(request, "cancelled"); 
    },

    setClasspassId : function(request, classpassId) {
        var this_ = this;

        $.ajax(this.datastoreUrl+'/fieldtrip/setRequestClasspassId', {
            type: 'POST',
            
            data: {
                sessionId : this.sessionManager.sessionId,
                requestId : request.id,
                classpassId : classpassId
            },
            
            success: function(data) {
                this_.loadRequests();
            },
            
            error: function(data) {
                console.log("error setting classpass id:");
                console.log(data);
            }
        });
    },
                
    showRequest : function(request) {
        if(_.has(this.requestWidgets, request.id)) {
            var widget = this.requestWidgets[request.id];
            if(widget.minimized) widget.unminimize();
        }
        else {
            var widget = new otp.modules.fieldtrip.FieldTripRequestWidget('otp-'+this.id+'-requestWidget-'+request.id, this, request);
            this.requestWidgets[request.id] = widget;
        }
        widget.bringToFront();
    },
    
    planOutbound : function(request) {
        this.clearTrip();
        var queryParams = {
            time : moment(request.arriveDestinationTime).format("h:mma"),
            date : moment(request.travelDate).format("MM-DD-YYYY"),            
            arriveBy : true,
            groupSize : otp.util.FieldTrip.getGroupSize(request),
        };
        
        this.optionsWidget.applyQueryParams(queryParams);

        var geocodedOrigin = this.geocodedOrigins[request.id];
        var geocodedDestination = this.geocodedDestinations[request.id];
        
        if(geocodedOrigin) {
            this.setStartPoint(new L.LatLng(geocodedOrigin.lat, geocodedOrigin.lng),
                               false, geocodedOrigin.description);
        }       
        if(geocodedDestination) {
            this.setEndPoint(new L.LatLng(geocodedDestination.lat, geocodedDestination.lng),
                             false, geocodedDestination.description);
        }       
        if(!geocodedOrigin || !geocodedDestination) {
            this.showGeocoder(request, "outbound");            
        }
    },

    planInbound : function(request) {
        this.clearTrip();
        var queryParams = {
            time : moment(request.leaveDestinationTime).format("h:mma"),
            date : moment(request.travelDate).format("MM-DD-YYYY"),            
            arriveBy : false,
            groupSize : otp.util.FieldTrip.getGroupSize(request),
        };
        
        this.optionsWidget.applyQueryParams(queryParams);
        
        var geocodedOrigin = this.geocodedOrigins[request.id];
        var geocodedDestination = this.geocodedDestinations[request.id];
        
        if(geocodedOrigin) {
            this.setEndPoint(new L.LatLng(geocodedOrigin.lat, geocodedOrigin.lng),
                             false, geocodedOrigin.description);
        }       
        if(geocodedDestination) {
            this.setStartPoint(new L.LatLng(geocodedDestination.lat, geocodedDestination.lng),
                               false, geocodedDestination.description);
        }       
        if(!geocodedOrigin || !geocodedDestination) {
            this.showGeocoder(request, "inbound");
        }
    },
    
    showGeocoder : function(request, mode) {
        //console.log("showing geocoder for request "+request.id);
        
        if(_.has(this.geocoderWidgets, request.id)) {
            var widget = this.geocoderWidgets[request.id];
            widget.mode = mode;
            if(widget.minimized) widget.unminimize();
            widget.show();
        }
        else {
            var widget = new otp.modules.fieldtrip.FieldTripGeocoderWidget('otp-'+this.id+'-geocoderWidget-'+request.id, this, request, mode);
            this.geocoderWidgets[request.id] = widget;
        }
        widget.bringToFront();
        
    },
       
    saveRequestTrip : function(request, type) {
        if(!this.checkPlanValidity(request)) return;
        
        var outboundTrip = otp.util.FieldTrip.getOutboundTrip(request);
        if(type === 'outbound' && outboundTrip) {
            this.deleteTrip(outboundTrip);
            alert("Note: request already had planned outbound trip; previously planned trip overwritten.");
        }

        var inboundTrip = otp.util.FieldTrip.getInboundTrip(request);
        if(type === 'inbound' && inboundTrip) {
            this.deleteTrip(inboundTrip);
            alert("Note: request already had planned inbound trip; previously planned trip overwritten.");
        }
        
        var this_ = this;
        if(type === "outbound") var requestOrder = 0;
        if(type === "inbound") var requestOrder = 1;

        this.saveTrip(request, requestOrder, function(tripId) {
            console.log("saved "+type+" trip for req #"+request.id);
            this_.loadRequests();
        });
        /*this.saveTrip(request, function(tripId) {
            if(type === "outbound") var url = this.datastoreUrl+'/fieldtrip/setOutboundTrip';
            if(type === "inbound") var url = this.datastoreUrl+'/fieldtrip/setInboundTrip';
            $.ajax(url, {
                type: 'POST',
                
                data: {
                    requestId : request.id,
                    tripId : tripId,
                },
                      
                success: function(data) {
                    console.log("set " + type + "trip");
                    this_.loadRequests();
                },
                
                error: function(data) {
                    console.log("error setting " + type + "trip");
                    console.log(data);
                }
            });          
        });*/
             
    },
    
    checkPlanValidity : function(request) {
        if(this.groupPlan == null) {
            alert("No active plan to save");
            return false;
        }
        
        var planDeparture = moment(this.groupPlan.earliestStartTime).add("hours", otp.config.timeOffset);
        var requestDate = moment(request.travelDate);
        
        if(planDeparture.date() != requestDate.date() ||
                planDeparture.month() != requestDate.month() ||
                planDeparture.year() != requestDate.year()) {
            alert("Planned trip date (" + planDeparture.format("MM/DD/YYYY") + ") is not the requested day of travel (" + requestDate.format("MM/DD/YYYY") + ")");
            return false;
        }
        
        return true;
    },
});


otp.util.FieldTrip = {
    
    constructPlanInfo : function(trip) {
        return trip.groupItineraries.length + " group itineraries, planned by " + trip.createdBy + " at " + trip.timeStamp;
    },
    
    getOutboundTrip : function(request) {
        if(request.outboundTrip) return request.outboundTrip;
        return otp.util.FieldTrip.tripAtIndex(request, 0);
    },

    getInboundTrip : function(request) {
        if(request.inboundTrip) return request.inboundTrip;
        return otp.util.FieldTrip.tripAtIndex(request, 1);
    },
    
    tripAtIndex : function(request, index) {
        if(request.trips) {
            for(var i = 0; i < request.trips.length; i++) {
                if(request.trips[i].requestOrder === index) return request.trips[i];
            }
        }
        return null;
    },

    getGroupSize : function(request) {
        var groupSize = 0;
        if(request.numStudents) groupSize += request.numStudents;
        if(request.numChaperones) groupSize += request.numChaperones;
        return groupSize;
    },    
    
    readItinData : function(groupItin) {
        if(groupItin.timeOffset) otp.config.timeOffset = groupItin.timeOffset;
        return JSON.parse(otp.util.Text.lzwDecode(groupItin.itinData));
    },
    
    
    
};

