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

otp.namespace("otp.planner");

/**
  * Web Map / TripPlanner
  */
otp.planner.ParamTemplate = 'submit'
        + '&fromPlace={[values.fromPlace.replace(/&/g,"@")]}'
        + '&toPlace={[values.toPlace.replace(/&/g,"@")]}'
        + '&mode={mode}'
        + '&min={opt}'
        + '<tpl if="opt == \'TRIANGLE\'">&triangleTimeFactor={triangleTimeFactor}&triangleSlopeFactor={triangleSlopeFactor}&triangleSafetyFactor={triangleSafetyFactor}</tpl>'
        + '&maxWalkDistance={maxWalkDistance}'
        + '&time={time}'
        + '&date={date}'
        + '&arr={arriveBy}&itinID={itinID}';

otp.planner.Templates = {

    THIS                : null,
    locale              : null,

    TP_ITINERARY        : null,
    TP_LEG_CONTINUES    : null,
    TP_TRIPDETAILS      : null,
    TP_LEG_BASE_STR     : null,
    TP_LEG_MODE         : null,
    TP_END              : null,
    TP_START            : null,
    TP_BICYCLE_LEG      : null,
    TP_WALK_LEG         : null,
    TP_CAR_LEG          : null,

    tripFeedbackDetails : null,
    tripPrintTemplate   : null,
    streetviewTemplate  : null,

    initialize : function(config) {
        otp.configure(this, config);

        otp.planner.Templates.THIS   = this;
        otp.planner.Templates.locale = this.locale;

        if(this.TP_ITINERARY == null)
            this.TP_ITINERARY = new Ext.XTemplate(
                  '<p><a href="#">{id}</a>: ',
                  ' {startTimeDisplay} - {endTimeDisplay} ',
                  '<tpl if="numTransfers">',
                    '<br/><span class="transfers">',
                    '({numTransfers} ',
                    '<tpl if="numTransfers == 1">' + this.locale.instructions.transfer  + '</tpl>',
                    '<tpl if="numTransfers != 1">' + this.locale.instructions.transfers + '</tpl>',
                    ', {duration} ' + this.getDurationTemplateString(),
                    ')</span>',
                  '</tpl>',
                  '</p>'
            ).compile();

        if(this.tripFeedbackDetails == null)
            // Trip Planner state messaging (eg: feedback emails, etc...).
            this.tripFeedbackDetails = new Ext.XTemplate( 
                this.locale.labels.trip_details 
                + ' {fromPlace} ' + this.locale.directions.to + ' {toPlace}'
                + ' {arriveBy} {time} ' + this.locale.directions.on + ' {date}, '
                + this.locale.directions.via + ' {[otp.util.Modes.translate(values["mode"])]}.'
                + ' {opt}'
                + '<tpl if="opt == \'TRIANGLE\'"> (tf={triangleTimeFactor}, sf={triangleSlopeFactor}, hf={triangleSafetyFactor})</tpl>'
                + ','
                + this.locale.labels.with_a_walk + ' {maxWalkDistance}m <br/>' 
            ).compile();


        if(this.tripPrintTemplate == null)
            this.tripPrintTemplate = new Ext.XTemplate( 
                '{url}?' + otp.planner.ParamTemplate
            ).compile();

        if(this.streetviewTemplate == null)
            this.streetviewTemplate =  new Ext.XTemplate(
                '<a href="#" onClick="otp.core.MapStatic.streetview({x}, {y});">{name}</a>'
            ).compile();

        if(this.TP_TRIPDETAILS == null)
            this.TP_TRIPDETAILS = new Ext.XTemplate(
                '<div id="trip-details">',
                '<h3>' + this.locale.labels.trip_details + '</h3>',
                '<table cellpadding="3" cellspacing="0" border="0">',
                    '<tpl if="regularFare != null">',
                      '<tr><td><strong>' + this.locale.labels.fare        + '</strong></td><td>{regularFare}</td></tr></tpl>',
                      '<tr><td><strong>' + this.locale.labels.travel      + '</strong></td><td>{startTimeDisplay}</td></tr>',
                      '<tr><td><strong>' + this.locale.labels.valid       + '</strong></td><td>{[new Date().format("' + this.locale.time.format + '")]}</td></tr>',
                      '<tr><td><strong>' + this.locale.labels.trip_length + '</strong></td><td>{duration} ' + this.getDurationTemplateString() + '</td></tr>',
                      '<tpl if="walkDistance"><tr><td><strong>{distanceVerb}</strong></td><td>{walkDistance}</td></tr></tpl>',
                '</table></div>'
            ).compile();

        if(this.HEADSIGN == null)
            this.HEADSIGN = '<tpl if="headsign != null && headsign.length &gt; 0"> ' + this.locale.directions.to + ' {headsign}</tpl>';

        if(this.TP_LEG_MODE == null)
            this.TP_LEG_MODE = ''
                + '<h4>' 
                + '<a href="#">{[otp.util.Modes.translate(values["mode"])]}</a>'
                + ' {routeName} '
                + this.HEADSIGN
                + '</h4>';

        if(this.TP_LEG_CONTINUES == null)
            this.TP_LEG_CONTINUES = ''
                + '<h4>'
                + '<a href="#">' + this.locale.instructions.continue_as + '</a> '
                + ' {routeName} '
                + this.HEADSIGN
                + '<span class="transfers">(' + this.locale.instructions.stay_aboard + ')</span>'
                + '</h4>';

        if(this.TP_LEG_BASE_STR == null)
            this.TP_LEG_BASE_STR = ''
                + '<p class="leg-info">'
                + '<span class="time">{startTimeDisplayShort}</span> ' + this.locale.instructions.depart + ' {fromName}'
                + '<tpl if="fromStopId != null && fromStopId.length &gt; 0 && showStopIds">'
                +   '<br/>'
                +   '<span class="stopid">' + this.locale.labels.stop_id + ' {fromStopId}</span>'
                + '</tpl>'
                + '</p>'
                + '<tpl if="duration != null">'
                +   '<div class="duration">{duration} ' + this.getDurationTemplateString() + '</div>'
                + '</tpl>'
                + '<p class="leg-info">'
                + '<span class="time">{endTimeDisplayShort}</span> ' + this.locale.instructions.arrive + ' {toName}'
                + '<tpl if="toStopId != null && toStopId.length &gt; 0 && showStopIds">'
                +   '<br/>'
                +   '<span class="stopid">' + this.locale.labels.stop_id + ' {toStopId}</span>'
                + '</tpl>'
                + '</p>'
                + '<tpl if="alerts != null && alerts.length &gt; 0">'
                + '<tpl for="alerts">'
                +   '<p class="alert"><img src="images/ui/trip/alert.png" align="absmiddle"/> '
                +   '<b>' + this.locale.labels.alert_for_rt + ' {parent.routeShortName}: </b>{.}</p>'
                + '</tpl>'
                + '</tpl>';

        if(this.TP_WALK_LEG == null)
            this.TP_WALK_LEG = this.makeLegTemplate(this.locale.instructions.walk);

        if(this.TP_BICYCLE_LEG == null)
            this.TP_BICYCLE_LEG = this.makeLegTemplate(this.locale.instructions.bike);

        if(this.TP_CAR_LEG == null)
            this.TP_CAR_LEG = this.makeLegTemplate(this.locale.instructions.drive);

        if(this.TP_START == null)
            this.TP_START = new Ext.XTemplate(
                  '<h4><a href="#">' + this.locale.instructions.start_at + '</a> {name}</h4>'
            ).compile();

        if(this.TP_END == null)
            this.TP_END = new Ext.XTemplate(
                  '<h4><a href="#">' + this.locale.instructions.end_at + '</a> {name}</h4>'
            ).compile(); 
    },

    makeLegTemplate : function(mode)
    {
        return new Ext.XTemplate(
                  '<h4><a href="#">' + mode + ' </a>',
                    '{[otp.util.StringFormattingUtils.getDirection(values.direction)]} ',
                    this.locale.directions.to + ' {toName}',
                  '</h4>',
                  '<tpl if="toStopId != null && toStopId.length &gt; 0 && showStopIds">',
                    '<p class="leg-info"><span class="stopid">' + this.locale.labels.stop_id + ' {toStopId}</span></p>',
                  '</tpl>',
                  '<tpl if="duration != null && duration &gt; 0">',
                    '<p class="leg-info transfers">' + this.locale.labels.about + ' {duration} ' + this.getDurationTemplateString() + ' - {distance}</p>', 
                  '</tpl>',
                  '<tpl for="formattedSteps">',
                    '{.}',
                  '</tpl>'
            ).compile();
    },

    getDurationTemplateString : function()
    {
        return '<tpl if="duration == 1.0">' + this.locale.time.minute  + '</tpl>' +
               '<tpl if="duration != 1.0">' + this.locale.time.minutes + '</tpl>';
    
    },

    m_transitLeg  : null,
    getTransitLeg : function()
    {
        if (this.m_transitLeg == null)
            this.m_transitLeg = new Ext.XTemplate(this.TP_LEG_MODE + this.TP_LEG_BASE_STR).compile();

        return this.m_transitLeg;
    },

    m_interlineLeg  : null,
    getInterlineLeg : function()
    {
        if (this.m_interlineLeg == null)
            this.m_interlineLeg = new Ext.XTemplate(this.TP_LEG_CONTINUES + this.TP_LEG_BASE_STR).compile();

        return this.m_interlineLeg;
    },

    applyTransitLeg : function(leg)
    {
        var retVal = null;
        if(leg)
        {
            var interline = leg.get('interline');
            if (interline == null || (interline != "true" && interline !== true)) 
            {
                retVal = this.getTransitLeg().applyTemplate(leg.data); 
            }
            else 
            {
                retVal = this.getInterlineLeg().applyTemplate(leg.data);
            }
        }
        return retVal;
    },

    CLASS_NAME: "otp.planner.Templates"
};

otp.planner.Templates = new otp.Class(otp.planner.Templates);