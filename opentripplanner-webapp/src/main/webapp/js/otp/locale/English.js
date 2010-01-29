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

    contextMenu : 
    {
        fromHere         : "Start a trip here",
        toHere           : "End a trip here",

        centerHere       : "Center map here",
        zoomInHere       : "Zoom in here",
        zoomOutHere      : "Zoom out of here",
        previous         : "Last map position",
        next             : "Next map position"
    },

    service : 
    {
        weekdays:  "Weekdays",
        saturday:  "Saturday",
        sunday:    "Sunday",
        schedule:  "Schedule"
    },

    indicators : 
    {
        ok         : "OK",
        date       : "Date",
        loading    : "Loading",
        searching  : "Searching...",
        qEmptyText : "Address, intersection, landmark or Stop ID..."
    },

    buttons: 
    {
        reverse       : "Reverse",
        reverseTip    : "<b>Reverse directions</b><br/>Plan a return trip by reversing this trip's start and end points, and adjusting the time forward.",
        reverseMiniTip: "Reverse directions",

        edit          : "Edit",
        editTip       : "<b>Edit trip</b><br/>Return to the main trip planner input form with the details of this trip.",

        clear         : "Clear",
        clearTip      : "<b>Clear</b><br/>Clear the map and all active tools.",

        fullScreen    : "Full Screen",
        fullScreenTip : "<b>Full Screen</b><br/>Show -or- hide tool panels",
        
        print         : "Print",
        printTip      : "<b>Print</b><br/>Print friendly version of the trip plan (without map).",
        
        feedback      : "Feedback",
        feedbackTip   : "<b>Feedback</b><br/>Send your thoughts or experiences with the map",

        submit       : "Submit",
        clear        : "Clear",
        ok           : "OK",
        cancel       : "Cancel",
        yes          : "Yes",
        no           : "No"
    },

    // note: keep these lower case (and uppercase via template / code if needed)
    directions : 
    {
        southEast: "south east",
        southWest: "south west",
        northEast: "north east",
        northWest: "north west",
        north:     "north",
        west:      "west",
        south:     "south",
        east:      "east",
        bound:     "bound"
    },

    tripPlanner :
    {
        labels : 
        {
            panelTitle    : "Trip Planner",
            tabTitle      : "Plan a Trip",
            inputTitle    : "Trip Details",
            optTitle      : "Trip Preferences (optional)",
            submitMsg     : "Planning Your Trip...",
            optionalTitle : "",
            date          : "Date",
            time          : "Time",
            when          : "When",
            from          : "From",
            fromHere      : "From here",
            to            : "To",
            toHere        : "To here",
            minimize      : "Show me the",
            maxWalkDistance: "Maximum walk",
            arriveDepart  : "Arrive by/Depart at",
            mode          : "Travel by",
            wheelchair    : "Wheelchair accessible trip", 
            go            : "Go",
            planTrip      : "Plan Your Trip",
            newTrip       : "New Trip"
        },

        error:
        {
            title        : 'Trip Planner Error',
            deadMsg      : "Map Trip Planner is currently not responding. Please wait a few minutes to try again, or try the text trip planner (see link below).",
            geoFromMsg   : "Please select the 'From' location for your trip: ",
            geoToMsg     : "Please select the 'To' location for your trip: "
        },

        options: 
        [
          ['TRANSFERS', 'Fewest Transfers'],
          ['SAFE', 'Safest Trip'],
          ['QUICK', 'Quickest Trip'] 
        ],
    
        arriveDepart: 
        [
          ['false', 'Depart'], 
          ['true', 'Arrive']
        ],
    
        maxWalkDistance : 
        [
            ['160',   '1/10 mile'],
            ['420',   '1/4 mile'],
            ['840',   '1/2 mile'],
            ['1260',  '3/4 mile'],
            ['1600',  '1 mile'],
            ['3200',  '2 miles'],
            ['4800',  '3 miles'],
            ['6400',  '4 miles'],
            ['7600',  '5 miles'],
            ['7601',  '10 miles'],
            ['7602',  '15 miles'],
            ['7603',  '20 miles'],
            ['7604',  '30 miles'],
            ['7605',  '40 miles'],
            ['7606',  '50 miles'],
            ['7607',  '100 miles'],
        ],
    
        mode : 
        [
            ['TRANSIT,WALK', 'Transit'],
            ['BUSISH,TRAINISH,WALK', 'Bus & Train'],
            ['BUSISH,WALK', 'Bus only'],
            ['TRAINISH,WALK', 'Train only'],
            ['WALK', 'Walk only'],
            ['BICYCLE', 'Bicycle']
        ],

        wheelchair :
        [
            ['false', 'Not required'],
            ['true', 'Required']
        ]
    },

    CLASS_NAME : "otp.locale.English"
};
