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
try
{
otp.planner.Templates = {

    TODO_FARE_ZONE : "2.30 - SEE BELOW - TODO PUT FARE ZONE IN XML",

    TP_ITINERARY : new Ext.XTemplate(
          '<p><a href="#">{id}</a>: {startTimeDisplay} - {endTimeDisplay} <tpl if="numTransfers"><br /><span class="transfers">({numTransfers} transfer<tpl if="numTransfers != 1">s</tpl>, {duration} minute<tpl if="duration != 1.0">s</tpl>)</span></tpl></p>'
    ).compile(),

    /**
     * templates for Trip Planner state messaging (eg: feedback emails, etc...).
     */
    // TODO - localize
    tripFeedbackDetails : new Ext.XTemplate( 
        'Trip Details: {from} to {to} {arr} {time} on {date}, {opt} with a walk of {walk} via {mode}.'
    ).compile(),

    txtPlannerURL : new Ext.XTemplate( 
        '{url}?',
        'from={[values.from.replace(/&/g,"@")]}',
        '&to={[values.to.replace(/&/g,"@")]}',
        '&min={opt}&walk_speed={walk_speed}&mode={mode}&id={id}',
        '&on={date}',
        '&{[values.arriveBy=="false" ? "after" : "by"]}={time}'
    ).compile(),

    tripPrintTemplate : new Ext.XTemplate( 
        '{url}?',
        'fromPlace={[values.from.replace(/&/g,"@")]}',
        '&toPlace={[values.to.replace(/&/g,"@")]}',
        '<tpl if="fromCoord != null">&fromCoord={fromCoord}</tpl>',
        '<tpl if="toCoord   != null">&toCoord={toCoord}</tpl>',
        '&arr={arriveBy}&min={opt}&maxWalkDistance={maxWalkDistance}&mode={mode}&itinID={itinID}&submit',
        '&date={date}',
        '&time={time}'
    ).compile(),

    TP_TRIPDETAILS : new Ext.XTemplate(
    '<div id="trip-details">',
    '<h3>Trip Details:</h3>',
    '<table cellpadding="3" cellspacing="0" border="0">',
        '<tr><td>Travel</td><td>{startTimeDisplay}</td></tr>',
        '<tr><td>Valid</td><td>{[new Date().format("F jS, Y @ g:ia")]}</td></tr>',
        '<tr><td>Time</td><td>{transitTime} minute<tpl if="transitTime != 1">s</tpl>',
            '<tpl if="waitingTime &gt; 0"> (plus {waitingTime} minute<tpl if="waitingTime != 1.0">s</tpl> transfer)</tpl>',
         '</td></tr>',
        '<tpl if="walkDistance"><tr><td>Walk</td><td>{walkDistance}</td></tr></tpl>',
    '</table></div>'
    ).compile(),

    TP_LEG_BASE_STR : ''
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
        + '</tpl>',
    
    TP_LEG_MODE : '<h4><a href="#">{mode}</a> {routeName}</h4>',
    TP_LEG_CONTINUES : '<h4><a href="#">Continues as</a> {routeName} <span class="transfers">(stay on board)</span></h4>',

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

    TP_WALK_LEG : new Ext.XTemplate(
          '<h4><a href="#">Walk</a> {[otp.util.StringFormattingUtils.getDirection(values.direction)]} to {toName}</h4>',
          '<tpl if="toStopId != null && toStopId.length &gt; 0 && showStopIds"><p>Stop ID {toStopId}</p></tpl>',
          '<p class="transfers">About {duration} minute<tpl if="duration != 1.0">s</tpl> - {distance}</p>',
          '<ol class="steps"><tpl for="formattedSteps">',
              '{.}',
          '</tpl></ol>'
    ).compile(),
    
    TP_BICYCLE_LEG : new Ext.XTemplate(
            '<h4><a href="#">Bike</a> {[otp.util.StringFormattingUtils.getDirection(values.direction)]} to {toName}</h4>',
            '<tpl if="toStopId != null && toStopId.length &gt; 0 && showStopIds"><p>Stop ID {toStopId}</p></tpl>',
            '<p class="transfers">About {duration} minute<tpl if="duration != 1.0">s</tpl> - {distance}</p>',
            '<ol class="steps"><tpl for="formattedSteps">',
                '{.}',
            '</tpl></ol>'
      ).compile(),
      
    TP_START : new Ext.XTemplate(
          '<h4><a href="#">Start at</a> {name}</h4>'
    ).compile(), 

    TP_END : new Ext.XTemplate(
          '<h4><a href="#">End at</a> {name}</h4>'
    ).compile(), 

    CLASS_NAME: "otp.planner.Templates"
}
}
catch(e)
{
    console.log("planner.Templates Ext exception can be ignored -- just means you aren't including Ext.js in your app, which is OK");
}
