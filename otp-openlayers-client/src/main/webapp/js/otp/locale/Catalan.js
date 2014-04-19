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
//Ã¡:\xE1, Ã©:\xE9, Ã­:\xED Ã³:\xF3, Ãº:\xFA, Ã�:\xC1, Ã‰:\xC9, Ã�:\xCD, Ã“:\xD3, Ãš:\xDA, Ã±:\xF1, Ã‘:\xD1
otp.locale.Catalan = {

    config : 
    {
        metricsSystem : "international",
        rightClickMsg : "Feu clic amb el bot\xF3 dret sobre el mapa per triar els punts d'origen i dest\xED.",
        attribution   : {
            title   : "License Attribution",
            content : "Disclaimer goes here"  // TODO localize
        }
    },

    contextMenu : 
    {
        fromHere         : "Sortir des d'aqu\xED",
        toHere           : "Arribar fins aqu\xED",
        intermediateHere : "Afegir punt intermedi",

        centerHere       : "Centrar mapa aqu\xED",
        zoomInHere       : "Apropar",
        zoomOutHere      : "Allunyar",
        previous         : "\xDAltim enquadrament",
        next             : "Següent enquadrament"
    },

    // TODO Localize Me
    bikeTriangle : 
    {
        safeName : "Bike friendly",
        safeSym  : "B",

        hillName : "Flat",
        hillSym  : "F",

        timeName : "Quick",
        timeSym  : "Q"
    },

    service : 
    {
        weekdays:  "entre setmana",
        saturday:  "Dissabte",
        sunday:    "Diumenge",
        schedule:  "Horari"
    },

    indicators : 
    {
        ok         : "OK",
        date       : "Data",
        loading    : "Carregant",
        searching  : "Cercant...",
        qEmptyText : "Direcci\xF3, intersecci\xF3, punto d'inter\xE9s o identificador de parada..."
    },

    buttons: 
    {
        reverse       : "Canviar",
        reverseTip    : "<b>Canviar origen-dest\xED</b><br/>Planifica el viatge de tornada intercanviant origen i dest\xED, i ajustant l'hora de sortida.",
        reverseMiniTip: "Canviar origen-dest\xED",

        edit          : "Editar",
        editTip       : "<b>Editar el viatge</b><br/>Torna a la pantalla principal per canviar aspectes del viatge.",

        clear         : "Esborrar",
        clearTip      : "<b>Esborrar</b><br/>Esborra el mapa i totes les eines actives.",

        fullScreen    : "Pantalla completa",
        fullScreenTip : "<b>Pantalla completa</b><br/>Mostrar - ocultar panells",

        print         : "Imprimir",
        printTip      : "<b>Imprimir</b><br/>Imprimir aquest pla de viatge juntament amb les parades.",

        link          : "Enllaç",
        linkTip      : "<b>Enllaç</b><br/>Mostra distintas url per aquest viatge.",

        feedback      : "Feedback",
        feedbackTip   : "<b>Feedback</b><br/>Send your thoughts or experiences with the map",

        submit       : "Enviar",
        clearButton  : "Esborrar",
        ok           : "OK",
        cancel       : "Cancel·lar",
        yes          : "S\xED",
        no           : "No",
        showDetails  : "Mostrar detalls...",
        hideDetails  : "Amagar detalls..."
    },

    // note: keep these lower case (and uppercase via template / code if needed)
    directions : 
    {
        southeast:      "sud-est",
        southwest:      "sud-oest",
        northeast:      "nord-est",
        northwest:      "nord-oest",
        north:          "nord",
        west:           "oest",
        south:          "sud",
        east:           "est",
        bound:          "l\xEDmit",
        left:           "gira a la esquerra",
        right:          "gira a la dreta",
        slightly_left:  "gira lleugerament a la esquerra",
        slightly_right: "gira lleugerament a la dreta",
        hard_left:      "gira completament a la esquerra",
        hard_right:     "gira completament a la dreta",
        'continue':     "segueix recte per", 
        to_continue:    "per a continuar a", 
        becomes:        "esdevé", 
        at:             "a",
        on:             "en",
        to:             "fins",
        via:            "via",
        circle_counterclockwise: "take roundabout counterclockwise",
        circle_clockwise:        "take roundabout clockwise"
    },

    // see otp.planner.Templates for use
    instructions : 
    {
        walk         : "Caminar",
        walk_toward  : "Camina fins el",
        walk_verb    : "Caminant",
        bike         : "Bicicleta",
        bike_toward  : "Pedala fins el",
        bike_verb    : "En bicicleta",
        drive        : "Cotxe",
        drive_toward : "Avança fins el",
        drive_verb   : "Cotxe",
        move         : "Avança",
        move_toward  : "Avança fins el",

        transfer     : "transbord",
        transfers    : "transbords",

        continue_as  : "Continues as",
        stay_aboard  : "stay on board",

        depart       : "Sortida des de",
        arrive       : "Arribada a",

        start_at     : "Origen:",
        end_at       : "Dest\xED:"
    },

    // see otp.planner.Templates for use
    labels : 
    {
        agency_msg   : "Service run by", // TODO
        agency_msg_tt: "Open agency website in separate window...", // TODO
        about        : "Al voltant de ",
        stop_id      : "Stop ID",
        trip_details : "Detalls del viatge",
        fare         : "Tarifa",
        fare_symbol  : "\u20ac",

        // TODO  -- used in the Trip Details summary to describe different fares 
        regular_fare : "",
        student_fare : "",
        senior_fare  : "",

        travel       : "Hora de sortida",
        valid        : "Hora actual",
        trip_length  : "Temps",
        with_a_walk  : "with a walk of",
        alert_for_rt : "Alert for route"
    },

    // see otp.planner.Templates for use -- one output are the itinerary leg headers
    modes : 
    {
        WALK:           "A PEU",
        BICYCLE:        "BICICLETA",
        CAR:            "COTXE",
        TRAM:           "TRAMVIA",
        SUBWAY:         "METRO",
        RAIL:           "TREN",
        BUS:            "AUTOB\xDAS",
        FERRY:          "BOT",
        CABLE_CAR:      "PONT PENJANT",
        GONDOLA:        "GONDOLA",
        FUNICULAR:      "FUNICULAR"
    },

    ordinal_exit:
    {
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
        hour_abbrev    : "hora",
        hours_abbrev   : "hores",
        hour           : "hora",
        hours          : "hores",

        minute         : "minut",
        minutes        : "minuts",
        minute_abbrev  : "min",
        minutes_abbrev : "mins",
        second_abbrev  : "seg",
        seconds_abbrev : "segs",
        format         : "D, j M H:i",
        date_format    : "d-m-Y",
        time_format    : "H:i",
        months         : ['gen', 'feb', 'mar', 'abr', 'mai', 'jun', 'jul', 'ago', 'set', 'oct', 'nov', 'des']
    },

    systemmap :
    {
        labels :
        {
            panelTitle : "System Map"
        }
    },

    tripPlanner :
    {
        // see otp/planner/*.js for where these values are used
        labels : 
        {
            panelTitle    : "Planificador multimodal",
            tabTitle      : "Planificar un viatge",
            inputTitle    : "Detalls del viatge",
            optTitle      : "Preferències (opcional)",
            submitMsg     : "Planificant el vostre viatge...",
            optionalTitle : "",
            date          : "Data",
            time          : "Hora",
            when          : "Temps",
            from          : "Des de",
            fromHere      : "Des d'aqu\xED",
            to            : "Fins",
            toHere        : "Fins aqu\xED",
            intermediate  : "Intermediate Place",          // TODO
            minimize      : "Mostrar el",
            maxWalkDistance: "Màxima distància fins la parada",
            walkSpeed     : "velocitat de caminar",
            maxBikeDistance: "Màxima distància anb bicicleta",
            walkSpeed     : "velocitat de bicicleta",
            arriveDepart  : "Arribada/Sortida a",
            mode          : "Mode de viatge",
            wheelchair    : "Viatge amb accessibilitat",
            go            : "Començar",
            planTrip      : "Planificar el viatge",
            newTrip       : "Nou viatge"
        },

        // see otp/config.js for where these values are used
        link : 
        {
            text           : "Link a este viaje (OTP)",
            trip_separator : "Este viaje en otros planificadores intermodales",
            bike_separator : "En otros planificadores de bicicletas",
            walk_separator : "En otros planificadores pedestres",
//TODO
            google_transit : "Google Transit",
            google_bikes   : "Google Bike Directions",
            google_walk    : "Google Walking Directions",
            google_domain  : "http://www.google.es"
        },

        // see otp.planner.Forms for use
        geocoder:
        {
//TODO
            working      : "Looking up address ....",
            error        : "Did not receive any results",
            msg_title    : "Donde es review trip plan",
            msg_content  : "Donde es correct errors before planning your trip",
            select_result_title : "Please select a result",
            address_header : "Address"
        },

        error:
        {
            title        : 'Error del planificador',
            deadMsg      : "El planificador no responde. Por favor, int\xE9ntelo m\xE1s tarde",
            geoFromMsg   : "Por favor, elija la direcci\xF3n de salida del viaje: ",
            geoToMsg     : "Por favor, elija la direcci\xF3n de llegada del viaje: "
        },
        
        // default messages from server if a message was not returned
        msgcodes:
        {
            200: "Plan OK",
            500: "Server error",
            400: "Trip out of bounds",
            404: "Path not found",
            406: "No transit times",
            408: "Request timed out",
            413: "Invalid parameter",
            440: "From geocode not found",
            450: "To geocode not found",
            460: "Geocode from and to not found",
            409: "Too close",
            340: "Geocode from ambiguous",
            350: "Geocode to ambiguous",
            360: "Geocode from and to ambiguous"
        },

        options: 
        [
          ['TRANSFERS', 'M\xEDnim n\xFAmero de transbords'],
          ['QUICK',     'Viatge més curt'],
          ['SAFE',      'Viatge més segur'],
          ['TRIANGLE',  'Custom trip...'] // TODO localize
        ],
    
        arriveDepart: 
        [
          ['false', 'Sortida'], 
          ['true',  'Arribada']
        ],

        walkSpeed :
        [
            ['0.278',  '1 km/h'],
            ['0.556',  '2 km/h'],
            ['0.833',  '3 km/h'],
            ['1.111',  '4 km/h'],
            ['1.389',  '5 km/h'],
            ['1.667',  '6 km/h'],
            ['1.944',  '7 km/h'],
            ['2.222',  '8 km/h'],
            ['2.500',  '9 km/h'],
            ['2.778',  '10 km/h']
        ],

        maxWalkDistance : 
        [
            ['500',   '500 metres'],
            ['1000',   '1 km'],
            ['5000',   '5 km'],
            ['10000',  '10 km'],
            ['20000',  '20 km']
        ],
    
        mode : 
        [
            ['TRANSIT,WALK', 'Transport p\xFAblic'],
            ['BUSISH,TRAINISH,WALK', 'Bus i tren'],
            ['BUSISH,WALK', 'Només bus'],
            ['TRAINISH,WALK', 'Només tren'],
            ['WALK', 'Només a peu'],
            ['BICYCLE', 'Bicicleta'],
            ['TRANSIT,BICYCLE', 'Transport p\xFAblic i bicicleta'],
            ['CAR', 'Cotxe']
        ],

        wheelchair :
        [
            ['false', 'No es requereix un viatge amb accessibilitat'],
            ['true', 'S\xED es requereix un viatge amb accessibilitat']
        ]
    },

    CLASS_NAME : "otp.locale.Catalan"
};
