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
otp.planner.ParamTemplate = 'fromPlace={[values.fromPlace.replace(/&/g,"@")]}'
        + '&toPlace={[values.toPlace.replace(/&/g,"@")]}'
        + '&arr={arriveBy}&min={opt}&maxWalkDistance={maxWalkDistance}&mode={mode}&itinID={itinID}&submit'
        + '&date={date}'
        + '&time={time}';

otp.planner.Templates = {

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

    initialize : function(config) {
        otp.configure(this, config);

        if(this.TP_LEG_MODE == null)
            this.TP_LEG_MODE = '<h4><a href="#">{mode}</a> {routeName}</h4>';

        if(this.TP_ITINERARY == null)
            this.TP_ITINERARY = new Ext.XTemplate(
                  '<p><a href="#">{id}</a>: '
                  + ' {startTimeDisplay} - {endTimeDisplay} '
                  + '<tpl if="numTransfers &gt; 0"><br/><span class="transfers">'
                  + '({numTransfers} ' + 'transfer' + '<tpl if="numTransfers != 1">s</tpl>, '
                  + '{duration} minute<tpl if="duration != 1.0">s</tpl>)</span></tpl></p>'
            ).compile();

        if(this.TP_LEG_CONTINUES == null)
            this.TP_LEG_CONTINUES = '<h4><a href="#">Continues as</a> {routeName} <span class="transfers">(stay on board)</span></h4>';

        if(this.tripFeedbackDetails == null)
            // Trip Planner state messaging (eg: feedback emails, etc...).
            this.tripFeedbackDetails = new Ext.XTemplate( 
                'Trip Details: {fromPlace} to {toPlace} {arr} {time} on {date}, {opt} with a walk of {walk} via {mode}.'
            ).compile();

        if(this.tripPrintTemplate == null)
            this.tripPrintTemplate = new Ext.XTemplate( 
                '{url}?' + otp.planner.ParamTemplate
            ).compile();

        if(this.TP_TRIPDETAILS == null)
            this.TP_TRIPDETAILS = new Ext.XTemplate(
                '<div id="trip-details">',
                '<h3>Trip Details:</h3>',
                '<table cellpadding="3" cellspacing="0" border="0">',
                    '<tpl if="regularFare != null"><tr><td>Fare</td><td>{regularFare}</td></tr></tpl>',
                    '<tr><td>Travel</td><td>{startTimeDisplay}</td></tr>',
                    '<tr><td>Valid</td><td>{[new Date().format("F jS, Y @ g:ia")]}</td></tr>',
                    '<tr><td>Time</td><td>{duration} minute<tpl if="duration != 1">s</tpl></td></tr>',
                    '<tpl if="walkDistance"><tr><td>{distanceVerb}</td><td>{walkDistance}</td></tr></tpl>',
                '</table></div>'
            ).compile();

        if(this.TP_LEG_BASE_STR == null)
            this.TP_LEG_BASE_STR = ''
                + '<p><b>{startTimeDisplayShort}</b> Depart {fromName}'
                + '<tpl if="headsign != null && headsign.length &gt; 0"> ({headsign})</tpl>'
                + '<tpl if="fromStopId != null && fromStopId.length &gt; 0 && showStopIds"><br />Stop ID {fromStopId}</tpl>'
                + '</p>'
                + '<tpl if="duration != null"><div class="duration">{duration} minute<tpl if="duration != 1.0">s</tpl></div></tpl>'
                + '<p><b>{endTimeDisplayShort}</b> Arrive {toName}'
                + '<tpl if="toStopId != null && toStopId.length &gt; 0 && showStopIds"><br/>Stop ID {toStopId}</tpl>'
                + '</p>'
                + '<tpl if="alerts != null && alerts.length &gt; 0">'
                + '<tpl for="alerts">'
                +   '<p><br/><img src="images/ui/alert.gif" align="absmiddle"/> '
                +   '<b>Alert for route {parent.routeNumber}: </b>{.}</p>'
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
                    this.locale.instructions.toward + ' {toName}',
                  '</h4>',
                  '<tpl if="toStopId != null && toStopId.length &gt; 0 && showStopIds">',
                    '<p>' + this.locale.labels.stopID + ' {toStopId}</p>',
                  '</tpl>',
                  '<tpl if="duration != null && duration &gt; 0">',
                    '<p class="transfers">' + this.locale.labels.about + ' {duration} ', 
                    '<tpl if="duration == 1.0">' + this.locale.time.minute  + '</tpl>',
                    '<tpl if="duration != 1.0">' + this.locale.time.minutes + '</tpl>',
                    ' - {distance}</p>',
                  '</tpl>',
                  '<ol class="steps"><tpl for="formattedSteps">',
                    '{.}',
                  '</tpl></ol>'
            ).compile();
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

    CLASS_NAME: "otp.planner.Templates"
};

otp.planner.Templates = new otp.Class(otp.planner.Templates);