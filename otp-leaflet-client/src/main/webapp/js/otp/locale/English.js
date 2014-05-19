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

otp.namespace("otp.locale");

/**
  * @class
  */
otp.locale.English = {

    config :
    {
        metricsSystem : "english",
        attribution   : {
            title   : "License Attribution",
            content : "Disclaimer goes here"
        }
    },


    time:
    {
        hour_abbrev    : "hour",
        hours_abbrev   : "hours",
        hour           : "hour",
        hours          : "hours",

        minute         : "minute",
        minutes        : "minutes",
        minute_abbrev  : "min",
        minutes_abbrev : "mins",
        second_abbrev  : "sec",
        seconds_abbrev : "secs",
        format         : "F jS, Y @ g:ia",
        date_format    : "n/j/Y",
        time_format    : "g:ia",
        months         : ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']
    },


    tripPlanner :
    {
        // see otp/planner/*.js for where these values are used

        // see otp/config.js for where these values are used
        link : 
        {
            text           : "Link to this trip (OTP)",
            trip_separator : "This trip on other transit planners",
            bike_separator : "On other bike trip planners",
            walk_separator : "On other walking direction planners",
            google_transit : "Google Transit",
            google_bikes   : "Google Bike Directions",
            google_walk    : "Google Walking Directions",
            google_domain  : "http://www.google.com"
        },

        // see otp.planner.Forms for use
        geocoder:
        {
            working      : "Looking up address ....",
            error        : "Did not receive any results",
            msg_title    : "Review trip plan",
            msg_content  : "Please correct errors before planning your trip",
            select_result_title : "Please select a result",
            address_header : "Address"
        },

        error:
        {
            title        : 'Trip Planner Error',
            deadMsg      : "Map Trip Planner is currently not responding. Please wait a few minutes to try again, or try the text trip planner (see link below).",
            geoFromMsg   : "Please select the 'From' location for your trip: ",
            geoToMsg     : "Please select the 'To' location for your trip: "
        },
        
        // default messages from server if a message was not returned ... 'Place' error messages also used when trying to submit without From & To coords.
        msgcodes:
        {
            200: "Plan OK",
            500: "Server error",
            400: "Trip out of bounds",
            404: "Path not found",
            406: "No transit times",
            408: "Request timed out",
            413: "Invalid parameter",
            440: "The 'From' place is not found ... please re-enter it.",
            450: "The 'To' place is not found ... please re-enter it.",
            460: "Places 'From' and 'To' are not found ... please re-enter them.",
            470: "Places 'From' or 'To' are not wheelchair accessible",
            409: "Too close",
            340: "Geocode 'From' ambiguous",
            350: "Geocode 'To' ambiguous",
            360: "Geocodes 'From' and 'To' are ambiguous"
        },

        options: 
        [
          ['TRANSFERS', 'Fewest transfers'],
          ['QUICK',     'Quick trip'],
          ['SAFE',      'Bike friendly trip'],
          ['TRIANGLE',  'Custom trip...']
        ],
    
    
        maxWalkDistance : 
        [
            ['160',   '1/10 mile'],
            ['420',   '1/4 mile'],
            ['840',   '1/2 mile'],
            ['1260',  '3/4 mile'],
            ['1609',  '1 mile'],
            ['3219',  '2 miles'],
            ['4828',  '3 miles'],
            ['6437',  '4 miles'],
            ['8047',  '5 miles'],
            ['16093',  '10 miles'],
            ['24140',  '15 miles'],
            ['32187',  '20 miles'],
            ['48280',  '30 miles'],
            ['64374',  '40 miles'],
            ['80467',  '50 miles'],
            ['160934',  '100 miles']
        ],

    	walkSpeed :
    	[
    		['0.447',  '1 mph'],
    		['0.894',  '2 mph'],
    		['1.341',  '3 mph'],
    		['1.788',  '4 mph'],
    		['2.235',  '5 mph']
    	],


    },

    CLASS_NAME : "otp.locale.English"
};

