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

otp.locale.Dutch = {

    config :
    {
        metricsSystem : "international",
        rightClickMsg : "Klik met de rechtermuisknop op de plattegrond om het begin en einde van je reis aan te geven.",
        attribution   : {
            title   : "License Attribution",
            content : "Disclaimer"
        }
    },

    contextMenu : 
    {
        fromHere         : "Begin je reis hier",
        toHere           : "Eindig je reis hier",

        centerHere       : "Centreer kaart",
        zoomInHere       : "Inzoomen",
        zoomOutHere      : "Uitzoemen",
        previous         : "Vorige positie",
        next             : "Volgende positie"
    },
	
	  // TODO Localize Me
    bikeTriangle : 
    {
        safeName : "Veiligst",
        safeSym  : "V",

        hillName : "Hellingen",
        hillSym  : "H",

        timeName : "Snelst",
        timeSym  : "S"
    },

    service : 
    {
        weekdays:  "Weekdagen",
        saturday:  "Zaterdag",
        sunday:    "Zondag",
        schedule:  "Dienstregeling"
    },

    indicators : 
    {
        ok         : "OK",
        date       : "Datum",
        loading    : "Wordt geladen",
        searching  : "Zoeken...",
        qEmptyText : "Adres, kruising, herkeningspunt, of haltenummer..."
    },

    buttons: 
    {
        reverse       : "Terugreis",
        reverseTip    : "<b>Terugreis</b><br/>Plan een terugreis op een later tijdstip.",
        reverseMiniTip: "Terugreis",

        edit          : "Bewerk",
        editTip       : "<b>Bewerk</b><br/>Ga terug naar het zoekformulier.",

        clear         : "Wis",
        clearTip      : "<b>Wis</b><br/>Wis de plattegrond en gereedschappen.",

        fullScreen    : "Volledig scherm",
        fullScreenTip : "<b>Volledig scherm</b><br/>Verberg gereedschappen",

        print         : "Print",
        printTip      : "<b>Print</b><br/>Print vriendelijke versie (zonder plattegrond)",

        link          : "Link",
        linkTip      : "<b>Link</b><br/>Toon de URL voor deze reis.",

        feedback      : "Feedback",
        feedbackTip   : "<b>Feedback</b><br/>Stuur je ervaringen en feedback over deze reisplanner",

        submit       : "Submit",
        clearButton  : "Wis",
        ok           : "OK",
        cancel       : "Annuleer",
        yes          : "Ja",
        no           : "Nee",
        showDetails  : "&darr; Toon details &darr;",
        hideDetails  : "&uarr; Verberg details &uarr;"

    },

    // note: keep these lower case (and uppercase via template / code if needed)
    directions : 
    {
        southeast:      "zuidoost",
        southwest:      "zuidwest",
        northeast:      "noordoost",
        northwest:      "noordwest",
        north:          "noord",
        west:           "west",
        south:          "zuid",
        east:           "oost",
        bound:          "richting",
        left:           "links",
        right:          "rechts",
        slightly_left:  "flauwe bocht links",
        slightly_right: "flauwe bocht rechts",
        hard_left:      "scherpe bocht links",
        hard_right:     "scherpe bocht rechts",
        'continue':     "ga verder op",
        to_continue:    "om door te gaan op",
        becomes:        "wordt",
        at:             "bij",
        on:             "op",
        to:             "naar",
        via:            "via",
        circle_counterclockwise: "neem rotonde linksom",
        circle_clockwise:        "neem rotonde rechtsom",
        elevator: "neem lift naar"
    },

    // see otp.planner.Templates for use
    instructions :
    {
        walk         : "Lopen",
        walk_toward  : "Loop richting",
        walk_verb    : "Loop",
        bike         : "Fietsen",
        bike_toward  : "Fiets",
        bike_verb    : "Fiets",
        drive        : "Auto",
        drive_toward : "Rij richting",
        drive_verb   : "Rij",
        move         : "Ga",
        move_toward  : "Ga naar",

        transfer     : "Stap over",
        transfers    : "Overstappen",

        continue_as  : "Gaat verder als",
        stay_aboard  : "stap niet uit",

        depart       : "Vertrek",
        arrive       : "Aankomst",

        start_at     : "Begin bij",
        end_at       : "Eindig bij"
    },

    // see otp.planner.Templates for use
    labels : 
    {
        agency_msg   : "Uitgevoerd door",
        agency_msg_tt: "Open website van vervoerder in nieuw venster...",
        about        : "Over",
        stop_id      : "Haltenummer",
        trip_details : "Reisdetails",
        fare         : "Ritprijs",
        fare_symbol  : "\u20ac",
        travel       : "Reis",
        valid        : "Geldig",
        trip_length  : "Tijd",
        with_a_walk  : "met een wandeling van",
        alert_for_rt : "Melding voor deze route"
    },

    // see otp.planner.Templates for use -- one output are the itinerary leg headers
    modes :
    {
        WALK:           "Lopen",
        BICYCLE:        "Fietsen",
        CAR:            "Auto",
        TRAM:           "Tram",
        SUBWAY:         "Metro",
        RAIL:           "Trein",
        BUS:            "Bus",
        FERRY:          "Veer",
        CABLE_CAR:      "Kabeltram",
        GONDOLA:        "Cabinelift",
        FUNICULAR:      "Kabelspoorweg"
    },

    ordinal_exit:
    {
        1:  "tot de eerste afslag",
        2:  "tot de tweede afslag",
        3:  "tot de derde afslag",
        4:  "tot de vierde afslag",
        5:  "tot de vijfde afslag",
        6:  "tot de zesde afslag",
        7:  "tot de zevende afslag",
        8:  "tot de achtste afslag",
        9:  "tot de negende afslag",
        10: "tot de tiende afslag"
    },

    time:
    {
        minute         : "minuut",
        minutes        : "minuten",
        minute_abbrev  : "min",
        minutes_abbrev : "min",
        second_abbrev  : "s",
        seconds_abbrev : "s",
        format         : "j F Y @ H:i",
        date_format    : "d-m-Y",
        time_format    : "H:i",
        months         : ['jan', 'feb', 'mar', 'apr', 'mei', 'jun', 'jul', 'aug', 'sep', 'oct', 'nov', 'dec']
    },

    systemmap :
    {
        labels :
        {
            panelTitle : "Lijnenkaart"
        }
    },

    tripPlanner :
    {
        // see otp/planner/*.js for where these values are used
        labels : 
        {
            panelTitle    : "Reisplanner",
            tabTitle      : "Plan je reis",
            inputTitle    : "Reisdetails",
            optTitle      : "Reisvoorkeuren (optioneel)",
            submitMsg     : "Reis wordt gepland...",
            optionalTitle : "",
            date          : "Datum",
            time          : "Tijd",
            when          : "Op",
            from          : "Van",
            fromHere      : "Van hier",
            to            : "Naar",
            toHere        : "Naar hier",
            intermediate  : "Tussenstops",
            minimize      : "Toon reis met",
            maxWalkDistance: "Maximum loopafstand",
            maxBikeDistance: "Maximum fietsafstand",
            arriveDepart  : "Vertrek/Aankomst voor",
            mode          : "Reis per",
            wheelchair    : "Rolstoeltoegankelijk", 
            go            : "Ga",
            planTrip      : "Plan je reis",
            newTrip       : "Nieuwe reis"
        },

        // see otp/config.js for where these values are used
        link : 
        {
            text           : "Link naar deze reis",
            trip_separator : "Deze reis met andere reisplanners",
            bike_separator : "Op andere fietsrouteplanners",
            walk_separator : "Op andere looprouteplanners",
            google_transit : "Google Transit",
            google_bikes   : "Google Fietsroutes",
            google_walk    : "Google Looproutes",
            google_domain  : "http://www.google.com"
        },

        // see otp.planner.Forms for use
        geocoder:
        {
            working      : "Adres wordt opgezocht ....",
            error        : "Kon geen resultaten vinden",
            msg_title    : "Bekijk reisplanning",
            msg_content  : "Bekijk deze fouten voor het plannen van je reis",
            select_result_title : "Kies een resultaat",
            address_header : "Adres"
        },

        error:
        {
            title        : 'Reisplanner fout',
            deadMsg      : "De reisplanner reageert momenteel even niet. Wacht een paar minuten, of probeer de tekstversie (zie link hieronder).",
            geoFromMsg   : "Selecteer een vertreklocatie voor je reis: ",
            geoToMsg     : "Selecteer een bestemming voor je reis: "
        },
        
        // default messages from serv er if a message was not returned
        msgcodes:
        {
            200: "Plan OK",
            500: "Server fout",
            400: "Reis buiten bekend gebied",
            404: "Geen route gevonden",
            406: "Geen OV tijden gevonden",
            408: "Resultaat niet op tijd gevonden",
            413: "Ongeldige parameter",
            440: "Vertrekpunt niet gevonden",
            450: "Bestemming niet gevonden",
            460: "Vertekpunt en bestemming niet gevonden",
            470: "Vertrekpunt of bestemming niet rolstoeltoegankelijk",
            409: "Te dichtbij",
            340: "Vertrekpunt onduidelijk",
            350: "Bestemming onduidelijk",
            360: "Vertrekpunt en bestemming onduidelijk"
        },

        options: 
        [
          ['TRANSFERS', 'Minste overstappen'],
          ['QUICK',     'Snelste reis'],
          ['SAFE',      'Veiligste reis'],
          ['TRIANGLE',  'Eigen voorkeuren...']
        ],
    
        arriveDepart: 
        [
          ['false', 'Vertrek'], 
          ['true',  'Aankomst']
        ],
    
        maxWalkDistance : 
        [
            ['100',   '100 m'],
            ['250',   '250 m'],
            ['500',   '500 m'],
            ['750',  '750 m'],
            ['1000',  '1 km'],
            ['2000',  '2 km'],
            ['3000',  '3 km'],
            ['4000',  '4 km'],
            ['5000',  '5 km'],
            ['10000',  '10 km'],
            ['15000',  '15 km'],
            ['20000',  '20 km'],
            ['30000',  '30 km']
        ],
    
        mode : 
        [
            ['TRANSIT,WALK', 'OV'],
            ['BUSISH,WALK', 'Alleen bus'],
            ['TRAINISH,WALK', 'Alleen trein'],
            ['WALK', 'Alleen lopen'],
            ['BICYCLE', 'Fiets'],
            ['TRANSIT,BICYCLE', 'OV & Fiets'],
        ],

        // TODO: remove this hack, and provide code that allows the mode array to be configured with different transit modes.
        //       (note that we've been broken for awhile here, since many agencies don't have a 'Train' mode either...this needs attention)
        // IDEA: maybe we start with a big array (like below), and the pull out modes from this array when turning off various modes...

        with_bikeshare_mode :
        [
            ['TRANSIT,WALK', 'OV'],
            ['BUSISH,WALK', 'Alleen bus'],
            ['TRAINISH,WALK', 'Alleen trein'],
            ['WALK', 'Alleen lopen'],
            ['BICYCLE', 'Fiets'],
            ['WALK,BICYCLE', 'Huurfiets'],
            ['TRANSIT,BICYCLE', 'OV & Fiets'],
            ['TRANSIT,WALK,BICYCLE', 'OV & OV-fiets']
        ],

        wheelchair :
        [
            ['false', 'Nee'],
            ['true', 'Ja']
        ]
    },

    CLASS_NAME : "otp.locale.Dutch"
};
