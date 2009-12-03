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

otp.namespace("otp.util");

/**
 * Utility routines for Analytics
 */
otp.util.AnalyticsUtils = {

    TRIP_SUBMIT      : "/imap/planner/submit",
    TRIP_SUCCESS     : "/imap/planner/success",
    TRIP_GEO_ERROR   : "/imap/planner/error/geocoder",
    TRIP_ERROR       : "/imap/planner/error/other",
    TRIP_PRINT       : "/imap/planner/print",
    TRIP_EDIT        : "/imap/planner/edit",
    TRIP_REVERSE     : "/imap/planner/reverse",
    TRIP_FORM_REVERSE: "/imap/planner/form-reverse",

    VEHICLES         : "/imap/vehicles",
    SEARCH           : "/imap/search",
    TC               : "/imap/tc",
    PR               : "/imap/pr",
    ZIPCAR           : "/imap/zipcar",
    ROUTES           : "/imap/routes",
    MEASURE          : "/imap/measure",
    DIALOG           : "/imap/dialog",

    MOBILITY         : "/imap/mobility/start",
    MOBILITY_LAYER   : "/imap/mobility/layer",

    gaJsHost         : (("https:" == document.location.protocol) ? "https://ssl." : "http://www."),
    pageTracker      : null,

    defaultDomain    : "demo.opentripplanner.org", 
    defaultEventName : "unknown event",
    defaultGoogleId  : "UA-11476476-2",

    /** */
   importGoogleAnalytics : function(domain, id)
   {
       console.log("enter AnalyticsUtils.importGoogleAnalytics");
        try
        {
            document.write(unescape("%3Cscript src='" + this.gaJsHost + "google-analytics.com/ga.js' type='text/javascript'%3E%3C/script%3E"));
        }
        catch(e)
        {
            console.log("GA EXCEPTION: AnalyticsUtils.importGoogleAnalytics threw exception " + e);
        }

        console.log("exit AnalyticsUtils.importGoogleAnalytics");
   },

   /** */
   initGoogleAnalytics : function(domain, id)
   {
       console.log("enter AnalyticsUtils.initGoogleAnalytics");
        try
        {
            if(domain == null) domain = this.defaultDomain;
            if(id     == null) id     = this.defaultGoogleId;

            this.pageTracker = _gat._getTracker(id);
            this.pageTracker._setDomainName(domain);
            this.pageTracker._trackPageview();
        }
        catch(e)
        {
            console.log("GA EXCEPTION: AnalyticsUtils.initGoogleAnalytics threw exception " + e);
        }
        console.log("exit initGoogleAnalytics");
   },

    /** */
    initGoogleAnalyticsExtOnReady : function(domain, id)
    {
        Ext.onReady(function()
        {
            otp.util.AnalyticsUtils.initGoogleAnalytics(domain, id);
        });
    },

    /** */ 
    gaEvent : function(name)
    {
        try
        {
            if(name == null) name = this.defaultEventName;
            this.pageTracker._trackPageview(name);
        }
        catch(e)
        {
            console.log("GA EXCEPTION: AnalyticsUtils.gaEvent for " + name + ", threw this exception:" + e);
        }
    },

    CLASS_NAME : "otp.utl.AnalyticsUtils"
};
