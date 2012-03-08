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
otp.locale.Polish = {

    config : 
    {
        metricsSystem : "international",
        rightClickMsg : "TODO - localize me - Right-click on the map to designate the start and end of your trip.",
        attribution   : {
            title   : "License Attribution",
            content : "Disclaimer goes here"
        }
    },

    contextMenu : 
    {
        fromHere         : "Rozpocznij podróż tutaj",
        toHere           : "Zakoñcz podróż tutaj",
        intermediateHere : "Add intermediate point",  // TODO localize

        centerHere       : "Centruj mapê tutaj",
        zoomInHere       : "Przybliż tutaj",
        zoomOutHere      : "Oddal st¹d",
        previous         : "Poprzednia pozycja na mapie",
        next             : "Nastêpna pozycja na mapie"
    },

    // TODO Localize Me
    bikeTriangle : 
    {
        safeName : "Safest",
        safeSym  : "S",

        hillName : "Flattest",
        hillSym  : "F",

        timeName : "Quickest",
        timeSym  : "Q"
    },

    service : 
    {
        weekdays:  "Dni robocze",
        saturday:  "Sobota",
        sunday:    "Niedziela",
        schedule:  "Rozk³ad"
    },

    indicators : 
    {
        ok         : "OK",
        date       : "Data",
        loading    : "£adowanie",
        searching  : "Szukanie...",
        qEmptyText : "Adres, skrzyżowanie, obiekt lub ID przystanku..."
    },

    buttons: 
    {
        reverse       : "Odwróæ",
        reverseTip    : "<b>Odwróæ kierunki</b><br/>Zaplanuj podróż powrotn¹ poprzez zamianê miejscami punktu startowego i koñcowego podróży i przeskok czasu do przodu.",
        reverseMiniTip: "Odwróæ kierunki",

        edit          : "Edytuj",
        editTip       : "<b>Edytuj podróż</b><br/>Powróæ do planowania podróży z detalami tej podróży.",

        clear         : "Wyczyœæ",
        clearTip      : "<b>Wyczyœæ</b><br/>Wyczyœæ mapê i wszystkie aktywne narzêdzia.",

        fullScreen    : "Pe³en ekran",
        fullScreenTip : "<b>Pe³en ekran</b><br/>Pokaż lub ukryj panele narzêdzi",

        print         : "Drukuj",
        printTip      : "<b>Drukuj</b><br/>Wydrukuj plan podróży (bez mapy).",

        link          : "Link",
        linkTip      : "<b>Link</b><br/>Pokaż link do tego planu podróży.",

        feedback      : "Opinie",
        feedbackTip   : "<b>Opinie</b><br/>Wyœlij swoje uwagi i doœwiadczenia z narzêdzia",

        submit       : "Wyœlij",
        clearButton  : "Wyczyœæ",
        ok           : "OK",
        cancel       : "Anuluj",
        yes          : "Tak",
        no           : "Nie",
// TODO
        showDetails  : "Show details...",
        hideDetails  : "Hide details..."
    },

    // note: keep these lower case (and uppercase via template / code if needed)
    directions : 
    {
        southeast:      "po³udniowy wschód",
        southwest:      "po³udniowy zachód",
        northeast:      "pó³nocny wschód",
        northwest:      "pó³nocny zachód",
        north:          "pó³noc",
        west:           "zachód",
        south:          "po³udnie",
        east:           "wschód",
        bound:          "w kierunku",
        left:           "lewo",
        right:          "prawo",
        slightly_left:  "lekko w lewo",
        slightly_right: "lekko w prawo",
        hard_left:      "mocno w lewo",
        hard_right:     "mocno w prawo",
        'continue':     "kontynuuj",
        to_continue:    "kontynuowaæ",
        becomes:        "zmienia siê w",
        at:             "o",
// TODO
        on:             "on",
        to:             "to",
        via:            "via",
        circle_counterclockwise: "take roundabout counterclockwise",
        circle_clockwise:        "take roundabout clockwise"
    },

    // see otp.planner.Templates for use ... these are used on the trip itinerary as well as forms and other places
    instructions :
    {
// TODO
        walk         : "Walk",
        walk_toward  : "Walk",
        walk_verb    : "Walk",
        bike         : "Bike",
        bike_toward  : "Bike",
        bike_verb    : "Bike",
        drive        : "Drive",
        drive_toward : "Drive",
        drive_verb   : "Drive",
        move         : "Proceed",
        move_toward  : "Proceed",

        transfer     : "transfer",
        transfers    : "transfers",

        continue_as  : "Continues as",
        stay_aboard  : "stay on board",

        depart       : "Depart",
        arrive       : "Arrive",

        start_at     : "Start at",
        end_at       : "End at"
    },

    // see otp.planner.Templates for use
    labels : 
    {
// TODO
        agency_msg   : "Service run by", // TODO
        about        : "About",
        stop_id      : "Stop ID",
        trip_details : "Trip Details",
        fare         : "Fare",
        fare_symbol  : "\u20ac",
        travel       : "Travel",
        valid        : "Valid",
        trip_length  : "Czasowy",
        with_a_walk  : "with a walk of",
        alert_for_rt : "Alert for route"
    },

    // see otp.planner.Templates for use -- one output are the itinerary leg headers
    modes : 
    {
// TODO
        WALK:           "WALK",
        BICYCLE:        "BICYCLE",
        CAR:            "CAR",
        TRAM:           "TRAM",
        SUBWAY:         "SUBWAY",
        RAIL:           "RAIL",
        BUS:            "BUS",
        FERRY:          "FERRY",
        CABLE_CAR:      "CABLE CAR",
        GONDOLA:        "GONDOLA",
        FUNICULAR:      "FUNICULAR"
    },

    ordinal_exit:
    {
// TODO
        1:  "to first exit",
        2:  "to second exit",
        3:  "to third exit",
        4:  "to fourth exit",
        5:  "to fifth exit",
        6:  "to sixth exit",
        7:  "to seventh exit",
        8:  "to eighth exit",
        9:  "to ninth exit",
        10: "to tenth exit"
    },

    time:
    {
// TODO
        format        : "D, j M H:i",
        minute        : "minute",
        minutes       : "protokół",
        minute_abbrev : "min",
        minutes_abbrev: "minut",
        second_abbrev : "sek",
        seconds_abbrev: "sekund",
        months:         ['Sty', 'Lut', 'Mar', 'Kwi', 'Maj', 'Cze', 'Lip', 'Sie', 'Wrz', 'Paz', 'Lis', 'Gru']
    },
    
    systemmap :
    {
        labels :
        {
            panelTitle : "Mapa systemowa"
        }
    },

    tripPlanner :
    {
        labels : 
        {
            panelTitle    : "Planer podróży",
            tabTitle      : "Zaplanuj podróż",
            inputTitle    : "Szczegó³y podróży",
            optTitle      : "Preferencje podróży (opcjonalne)",
            submitMsg     : "Planuje Twoj¹ podróż...",
            optionalTitle : "",
            date          : "Data",
            time          : "Godzina",
            when          : "Kiedy",
            from          : "Z",
            fromHere      : "Sk¹d",
            to            : "Do",
            toHere        : "Dok¹d",
            intermediate  : "Intermediate Place",            // TODO
            minimize      : "Pokaż",
            maxWalkDistance: "Maksymalny spacer",
            maxBikeDistance: "Maksymalny bike",              // TODO
            arriveDepart  : "Dojazd/odjazd o",
            mode          : "Podróżuj",
            wheelchair    : "Podróż dostêpna dla niepe³nosprawnych", 
            go            : "IdŸ",
            planTrip      : "Planuj swoj¹ podróż",
            newTrip       : "Nowa podróż"
        },

        // see otp/config.js for where these values are used
        link : 
        {
// TODO
            text           : "Link to this trip (OTP)",
            trip_separator : "This trip on other transit planners",
            bike_separator : "On other bike trip planners",
            walk_separator : "On other walking direction planners",
            google_transit : "Google Transit",
            google_bikes   : "Google Bike Directions",
            google_walk    : "Google Walking Directions",
            google_domain  : "http://www.google.pl"
        },

        // see otp.planner.Forms for use
        geocoder:
        {
//TODO
            working      : "Looking up address ....",
            error        : "Did not receive any results",
            msg_title    : "Voudrais vous review trip plan",
            msg_content  : "Les correct errors before planning your trip",
            select_result_title : "Please select a result",
            address_header : "Address"
        },

        error:
        {
            title        : 'B³ad planera podróży',
            deadMsg      : "Planer podróży nie odpowiada. Odczekaj kilka minut i spróbuj ponownie, lub spróbuj wersji tekstowej planera (zobacz link poniżej).",
            geoFromMsg   : "Wybierz lokalizacjê 'Z' dla Twojej podróży: ",
            geoToMsg     : "Wybierz lokalizacjê 'Do' dla Twojej podróży: "
        },
        
        // default messages from server if a message was not returned
        msgcodes:
        {
            200: "Plan OK",
            500: "B³¹d serwera",
            400: "Podróż poza obs³ugiwanym obszarem",
            404: "Trasa nieodnaleziona",
            406: "Brak czasów w rozk³adzie",
            408: "Limit czasu osi¹gniêty",
            413: "Niew³aœciwy parametr",
            440: "Geokod Z nieodnaleziony",
            450: "Geokod Do nieodnaleziony",
            460: "Geokody Z i Do nieodnalezione",
            409: "Zbyt blisko",
            340: "Geokod Z niejednoznaczny",
            350: "Geokod Do niejednoznaczny",
            360: "Geokody Z i Do niejednoznaczne"
        },

        options: 
        [
          ['TRANSFERS', 'Ma³o przesiadek'],
          ['QUICK',     'Najszybsza podróż'],
          ['SAFE',      'Najbezpieczniejsza podróż'],
          ['TRIANGLE',  'Custom trip...']  // TODO localize
        ],
    
        arriveDepart: 
        [
          ['false', 'Odjazd'], 
          ['true',  'Przyjazd']
        ],
    
        maxWalkDistance : 
        [
            ['200',   '200 m'],
            ['500',   '500 m'],
            ['1000',   '1 km'],
            ['1500',  '1,5 km'],
            ['5000',  '5 km'],
            ['10000',  '10 km']
        ],
    
        mode : 
        [
            ['TRANSIT,WALK', 'Transport publiczny'],
            ['BUSISH,TRAINISH,WALK', 'Autobus i tramwaj'],
            ['BUSISH,WALK', 'Tylko autobus'],
            ['TRAINISH,WALK', 'Tylko tramwaj'],
            ['WALK', 'Tylko spacer'],
            ['BICYCLE', 'Rower'],
            ['TRANSIT,BICYCLE', 'Transport publiczny i rower']
        ],

        wheelchair :
        [
            ['false', 'Niewymagane'],
            ['true', 'Wymagane']
        ]
    },

    CLASS_NAME : "otp.locale.Polish"
};
