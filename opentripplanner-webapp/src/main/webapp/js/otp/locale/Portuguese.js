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

otp.locale.Portuguese = {

    config : 
    {
        metricsSystem : "international",
        rightClickMsg : "Clique com o botão direito em cima do mapa para escolher os pontos de origem e destino.",
        attribution   : {
            title   : "License Attribution",
            content : "Disclaimer goes here"  // TODO localize
        }
    },

    contextMenu : 
    {
        fromHere         : "Começar uma viagem daqui",
        toHere           : "Acabar uma viagem aqui",
        intermediateHere : "Adicionar um ponto intermediário",

        centerHere       : "Centralizar o mapa aqui",
        zoomInHere       : "Mais zoom",
        zoomOutHere      : "Menos zoom",
        previous         : "Última posição do mapa",
        next             : "Próxima posição do mapa"
    },

    // TODO Localize Me
    bikeTriangle : 
    {
        safeName : "Seguro para bicicletas",
        safeSym  : "S",

        hillName : "Sem Morros",
        hillSym  : "M",

        timeName : "Rápido",
        timeSym  : "R"
    },

    service : 
    {
        weekdays:  "Dias de semana",
        saturday:  "Sábado",
        sunday:    "Domingo",
        schedule:  "Horário"
    },

    indicators : 
    {
        ok         : "OK",
        date       : "Data",
        loading    : "Carregando",
        searching  : "Buscando...",
        qEmptyText : "Endereço, cruzamento, ponto de referência ou ID de Parada..."
    },

    buttons: 
    {
        reverse       : "Inverter",
        reverseTip    : "<b>Inverter direções</b><br/>Planejar uma viagem de volta invertendo a origem e o destino desse plano e ajustando o tempo pra frente.",
        reverseMiniTip: "Direções inversas",

        edit          : "Editar",
        editTip       : "<b>Editar viagem</b><br/>Volta ao original forma de planeajamento com os detalhes dessa viagem.",

        clear         : "Limpar mapa",
        clearTip      : "<b>Limpar mapa</b><br/>Limpar o mapa e todas as ferramentas ativas.",

        fullScreen    : "Tela Cheia",
        fullScreenTip : "<b>Tela Cheia</b><br/>Mostra -ou- oculta os paneis de ferramentas",

        print         : "Imprimir",
        printTip      : "<b>Imprimir</b><br/>Versão de impressão do plano de viagem (sem mapa).",

        link          : "Link",
        linkTip      : "<b>Link</b><br/>Mostra o endereço url para este plano de viagem",

        feedback      : "Feedback",
        feedbackTip   : "<b>Feedback</b><br/>Mande suas experiências ou problemas com o mapa",

        submit       : "Submeter",
        clearButton  : "Limpar",
        ok           : "OK",
        cancel       : "Cancelar",
        yes          : "Sim",
        no           : "Não",
        showDetails  : "Mostrar detalhes...",
        hideDetails  : "Ocultar detalhes..."
    },

    // note: keep these lower case (and uppercase via template / code if needed)
    directions : 
    {
        southeast:      "sudeste",
        southwest:      "sudoeste",
        northeast:      "nordeste",
        northwest:      "noroeste",
        north:          "norte",
        west:           "oeste",
        south:          "sul",
        east:           "este",
        bound:          "pelo",
        left:           "esquerda",
        right:          "direito",
        slightly_left:  "curva suave à esquerda",
        slightly_right: "curva suave à direita",
        hard_left:      "curva acentuada à esquerda",
        hard_right:     "curva acentuada à direita",
        'continue':     "continue",
        to_continue:    "continuar",
        becomes:        "torna-se",
        at:             "a(o)",
        on:             "no(a)",
        to:             "à",
        via:            "pelo(a)",
        circle_counterclockwise: "entre na rotatória em sentido anti-horário",
        circle_clockwise:        "entre na rotatória em sentido horário"
    },

    // see otp.planner.Templates for use
    instructions :
    {
        walk         : "Andar",
        walk_toward  : "Anda para",
        walk_verb    : "Anda",
        bike         : "Bicicleta",
        bike_toward  : "Bicicleta para",
        bike_verb    : "Anda de bicicleta",
        drive        : "Dirigir",
        drive_toward : "Dirige para",
        drive_verb   : "Dirige",
        move         : "Segue",
        move_toward  : "Segue em frente",

        transfer     : "transferir",
        transfers    : "transfere",

        continue_as  : "Prosegue como",
        stay_aboard  : "Fica-se a bordo",

        depart       : "Embarcar",
        arrive       : "Chegar",

        start_at     : "Começar a",
        end_at       : "Terminar a"
    },

    // see otp.planner.Templates for use
    labels : 
    {
        agency_msg   : "Operado por", // TODO
        agency_msg_tt: "Abre o site da agência numa nova página...", // TODO
        about        : "Sobre",
        stop_id      : "ID da parada",
        trip_details : "Detalhes da Viagem",
        fare         : "Tarifa",
        fare_symbol  : "\u20ac",

        // TODO  -- used in the Trip Details summary to describe different fares 
        regular_fare : "",
        student_fare : "",
        senior_fare  : "",

        travel       : "Viagem",
        valid        : "Válido(a)",
        trip_length  : "Tempo",
        with_a_walk  : "com um andamento de",
        alert_for_rt : "Aviso pra viagem"
    },

    // see otp.planner.Templates for use -- one output are the itinerary leg headers
    modes :
    {
        WALK:           "De pé",
        BICYCLE:        "Bicicleta",
        CAR:            "Carro",
        TRAM:           "Bonde",
        SUBWAY:         "Mêtro",
        RAIL:           "Trem",
        BUS:            "Ônibus",
        FERRY:          "Ferry",
        CABLE_CAR:      "Bonde",
        GONDOLA:        "Teleférico",
        FUNICULAR:      "Funicular"
    },

    ordinal_exit:
    {
        1:  "à primeira saída",
        2:  "pra segunda saída",
        3:  "pra terceira saída",
        4:  "pra quarta saída",
        5:  "pra quinta saída",
        6:  "pra saída sexta",
        7:  "pra saída séptimo",
        8:  "pra saída oitavo",
        9:  "pra saída nona",
        10: "pra saída décimo"
    },

    time:
    {
        // TODO
        hour_abbrev    : "hra",
        hours_abbrev   : "hras",
        hour           : "hora",
        hours          : "horas",

        minute         : "minuto",
        minutes        : "minutos",
        minute_abbrev  : "min",
        minutes_abbrev : "mins",
        second_abbrev  : "seg",
        seconds_abbrev : "segs",
        format         : "D, j M H:i",
        date_format    : "d-m-Y",
        time_format    : "H:i",
        months         : ['jan', 'fev', 'mar', 'abr', 'maio', 'jun', 'jul', 'ago', 'set', 'out', 'nov', 'dez']
    },

    systemmap :
    {
        labels :
        {
            panelTitle : "Mapa do Sistema"
        }
    },

    tripPlanner :
    {
        // see otp/planner/*.js for where these values are used
        labels : 
        {
            panelTitle    : "Planejador de Viagem",
            tabTitle      : "Planejar uma Viagem",
            inputTitle    : "Detalhes da Viagem",
            optTitle      : "Preferências da Viagem (opcional)",
            submitMsg     : "Planejeando sua viagem...",
            optionalTitle : "",
            date          : "Data",
            time          : "Tempo",
            when          : "Quando",
            from          : "Desde",
            fromHere      : "Daqui",
            intermediate  : "Lugar intermediario",
            to            : "À",
            toHere        : "Até aqui",
            minimize      : "Me mostra o(a)",
            maxWalkDistance: "Distância máxima andando",
            maxBikeDistance: "Distância máxima de bicicleta",
            arriveDepart  : "Chegar por/Embarcar à",
            mode          : "Viaga por",
            wheelchair    : "Accessível a cadeira de rodas", 
            go            : "Ir",
            planTrip      : "Planeja sua viagem",
            newTrip       : "Nova viagem"
        },

        // see otp/config.js for where these values are used
        link : 
        {
            text           : "Link para esta viagem (OTP)",
            trip_separator : "Esta viagem nos outros planejadores de viagem",
            bike_separator : "Nos outros planejadores de viagem por bicicleta",
            walk_separator : "Nos outros planejadores de viagem de pé",
            google_transit : "Google Transit",
            google_bikes   : "Google Bike Directions",
            google_walk    : "Google Walking Directions",
            google_domain  : "http://www.google.com"
        },

        // see otp.planner.Forms for use
        geocoder:
        {
            working      : "Buscando o endereço ....",
            error        : "Não há resultados",
            msg_title    : "Revisar plano da viagem",
            msg_content  : "Favor, corrija os erros antes de planejar sua viagem",
            select_result_title : "Favor, escolha um resultado",
            address_header : "Endereço"
        },

        error:
        {
            title        : 'Error de Planejador de Viagem',
            deadMsg      : "O Map Trip Planner não está respondendo no momento. Favor, espere uns minutos e tente de novo, ou usa o planejador de texto (veja o link em baixo).",
            geoFromMsg   : "Escolha a 'Origem' da sua viagem: ",
            geoToMsg     : "Escolha o 'Destino' da sua viagem: "
        },
        
        // default messages from server if a message was not returned
        msgcodes:
        {
            200: "Plano OK",
            500: "Erro do servidor",
            400: "Viagem fora dos limites",
            404: "Caminho não encontrado",
            406: "Sem tempos de trânsito disponíveis",
            408: "Solicitação expirou",
            413: "Parâmetro inválido",
            440: "Geocode do destino não encontrado",
            450: "Geocode do origem não encontrado",
            460: "Geocodes não encontrados (origem e destino)",
            470: "Não accesivel por cadeira de rodas",
            409: "Perto demais",
            340: "Ambíguo Geocode de origem",
            350: "Ambíguo Geocode de destino",
            360: "Amíguo Geocodes (origem e destino)"
        },

        options: 
        [
          ['TRANSFERS', 'Menos transferências'],
          ['QUICK',     'Viagem curta'],
          ['SAFE',      'Possível por bicicleta'],
          ['TRIANGLE',  'Viagem personalizada...']
        ],
    
        arriveDepart: 
        [
          ['false', 'Embarcar'], 
          ['true',  'Chegar']
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
            ['TRANSIT,WALK', 'Trânsito'],
// DO WE REALLY NEED THIS?  ISN'T BUS & TRAIN the same as TRANSIT, WALK
//          ['BUSISH,TRAINISH,WALK', 'Bus & Train'],
            ['BUSISH,WALK', 'Apenas ônibus'],
            ['TRAINISH,WALK', 'Apenas trem'],
            ['WALK', 'Apenas caminhando'],
            ['BICYCLE', 'Bicicleta'],
            ['TRANSIT,BICYCLE', 'Trânsito & Bicicleta']
        ],

        wheelchair :
        [
            ['false', 'Não exigido'],
            ['true', 'Exigido']
        ]
    },

    CLASS_NAME : "otp.locale.Portuguese"
};
