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
otp.locale.Spanish = {

    contextMenu : 
    {
        fromHere         : "Salir desde aquí",
        toHere           : "Llegar hasta aquí",

        centerHere       : "Centrar mapa aquí",
        zoomInHere       : "Acercar",
        zoomOutHere      : "Alejar",
        previous         : "Último encuadre",
        next             : "Siguiente encuadre"
    },

    service : 
    {
        weekdays:  "días de la semana",
        saturday:  "Sábado",
        sunday:    "Domingo",
        schedule:  "Horario"
    },

    indicators : 
    {
        ok         : "OK",
        date       : "Fecha",
        loading    : "Cargando",
        searching  : "Buscando...",
        qEmptyText : "Dirección, intersección,  punto de interés o Identificador de Parada..."
    },

    buttons: 
    {
        reverse       : "Cambiar",
        reverseTip    : "<b>Camibar orígen-destino</b><br/>Plan a return trip by reversing this trip's start and end points, and adjusting the time forward.",
        reverseMiniTip: "Cambiar orígen-destino",

        clear         : "Inicializar",
        clearTip      : "<b>Inicializar</b><br/>Inicializar el mapa y los botones activos.",

        fullScreen    : "Pantalla completa",
        fullScreenTip : "<b>Pantalla completa</b><br/>Mostrar - ocultar paneles",

        print         : "Imprimir",
        printTip      : "<b>Imprimir</b><br/>Imprimir este plan de viaje junto con las paradas.",

        link          : "Link",
        linkTip      : "<b>(translate me) Link</b><br/>Show link url for this trip plan.",

        feedback      : "Feedback",
        feedbackTip   : "<b>(translate me) Feedback</b><br/>Send your thoughts or experiences with the map",

        submit       : "Enviar",
        clearButton  : "Borrar",
        ok           : "OK",
        cancel       : "Cancelar",
        yes          : "Sí",
        no           : "No"
    },

    // note: keep these lower case (and uppercase via template / code if needed)
    directions : 
    {
        southEast:      "sureste",
        southWest:      "sudoeste",
        northEast:      "nordeste",
        northWest:      "noroeste",
        north:          "norte",
        west:           "oeste",
        south:          "sur",
        east:           "este",
        bound:          "límite",
        left:           "izquierda",
        right:          "derecha",
        slightly_left:  "un poco izquierda",
        slightly_right: "un poco derecha",
        hard_left:      "muy izquierda",
        hard_right:     "muy derecha",
        'continue':     "sigue derecho", 
        to_continue:    "para continuar en", 
        becomes:        "se hace", 
        at:             "en" 
    },

    time:
    {
        minute_abbrev:  "min",
        minutes_abbrev: "mins",
        second_abbrev: "sec",
        seconds_abbrev: "secs",
        months:         ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic']
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
        labels : 
        {
            panelTitle    : "Planificador de viajes Autobuses Interurbanos",
            tabTitle      : "Planificar un Viaje",
            inputTitle    : "Detalles del Viaje",
            optTitle      : "Preferencias (opcional)",
            submitMsg     : "Planificando su Viaje...",
            optionalTitle : "",
            date          : "Fecha",
            time          : "Hora",
            when          : "Tiempo",
            from          : "Desde",
            fromHere      : "Desde aquí",
            to            : "Hasta",
            toHere        : "Hasta aquí",
            minimize      : "Mostrar el",
            maxWalkDistance: "Máxima distancia hasta la parada",
            arriveDepart  : "Llegada/Salida a",
            mode          : "Modo de viaje",
            go            : "Empezar",
            planTrip      : "Planifique su Viaje",
            newTrip       : "Nuevo Viaje"
        },

        error:
        {
            title        : 'Trip Planner Error',
            deadMsg      : "Map Trip Planner is currently not responding. Please wait a few minutes to try again, or try the text trip planner (see link below).",
            geoFromMsg   : "Por favor, seleccione la posición de salida de su Viaje: ",
            geoToMsg     : "Por favor, seleccione la posición de llegada de su Viaje: "
        },
        
        // default messages from server if a message was not returned
        // TODO translate me
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
          ['TRANSFERS', 'Mínimo número de Transbordos'],
          ['SAFE', 'Viaje más seguro'],
          ['QUICK', 'Viaje más corto'] 
        ],
    
        arriveDepart: 
        [
          ['false', 'Salida'], 
          ['true',  'Llegada']
        ],
    
        maxWalkDistance : 
        [
            ['500',   '500 metros'],
            ['1000',   '1 Km'],
            ['5000',   '5 Km'],
            ['10000',  '10 Km'],
            ['20000',  '20 Km']
        ],
    
        mode : 
        [
            ['TRANSIT,WALK', 'Tránsito'],
            ['BUSISH,TRAINISH,WALK', 'Bus y Tren'],
            ['BUSISH,WALK', 'Solo Bus'],
            ['TRAINISH,WALK', 'Solo Tren'],
            ['WALK', 'Solo a pié'],
            ['BICYCLE', 'Bicicleta'],
            ['TRANSIT,BICYCLE', 'Tránsito y Bicicleta']
        ],

        wheelchair :
        [
            ['false', 'No se requiere un viaje con accesibilidad'],
            ['true', 'Se requiere un viaje adaptado a Silla de Ruedas']
        ]
    },

    CLASS_NAME : "otp.locale.Spanish"
};
