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
otp.locale.Italian = {

    config :
    {
        metricsSystem : "international",
        rightClickMsg : "Clicca col pulsante destro per indicare il punto di partenza e di arrivo.",
        attribution   : {
            title   : "Termini di Licenza",
            content : "Informativa"
        }
    },

    contextMenu : 
    {
        fromHere         : "Partenza",
        toHere           : "Arrivo",
        intermediateHere : "Aggiungi punto intermedio",

        centerHere       : "Centra la mappa qui",
        zoomInHere       : "Zoom in",
        zoomOutHere      : "Zoom out",
        previous         : "Ultima posizione",
        next             : "Posizione successiva",

        analysisLocation : "Origine dell'analisi",

        minimize         : "Riduci a icona",
        bringToFront     : "Porta in primo piano",
        sendToBack       : "Porta in secondo piano"
    },

    widgets : 
    {
        managerMenu : 
        {
            minimizeAll     : "Minimizza tutto",
            unminimizeAll   : "Ripristina tutto"
        },

        ItinerariesWidget : 
        {
            title               : "Percorsi",
            itinerariesLength   : "Percorsi trovati",
            linkToSearch        : "Link alla ricerca",
            buttons             :
            {
                first           : "Primo",
                previous        : "Precedente",
                next            : "Successivo",
                last            : "Ultimo"
            },
            realtimeDelay       :
            {
                late            : "min ritardo",
                early           : "min anticipo", 
                onTime          : "in orario"
            },
            tooMuchWalking		: "Attenzione, la distanza a piedi è considerevole!"
        },

        AnalystLegend :
        {
            title               : "Legenda: tempo di viaggio in minuti"
        },

        MultimodalPlannerModule : 
        {
            title               : "Opzioni di viaggio"
        },

        TripOptionsWidget       :
        {
            title               : "Opzioni di viaggio",
            use                 : "Usa: ",
            ownBike             : "Bici propria",
            sharedBike          : "Bici [TO]Bike"
        },
        
        BikeStationsWidget       :
        {
            pickUp              : "Prendi la bici qui",
            dropOff             : "Lascia la bici qui",
            bikes               : "Bici&nbsp;  disponibili",
            spaces              : "Posti disponibili ",
            pick_up_bike		: "PRENDI LA BICI",
            alternate_pick_up	: "STAZIONE ALTERNATIVA",
            drop_off_bike		: "LASCIA LA BICI",
            alternate_drop_off	: "STAZIONE ALTERNATIVA",
            bike_station		: "<i>TO[BIKE]</i>",
            station				: "Stazione",
            bikes_available		: "Bici disponibili",
            docks_available		: "Posti disponibili",            
        },
        
        InfoWidgets:
        {
           about:{
               title: 'Il progetto',
               content: '<p>Bunet è il progetto di mobilità ciclabile promosso dal Comune di Torino e dalla Provincia di Torino</p>',
           },
           contact:{
               title: 'Contatti',
               content: '<p>Manda i tuoi commenti e suggerimenti a bunet@5t.torino.it</p>'
            },           
    	}
    },

    modules :
    {
        analyst : {
            AnalystModule : 
            {
                name        : "Analisi isocrone TPL",
                refresh     : "Aggiorna"
            }
        },

        multimodal : {
            MultimodalPlannerModule : 
            {
                name        : "Calcolo percorso multimodale"
            }
        }
       ,bikeshare : {
            BikeShareModule : 
            {
                name        : "Calcolo percorso ciclabile"
            }
        }
    },

    bikeTriangle : 
    {
        safeName : "Sicuro",
        safeSym  : "S",

        hillName : "Pianeggiante",
        hillSym  : "P",

        timeName : "Veloce",
        timeSym  : "V"
    },

    service : 
    {
        weekdays:  "Giorni feriali",
        saturday:  "Sabato",
        sunday:    "Domenica",
        schedule:  "Orari"
    },

    indicators : 
    {
        ok         : "OK",
        date       : "Data",
        loading    : "Caricamento",
        searching  : "Sto cercando...",
        qEmptyText : "Indirizzo, incrocio, id fermata..."
    },

    buttons: 
    {
        reverse       : "Ritorno",
        reverseTip    : "<b>Ritorno</b><br/>Calcola il ritorno invertendo origine e destinazione.",
        reverseMiniTip: "Calcola il ritorno",

        edit          : "Modifica",
        editTip       : "<b>Modifica </b><br/>Torna ai parametri del percorso.",

        clear         : "Clear",
        clearTip      : "<b>Clear</b><br/>Clear the map and all active tools.",

        fullScreen    : "Schermo intero",
        fullScreenTip : "<b>Schermo intero</b><br/>Show -or- hide tool panels",

        print         : "Stampa",
        printTip      : "<b>Stampa</b><br/>Versione stampabile del percorso (senza mappa)",

        link          : "Link",
        linkTip      : "<b>Link</b><br/>Mostra la url per questo calcolo percorso.",

        feedback      : "Feedback",
        feedbackTip   : "<b>Feedback</b><br/>Mandaci i tuoi commenti",

        submit       : "Invio",
        clearButton  : "Annulla",
        ok           : "OK",
        cancel       : "Annulla",
        yes          : "Si",
        no           : "NO",
        showDetails  : "&darr; Mostra dettagli &darr;",
        hideDetails  : "&uarr; Nascondi dettagli &uarr;"
    },

    // note: keep these lower case (and uppercase via template / code if needed)
    directions : 
    {
    	depart:			"partenza",
    	southeast:      "sud-est",
        southwest:      "sud-ovest",
        northeast:      "nord-est",
        northwest:      "nod-ovest",
        north:          "nord",
        west:           "ovest",
        south:          "sud",
        east:           "est",
        bound:          "bound",
        left:           "sinistra",
        right:          "destra",
        slightly_left:  "leggermente a sinistra",
        slightly_right: "leggermente a destra",
        hard_left:      "decisamente a sinistra",
        hard_right:     "decisamente a destra",
        'continue':     "continua",
        to_continue:    "continua su",
        becomes:        "diventa",
        at:             "a",
        on:             "su",
        to:             "su",
        on_to:			"su",
        via:            "via",
        uturn_left:	"svolta ad U e ",
        uturn_right:	"svolta ad U e",
        circle_counterclockwise: "Alla rotonda prendi ",
        circle_clockwise:        "Alla rotonda prendi ",
        // rather than just being a direction, this should be
        // full-fledged to take just the exit name at the end
        elevator: "prendi l'ascensore"
    },

    // see otp.planner.Templates for use
    instructions :
    {
        walk         : "Cammina",
        walk_toward  : "Cammina verso",
        walk_verb    : "Cammina",
        bike         : "In bicicletta",
        bike_toward  : "In bicicletta",
        bike_verb    : "In bicicletta",
        drive        : "In auto",
        drive_toward : "In auto",
        drive_verb   : "In auto",
        move         : "Procedi",
        move_toward  : "Procedi verso",

        transfer     : "Cambia",
        transfers    : "Cambi",

        continue_as  : "Continua come",
        stay_aboard  : "Rimani a bordo",

        depart       : "Da",
        arrive       : "A",
        now          : "Ora",

        presets_label: "Presets",

        preferredRoutes_label : "Linee preferita",
        edit         : "Modifica",
        none         : "Nessuna",
        weight       : "Peso",

        allRoutes    : "Tutte",

        save         : "Salva",
        close        : "Chiudi",

        start_at     : "Parti alle",
        end_at       : "Arriva alle",

        start        : "Da",
        end          : "A",

        geocoder     : "Geocoder",
        to			 : " fino a "
    },

    // see otp.planner.Templates for use
    labels : 
    {
        agency_msg   : "Servizio operato da",
        agency_msg_tt: "Apri il sito...",
        about        : "About",
        stop_id      : "Stop ID",
        trip_details : "Dettagli percorso",
        travel       : "Partenza",
        time		 : "Durata",
        totalWalk	 : "A piedi",
        totalBike	 : "In bici",
        transfers	 : "Cambi",
        valid        : "Valido",
        link		 : "Link al percorso",
        print		 : "Stampa",
        email		 : "Invia mail",
        email_subj	 : "Il tuo percorso",
        trip_length  : "Durata",
        with_a_walk  : "with a walk of",
        alert_for_rt : "Avviso per la linea",
        fare         : "Costo",
        regular_fare : "Tariffa ordinaria",
        student_fare : "Tariffa studenti",
        senior_fare  : "Tariffa anziani",
        fare_symbol  : "\u20ac",
        start		 : "Partenza",
        end			 : "Arrivo",
        at			 : " alle ",
        tripSummary  :"Riepilogo",
        view_itin_online:"Visualizza il percorso online",
    },

    // see otp.planner.Templates for use -- one output are the itinerary leg headers
    modes :
    {
        WALK:           "A piedi",
        BICYCLE:        "In bicicletta",
        CAR:            "In auto",
        TRAM:           "In tram",
        SUBWAY:         "In metro",
        RAIL:           "In treno",
        BUS:            "In bus",
        FERRY:          "In Ferry",
        CABLE_CAR:      "In tram",
        GONDOLA:        "In gondola",
        FUNICULAR:      "In funivia"
    },

    ordinal_exit:
    {
        1:  "la prima uscita",
        2:  "la seconda uscita",
        3:  "la terza uscita",
        4:  "la quarta uscita",
        5:  "la quinta uscita",
        6:  "la sesta uscita",
        7:  "la settima uscita",
        8:  "la ottava uscita",
        9:  "la nona uscita",
        10: "la decima uscita"
    },

    time:
    {
        hour_abbrev    : "ora",
        hours_abbrev   : "ore",
        hour           : "ora",
        hours          : "ore",

        minute         : "minuto",
        minutes        : "minuti",
        minute_abbrev  : "min",
        minutes_abbrev : "min",
        second_abbrev  : "sec",
        seconds_abbrev : "sec",
        format: "DD.MM.YYYY, h:mm",//"d.m.Y \\H:i"
        date_format: "DD/MM/YYYY",//"d.m.Y",
        time_format: "HH:mm",//"H:i",
        date_format_picker: "dd/mm/yy",
        time_format_picker : "hh:mmtt", //http://trentrichardson.com/examples/timepicker/#tp-formatting
        months         : ['Gen', 'Feb', 'Mar', 'Apr', 'Mag', 'Giu', 'Lug', 'Ago', 'Set', 'Ott', 'Nov', 'Dic']
    },

    systemmap :
    {
        labels :
        {
            panelTitle : "Mappa di sistema"
        }
    },

    tripPlanner :
    {
        // see otp/planner/*.js for where these values are used
        labels : 
        {
            panelTitle    : "Calcolo percorso",
            tabTitle      : "Calcola un percorso ",
            inputTitle    : "Dettagli percorso",
            optTitle      : "Preferenze",
            submitMsg     : "Elaborazione in corso...",
            optionalTitle : "",
            date          : "Data",
            time          : "Ora",
            when          : "Quando",
            from          : "Da",
            fromHere      : "Da qui",
            to            : "A",
            toHere        : "A qui",
            intermediate  : "Tappe intermedie",
            minimize      : "Show me the",
            maxWalkDistance: "Massima distanza a piedi",
            walkSpeed     : "Velocità a piedi",
            maxBikeDistance: "Massima distanza in bici",
            bikeSpeed     : "Velocità in bici",
            arriveDepart  : "Arrivo alle/Partenza alle",
            mode          : "Viaggia in",
            wheelchair    : "Percorso accessibile", 
            go            : "Vai",
            planTrip      : "Calcola",
            newTrip       : "Nuovo percorso",
            bannedRoutes  : "Linee da evitare",
            your_walk_route: "a piedi",
            your_bike_route: "in bici",
          	your_bikeshare_route: "in bici",
            with_		  : "con",
          	walk_to_the   : "a piedi fino alla ",
          	walk_from_the : 'a piedi dalla ',
          	to_your_dest  : ' alla destinazione',
          	dock		  : " stazione ",
          	
        },

        // see otp/config.js for where these values are used
        link : 
        {
            text           : "Link a questo calcolo",
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
            working      : "Ricerca indirizzo...",
            error        : "Nessun risultato",
            msg_title    : "Rivedi il calcolo",
            msg_content  : "Correggi gli errori",
            select_result_title : "Seleziona un risultato",
            address_header : "Indirizzo"
        },

        error:
        {
            title        : 'Errore sul calcolo percorso',
            deadMsg      : "Map Trip Planner is currently not responding. Please wait a few minutes to try again, or try the text trip planner (see link below).",
            geoFromMsg   : "Please select the 'From' location for your trip: ",
            geoToMsg     : "Please select the 'To' location for your trip: "
        },
        
        // default messages from server if a message was not returned ... 'Place' error messages also used when trying to submit without From & To coords.
        msgcodes:
        {
            200: "Percorso calcolato correttamente",
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
          ['TRANSFERS', 'Il più diretto'],
          ['QUICK',     'Il più veloce'],
          ['SAFE',      'Il più sicuro'],
          ['TRIANGLE',  'Personalizzato...']
        ],
    
        arriveDepart: 
        [
          ['false', 'Partenza'], 
          ['true',  'Arrivo']
        ],
    
        maxWalkDistance : [ [ '200', '200 m' ], [ '500', '500 m' ],
                            [ '1000', '1 km' ], [ '1500', '1,5 km' ], [ '5000', '5 km' ],
                            [ '10000', '10 km' ] ],
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
        
        modes : // leaflet client
        {
            "TRANSIT,WALK"      : "Trasporto pubblico", 
            "BUSISH,WALK"       : "Utilizza solo Bus", 
            "TRAINISH,WALK"     : "Utilizza solo Treno", 
            "BICYCLE"           : 'In bici',
            "WALK"              : 'A piedi',
            "TRANSIT,BICYCLE"   : "Bici &amp; trasporto pubblico",
            "CAR"               : 'In auto'
        },

        mode : // OL client
        [
            ['TRANSIT,WALK', 'Trasporto pubblico'],
            ['BUSISH,WALK', 'Utilizza solo Bus'],
            ['TRAINISH,WALK', 'Utilizza solo Treno'],
            ['WALK', 'A piedi'],
            ['BICYCLE', 'In bici'],
            ['TRANSIT,BICYCLE', 'Bici &amp; trasporto pubblico']
        ],

        // TODO: remove this hack, and provide code that allows the mode array to be configured with different transit modes.
        //       (note that we've been broken for awhile here, since many agencies don't have a 'Train' mode either...this needs attention)
        // IDEA: maybe we start with a big array (like below), and the pull out modes from this array when turning off various modes...
        with_bikeshare_mode : 
        [
            ['TRANSIT,WALK', 'Trasporto pubblico'],
            ['BUSISH,WALK', 'Utilizza solo Bus'],
            ['TRAINISH,WALK', 'Utilizza solo Treno'],
            ['WALK', 'A piedi'],
            ['BICYCLE', 'In bici'],
            ['WALK,BICYCLE', 'Bike sharing'],
            ['TRANSIT,BICYCLE', 'Bici &amp; trasporto pubblico'],
            ['TRANSIT,WALK,BICYCLE', 'Bike sharing &amp; trasporto pubblico']
        ],

        wheelchair :
        [
            ['false', 'Non richiesto'],
            ['true', 'Richiesto']
        ]
    },

    CLASS_NAME : "otp.locale.Italian"
};

