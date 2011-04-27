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
        rightClickMsg : "Utilizza il tasto destro sulla mappa per selezionare la partenza e l'arrivo del tuo viaggio",
        attribution   : {
            title   : "Licenza d'uso",
            content : "Termini e Condizioni"
        }
    },

    contextMenu : 
    {
        fromHere         : "Punto di partenza",
        toHere           : "Punto di arrivo",

        centerHere       : "Centra la mappa",
        zoomInHere       : "Zoom su questa zona",
        zoomOutHere      : "Zoom out da questa zona",
        previous         : "Posizione precedente sulla mappa",
        next             : "Posizione successiva sulla mappa"
    },

    service : 
    {
        weekdays:  "Feriali",
        saturday:  "Sabato",
        sunday:    "Domenica",
        schedule:  "Schedule"
    },

    indicators : 
    {
        ok         : "OK",
        date       : "Data",
        loading    : "In caricamento",
        searching  : "Ricerca in corso...",
        qEmptyText : "Indirizzo, incrocio, punto di riferimento o ID della fermata..."        
    },

    buttons: 
    {
        reverse       : "Inverti",
        reverseTip    : "<b>Direzione inversa</b><br/>Pianifica un viaggio di ritorno invertendo i punti di partenza e arrivo, correggendo i tempi",
        reverseMiniTip: "Direzione inversa",

        edit          : "Edita",
        editTip       : "<b>Edit trip</b><br/>Ritorna alla form principale di trip planner con i dettagli del viaggio pianificato.",

        clear         : "Svuota",
        clearTip      : "<b>Svuota</b><br/>Svuota la mappa e tutti gli strumenti attivi.",

        fullScreen    : "Schermo intero",
        fullScreenTip : "<b>Schermo intero</b><br/>Mostra o nascondi il pannello degli strumenti",

        print         : "Stampa",
        printTip      : "<b>Stampa</b><br/>Stampa una versione semplificata del percorso (senza mappa).",

        link          : "Link",
        linkTip      : "<b>Link</b><br/>Mostra il link per il percorso scelto.",

        feedback      : "Feedback",
        feedbackTip   : "<b>Feedback</b><br/>Manda i tuoi commenti sulle mappe",

        submit       : "Conferma",
        clearButton  : "Svuota",
        ok           : "OK",
        cancel       : "Annulla",
        yes          : "Si",
        no           : "No"
    },

    // note: keep these lower case (and uppercase via template / code if needed)
    directions : 
    {
        southEast:      "sud est",
        southWest:      "sud ovest",
        northEast:      "nord est",
        northWest:      "nord ovest",
        north:          "nord",
        west:           "ovest",
        south:          "sud",
        east:           "est",
        bound:          "limiti",
        left:           "sinistra",
        right:          "destra",
        slightly_left:  "tenere la sinistra",
        slightly_right: "tenere la destra",
        hard_left:      "girare a sinistra",
        hard_right:     "girare a destra",
        'continue':     "prosegui su",
        to_continue:    "continuare per",
        becomes:        "diventa",
        at:             "a",
        on:             "su",
        to:             "a",
        via:            "via",
        circle_counterclockwise: "prendere la rotatoria in senso anti-orario",
        circle_clockwise:        "prendere la rotatoria in senso orario"        
    },

    // see otp.planner.Templates for use
    instructions :
    {
        walk         : "a piedi",
        walk_toward  : "a piedi",
        walk_verb    : "a piedi",
        bike         : "Bicicletta",
        bike_toward  : "Bicicletta",
        bike_verb    : "Bicicletta",
        drive        : "Guida",
        drive_toward : "Guida",
        drive_verb   : "Guida",
        move         : "Procedi",
        move_toward  : "Procedi",

        transfer     : "Trasferisci",
        transfers    : "Trasferisci",

        continue_as  : "Avanza verso",
        stay_aboard  : "Resta a bordo",

        depart       : "Partenza",
        arrive       : "Arrivo",

        start_at     : "Inizio",
        end_at       : "Fine"
    },

    // see otp.planner.Templates for use
    labels : 
    {
        about        : "Informazioni",
        stop_id      : "ID Fermata",
        trip_details : "Dettagli viaggio",
        fare         : "Tariffa",
        fare_symbol  : "\u20ac",
        travel       : "Viaggio",
        valid        : "Valido",
        trip_length  : "Tempo",
        with_a_walk  : "A piedi",
        alert_for_rt : "Attenzione al percorso"
    },

    modes : 
    {
        WALK:           "A PIEDI",
        BICYCLE:        "BICICLETTA",
        CAR:            "AUTO",
        TRAM:           "TRAM",
        SUBWAY:         "METROPOLITANA",
        RAIL:           "FERROVIA",
        BUS:            "BUS",
        FERRY:          "TRAGHETO",
        CABLE_CAR:      "FUNIVIA",
        GONDOLA:        "GONDOLA",
        FUNICULAR:      "FUNICOLARE"
    },

    ordinal_exit:
    {
        1:  "prima uscita",
        2:  "seconda uscita",
        3:  "terza uscita",
        4:  "quarta uscita",
        5:  "quinta uscita",
        6:  "sesta uscita",
        7:  "settima uscita",
        8:  "ottava uscita",
        9:  "nona uscita",
        10: "decima uscita"
    },

    time:
    {
        minute         : "minuto",
        minutes        : "minuti",
        minute_abbrev  : "min",
        minutes_abbrev : "min",
        second_abbrev  : "sec",
        seconds_abbrev : "sec",
        format         : "D, j M H:i",
        months         : ['Gen', 'Feb', 'Mar', 'Apr', 'Mag', 'Giu', 'Lug', 'Aug', 'Set', 'Ott', 'Nov', 'Dic']
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
            panelTitle    : "Pianificazione del percorso",
            tabTitle      : "Pianifica percorso",
            inputTitle    : "Dettagli del percorso",
            optTitle      : "Preferenze per il percorso (opzionale)",
            submitMsg     : "Percorso in elaborazione...",
            optionalTitle : "",
            date          : "Data",
            time          : "Orario",
            when          : "Quando",
            from          : "Da",
            fromHere      : "Da qui",
            to            : "A",
            toHere        : "A qui",
            minimize      : "Mostrami",
            maxWalkDistance: "Massimo cammino a piedi",
            arriveDepart  : "Arriva entro/Parti a",
            mode          : "Passa per",
            wheelchair    : "Percorso accessibile ai portatori di sedia a rotelle", 
            go            : "Vai",
            planTrip      : "Pianifica il tuo percorso",
            newTrip       : "Nuovo percorso"
        },

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
            error        : "Nessun risultato trovato",
            msg_title    : "Pianificazione del viaggio",
            msg_content  : "Correggere gli errori prima di avviare la ricerca"
        },

        error:
        {
            title        : 'Errore Pianificazione Viaggio',
            deadMsg      : "Map Trip Planner non sta rispondendo. preghiamo di attendere qualche minuto e riprovare, oppure provare la funzionalit� testuale (vedi link sottostante).",
            geoFromMsg   : "Selezionare la partenza: ",
            geoToMsg     : "Selezionare l'arrivo: "
        },
        
        // default messages from server if a message was not returned
        msgcodes:
        {
            200: "Pianificazione corretta",
            500: "Server error",
            400: "Percorso fuori dall'area servita",
            404: "Percorso non trovato",
            406: "No transit times",
            408: "Richiesta scaduta",
            413: "Parametri non validi",
            440: "Geocode di partenza non trovato",
            450: "Geocode di arrivo non trovato",
            460: "Geocode di partenza e arrivo non trovati",
            409: "Troppo vicino",
            340: "Geocode di partenza ambiguo",
            350: "Geocode di arrivo ambiguo",
            360: "Geocode di partenza e di arrivo ambigui"
        },

        options: 
        [
          ['TRANSFERS', 'Minori cambi di mezzo'],
          ['QUICK',     'Percorso piu\' veloce'],
          ['SAFE',      'percorso piu\' sicuro']
        ],
    
        arriveDepart: 
        [
          ['false', 'Partenza'], 
          ['true',  'Arrivo']
        ],
    
        maxWalkDistance : 
        [
            ['500',   '500 metros'],
            ['1000',   '1 km'],
            ['5000',   '5 km'],
            ['10000',  '10 km'],
            ['20000',  '20 km']
        ],
    
        mode : 
        [
            ['TRANSIT,WALK', 'Passaggio a piedi'],
            ['BUSISH,TRAINISH,WALK', 'Bus & Treni'],
            ['BUSISH,WALK', 'Solo Bus'],
            ['TRAINISH,WALK', 'Solo treni'],
            ['WALK', 'Solo a piedi'],
            ['BICYCLE', 'Bici'],
            ['TRANSIT,BICYCLE', 'Passaggio & Bici']
        ],

        wheelchair :
        [
            ['false', 'Non richiesto'],
            ['true', 'Richiesto']
        ]
    },

    CLASS_NAME : "otp.locale.Italian"
};
