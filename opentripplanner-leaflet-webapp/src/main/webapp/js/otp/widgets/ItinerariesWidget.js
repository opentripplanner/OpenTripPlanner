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

otp.namespace("otp.widgets");

otp.widgets.ItinerariesWidget = 
    otp.Class(otp.widgets.Widget, {

    module : null,
    
    itinsAccord : null,
    footer : null,
    
    itineraries : null,
    activeIndex : 0,
    
    // set to true by next/previous/etc. to indicate to only refresh the currently active itinerary
    refreshActiveOnly : false,
    
    initialize : function(id, module) {
        this.module = module;

        otp.widgets.Widget.prototype.initialize.call(this, id, module.webapp.widgetManager);
        this.$().addClass('otp-itinsWidget');
        this.$().resizable();
        this.minimizable = true;
        this.addHeader("X Itineraries Returned");
    },
    
    activeItin : function() {
        return this.itineraries[this.activeIndex];
    },
    
    updateItineraries : function(itineraries, queryParams, itinIndex) {
        
        var this_ = this;
        var divId = this.id+"-itinsAccord";

        if(this.minimized) this.unminimize();
        
        if(this.refreshActiveOnly == true) {
            var newItin = itineraries[0];
            var oldItin = this.itineraries[this.activeIndex];
            //console.log("uI: "+oldItin.itinData.startTime+" to "+newItin.itinData.startTime);
            //console.log(oldItin);
            var alerts = null;
            if(newItin.differentServiceDayFrom(oldItin)) {
                alerts = [ "This itinerary departs on a different day from the previous one"];
            }
            this.itineraries[this.activeIndex] = newItin;
            var itinHeader = $('#'+divId+'-headerContent-'+this.activeIndex);
            itinHeader.html(this.headerContent(newItin, this.activeIndex));
            var itinContainer = $('#'+divId+'-'+this.activeIndex);
            itinContainer.empty();
            this.renderItinerary(newItin, this.activeIndex, alerts).appendTo(itinContainer);
            this.refreshActiveOnly = false;
            return;
        }            
        this.itineraries = itineraries;
        //this.header.html(this.itineraries.length+" Itineraries Returned:");
        this.setTitle(this.itineraries.length+" Itineraries Returned:");
        
        if(this.itinsAccord !== null) {
            this.itinsAccord.remove();
        }
        if(this.footer !== null) {
            this.footer.remove();
        }
        var html = "<div id='"+divId+"' class='otp-itinsAccord'></div>";
        this.itinsAccord = $(html).appendTo(this.$());
        
        if(queryParams.mode !== "WALK" && queryParams.mode !== "BICYCLE") {
            this.appendFooter();
        }
        
        for(var i=0; i<this.itineraries.length; i++) {
            var itin = this.itineraries[i];
            //$('<h3><span id='+divId+'-headerContent-'+i+'>'+this.headerContent(itin, i)+'<span></h3>').appendTo(this.itinsAccord).click(function(evt) {
            //$('<h3>'+this.headerContent(itin, i)+'</h3>').appendTo(this.itinsAccord).click(function(evt) {
            
            $('<h3><div id='+divId+'-headerContent-'+i+'>'+this.headerContent(itin, i)+'</div></h3>')
            .appendTo(this.itinsAccord)
            .data('itin', itin)
            .data('index', i)
            .click(function(evt) {
                var itin = $(this).data('itin');
                this_.module.drawItinerary(itin);
                this_.activeIndex = $(this).data('index');
            });
            
            $('<div id="'+divId+'-'+i+'"></div>')
            .appendTo(this.itinsAccord)
            .append(this.renderItinerary(itin, i));
        }
        this.activeIndex = parseInt(itinIndex) || 0;
        
        this.itinsAccord.accordion({
            active: this.activeIndex,
            heightStyle: "fill"
        });
        
        this.$().resize(function(){
            this_.itinsAccord.accordion("resize");
        });

        this.$().draggable({ cancel: "#"+divId });
        
    },
    
    appendFooter : function() {
        var this_ = this;
        this.footer = $("<div class='otp-itinsButtonRow'></div>").appendTo(this.$());
        $('<button>First</button>').button().appendTo(this.footer).click(function() {
            var itin = this_.itineraries[this_.activeIndex];
            var params = itin.tripPlan.queryParams;
            var stopId = itin.getFirstStopID();
            _.extend(params, {
                startTransitStopId :  stopId,
                time : "04:00am",
                arriveBy : false
            });
            this_.refreshActiveOnly = true;
            this_.module.planTrip(params);
        });
        $('<button>Previous</button>').button().appendTo(this.footer).click(function() {
            var itin = this_.itineraries[this_.activeIndex];
            var params = itin.tripPlan.queryParams;
            var newEndTime = itin.itinData.endTime - 90000;
            var stopId = itin.getFirstStopID();
            _.extend(params, { 
                startTransitStopId :  stopId,
                time : otp.util.Time.formatItinTime(newEndTime, "h:mma"),
                date : otp.util.Time.formatItinTime(newEndTime, "MM-DD-YYYY"),
                arriveBy : true
            });
            this_.refreshActiveOnly = true;
            this_.module.planTrip(params);            
        });
        $('<button>Next</button>').button().appendTo(this.footer).click(function() {
            var itin = this_.itineraries[this_.activeIndex];
            var params = itin.tripPlan.queryParams;
            var newStartTime = itin.itinData.startTime + 90000;
            var stopId = itin.getFirstStopID();
            _.extend(params, {
                startTransitStopId :  stopId,
                time : otp.util.Time.formatItinTime(newStartTime, "h:mma"),
                date : otp.util.Time.formatItinTime(newStartTime, "MM-DD-YYYY"),
                arriveBy : false
            });
            this_.refreshActiveOnly = true;
            this_.module.planTrip(params);      
        });
        $('<button>Last</button>').button().appendTo(this.footer).click(function() {
            var itin = this_.itineraries[this_.activeIndex];
            var params = itin.tripPlan.queryParams;
            var stopId = itin.getFirstStopID();
            _.extend(params, {
                startTransitStopId :  stopId,
                date : moment().add('days', 1).format("MM-DD-YYYY"),
                time : "04:00am",
                arriveBy : true
            });
            this_.refreshActiveOnly = true;
            this_.module.planTrip(params);
        });
    },
    
    // returns HTML text
    headerContent : function(itin, index) {
        // show number of this itinerary (e.g. "1.")
        var html= '<div class="otp-itinsAccord-header-number">'+(index+1)+'.</div>';
        
        // show iconographic trip leg summary  
        html += '<div class="otp-itinsAccord-header-icons">'+itin.getIconSummaryHTML()+'</div>';
        
        // show trip duration
        html += '<div class="otp-itinsAccord-header-duration">('+itin.getDurationStr()+')</div>';
    
        if(itin.groupSize) {
            html += '<div class="otp-itinsAccord-header-groupSize">[Group size: '+itin.groupSize+']</div>';
        }    
        // clear div
        html += '<div style="clear:both;"></div>';
        return html;
    },
    
    municoderResultId : 0,
    
    // returns jQuery object
    renderItinerary : function(itin, i, alerts) {
        var this_ = this;

        // render legs
        var divId = this.moduleId+"-itinAccord-"+i;
        var accordHtml = "<div id='"+divId+"' class='otp-itinAccord'></div>";
        var itinAccord = $(accordHtml);
        for(var l=0; l<itin.itinData.legs.length; l++) {
            var leg = itin.itinData.legs[l];
            var headerHtml = "<b>"+otp.util.Itin.modeString(leg.mode).toUpperCase()+"</b>";
            if(leg.mode === "WALK" || leg.mode === "BICYCLE") {
                headerHtml += " "+otp.util.Itin.distanceString(leg.distance)+ " to "+leg.to.name;
                
                if(otp.config.municoderHostname) {
                    var spanId = this.newMunicoderRequest(leg.to.lat, leg.to.lon);
                    headerHtml += '<span id="'+spanId+'"></span>';
                }
            }
            else if(leg.agencyId !== null) {
                headerHtml += ": "+leg.agencyId+", ";
                if(leg.route !== leg.routeLongName) headerHtml += "("+leg.route+") ";
                headerHtml += leg.routeLongName;
                if(leg.headsign) headerHtml +=  " toward " + leg.headsign;
            }
            $("<h3>"+headerHtml+"</h3>").appendTo(itinAccord).hover(function(evt) {
                var arr = evt.target.id.split('-');
                var index = parseInt(arr[arr.length-1]);
                this_.module.highlightLeg(itin.itinData.legs[index]);
                this_.module.pathMarkerLayer.clearLayers();
                this_.module.drawStartBubble(itin.itinData.legs[index], true);
                this_.module.drawEndBubble(itin.itinData.legs[index], true);
            }, function(evt) {
                this_.module.clearHighlights();
                this_.module.pathMarkerLayer.clearLayers();
                this_.module.drawAllStartBubbles(itin);
            });
            this_.renderLeg(leg, (l>0 ? itin.itinData.legs[l-1] : null)).appendTo(itinAccord);
        }
        itinAccord.accordion({
            active: false,
            heightStyle: "content",
            collapsible: true
        });

        var itinDiv = $("<div></div>");

        // add alerts, if applicable
        alerts = alerts || [];
        if(itin.totalWalk > itin.tripPlan.queryParams.maxWalkDistance && itin.tripPlan.queryParams.maxWalkDistance > 804) {
            alerts.push("Total walk distance for this trip exceeds specified maximum");
        }
        
        for(var i = 0; i < alerts.length; i++) {
            itinDiv.append("<div class='otp-itinAlertRow'>"+alerts[i]+"</div>");
        }
        
        // add start and end time rows and the main leg accordian display 
        itinDiv.append("<div class='otp-itinStartRow'><b>Start</b>: "+itin.getStartTimeStr()+"</div>");
        itinDiv.append(itinAccord);
        itinDiv.append("<div class='otp-itinEndRow'><b>End</b>: "+itin.getEndTimeStr()+"</div>");

        // add trip summary
        
        var html = '<div class="otp-itinTripSummary">';
        html += '<div class="otp-itinTripSummaryHeader">Trip Summary</div>';
        html += '<div class="otp-itinTripSummaryLabel">Travel</div><div class="otp-itinTripSummaryText">'+itin.getStartTimeStr()+'</div>';
        html += '<div class="otp-itinTripSummaryLabel">Time</div><div class="otp-itinTripSummaryText">'+itin.getDurationStr()+'</div>';
        if(itin.hasTransit) {
            html += '<div class="otp-itinTripSummaryLabel">Transfers</div><div class="otp-itinTripSummaryText">'+itin.itinData.transfers+'</div>';
            html += '<div class="otp-itinTripSummaryLabel">Fare</div><div class="otp-itinTripSummaryText">'+itin.getFareStr()+'</div>';
        }
        html += '<div class="otp-itinTripSummaryFooter">Valid ' + moment().format('MMM Do YYYY, h:mma') + ' | <a href="'+itin.getLink(i)+'">Link to Itinerary</a></div>';
        
        html += '</div>';
        itinDiv.append(html);
        
        return itinDiv;
    },
    
    renderLeg : function(leg, previousLeg) {
        var this_ = this;
        if(otp.util.Itin.isTransit(leg.mode)) {
            var legDiv = $('<div></div>');
            
            // show the start time and stop

            $('<div class="otp-itin-leg-leftcol">'+otp.util.Time.formatItinTime(leg.startTime, "h:mma")+"</div>").appendTo(legDiv);

            var startHtml = '<div class="otp-itin-leg-endpointDesc"><b>Board</b> at '+leg.from.name;
            if(otp.config.municoderHostname) {
                var spanId = this.newMunicoderRequest(leg.from.lat, leg.from.lon);
                startHtml += '<span id="'+spanId+'"></span>';
            }
            startHtml += '</div>';
            
            $(startHtml).appendTo(legDiv)
            .click(function(evt) {
                this_.module.webapp.map.lmap.panTo(new L.LatLng(leg.from.lat, leg.from.lon));
            }).hover(function(evt) {
                this_.module.pathMarkerLayer.clearLayers();
                this_.module.drawStartBubble(leg, true);            
            }, function(evt) {
                this_.module.pathMarkerLayer.clearLayers();
                this_.module.drawAllStartBubbles(this_.itineraries[this_.activeIndex]);
            });
            
            $('<div class="otp-itin-leg-endpointDescSub">Stop #'+leg.from.stopId.id+' [<a href="#">Show other departures</a>]</div>')
            .appendTo(legDiv)
            .click(function(evt) {
                var stopID = leg.from.stopId.id;
                var times = this_.activeItin().stopTimesMap[stopID];
                console.log(evt);
                //var stopsWidget = new otp.widgets.StopTimesWidget(this_.id+"-stopWidget-"+stopID, this_.widgetManager);
                if(!this_.module.stopsWidget) {
                    this_.module.stopsWidget = new otp.widgets.StopTimesWidget("otp-"+this.moduleId+"-stopsWidget", this_.widgetManager);
                    this_.module.stopsWidget.$().offset({top: evt.clientY, left: evt.clientX});
                }
                this_.module.stopsWidget.show();
                this_.module.stopsWidget.update(stopID, (leg.routeShortName || leg.routeLongName), times, leg.startTime);
                this_.module.stopsWidget.bringToFront();
                //this_.widgetManager.addWidget(stopsWidget);
            });

            $('<div class="otp-itin-leg-buffer"></div>').appendTo(legDiv);            

            // show the "time in transit" line

            $('<div class="otp-itin-leg-elapsedDesc">Time in transit: '+otp.util.Time.msToHrMin(leg.duration)+'</div>').appendTo(legDiv);

            // show the intermediate stops, if applicable
            
            if(this.module.showIntermediateStops) {

                $('<div class="otp-itin-leg-buffer"></div>').appendTo(legDiv);            
                var intStopsDiv = $('<div class="otp-itin-leg-intStops"></div>').appendTo(legDiv);
                
                var intStopsListDiv = $('<div class="otp-itin-leg-intStopsList"></div>')
                
                $('<div class="otp-itin-leg-intStopsHeader">'+leg.intermediateStops.length+' Intermediate Stops</div>')
                .appendTo(intStopsDiv)
                .click(function(event) {
                    intStopsListDiv.toggle();
                });
                
                intStopsListDiv.appendTo(intStopsDiv);
                
                for(var i=0; i < leg.intermediateStops.length; i++) {
                    var stop = leg.intermediateStops[i];
                    $('<div class="otp-itin-leg-intStopsListItem">'+(i+1)+'. '+stop.name+' (ID #'+stop.stopId.id+')</div>').
                    appendTo(intStopsListDiv)
                    .data("stop", stop)
                    .click(function(evt) {
                        var stop = $(this).data("stop");
                        this_.module.webapp.map.lmap.panTo(new L.LatLng(stop.lat, stop.lon));
                    }).hover(function(evt) {
                        var stop = $(this).data("stop");
                        $(this).css('color', 'red');
                        var popup = L.popup()
                            .setLatLng(new L.LatLng(stop.lat, stop.lon))
                            .setContent(stop.name)
                            .openOn(this_.module.webapp.map.lmap);
                    }, function(evt) {
                        $(this).css('color', 'black');
                        this_.module.webapp.map.lmap.closePopup();
                    });                    
                }
                intStopsListDiv.hide();
            }

            // show the end time and stop

            $('<div class="otp-itin-leg-buffer"></div>').appendTo(legDiv);            

            $('<div class="otp-itin-leg-leftcol">'+otp.util.Time.formatItinTime(leg.endTime, "h:mma")+"</div>").appendTo(legDiv);           

            var endHtml = '<div class="otp-itin-leg-endpointDesc"><b>Alight</b> at '+leg.to.name;
            if(otp.config.municoderHostname) {
                spanId = this.newMunicoderRequest(leg.to.lat, leg.to.lon);
                endHtml += '<span id="'+spanId+'"></span>';
            }
            endHtml += '</div>';
            
            $(endHtml).appendTo(legDiv)
            .click(function(evt) {
                this_.module.webapp.map.lmap.panTo(new L.LatLng(leg.to.lat, leg.to.lon));
            }).hover(function(evt) {
                this_.module.pathMarkerLayer.clearLayers();
                this_.module.drawEndBubble(leg, true);            
            }, function(evt) {
                this_.module.pathMarkerLayer.clearLayers();
                this_.module.drawAllStartBubbles(this_.itineraries[this_.activeIndex]);
            });
            
            return legDiv;

            /*if(previousLeg) {
                html += '<div class="otp-itin-leg-leftcol">'+otp.util.Time.formatItinTime(previousLeg.endTime, "h:mma")+"</div>";
                html += '<div class="otp-itin-leg-endpointDesc">Arrive at '+leg.from.name+'</div>';
                html += '<div class="otp-itin-leg-elapsedDesc">Wait time: '+otp.util.Time.msToHrMin(leg.startTime-previousLeg.endTime)+'</div>';
            }*/
        }
        else { // walk / bike / car
            var legDiv = $('<div></div>');
            
            for(var i=0; i<leg.steps.length; i++) {
                var step = leg.steps[i];
                var text = otp.util.Itin.getLegStepText(step);
                
                var html = '<div id="foo-'+i+'" class="otp-itin-step-row">';
                html += '<div class="otp-itin-step-icon">';
                if(step.relativeDirection)
                    html += '<img src="images/directions/' +
                        step.relativeDirection.toLowerCase()+'.png">';
                html += '</div>';                
                var distArr= otp.util.Itin.distanceString(step.distance).split(" ");
                html += '<div class="otp-itin-step-dist">' +
                    '<span style="font-weight:bold; font-size: 1.2em;">' + 
                    distArr[0]+'</span><br>'+distArr[1]+'</div>';
                html += '<div class="otp-itin-step-text">'+text+'</div>';
                html += '<div style="clear:both;"></div></div>';

                $(html).appendTo(legDiv)
                .data("step", step)
                .data("stepText", text)
                .click(function(evt) {
                    var step = $(this).data("step");
                    this_.module.webapp.map.lmap.panTo(new L.LatLng(step.lat, step.lon));
                }).hover(function(evt) {
                    var step = $(this).data("step");
                    $(this).css('background', '#f0f0f0');
                    var popup = L.popup()
                        .setLatLng(new L.LatLng(step.lat, step.lon))
                        .setContent($(this).data("stepText"))
                        .openOn(this_.module.webapp.map.lmap);
                }, function(evt) {
                    $(this).css('background', '#e8e8e8');
                    this_.module.webapp.map.lmap.closePopup();
                });
            }
            return legDiv;                        
        }
        return $("<div>Leg details go here</div>");
    },
    
    newMunicoderRequest : function(lat, lon) {
    
        this.municoderResultId++;
        var spanId = 'otp-municoderResult-'+this.municoderResultId;
        
        $.ajax(otp.config.municoderHostname+"/opentripplanner-municoder/municoder", {
        
            data : { location : lat+","+lon },           
            dataType:   'jsonp',
                
            success: function(data) {
                if(data.name) {
                    $('#'+spanId).html(", "+data.name);
                }
            }
        });
        return spanId;
    }
    
});

    
