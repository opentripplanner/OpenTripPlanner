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

        clear         : "Clear",
        clearTip      : "<b>Clear</b><br/>Clear the map and all active tools.",

        fullScreen    : "Full Screen",
        fullScreenTip : "<b>Full Screen</b><br/>Show -or- hide tool panels",
        
        print         : "Print",
        printTip      : "<b>Print</b><br/>Print this trip plan with stop maps.",
        
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
            walk          : "Maximum walk",
            arriveDepart  : "DELETE ME",
            mode          : "Travel by",
            go            : "Go",
            planTrip      : "Plan Your Trip",
            newTrip       : "New Trip",
            tripOnTrimet  : "Plan a trip on TriMet"
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
          ['X', 'Fewest Transfers'],
          ['T', 'Quickest Trip'] 
        ],
    
        arriveDepart: 
        [
          ['false', 'Depart'], 
          ['true', 'Arrive']
        ],
    
        walkDistance : 
        [
            ['0.10',   '1/10 mile'],
            ['0.25',   '1/4 mile'],
            ['0.50',   '1/2 mile'],
            ['0.75',   '3/4 mile'],
            ['0.9999', '1 mile']
        ],
    
        mode : 
        [
            ['A', 'Bus & Train'],
            ['B', 'Bus only'],
            ['T', 'Train only']
        ]
    },

    CLASS_NAME : "otp.locale.English"
};
