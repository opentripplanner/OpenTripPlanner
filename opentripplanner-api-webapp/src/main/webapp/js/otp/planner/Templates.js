otp.namespace("otp.planner");

/**
  * Web Map / TripPlanner
  */
try
{
otp.planner.Templates = {

    TODO_FARE_ZONE : "2.30 - SEE BELOW - TODO PUT FARE ZONE IN XML",

    TP_ITINERARY : new Ext.XTemplate(
          '<p><a href="#">{id}</a>: {startTime} - {endTime} <tpl if="numTransfers"><h10 class="transfers">({numTransfers} transfer<tpl if="numTransfers != 1">s</tpl>, {duration} minute<tpl if="duration != 1.0">s</tpl>)</h10></tpl></p>'
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
        '&min={optParam}&walk={walkParam}&mode={modeParam}&id={id}',
        '&on={date}',
        '&{[values.arrParam=="D" ? "after" : "by"]}={time}'
    ).compile(),

    tripPrintTemplate : new Ext.XTemplate( 
        '{url}?',
        'fromPlace={[values.from.replace(/&/g,"@")]}',
        '&toPlace={[values.to.replace(/&/g,"@")]}',
        '<tpl if="fromCoord != null">&fromCoord={fromCoord}</tpl>',
        '<tpl if="toCoord   != null">&toCoord={toCoord}</tpl>',
        '&arr={arrParam}&min={optParam}&walk={walkParam}&mode={modeParam}&itinID={itinID}&submit',
        '&date={date}',
        '&time={time}'
    ).compile(),

    TP_TRIPDETAILS : new Ext.XTemplate(
    '<div id="trip-details">',
    '<h3>Trip Details:</h3>',
    '<table cellpadding="3" cellspacing="0" border="0">',
        '<tr><td>Travel</td><td>{date} @ {startTime}</td></tr>',
        '<tr><td>Valid</td><td>{[new Date().format("F jS, Y @ g:ia")]}</td></tr>',
        '<tr><td>Time</td><td>{transitTime} minute<tpl if="transitTime != 1">s</tpl>',
            '<tpl if="waitingTime &gt; 0"> (plus {waitingTime} minute<tpl if="waitingTime != 1.0">s</tpl> transfer)</tpl>',
         '</td></tr>',
        '<tpl if="walkDistance &gt; 0.0"><tr><td>Walk</td><td>{walkDistance} mile<tpl if="walkDistance != 1.0">s</tpl></td></tr></tpl>',
        '<tr><td>Fares</td><td>',
            '<tpl if="regularFare &gt; 0.00">Adult <tpl if="regularFare == 2.30">All Zone</tpl> (${regularFare})<br/>Honored Citizen (${honoredFare})<br/>Youth/Student (${youthFare})</tpl>',
            '<tpl if="regularFare == \'\'">This trip is within Fareless Square, so no fare is required.</tpl>',
            '<tpl if="tramFare &gt; 0"><br/><I>+ Tram Fare</I> (${tramFare} round-trip)</tpl>',
        '</td></tr>',
    '</table></div>'
    ).compile(),

    // TODO - does Date().format() exist on all browsers????  IE 7 and FF seem OK, but...
    TP_TRIPDETAILS_OOOLLLLLDDDDDD : new Ext.XTemplate(
        '<H4>Trip Details</H4>',
        '<P><B>Travel on:</B> {date} @ {startTime}</P>',
        '<P><B>Valid:</B> {[new Date().format("F jS, Y @ g:ia")]}</P>',
        '<P><B>Transit time:</B> {transitTime} minute<tpl if="transitTime != 1">s</tpl></P>',
        '<tpl if="waitingTime &gt; 0">', '<P><B>Waiting time:</B> </P>', '</tpl>',
        '<P><B>Total walk:</B> {walkDistance} mile<tpl if="walkDistance != 1.0">s</tpl></P>',
          '<P><B>Fares: </B><tpl if="regularFare &gt; 0.00">Adult <tpl if="regularFare == 2.30">All Zone</tpl> (${regularFare}), Youth/Student (${youthFare}) or Honored Citizen (${honoredFare})</tpl>',
          '<tpl if="regularFare == \'\'">This trip is within Fareless Square, so no fare is required.</tpl>',
          '<tpl if="tramFare &gt; 0"> <I>+ Tram Fare</I> (${tramFare} round-trip)</tpl>',
        '</P>'
    ).compile(),

    TP_LEG_BASE_STR : ''
        + '<tpl if="fromStopId != null"><p><b>{startTime}</b> Depart {fromDescription}<br/>Stop ID {fromStopId}</p></tpl>'
        + '<tpl if="duration != null"><div class="duration">{duration} minute<tpl if="duration != 1.0">s</tpl></div></tpl>'
        + '<tpl if="toStopId &gt; 0"><p><b>{endTime}</b> Arrive {toDescription}<br/>Stop ID {toStopId}</p></tpl>'
        + '<tpl if="alerts != null && alerts.length &gt; 0">'
        + '<tpl for="alerts">'
        +   '<p><br/><img src="/images/ui/alert.gif" align="absmiddle"/> '
        +   '<b>Alert for route {parent.routeNumber}: </b>{.}</p>'
        + '</tpl>'
        + '</tpl>',
    
    TP_LEG_MODE : '<h4><a href="#">{mode}</a> {routeName}</h4>',
    TP_LEG_CONTINUES : '<h4><a href="#">Continues as</a> {routeName} <h10 class="transfers">(stay on board)</h10></h4>',

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
          '<h4><a href="#">Walk</a> {[otp.util.StringFormattingUtils.getDirection(values.direction)]} to {toDescription}</h4>',
          '<tpl if="toStopId &gt; 0"><p>Stop ID {toStopId}</p></tpl>',
          '<p class="transfers">About {duration} minute<tpl if="duration != 1.0">s</tpl> - {distance} miles</p>'
    ).compile(),
    
    
    TP_START : new Ext.XTemplate(
          '<h4><a href="#">Start at</a> {description}</h4>'
    ).compile(), 

    TP_END : new Ext.XTemplate(
          '<h4><a href="#">End at</a> {description}</h4>'
    ).compile(), 

    CLASS_NAME: "otp.planner.Templates"
}
}
catch(e)
{
    console.log("planner.Templates Ext exception can be ignored -- just means you aren't including Ext.js in your app, which is OK");
}
