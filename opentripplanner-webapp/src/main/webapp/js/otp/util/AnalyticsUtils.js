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

    OTP_TRIP_SUBMIT      : "/imap/planner/submit",
    OTP_TRIP_SUCCESS     : "/imap/planner/success",
    OTP_TRIP_GEO_ERROR   : "/imap/planner/error/geocoder",
    OTP_TRIP_ERROR       : "/imap/planner/error/other",
    OTP_TRIP_PRINT       : "/imap/planner/print",
    OTP_TRIP_EDIT        : "/imap/planner/edit",
    OTP_TRIP_REVERSE     : "/imap/planner/reverse",
    OTP_TRIP_FORM_REVERSE: "/imap/planner/form-reverse",

    MEASURE          : "/imap/measure",
    DIALOG           : "/imap/dialog",

    gaJsHost         : (("https:" == document.location.protocol) ? "https://ssl." : "http://www."),
    pageTracker      : null,

    defaultDomain    : "demo.opentripplanner.org", 
    defaultEventName : "unknown event",
    defaultGoogleId  : "UA-11476476-2",

    /** */
   importGoogleAnalytics : function(domain, id)
   {
        try
        {
            document.write(unescape("%3Cscript src='" + this.gaJsHost + "google-analytics.com/ga.js' type='text/javascript'%3E%3C/script%3E"));
        }
        catch(e)
        {
            console.log("GA EXCEPTION: AnalyticsUtils.importGoogleAnalytics threw exception " + e);
        }
   },

   /** */
   initGoogleAnalytics : function(domain, id)
   {
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
   },

    /** */
    initGoogleAnalyticsExtOnReady : function(domain, id)
    {
        Ext.onReady(function()
        {
            otp.util.Analytics.initGoogleAnalytics(domain, id);
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

    CLASS_NAME : "otp.util.AnalyticsUtils"
};
otp.util.Analytics = otp.util.AnalyticsUtils;

