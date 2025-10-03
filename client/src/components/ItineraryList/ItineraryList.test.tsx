import { it } from 'vitest';
import { ItineraryListContainer } from './ItineraryListContainer';
import { render } from '@testing-library/react';
import { QueryType } from '../../gql/graphql.ts';

const tripQueryResult = {
  trip: {
    previousPageCursor:
      'MXxQUkVWSU9VU19QQUdFfDIwMjQtMDEtMTJUMTA6MzI6MThafHwxaHxTVFJFRVRfQU5EX0FSUklWQUxfVElNRXxmYWxzZXwyMDI0LTAxLTEyVDExOjQxOjAwWnwyMDI0LTAxLTEyVDEzOjE4OjUwWnwzfDExMTM0fA==',
    nextPageCursor:
      'MXxORVhUX1BBR0V8MjAyNC0wMS0xMlQxMTozOTowN1p8fDFofFNUUkVFVF9BTkRfQVJSSVZBTF9USU1FfGZhbHNlfDIwMjQtMDEtMTJUMTE6NDE6MDBafDIwMjQtMDEtMTJUMTM6MTg6NTBafDN8MTExMzR8',
    tripPatterns: [
      {
        aimedStartTime: '2024-01-12T12:39:07+01:00',
        aimedEndTime: '2024-01-12T13:54:09+01:00',
        expectedEndTime: '2024-01-12T13:54:09+01:00',
        expectedStartTime: '2024-01-12T12:39:07+01:00',
        duration: 4502,
        distance: 26616.219999999998,
        legs: [
          {
            id: null,
            mode: 'foot',
            aimedStartTime: '2024-01-12T12:39:07+01:00',
            aimedEndTime: '2024-01-12T12:50:00+01:00',
            expectedEndTime: '2024-01-12T12:50:00+01:00',
            expectedStartTime: '2024-01-12T12:39:07+01:00',
            realtime: false,
            distance: 727.21,
            duration: 653,
            fromPlace: {
              name: 'Origin',
            },
            toPlace: {
              name: 'IKEA Slependen',
            },
            toEstimatedCall: null,
            line: null,
            authority: null,
            pointsOnLink: {
              points:
                'o`nlJaac_AZ|@\\z@On@MAk@~@UZ@J@HEX@RLv@@DJVVd@LRNR\\^p@t@|@fA\\b@JTAHGVCJ@LBHDPP`@Tb@Xp@f@zAj@nBLd@Hj@@`ACz@@J?JDR@H?D?BAD?BEPAF?D?D@HBCNQDA@BBLDEHKd@a@BAD??O?M@KBKBGBIFGdAeA@?HIZ[??',
            },
          },
          {
            id: 'rO0ABXeSABhTQ0hFRFVMRURfVFJBTlNJVF9MRUdfVjMAAAAKMjAyNC0wMS0xMgAAAB0AAAAmABBSQjpOU1I6UXVheTo3OTQ0ABFSQjpOU1I6UXVheToxMTk2OQA7UkI6UlVUOkRhdGVkU2VydmljZUpvdXJuZXk6ODIyMzhhN2VmZmNhMzY2YzAwZmNkYmJjMWU2MWQ2N2E=',
            mode: 'bus',
            aimedStartTime: '2024-01-12T12:50:00+01:00',
            aimedEndTime: '2024-01-12T13:13:00+01:00',
            expectedEndTime: '2024-01-12T13:13:00+01:00',
            expectedStartTime: '2024-01-12T12:50:00+01:00',
            realtime: true,
            distance: 17846.09,
            duration: 1380,
            fromPlace: {
              name: 'IKEA Slependen',
            },
            toPlace: {
              name: 'Oslo bussterminal',
            },
            toEstimatedCall: {
              destinationDisplay: {
                frontText: 'Oslo bussterminal',
              },
            },
            line: {
              publicCode: '250',
              name: 'Sætre - Slemmestad - Oslo bussterminal',
            },
            authority: {
              name: 'Ruter',
            },
            pointsOnLink: {
              points:
                '{lmlJa|a_AFEJKHEFEH?FEFKBM@MASDSLe@@KFSHWFYH[F[Nw@JSHWFMDKDIDABABEL@F@FBDBHHDJFPBP?P@RARERCNGNGFIFI@K@KAw@Sg@o@c@k@m@q@o@m@aA_Aq@o@s@s@k@m@c@e@e@i@]c@]g@U_@S_@Q_@Ue@Uc@]y@[{@_@gA[gA_@qAa@{A[sAmAyEs@uCaAgDwAmEqC{G}DaIgGkKmBiDcAcBcBqCqFeJc@q@U_@mAwB{@wA}@cBO[mA_CcAqB[q@Ys@k@uAQa@GOYs@o@iBe@wAg@{AY_Ag@iBYeAYeAS_A]}A_@_BYyAWqAQeAQcAO}@{@_GaAeIgAiKmAwLaAgJ{@qGmA{H]kBc@_CaAgFYgBMeAIk@Io@Gs@Gy@Gy@Ey@Cw@Ey@A}@Cu@C_BCkDAiACgAAaAE_AC{@Ew@Gy@G{@MsACQUaCuB}SmAoLOyAcBuP}@eIk@mEmA}Iy@_Iu@wLKaBKmAK_AK_AM_AS}ASmASkAMm@ScAq@_DY}A[_BKs@My@Iq@QkAOqAKcAE_@KiAIiAIiAGcAOaCa@sHS_DIwAGsAImAG{@Gu@KgAIcAK}@I_AK{@WsBaAuGcA}GaBaLwBwN{@gGKmAAk@CeCAoACs@E}@MiASsASwA_@yC]}B_@aCEc@Im@SuAM_AKq@CQEWOmAUwBOeBc@qFO_CKkBa@gFo@gFiBsL}A_KoBwMg@iFMuA[}FCY??Iu@QaASe@AGo@oBq@iBg@qBe@yCw@wEm@}Dq@mE_@eCO_A[uBO{@QaAUcAk@}BSw@Uw@e@cB}@sCeB_G}@yCWy@]wAI[[gAK]SaAOs@McAMyA_AyMSiCKyA??OsBKwAG}@Ea@UsBWuBUqBm@qES{AoAsJS}ASmASmAUkA_@mBg@_CYsAYyAYkBQkAMkAKcAKeAWcCIcA??EYKiAYyCUaCUoCSoBUuBKeAQyAW}BYyBc@gDa@_D[wBeC}PSqAMaAIg@Iy@Gm@Ei@Ek@EaAEcAG_BGoAEm@Ec@I{@Ee@EYE_@Mu@Kk@Ki@Mm@Qk@I[Sq@aAsDSm@W{@W}@U{@Sy@Qy@Mq@Ii@Ee@Ge@C_@Ea@Ce@Ci@K_BGwAOkCA[AO?Q@U@U@E@E?G?G?GAEAEAO@K?MBk@JsBFsA???KB]Ba@Dq@Dq@D]D{@LaBr@yFLy@PcAReAVaAPa@Pe@P[n@gAh@}@`@s@Tc@Vm@DOHUZqALq@PgAJ}@D}@DeABeA?eA?iA?q@Ay@Cm@Ew@GqAM}AEo@Aa@Ac@@c@Bk@Fk@D]DYLg@LYJYLSRWd@c@~@gA`AgAPUNSPYP]\\u@Rk@Pe@Le@Pu@Nw@Hc@Jy@LgAHsAF{@H}BF_AFy@NqAPkAVqA^mAf@{AVm@Zm@fB_DpJcP\\k@Tc@d@aAh@}AV{@d@uBZqAXsAJm@Lq@Hm@LaANsAJkAP}BRwCr@iL^eGT_DNmCHsADgA@i@@i@@y@?s@HwADgA?yA?i@Ac@A[A[Ca@E]Ky@_@iCG]EQESKUu@aBGMM[EQGYKc@CSESAO@M?O?MCKCKEGEEAAC?E[EYCY?Y?q@Ac@D{A@o@????Be@BaA@_A?a@?u@Ae@AUAKASCSEa@Ge@G]Ic@GWI[Uq@a@gAcAmCCGGEECCAGOACGOKUQKY{@EIu@kBUq@Oc@KWCEKUMWO_@KWAGGMiA}CiAwCEMGOUo@KWWq@Sk@AA??EMIU]{@MYKOMOEIIKIGAK?GCGCGGIECM?GIEIGOWy@EKEKCGIKCCCCOQCCAA[a@QSQU_@c@QU[_@i@o@KMCEOQIKkAyAMQY]KMAAAAACKKCEcAmAUU?M?IAOGyGAg@AQHQDK`AuBx@gB@Eh@cAh@gA??`@y@JSHSHWFOHWJ_@Lg@Pw@Lu@H_@De@Dg@Ba@@s@?s@Eq@GuAEs@MsBGqAEm@E}@C}@AcBAgA?e@?]?Q?U@_@@k@HoANiAT}B@SHu@F]D[J_@H[j@uBBK@EDOFe@@EDYH[VaAJ]@CFODIFG`AwAZa@??@AHKLOFC?A@?DA@?FA@?B?H@??D@F@NBF?D?FADCFCHGl@m@\\[LOFGFEBEDCDE@APMFChC_ALCNEDDB@F?BADEBIBK@OAOJy@VwCBWL_BD[DEBI@I?IAKB]Fi@JsAD{@HiALoAJ_AFg@@G??LeBLyANkBBSBQHk@BEBG@I?I?G?IAGCIACCAECA?C?C@C@ADCDAHAH?H?H@HBF?F?NAL?RMRILEHABINATKhAIbAIbAO`BALE`@ABGt@KlAKvAAL',
            },
          },
          {
            id: null,
            mode: 'foot',
            aimedStartTime: '2024-01-12T13:13:00+01:00',
            aimedEndTime: '2024-01-12T13:16:42+01:00',
            expectedEndTime: '2024-01-12T13:16:42+01:00',
            expectedStartTime: '2024-01-12T13:13:00+01:00',
            realtime: false,
            distance: 213.19,
            duration: 222,
            fromPlace: {
              name: 'Oslo bussterminal',
            },
            toPlace: {
              name: 'Oslo bussterminal',
            },
            toEstimatedCall: null,
            line: null,
            authority: null,
            pointsOnLink: {
              points: 'sptlJm|s`A??LoAB[@EPqB@MBa@JD@@FQ??D@HDJsA@@B@DBB@DBDB@@Fu@LwAHwA??',
            },
          },
          {
            id: 'rO0ABXeTABhTQ0hFRFVMRURfVFJBTlNJVF9MRUdfVjMAAAAKMjAyNC0wMS0xMgAAAAAAAAANABFSQjpOU1I6UXVheToxMTk2MAARUkI6TlNSOlF1YXk6MTE0NDIAO1JCOlJVVDpEYXRlZFNlcnZpY2VKb3VybmV5OmY0M2E3ZDUxNmY5ZDljMzUyMDFlYzU2ZDk4NjM5M2Jh',
            mode: 'bus',
            aimedStartTime: '2024-01-12T13:20:00+01:00',
            aimedEndTime: '2024-01-12T13:39:00+01:00',
            expectedEndTime: '2024-01-12T13:39:00+01:00',
            expectedStartTime: '2024-01-12T13:20:00+01:00',
            realtime: false,
            distance: 6755.19,
            duration: 1140,
            fromPlace: {
              name: 'Oslo bussterminal',
            },
            toPlace: {
              name: 'Alfaset',
            },
            toEstimatedCall: {
              destinationDisplay: {
                frontText: 'Kjeller',
              },
            },
            line: {
              publicCode: '100',
              name: 'Kjeller - Oslo bussterminal',
            },
            authority: {
              name: 'Ruter',
            },
            pointsOnLink: {
              points:
                '}ktlJynt`ADi@BQBSBQHk@BEBG@I?I?G?IAG@QBQBKH_@Ji@BNBN?DDUBGJm@BM?ARy@d@{Bt@mDZ{ANo@H[@E`@oBt@mDFUBM?CJe@DSFU@KHc@?GDe@Nq@TgAj@mC@A@KBMFa@x@wE@E??F_@l@iDXcB`@eCV_BDYV_@BE??LUBM?OOaBBa@VuC`@}D@KZwCb@iEZgDFk@BUKa@CKGWWqAe@{BOu@EMIc@C_@G[YwAAGM[EQI]EQMm@ScAG[Mq@??OaAKo@Ik@Is@Io@MqAQ_BC]YeCs@}GEi@Gi@KeAEY??E[Iu@Gc@Ga@G]Ic@Kc@i@aCa@aBMk@Kc@[sAIa@Kc@S_A?C??I[C_@AKAa@?W@G@E?GHSBMFYN_@Rm@FUDUBOBQ@O@Q@O?MD]@g@?_@BG@I@KAIAICIEEEAE?EDADADO?I?E?C?E@C@CBEFCBCDSd@EHEFCDEDCBE@E@A@C?I?EAIEECs@q@Uw@WcAe@qBeD_OS{ASgAOiAEy@Ca@?U?O@I@G?IAKCICECCCAA?C?CBEIKUKc@COG[Ge@OkAEU??AIGg@M_AIc@K]GUMc@Uo@q@gBo@yA{B_G_BeEeCyGiBaFeAqC]_Ae@qAY_AMa@??IWSu@Mi@Ke@Mk@Oq@Ki@[iB[cBk@iDqAyHc@eCMs@??Ii@i@wCKi@Ke@COMk@Ou@YsAk@oCoAcGBUGk@Ce@Ao@Ao@AWA[AQC]CWIe@COOgAMo@OaAGWcA}FCMUgA??AESaASu@WaA[gAUu@]cAWy@W{@K]wAsEIYKWMa@Qo@K_@I[CIAMCKAQ@M?Q@Y@QA]KKEGEEEIEOM_@AOK_AKaAIw@Ek@??CSGgAEy@Cs@AaAAs@Ay@As@?q@@yA@gDBsD@wD@_C?oDByD?_@@iB@gB@i@@m@@[@Y?EHg@@U@A@E@E?EAEAEACAAA?A?IIECGCOBG?C?[@[BYB_@FSDy@J??C@OBSEE?KAI?AEACCEEACAC@A@KIGKEGUa@KYQ_@o@oAeAuBiAyBS[MW]o@Wc@[c@[c@i@q@EG??c@i@[_@[]s@w@GIIIYc@EIGKIMGMAAAKAGCEEEEAC@A@KIGEEGQOYk@]s@[w@Um@Sk@IYIUESIYEWGYEYEWE_@Ge@Gk@Ec@C]C_@C_@A]GcBC_@A[Ca@EWCWE]G[G]Qy@Qy@e@yBS_AMq@GSCOEQCMKe@S_AAC',
            },
          },
          {
            id: null,
            mode: 'foot',
            aimedStartTime: '2024-01-12T13:39:00+01:00',
            aimedEndTime: '2024-01-12T13:54:09+01:00',
            expectedEndTime: '2024-01-12T13:54:09+01:00',
            expectedStartTime: '2024-01-12T13:39:00+01:00',
            realtime: false,
            distance: 1074.54,
            duration: 909,
            fromPlace: {
              name: 'Alfaset',
            },
            toPlace: {
              name: 'Destination',
            },
            toEstimatedCall: null,
            line: null,
            authority: null,
            pointsOnLink: {
              points:
                'egxlJeigaA@A?BRz@DZA@ILEDA@}@aEAECIACCGAE?IUiA?CAEMaA_Br@KBI?GBGHKNW^w@jA]f@ORSRQLMHMBAbD@fA@`ADtADv@s@?ODm@l@SPGJENGNEPI\\GGC?C?ORGZHLCJ?BV\\BDBH@J@NANAJEJEJUf@IN@@DLcApBmA`CIPKREREX@XBXSm@}@iCaAmCgAaE',
            },
          },
        ],
        systemNotices: [],
      },
      {
        aimedStartTime: '2024-01-12T12:39:07+01:00',
        aimedEndTime: '2024-01-12T14:03:50+01:00',
        expectedEndTime: '2024-01-12T14:03:50+01:00',
        expectedStartTime: '2024-01-12T12:39:07+01:00',
        duration: 5083,
        distance: 30479.82,
        legs: [
          {
            id: null,
            mode: 'foot',
            aimedStartTime: '2024-01-12T12:39:07+01:00',
            aimedEndTime: '2024-01-12T12:50:00+01:00',
            expectedEndTime: '2024-01-12T12:50:00+01:00',
            expectedStartTime: '2024-01-12T12:39:07+01:00',
            realtime: false,
            distance: 727.21,
            duration: 653,
            fromPlace: {
              name: 'Origin',
            },
            toPlace: {
              name: 'IKEA Slependen',
            },
            toEstimatedCall: null,
            line: null,
            authority: null,
            pointsOnLink: {
              points:
                'o`nlJaac_AZ|@\\z@On@MAk@~@UZ@J@HEX@RLv@@DJVVd@LRNR\\^p@t@|@fA\\b@JTAHGVCJ@LBHDPP`@Tb@Xp@f@zAj@nBLd@Hj@@`ACz@@J?JDR@H?D?BAD?BEPAF?D?D@HBCNQDA@BBLDEHKd@a@BAD??O?M@KBKBGBIFGdAeA@?HIZ[??',
            },
          },
          {
            id: 'rO0ABXeSABhTQ0hFRFVMRURfVFJBTlNJVF9MRUdfVjMAAAAKMjAyNC0wMS0xMgAAAB0AAAAmABBSQjpOU1I6UXVheTo3OTQ0ABFSQjpOU1I6UXVheToxMTk2OQA7UkI6UlVUOkRhdGVkU2VydmljZUpvdXJuZXk6ODIyMzhhN2VmZmNhMzY2YzAwZmNkYmJjMWU2MWQ2N2E=',
            mode: 'bus',
            aimedStartTime: '2024-01-12T12:50:00+01:00',
            aimedEndTime: '2024-01-12T13:13:00+01:00',
            expectedEndTime: '2024-01-12T13:13:00+01:00',
            expectedStartTime: '2024-01-12T12:50:00+01:00',
            realtime: true,
            distance: 17846.09,
            duration: 1380,
            fromPlace: {
              name: 'IKEA Slependen',
            },
            toPlace: {
              name: 'Oslo bussterminal',
            },
            toEstimatedCall: {
              destinationDisplay: {
                frontText: 'Oslo bussterminal',
              },
            },
            line: {
              publicCode: '250',
              name: 'Sætre - Slemmestad - Oslo bussterminal',
            },
            authority: {
              name: 'Ruter',
            },
            pointsOnLink: {
              points:
                '{lmlJa|a_AFEJKHEFEH?FEFKBM@MASDSLe@@KFSHWFYH[F[Nw@JSHWFMDKDIDABABEL@F@FBDBHHDJFPBP?P@RARERCNGNGFIFI@K@KAw@Sg@o@c@k@m@q@o@m@aA_Aq@o@s@s@k@m@c@e@e@i@]c@]g@U_@S_@Q_@Ue@Uc@]y@[{@_@gA[gA_@qAa@{A[sAmAyEs@uCaAgDwAmEqC{G}DaIgGkKmBiDcAcBcBqCqFeJc@q@U_@mAwB{@wA}@cBO[mA_CcAqB[q@Ys@k@uAQa@GOYs@o@iBe@wAg@{AY_Ag@iBYeAYeAS_A]}A_@_BYyAWqAQeAQcAO}@{@_GaAeIgAiKmAwLaAgJ{@qGmA{H]kBc@_CaAgFYgBMeAIk@Io@Gs@Gy@Gy@Ey@Cw@Ey@A}@Cu@C_BCkDAiACgAAaAE_AC{@Ew@Gy@G{@MsACQUaCuB}SmAoLOyAcBuP}@eIk@mEmA}Iy@_Iu@wLKaBKmAK_AK_AM_AS}ASmASkAMm@ScAq@_DY}A[_BKs@My@Iq@QkAOqAKcAE_@KiAIiAIiAGcAOaCa@sHS_DIwAGsAImAG{@Gu@KgAIcAK}@I_AK{@WsBaAuGcA}GaBaLwBwN{@gGKmAAk@CeCAoACs@E}@MiASsASwA_@yC]}B_@aCEc@Im@SuAM_AKq@CQEWOmAUwBOeBc@qFO_CKkBa@gFo@gFiBsL}A_KoBwMg@iFMuA[}FCY??Iu@QaASe@AGo@oBq@iBg@qBe@yCw@wEm@}Dq@mE_@eCO_A[uBO{@QaAUcAk@}BSw@Uw@e@cB}@sCeB_G}@yCWy@]wAI[[gAK]SaAOs@McAMyA_AyMSiCKyA??OsBKwAG}@Ea@UsBWuBUqBm@qES{AoAsJS}ASmASmAUkA_@mBg@_CYsAYyAYkBQkAMkAKcAKeAWcCIcA??EYKiAYyCUaCUoCSoBUuBKeAQyAW}BYyBc@gDa@_D[wBeC}PSqAMaAIg@Iy@Gm@Ei@Ek@EaAEcAG_BGoAEm@Ec@I{@Ee@EYE_@Mu@Kk@Ki@Mm@Qk@I[Sq@aAsDSm@W{@W}@U{@Sy@Qy@Mq@Ii@Ee@Ge@C_@Ea@Ce@Ci@K_BGwAOkCA[AO?Q@U@U@E@E?G?G?GAEAEAO@K?MBk@JsBFsA???KB]Ba@Dq@Dq@D]D{@LaBr@yFLy@PcAReAVaAPa@Pe@P[n@gAh@}@`@s@Tc@Vm@DOHUZqALq@PgAJ}@D}@DeABeA?eA?iA?q@Ay@Cm@Ew@GqAM}AEo@Aa@Ac@@c@Bk@Fk@D]DYLg@LYJYLSRWd@c@~@gA`AgAPUNSPYP]\\u@Rk@Pe@Le@Pu@Nw@Hc@Jy@LgAHsAF{@H}BF_AFy@NqAPkAVqA^mAf@{AVm@Zm@fB_DpJcP\\k@Tc@d@aAh@}AV{@d@uBZqAXsAJm@Lq@Hm@LaANsAJkAP}BRwCr@iL^eGT_DNmCHsADgA@i@@i@@y@?s@HwADgA?yA?i@Ac@A[A[Ca@E]Ky@_@iCG]EQESKUu@aBGMM[EQGYKc@CSESAO@M?O?MCKCKEGEEAAC?E[EYCY?Y?q@Ac@D{A@o@????Be@BaA@_A?a@?u@Ae@AUAKASCSEa@Ge@G]Ic@GWI[Uq@a@gAcAmCCGGEECCAGOACGOKUQKY{@EIu@kBUq@Oc@KWCEKUMWO_@KWAGGMiA}CiAwCEMGOUo@KWWq@Sk@AA??EMIU]{@MYKOMOEIIKIGAK?GCGCGGIECM?GIEIGOWy@EKEKCGIKCCCCOQCCAA[a@QSQU_@c@QU[_@i@o@KMCEOQIKkAyAMQY]KMAAAAACKKCEcAmAUU?M?IAOGyGAg@AQHQDK`AuBx@gB@Eh@cAh@gA??`@y@JSHSHWFOHWJ_@Lg@Pw@Lu@H_@De@Dg@Ba@@s@?s@Eq@GuAEs@MsBGqAEm@E}@C}@AcBAgA?e@?]?Q?U@_@@k@HoANiAT}B@SHu@F]D[J_@H[j@uBBK@EDOFe@@EDYH[VaAJ]@CFODIFG`AwAZa@??@AHKLOFC?A@?DA@?FA@?B?H@??D@F@NBF?D?FADCFCHGl@m@\\[LOFGFEBEDCDE@APMFChC_ALCNEDDB@F?BADEBIBK@OAOJy@VwCBWL_BD[DEBI@I?IAKB]Fi@JsAD{@HiALoAJ_AFg@@G??LeBLyANkBBSBQHk@BEBG@I?I?G?IAGCIACCAECA?C?C@C@ADCDAHAH?H?H@HBF?F?NAL?RMRILEHABINATKhAIbAIbAO`BALE`@ABGt@KlAKvAAL',
            },
          },
          {
            id: null,
            mode: 'foot',
            aimedStartTime: '2024-01-12T13:13:00+01:00',
            aimedEndTime: '2024-01-12T13:13:39+01:00',
            expectedEndTime: '2024-01-12T13:13:39+01:00',
            expectedStartTime: '2024-01-12T13:13:00+01:00',
            realtime: false,
            distance: 30.41,
            duration: 39,
            fromPlace: {
              name: 'Oslo bussterminal',
            },
            toPlace: {
              name: 'Oslo bussterminal',
            },
            toEstimatedCall: null,
            line: null,
            authority: null,
            pointsOnLink: {
              points: 'sptlJm|s`A@@@SAAGCGC?ACACACA@W@K',
            },
          },
          {
            id: 'rO0ABXeTABhTQ0hFRFVMRURfVFJBTlNJVF9MRUdfVjMAAAAKMjAyNC0wMS0xMgAAAAAAAAANABFSQjpOU1I6UXVheToxMTk3OQARUkI6TlNSOlF1YXk6MTA2NDMAO1JCOlJVVDpEYXRlZFNlcnZpY2VKb3VybmV5OjY3YzVkMTllZmMyOWI1MDdmMzkxYmYzY2Q3NDRiZDY1',
            mode: 'bus',
            aimedStartTime: '2024-01-12T13:25:00+01:00',
            aimedEndTime: '2024-01-12T13:44:00+01:00',
            expectedEndTime: '2024-01-12T13:44:00+01:00',
            expectedStartTime: '2024-01-12T13:25:00+01:00',
            realtime: false,
            distance: 8619.48,
            duration: 1140,
            fromPlace: {
              name: 'Oslo bussterminal',
            },
            toPlace: {
              name: 'Rødtvet T',
            },
            toEstimatedCall: {
              destinationDisplay: {
                frontText: 'Hellerudhaugen',
              },
            },
            line: {
              publicCode: '390',
              name: 'Kongskog/Hellerudhaugen - Oslo bussterminal',
            },
            authority: {
              name: 'Ruter',
            },
            pointsOnLink: {
              points:
                'qqtlJu~s`A??Be@AKIMCWJwA@UBUBW@SBWBW@WBU@UBUBW@WBU@I@M@UBUBW@UBUDE^W@AZUNDH@D?@?D?FAB?BABBB@B?@?BABCBEBG@I?I?G?IAGCIACCAECA?C?C@C@ADGAE?MEc@UEAGAIBIFGBC@EDEDEHCLENCLCTYpDSfCId@ENELELGJEBCFKAI?KCGCIEKIYUWSmBaB_BuA]YYYSSY[QSGGOQEE??EEKMACEIEIKSAKCIEGECEAGKCGGKGQI]K[W_AyAiFGUQo@_@wAc@cBKUIUEOEOE_@I]Su@Wo@GSCMCOEW]sAQ_@CICMIY_AjAI@G?EACAECIGW_@cAiBqCeF}C}FOe@EIGIIOGIEGKOSO{BoCKWGKSYMKGEKISK{@gAGIGO??KSIOGKIOOUGK]]IOyAwC_@s@mAcCMc@GQEI_AgBGKCGIQEIAIAOAOAK?IAC?I?Wc@G{AKyBQIAWAOCYIIAiAIMCCAEC@]AKAGACACG?YCC?C@ABADAB?BIKIGKKc@[MK??m@c@[UIIQKYUeAq@eAm@YOYQYOIEKCEAICKAMAyEe@[EgAKm@G??[C_@EoAKg@Cm@CoAK_@Ce@E]CWCWEMAIAGCAEAIAIAIEIEIEEEEGAE@G@EBCBCFQEg@UiAo@gAk@}@q@SQa@a@UUm@y@_@q@g@aAEGo@yBs@{BIS??IS_@iAyAkEw@}BG[Ie@?OAQCOEUIQIOKGGCI?A?S[OYK[m@iBWu@??Ma@_@oAY}@q@sBm@cDSoAUsAWaBQiAYqBYsBK{BQ{AUcCCu@C}@Gs@MqAUeBYqCQgBSkBGk@??AIGm@QkAGa@a@qB{@{IyBeTUaCMyAY_DIiDCaBI}CIyCCyA?k@?_@@c@Fo@@C@E?E?I?ICGAEECAAC?C?EEGKK[I_@OiAKw@OiA??SeBs@{DqAgKgAiIk@qEKq@??Ge@MiAI{@GcAE}@CaAA{@AcA@cAB}@BmAHgAJkANsAJu@N_AVgA~@mDXgAR{@Lo@PmALkAHiAFeABoA@oA?yAAoAAo@C_@??A]KoBK}AMaBWyBi@aFIy@Iw@IcAG{AGuACcBByB?}B?cA??AcAEyCGoBI_BCi@AQIw@SeBQkAO{@SeA[sAu@{FeB}Mm@mEUmBGc@??c@gD_@sCQcAMs@WkAQu@Sy@y@sCcAwDcCaJi@kBUu@Sq@Wm@Wi@We@U]_@e@[]a@g@Y]W]SWUa@S_@Q]Qe@Wq@So@Qo@Og@Ou@Os@O_AMs@K{@KaAIu@IaAGkAEy@C}@GqA',
            },
          },
          {
            id: null,
            mode: 'foot',
            aimedStartTime: '2024-01-12T13:44:00+01:00',
            aimedEndTime: '2024-01-12T13:49:09+01:00',
            expectedEndTime: '2024-01-12T13:49:09+01:00',
            expectedStartTime: '2024-01-12T13:44:00+01:00',
            realtime: false,
            distance: 367.92,
            duration: 309,
            fromPlace: {
              name: 'Rødtvet T',
            },
            toPlace: {
              name: 'Kalbakkstubben',
            },
            toEstimatedCall: null,
            line: null,
            authority: null,
            pointsOnLink: {
              points: 'cd|lJ}ghaA??DWGw@Cm@Co@MsDAs@?m@AcA?e@Ci@Cs@E}@GaAGo@Ei@K{@SgBAQ@IBIDKFEFAJ?D?N@J@d@F??',
            },
          },
          {
            id: 'rO0ABXeTABhTQ0hFRFVMRURfVFJBTlNJVF9MRUdfVjMAAAAKMjAyNC0wMS0xMgAAAAMAAAAIABFSQjpOU1I6UXVheToxMDY2OAARUkI6TlNSOlF1YXk6MTAyNTkAO1JCOlJVVDpEYXRlZFNlcnZpY2VKb3VybmV5OmQ5OTFhMTk1NGQzYjkyNjRhMTk5ZGY1OTA5YTBlMmE5',
            mode: 'bus',
            aimedStartTime: '2024-01-12T13:55:00+01:00',
            aimedEndTime: '2024-01-12T13:59:00+01:00',
            expectedEndTime: '2024-01-12T13:59:00+01:00',
            expectedStartTime: '2024-01-12T13:55:00+01:00',
            realtime: false,
            distance: 2578.69,
            duration: 240,
            fromPlace: {
              name: 'Kalbakkstubben',
            },
            toPlace: {
              name: 'Postnord',
            },
            toEstimatedCall: {
              destinationDisplay: {
                frontText: 'Helsfyr',
              },
            },
            line: {
              publicCode: '68',
              name: 'Helsfyr T - Grorud T via Alfaset',
            },
            authority: {
              name: 'Ruter',
            },
            pointsOnLink: {
              points:
                'sc|lJijiaA@?HBPNHBHBDBFBJBDBFBXDHFf@b@JLHHNPTXRZXd@`@r@^l@`@r@T`@PXPVTXNRVTZTVPTLZLXJVFVDVDz@JNBx@H^DRHH@l@B??J@X@RBH?LBHBT@`@TVN\\VVRXVTTLNNPX`@\\f@T`@Zn@Rd@lCnGR`@P^PZR\\PVJLNPNN??B@LLFDNF\\JXRFBFBFDDHDFLR?FBD@DBDD@DABC@C`@[TIH@ZErAL|@Jp@FlAHh@BH@RJHFHHJLDJBFFFD@FADCDEDKBKHIHGHCNMF?PARANANAPCd@Ib@Gh@K^Il@@PCjA[TG??`A[JALCH?RBBDBBBVBT@HBPDb@?TBbABlBFvBBf@Fr@Dh@Ht@Jr@Np@Lj@Nd@N`@Rb@Td@PZh@x@NP~@nAzAjBHLVZRVNVI\\Sd@Q^GN??KTcApBmA`CIPKREREX@XBXDPdAxCJ\\l@bB',
            },
          },
          {
            id: null,
            mode: 'foot',
            aimedStartTime: '2024-01-12T13:59:00+01:00',
            aimedEndTime: '2024-01-12T14:03:50+01:00',
            expectedEndTime: '2024-01-12T14:03:50+01:00',
            expectedStartTime: '2024-01-12T13:59:00+01:00',
            realtime: false,
            distance: 310.02,
            duration: 290,
            fromPlace: {
              name: 'Postnord',
            },
            toPlace: {
              name: 'Destination',
            },
            toEstimatedCall: null,
            line: null,
            authority: null,
            pointsOnLink: {
              points: 'q|xlJmweaACFA?m@aBBEDKK]eAyCEQSm@}@iCaAmCgAaE',
            },
          },
        ],
        systemNotices: [],
      },
      {
        aimedStartTime: '2024-01-12T12:54:07+01:00',
        aimedEndTime: '2024-01-12T14:03:50+01:00',
        expectedEndTime: '2024-01-12T14:03:50+01:00',
        expectedStartTime: '2024-01-12T12:54:07+01:00',
        duration: 4183,
        distance: 27503.719999999998,
        legs: [
          {
            id: null,
            mode: 'foot',
            aimedStartTime: '2024-01-12T12:54:07+01:00',
            aimedEndTime: '2024-01-12T13:05:00+01:00',
            expectedEndTime: '2024-01-12T13:05:00+01:00',
            expectedStartTime: '2024-01-12T12:54:07+01:00',
            realtime: false,
            distance: 727.21,
            duration: 653,
            fromPlace: {
              name: 'Origin',
            },
            toPlace: {
              name: 'IKEA Slependen',
            },
            toEstimatedCall: null,
            line: null,
            authority: null,
            pointsOnLink: {
              points:
                'o`nlJaac_AZ|@\\z@On@MAk@~@UZ@J@HEX@RLv@@DJVVd@LRNR\\^p@t@|@fA\\b@JTAHGVCJ@LBHDPP`@Tb@Xp@f@zAj@nBLd@Hj@@`ACz@@J?JDR@H?D?BAD?BEPAF?D?D@HBCNQDA@BBLDEHKd@a@BAD??O?M@KBKBGBIFGdAeA@?HIZ[??',
            },
          },
          {
            id: 'rO0ABXeRABhTQ0hFRFVMRURfVFJBTlNJVF9MRUdfVjMAAAAKMjAyNC0wMS0xMgAAADUAAAA7ABBSQjpOU1I6UXVheTo3OTQ0ABBSQjpOU1I6UXVheTo3MzczADtSQjpSVVQ6RGF0ZWRTZXJ2aWNlSm91cm5leTplNjBjM2M1ODE2ZjUxNGExZjk2NTdiZjEwYmQ3ZmJhNw==',
            mode: 'bus',
            aimedStartTime: '2024-01-12T13:05:00+01:00',
            aimedEndTime: '2024-01-12T13:21:00+01:00',
            expectedEndTime: '2024-01-12T13:21:00+01:00',
            expectedStartTime: '2024-01-12T13:05:00+01:00',
            realtime: true,
            distance: 15329.9,
            duration: 960,
            fromPlace: {
              name: 'IKEA Slependen',
            },
            toPlace: {
              name: 'Nationaltheatret',
            },
            toEstimatedCall: {
              destinationDisplay: {
                frontText: 'Oslo bussterminal',
              },
            },
            line: {
              publicCode: '250',
              name: 'Sætre - Slemmestad - Oslo bussterminal',
            },
            authority: {
              name: 'Ruter',
            },
            pointsOnLink: {
              points:
                '{lmlJa|a_AFEJKHEFEH?FEFKBM@MASDSLe@@KFSHWFYH[F[Nw@JSHWFMDKDIDABABEL@F@FBDBHHDJFPBP?P@RARERCNGNGFIFI@K@KAw@Sg@o@c@k@m@q@o@m@aA_Aq@o@s@s@k@m@c@e@e@i@]c@]g@U_@S_@Q_@Ue@Uc@]y@[{@_@gA[gA_@qAa@{A[sAmAyEs@uCaAgDwAmEqC{G}DaIgGkKmBiDcAcBcBqCqFeJc@q@U_@mAwB{@wA}@cBO[mA_CcAqB[q@Ys@k@uAQa@GOYs@o@iBe@wAg@{AY_Ag@iBYeAYeAS_A]}A_@_BYyAWqAQeAQcAO}@{@_GaAeIgAiKmAwLaAgJ{@qGmA{H]kBc@_CaAgFYgBMeAIk@Io@Gs@Gy@Gy@Ey@Cw@Ey@A}@Cu@C_BCkDAiACgAAaAE_AC{@Ew@Gy@G{@MsACQUaCuB}SmAoLOyAcBuP}@eIk@mEmA}Iy@_Iu@wLKaBKmAK_AK_AM_AS}ASmASkAMm@ScAq@_DY}A[_BKs@My@Iq@QkAOqAKcAE_@KiAIiAIiAGcAOaCa@sHS_DIwAGsAImAG{@Gu@KgAIcAK}@I_AK{@WsBaAuGcA}GaBaLwBwN{@gGKmAAk@CeCAoACs@E}@MiASsASwA_@yC]}B_@aCEc@Im@SuAM_AKq@CQEWOmAUwBOeBc@qFO_CKkBa@gFo@gFiBsL}A_KoBwMg@iFMuA[}FCY??Iu@QaASe@AGo@oBq@iBg@qBe@yCw@wEm@}Dq@mE_@eCO_A[uBO{@QaAUcAk@}BSw@Uw@e@cB}@sCeB_G}@yCWy@]wAI[[gAK]SaAOs@McAMyA_AyMSiCKyA??OsBKwAG}@Ea@UsBWuBUqBm@qES{AoAsJS}ASmASmAUkA_@mBg@_CYsAYyAYkBQkAMkAKcAKeAWcCIcA??EYKiAYyCUaCUoCSoBUuBKeAQyAW}BYyBc@gDa@_D[wBeC}PSqAMaAIg@Iy@Gm@Ei@Ek@EaAEcAG_BGoAEm@Ec@I{@Ee@EYE_@Mu@Kk@Ki@Mm@Qk@I[Sq@aAsDSm@W{@W}@U{@Sy@Qy@Mq@Ii@Ee@Ge@C_@Ea@Ce@Ci@K_BGwAOkCA[AO?Q@U@U@E@E?G?G?GAEAEAO@K?MBk@JsBFsA???KB]Ba@Dq@Dq@D]D{@LaBr@yFLy@PcAReAVaAPa@Pe@P[n@gAh@}@`@s@Tc@Vm@DOHUZqALq@PgAJ}@D}@DeABeA?eA?iA?q@Ay@Cm@Ew@GqAM}AEo@Aa@Ac@@c@Bk@Fk@D]DYLg@LYJYLSRWd@c@~@gA`AgAPUNSPYP]\\u@Rk@Pe@Le@Pu@Nw@Hc@Jy@LgAHsAF{@H}BF_AFy@NqAPkAVqA^mAf@{AVm@Zm@fB_DpJcP\\k@Tc@d@aAh@}AV{@d@uBZqAXsAJm@Lq@Hm@LaANsAJkAP}BRwCr@iL^eGT_DNmCHsADgA@i@@i@@y@?s@HwADgA?yA?i@Ac@A[A[Ca@E]Ky@_@iCG]EQESKUu@aBGMM[EQGYKc@CSESAO@M?O?MCKCKEGEEAAC?E[EYCY?Y?q@Ac@D{A@o@????Be@BaA@_A?a@?u@Ae@AUAKASCSEa@Ge@G]Ic@GWI[Uq@a@gAcAmCCGGEECCAGOACGOKUQKY{@EIu@kBUq@Oc@KWCEKUMWO_@KWAGGMiA}CiAwCEMGOUo@KWWq@Sk@AA',
            },
          },
          {
            id: null,
            mode: 'foot',
            aimedStartTime: '2024-01-12T13:21:00+01:00',
            aimedEndTime: '2024-01-12T13:24:57+01:00',
            expectedEndTime: '2024-01-12T13:24:57+01:00',
            expectedStartTime: '2024-01-12T13:21:00+01:00',
            realtime: false,
            distance: 234.7,
            duration: 237,
            fromPlace: {
              name: 'Nationaltheatret',
            },
            toPlace: {
              name: 'Nationaltheatret',
            },
            toEstimatedCall: null,
            line: null,
            authority: null,
            pointsOnLink: {
              points:
                'u~tlJ{}n`A??GYEKIS[w@GKMSOUGKAEAE?G?G@EAECGCGACAAACCGAC?AAECECGAEFOTo@ISEKAFKl@IJO^GRCAIRGRc@p@??',
            },
          },
          {
            id: 'rO0ABXeSABhTQ0hFRFVMRURfVFJBTlNJVF9MRUdfVjMAAAAKMjAyNC0wMS0xMgAAABAAAAAWABBSQjpOU1I6UXVheTo3MzMzABFSQjpOU1I6UXVheToxMTEzNAA7UkI6UlVUOkRhdGVkU2VydmljZUpvdXJuZXk6YzRjM2QwMjI1YWI5OGFhYjQ4NWNmZjAwZDExM2E0NzA=',
            mode: 'metro',
            aimedStartTime: '2024-01-12T13:27:00+01:00',
            aimedEndTime: '2024-01-12T13:37:00+01:00',
            expectedEndTime: '2024-01-12T13:37:00+01:00',
            expectedStartTime: '2024-01-12T13:27:00+01:00',
            realtime: true,
            distance: 4410.74,
            duration: 600,
            fromPlace: {
              name: 'Nationaltheatret',
            },
            toPlace: {
              name: 'Helsfyr',
            },
            toEstimatedCall: {
              destinationDisplay: {
                frontText: 'Mortensrud',
              },
            },
            line: {
              publicCode: '3',
              name: 'Kolsås - Mortensrud',
            },
            authority: {
              name: 'Ruter',
            },
            pointsOnLink: {
              points:
                'seulJ{co`AHMTm@FKBa@@KJu@NuAL{@Jw@Lu@Ls@Hk@Lo@?AVyAf@oCLs@Lw@Ho@Js@Fs@Hu@HwADaADkAD}@?WP{EDo@Fm@LiELeD??By@NmEBy@Du@Bk@JgAJcAL_APeAX{A\\aBd@iCL_AJ{@H{@Bi@@g@@e@FgCBcA?OHuD??@[JyE@uA@oAAiAGaAGy@Iy@M{@{@sEOeAIo@IiAEkAAeA?qAH}H??@i@D}DDqC@y@@e@B{@Be@H_ArAeMR}BFuA@yAEwAIsAO{ASqAWgAa@mAe@_Ag@m@mByB]k@[q@Yy@YkASkAQsAYkCKy@Ko@Ms@Ka@Me@Ma@O_@O_@Sa@Yi@uAeCa@u@??w@yAQc@Oa@K[K_@Ma@Mg@g@uBOk@Oi@Mc@Oa@O[Q[QYEc@Cs@Am@Am@@o@@g@@g@Bi@Di@De@Fc@Fc@Hg@Nm@Ja@La@N_@N]R_@RYPYVUTSRMPIPIl@WRITKNKNMLKNQPSRYLULUHSJUHUJYHUNc@Lg@Pk@Lg@Lg@P{@Ha@n@{CH]R}@??`@mBH_@Lq@DYDYD]Da@B]Dg@@_@Bi@@g@?c@?c@Ae@A_@Ck@Eo@?AWwCEq@Eq@Cu@Cw@Cc@Ao@?m@?k@?q@?y@@k@B{@j@oOD}@FgAHw@LuANgAn@uFHw@',
            },
          },
          {
            id: null,
            mode: 'foot',
            aimedStartTime: '2024-01-12T13:37:00+01:00',
            aimedEndTime: '2024-01-12T13:37:32+01:00',
            expectedEndTime: '2024-01-12T13:37:32+01:00',
            expectedStartTime: '2024-01-12T13:37:00+01:00',
            realtime: false,
            distance: 33.93,
            duration: 32,
            fromPlace: {
              name: 'Helsfyr',
            },
            toPlace: {
              name: 'Helsfyr T',
            },
            toEstimatedCall: null,
            line: null,
            authority: null,
            pointsOnLink: {
              points: '_utlJws|`ABBAb@i@G??',
            },
          },
          {
            id: 'rO0ABXeTABhTQ0hFRFVMRURfVFJBTlNJVF9MRUdfVjMAAAAKMjAyNC0wMS0xMgAAAAIAAAAFABFSQjpOU1I6UXVheToxMTE0MgARUkI6TlNSOlF1YXk6MTA0MDIAO1JCOlJVVDpEYXRlZFNlcnZpY2VKb3VybmV5OmIzNDRjZDFmM2FjZWU2MTE5ZGE2N2M3MDNjMTViYmQ1',
            mode: 'bus',
            aimedStartTime: '2024-01-12T13:40:00+01:00',
            aimedEndTime: '2024-01-12T13:44:00+01:00',
            expectedEndTime: '2024-01-12T13:44:00+01:00',
            expectedStartTime: '2024-01-12T13:40:00+01:00',
            realtime: false,
            distance: 4040.73,
            duration: 240,
            fromPlace: {
              name: 'Helsfyr T',
            },
            toPlace: {
              name: 'Trosterud',
            },
            toEstimatedCall: {
              destinationDisplay: {
                frontText: 'Blystadlia',
              },
            },
            line: {
              publicCode: '300',
              name: 'Blystadlia - Ahus - Oslo bussterminal',
            },
            authority: {
              name: 'Ruter',
            },
            pointsOnLink: {
              points:
                'wvtlJsr|`AAIGg@M_AIc@K]GUMc@Uo@q@gBo@yA{B_G_BeEeCyGiBaFeAqC]_Ae@qAY_AMa@??IWSu@Mi@Ke@Mk@Oq@Ki@[iB[cBk@iDqAyHc@eCMs@??Ii@i@wCKi@Ke@COMk@Ou@YsAk@oCoAcGo@{CWiAYiAcAmDqBeHgBiGqBcHc@cBc@eBUgASiAQoAIi@Gi@ScCKaBKmBMuBCw@CiB?{BAiBAuA?yA?sA@yB@gC@{D?{A?{A?qA?sAAuAAqACuACqAEwAEkACiAGmAGqAI{AKkBKsAKoAKqAMmAMqAGm@K{@MgAM{@UeBWaBSsAc@oCa@kCSwAYsBQqAQ}AQ{AMqAMkAQuBm@yGOcB]cDMoAYwBWmBc@mC_@qB_@mBi@sDSiAe@}BOu@Oq@YeAOk@Qg@Wu@Oe@Q[M[Wk@We@EI',
            },
          },
          {
            id: null,
            mode: 'foot',
            aimedStartTime: '2024-01-12T13:44:00+01:00',
            aimedEndTime: '2024-01-12T13:47:21+01:00',
            expectedEndTime: '2024-01-12T13:47:21+01:00',
            expectedStartTime: '2024-01-12T13:44:00+01:00',
            realtime: false,
            distance: 178.32,
            duration: 201,
            fromPlace: {
              name: 'Trosterud',
            },
            toPlace: {
              name: 'Trosterudkrysset',
            },
            toEstimatedCall: null,
            line: null,
            authority: null,
            pointsOnLink: {
              points: 'sywlJw}haA?A?ECOAM@K?G@E@I@EBCBEFCPEBABCBC@E@G@K@YBSBSDKJQ`@y@HMVv@DPBJ@LBXU[??',
            },
          },
          {
            id: 'rO0ABXeTABhTQ0hFRFVMRURfVFJBTlNJVF9MRUdfVjMAAAAKMjAyNC0wMS0xMgAAAAIAAAAFABFSQjpOU1I6UXVheToxMDM3NQARUkI6TlNSOlF1YXk6MTAzOTYAO1JCOlJVVDpEYXRlZFNlcnZpY2VKb3VybmV5OjliZTdhODEwN2RjMGEyODQ5NGVkNTRhNzQ3ZThjYzZi',
            mode: 'bus',
            aimedStartTime: '2024-01-12T13:51:00+01:00',
            aimedEndTime: '2024-01-12T13:53:00+01:00',
            expectedEndTime: '2024-01-12T13:53:00+01:00',
            expectedStartTime: '2024-01-12T13:51:00+01:00',
            realtime: false,
            distance: 1203.14,
            duration: 120,
            fromPlace: {
              name: 'Trosterudkrysset',
            },
            toPlace: {
              name: 'Alfasetveien',
            },
            toEstimatedCall: {
              destinationDisplay: {
                frontText: 'Majorstuen',
              },
            },
            line: {
              publicCode: '25',
              name: 'Majorstuen-Haugerud',
            },
            authority: {
              name: 'Ruter',
            },
            pointsOnLink: {
              points:
                'iuwlJ_diaAEIOg@Us@EKIWOi@IQKIKIIEMCCGCECACAE?C@C@ABCDADADAFORQTKNKNMVMZ??MXO\\qAbDGLMXGNKLKPE@CBCDCDAHAFAF?HGRGVGTMZqBbFm@vASh@A???Sf@OZMVKTMVQVKJKJMHQHE?E@GBCFEHAJAD?DGPGHGHKH]TIFSH[LQDWDc@HwCh@uB^qDz@i@JM@MAQ?GKGCGAGBGFEJCNKNIHIFMDODo@RcAZE@',
            },
          },
          {
            id: null,
            mode: 'foot',
            aimedStartTime: '2024-01-12T13:53:00+01:00',
            aimedEndTime: '2024-01-12T13:55:31+01:00',
            expectedEndTime: '2024-01-12T13:55:31+01:00',
            expectedStartTime: '2024-01-12T13:53:00+01:00',
            realtime: false,
            distance: 162.35,
            duration: 151,
            fromPlace: {
              name: 'Alfasetveien',
            },
            toPlace: {
              name: 'Alfasetveien',
            },
            toEstimatedCall: null,
            line: null,
            authority: null,
            pointsOnLink: {
              points: 'wnylJqygaA??^QTIHAPGTG@?f@S@P?B@FAJ?J?LAFADEHCHUFk@RAEJO??',
            },
          },
          {
            id: 'rO0ABXeTABhTQ0hFRFVMRURfVFJBTlNJVF9MRUdfVjMAAAAKMjAyNC0wMS0xMgAAAAYAAAAIABFSQjpOU1I6UXVheToxMDM5NAARUkI6TlNSOlF1YXk6MTAyNTkAO1JCOlJVVDpEYXRlZFNlcnZpY2VKb3VybmV5OmQ5OTFhMTk1NGQzYjkyNjRhMTk5ZGY1OTA5YTBlMmE5',
            mode: 'bus',
            aimedStartTime: '2024-01-12T13:58:00+01:00',
            aimedEndTime: '2024-01-12T13:59:00+01:00',
            expectedEndTime: '2024-01-12T13:59:00+01:00',
            expectedStartTime: '2024-01-12T13:58:00+01:00',
            realtime: false,
            distance: 872.68,
            duration: 60,
            fromPlace: {
              name: 'Alfasetveien',
            },
            toPlace: {
              name: 'Postnord',
            },
            toEstimatedCall: {
              destinationDisplay: {
                frontText: 'Helsfyr',
              },
            },
            line: {
              publicCode: '68',
              name: 'Helsfyr T - Grorud T via Alfaset',
            },
            authority: {
              name: 'Ruter',
            },
            pointsOnLink: {
              points:
                'elylJaygaA`A[JALCH?RBBDBBBVBT@HBPDb@?TBbABlBFvBBf@Fr@Dh@Ht@Jr@Np@Lj@Nd@N`@Rb@Td@PZh@x@NP~@nAzAjBHLVZRVNVI\\Sd@Q^GN??KTcApBmA`CIPKREREX@XBXDPdAxCJ\\l@bB',
            },
          },
          {
            id: null,
            mode: 'foot',
            aimedStartTime: '2024-01-12T13:59:00+01:00',
            aimedEndTime: '2024-01-12T14:03:50+01:00',
            expectedEndTime: '2024-01-12T14:03:50+01:00',
            expectedStartTime: '2024-01-12T13:59:00+01:00',
            realtime: false,
            distance: 310.02,
            duration: 290,
            fromPlace: {
              name: 'Postnord',
            },
            toPlace: {
              name: 'Destination',
            },
            toEstimatedCall: null,
            line: null,
            authority: null,
            pointsOnLink: {
              points: 'q|xlJmweaACFA?m@aBBEDKK]eAyCEQSm@}@iCaAmCgAaE',
            },
          },
        ],
        systemNotices: [],
      },
      {
        aimedStartTime: '2024-01-12T12:54:07+01:00',
        aimedEndTime: '2024-01-12T14:07:28+01:00',
        expectedEndTime: '2024-01-12T14:07:28+01:00',
        expectedStartTime: '2024-01-12T12:54:07+01:00',
        duration: 4401,
        distance: 26472.179999999997,
        legs: [
          {
            id: null,
            mode: 'foot',
            aimedStartTime: '2024-01-12T12:54:07+01:00',
            aimedEndTime: '2024-01-12T13:05:00+01:00',
            expectedEndTime: '2024-01-12T13:05:00+01:00',
            expectedStartTime: '2024-01-12T12:54:07+01:00',
            realtime: false,
            distance: 727.21,
            duration: 653,
            fromPlace: {
              name: 'Origin',
            },
            toPlace: {
              name: 'IKEA Slependen',
            },
            toEstimatedCall: null,
            line: null,
            authority: null,
            pointsOnLink: {
              points:
                'o`nlJaac_AZ|@\\z@On@MAk@~@UZ@J@HEX@RLv@@DJVVd@LRNR\\^p@t@|@fA\\b@JTAHGVCJ@LBHDPP`@Tb@Xp@f@zAj@nBLd@Hj@@`ACz@@J?JDR@H?D?BAD?BEPAF?D?D@HBCNQDA@BBLDEHKd@a@BAD??O?M@KBKBGBIFGdAeA@?HIZ[??',
            },
          },
          {
            id: 'rO0ABXeRABhTQ0hFRFVMRURfVFJBTlNJVF9MRUdfVjMAAAAKMjAyNC0wMS0xMgAAADUAAAA7ABBSQjpOU1I6UXVheTo3OTQ0ABBSQjpOU1I6UXVheTo3MzczADtSQjpSVVQ6RGF0ZWRTZXJ2aWNlSm91cm5leTplNjBjM2M1ODE2ZjUxNGExZjk2NTdiZjEwYmQ3ZmJhNw==',
            mode: 'bus',
            aimedStartTime: '2024-01-12T13:05:00+01:00',
            aimedEndTime: '2024-01-12T13:21:00+01:00',
            expectedEndTime: '2024-01-12T13:21:00+01:00',
            expectedStartTime: '2024-01-12T13:05:00+01:00',
            realtime: true,
            distance: 15329.9,
            duration: 960,
            fromPlace: {
              name: 'IKEA Slependen',
            },
            toPlace: {
              name: 'Nationaltheatret',
            },
            toEstimatedCall: {
              destinationDisplay: {
                frontText: 'Oslo bussterminal',
              },
            },
            line: {
              publicCode: '250',
              name: 'Sætre - Slemmestad - Oslo bussterminal',
            },
            authority: {
              name: 'Ruter',
            },
            pointsOnLink: {
              points:
                '{lmlJa|a_AFEJKHEFEH?FEFKBM@MASDSLe@@KFSHWFYH[F[Nw@JSHWFMDKDIDABABEL@F@FBDBHHDJFPBP?P@RARERCNGNGFIFI@K@KAw@Sg@o@c@k@m@q@o@m@aA_Aq@o@s@s@k@m@c@e@e@i@]c@]g@U_@S_@Q_@Ue@Uc@]y@[{@_@gA[gA_@qAa@{A[sAmAyEs@uCaAgDwAmEqC{G}DaIgGkKmBiDcAcBcBqCqFeJc@q@U_@mAwB{@wA}@cBO[mA_CcAqB[q@Ys@k@uAQa@GOYs@o@iBe@wAg@{AY_Ag@iBYeAYeAS_A]}A_@_BYyAWqAQeAQcAO}@{@_GaAeIgAiKmAwLaAgJ{@qGmA{H]kBc@_CaAgFYgBMeAIk@Io@Gs@Gy@Gy@Ey@Cw@Ey@A}@Cu@C_BCkDAiACgAAaAE_AC{@Ew@Gy@G{@MsACQUaCuB}SmAoLOyAcBuP}@eIk@mEmA}Iy@_Iu@wLKaBKmAK_AK_AM_AS}ASmASkAMm@ScAq@_DY}A[_BKs@My@Iq@QkAOqAKcAE_@KiAIiAIiAGcAOaCa@sHS_DIwAGsAImAG{@Gu@KgAIcAK}@I_AK{@WsBaAuGcA}GaBaLwBwN{@gGKmAAk@CeCAoACs@E}@MiASsASwA_@yC]}B_@aCEc@Im@SuAM_AKq@CQEWOmAUwBOeBc@qFO_CKkBa@gFo@gFiBsL}A_KoBwMg@iFMuA[}FCY??Iu@QaASe@AGo@oBq@iBg@qBe@yCw@wEm@}Dq@mE_@eCO_A[uBO{@QaAUcAk@}BSw@Uw@e@cB}@sCeB_G}@yCWy@]wAI[[gAK]SaAOs@McAMyA_AyMSiCKyA??OsBKwAG}@Ea@UsBWuBUqBm@qES{AoAsJS}ASmASmAUkA_@mBg@_CYsAYyAYkBQkAMkAKcAKeAWcCIcA??EYKiAYyCUaCUoCSoBUuBKeAQyAW}BYyBc@gDa@_D[wBeC}PSqAMaAIg@Iy@Gm@Ei@Ek@EaAEcAG_BGoAEm@Ec@I{@Ee@EYE_@Mu@Kk@Ki@Mm@Qk@I[Sq@aAsDSm@W{@W}@U{@Sy@Qy@Mq@Ii@Ee@Ge@C_@Ea@Ce@Ci@K_BGwAOkCA[AO?Q@U@U@E@E?G?G?GAEAEAO@K?MBk@JsBFsA???KB]Ba@Dq@Dq@D]D{@LaBr@yFLy@PcAReAVaAPa@Pe@P[n@gAh@}@`@s@Tc@Vm@DOHUZqALq@PgAJ}@D}@DeABeA?eA?iA?q@Ay@Cm@Ew@GqAM}AEo@Aa@Ac@@c@Bk@Fk@D]DYLg@LYJYLSRWd@c@~@gA`AgAPUNSPYP]\\u@Rk@Pe@Le@Pu@Nw@Hc@Jy@LgAHsAF{@H}BF_AFy@NqAPkAVqA^mAf@{AVm@Zm@fB_DpJcP\\k@Tc@d@aAh@}AV{@d@uBZqAXsAJm@Lq@Hm@LaANsAJkAP}BRwCr@iL^eGT_DNmCHsADgA@i@@i@@y@?s@HwADgA?yA?i@Ac@A[A[Ca@E]Ky@_@iCG]EQESKUu@aBGMM[EQGYKc@CSESAO@M?O?MCKCKEGEEAAC?E[EYCY?Y?q@Ac@D{A@o@????Be@BaA@_A?a@?u@Ae@AUAKASCSEa@Ge@G]Ic@GWI[Uq@a@gAcAmCCGGEECCAGOACGOKUQKY{@EIu@kBUq@Oc@KWCEKUMWO_@KWAGGMiA}CiAwCEMGOUo@KWWq@Sk@AA',
            },
          },
          {
            id: null,
            mode: 'foot',
            aimedStartTime: '2024-01-12T13:21:00+01:00',
            aimedEndTime: '2024-01-12T13:24:57+01:00',
            expectedEndTime: '2024-01-12T13:24:57+01:00',
            expectedStartTime: '2024-01-12T13:21:00+01:00',
            realtime: false,
            distance: 234.7,
            duration: 237,
            fromPlace: {
              name: 'Nationaltheatret',
            },
            toPlace: {
              name: 'Nationaltheatret',
            },
            toEstimatedCall: null,
            line: null,
            authority: null,
            pointsOnLink: {
              points:
                'u~tlJ{}n`A??GYEKIS[w@GKMSOUGKAEAE?G?G@EAECGCGACAAACCGAC?AAECECGAEFOTo@ISEKAFKl@IJO^GRCAIRGRc@p@??',
            },
          },
          {
            id: 'rO0ABXeSABhTQ0hFRFVMRURfVFJBTlNJVF9MRUdfVjMAAAAKMjAyNC0wMS0xMgAAABAAAAAWABBSQjpOU1I6UXVheTo3MzMzABFSQjpOU1I6UXVheToxMTEzNAA7UkI6UlVUOkRhdGVkU2VydmljZUpvdXJuZXk6YzRjM2QwMjI1YWI5OGFhYjQ4NWNmZjAwZDExM2E0NzA=',
            mode: 'metro',
            aimedStartTime: '2024-01-12T13:27:00+01:00',
            aimedEndTime: '2024-01-12T13:37:00+01:00',
            expectedEndTime: '2024-01-12T13:37:00+01:00',
            expectedStartTime: '2024-01-12T13:27:00+01:00',
            realtime: true,
            distance: 4410.74,
            duration: 600,
            fromPlace: {
              name: 'Nationaltheatret',
            },
            toPlace: {
              name: 'Helsfyr',
            },
            toEstimatedCall: {
              destinationDisplay: {
                frontText: 'Mortensrud',
              },
            },
            line: {
              publicCode: '3',
              name: 'Kolsås - Mortensrud',
            },
            authority: {
              name: 'Ruter',
            },
            pointsOnLink: {
              points:
                'seulJ{co`AHMTm@FKBa@@KJu@NuAL{@Jw@Lu@Ls@Hk@Lo@?AVyAf@oCLs@Lw@Ho@Js@Fs@Hu@HwADaADkAD}@?WP{EDo@Fm@LiELeD??By@NmEBy@Du@Bk@JgAJcAL_APeAX{A\\aBd@iCL_AJ{@H{@Bi@@g@@e@FgCBcA?OHuD??@[JyE@uA@oAAiAGaAGy@Iy@M{@{@sEOeAIo@IiAEkAAeA?qAH}H??@i@D}DDqC@y@@e@B{@Be@H_ArAeMR}BFuA@yAEwAIsAO{ASqAWgAa@mAe@_Ag@m@mByB]k@[q@Yy@YkASkAQsAYkCKy@Ko@Ms@Ka@Me@Ma@O_@O_@Sa@Yi@uAeCa@u@??w@yAQc@Oa@K[K_@Ma@Mg@g@uBOk@Oi@Mc@Oa@O[Q[QYEc@Cs@Am@Am@@o@@g@@g@Bi@Di@De@Fc@Fc@Hg@Nm@Ja@La@N_@N]R_@RYPYVUTSRMPIPIl@WRITKNKNMLKNQPSRYLULUHSJUHUJYHUNc@Lg@Pk@Lg@Lg@P{@Ha@n@{CH]R}@??`@mBH_@Lq@DYDYD]Da@B]Dg@@_@Bi@@g@?c@?c@Ae@A_@Ck@Eo@?AWwCEq@Eq@Cu@Cw@Cc@Ao@?m@?k@?q@?y@@k@B{@j@oOD}@FgAHw@LuANgAn@uFHw@',
            },
          },
          {
            id: null,
            mode: 'foot',
            aimedStartTime: '2024-01-12T13:37:00+01:00',
            aimedEndTime: '2024-01-12T13:37:32+01:00',
            expectedEndTime: '2024-01-12T13:37:32+01:00',
            expectedStartTime: '2024-01-12T13:37:00+01:00',
            realtime: false,
            distance: 33.93,
            duration: 32,
            fromPlace: {
              name: 'Helsfyr',
            },
            toPlace: {
              name: 'Helsfyr T',
            },
            toEstimatedCall: null,
            line: null,
            authority: null,
            pointsOnLink: {
              points: '_utlJws|`ABBAb@i@G??',
            },
          },
          {
            id: 'rO0ABXeTABhTQ0hFRFVMRURfVFJBTlNJVF9MRUdfVjMAAAAKMjAyNC0wMS0xMgAAAAIAAAAFABFSQjpOU1I6UXVheToxMTE0MgARUkI6TlNSOlF1YXk6MTA0MDIAO1JCOlJVVDpEYXRlZFNlcnZpY2VKb3VybmV5OmIzNDRjZDFmM2FjZWU2MTE5ZGE2N2M3MDNjMTViYmQ1',
            mode: 'bus',
            aimedStartTime: '2024-01-12T13:40:00+01:00',
            aimedEndTime: '2024-01-12T13:44:00+01:00',
            expectedEndTime: '2024-01-12T13:44:00+01:00',
            expectedStartTime: '2024-01-12T13:40:00+01:00',
            realtime: false,
            distance: 4040.73,
            duration: 240,
            fromPlace: {
              name: 'Helsfyr T',
            },
            toPlace: {
              name: 'Trosterud',
            },
            toEstimatedCall: {
              destinationDisplay: {
                frontText: 'Blystadlia',
              },
            },
            line: {
              publicCode: '300',
              name: 'Blystadlia - Ahus - Oslo bussterminal',
            },
            authority: {
              name: 'Ruter',
            },
            pointsOnLink: {
              points:
                'wvtlJsr|`AAIGg@M_AIc@K]GUMc@Uo@q@gBo@yA{B_G_BeEeCyGiBaFeAqC]_Ae@qAY_AMa@??IWSu@Mi@Ke@Mk@Oq@Ki@[iB[cBk@iDqAyHc@eCMs@??Ii@i@wCKi@Ke@COMk@Ou@YsAk@oCoAcGo@{CWiAYiAcAmDqBeHgBiGqBcHc@cBc@eBUgASiAQoAIi@Gi@ScCKaBKmBMuBCw@CiB?{BAiBAuA?yA?sA@yB@gC@{D?{A?{A?qA?sAAuAAqACuACqAEwAEkACiAGmAGqAI{AKkBKsAKoAKqAMmAMqAGm@K{@MgAM{@UeBWaBSsAc@oCa@kCSwAYsBQqAQ}AQ{AMqAMkAQuBm@yGOcB]cDMoAYwBWmBc@mC_@qB_@mBi@sDSiAe@}BOu@Oq@YeAOk@Qg@Wu@Oe@Q[M[Wk@We@EI',
            },
          },
          {
            id: null,
            mode: 'foot',
            aimedStartTime: '2024-01-12T13:44:00+01:00',
            aimedEndTime: '2024-01-12T14:07:28+01:00',
            expectedEndTime: '2024-01-12T14:07:28+01:00',
            expectedStartTime: '2024-01-12T13:44:00+01:00',
            realtime: false,
            distance: 1694.97,
            duration: 1408,
            fromPlace: {
              name: 'Trosterud',
            },
            toPlace: {
              name: 'Destination',
            },
            toEstimatedCall: null,
            line: null,
            authority: null,
            pointsOnLink: {
              points:
                'sywlJw}haA?A?ECOAM@K?G@E@I@EBCBEFCPEBABCBC@E@G@K@YBSBSDKJQIMCCCCCAE?G@k@JG?IAKESMGCK?I?GDGFGLYt@_@|@Ul@c@bAOZCFAFAFAFAPAVCZAF?HCNGAI@IFIHILKTUh@iA`Cc@dAw@rBQd@M^Od@Mh@Mh@Mb@GPGNINGDI@i@b@YPC@OHQFUHUHu@TBz@BlALADAD?DBFBJHHPCHIx@ABK`AIh@CNIb@Ir@Y~CO~BAbD@fA@`ADtADv@s@?ODm@l@SPGJENGNEPI\\GGC?C?ORGZHLCJ?BV\\BDBH@J@NANAJEJEJUf@IN@@DLcApBmA`CIPKREREX@XBXSm@}@iCaAmCgAaE',
            },
          },
        ],
        systemNotices: [],
      },
      {
        aimedStartTime: '2024-01-12T12:42:00+01:00',
        aimedEndTime: '2024-01-12T14:09:19+01:00',
        expectedEndTime: '2024-01-12T14:09:19+01:00',
        expectedStartTime: '2024-01-12T12:42:00+01:00',
        duration: 5239,
        distance: 28576.019999999997,
        legs: [
          {
            id: null,
            mode: 'foot',
            aimedStartTime: '2024-01-12T12:42:00+01:00',
            aimedEndTime: '2024-01-12T12:59:00+01:00',
            expectedEndTime: '2024-01-12T12:59:00+01:00',
            expectedStartTime: '2024-01-12T12:42:00+01:00',
            realtime: false,
            distance: 1019.02,
            duration: 1020,
            fromPlace: {
              name: 'Origin',
            },
            toPlace: {
              name: 'Slependen stasjon',
            },
            toEstimatedCall: null,
            line: null,
            authority: null,
            pointsOnLink: {
              points:
                'o`nlJaac_AZ|@\\z@On@MAk@~@UZ@J@HEX@REDCDMLAFERQSy@a@_@IM@sAHkDJcADa@B]Hg@Va@Zc@b@u@n@_ChCcA|@_@h@w@rAA@CICIu@jAq@bAQX_@d@QVMFWFG?GAGCIGCQAI?ICO[iDCQCSEe@AEQHKD_@@??',
            },
          },
          {
            id: 'rO0ABXeFABhTQ0hFRFVMRURfVFJBTlNJVF9MRUdfVjMAAAAKMjAyNC0wMS0xMgAAAAUAAAAQABBSQjpOU1I6UXVheToxMDYxAA9SQjpOU1I6UXVheTo1MDQAMFJCOk5TQjpEYXRlZFNlcnZpY2VKb3VybmV5OjIxMzRfQVNSLUxMU18yNC0wMS0xMg==',
            mode: 'rail',
            aimedStartTime: '2024-01-12T12:59:00+01:00',
            aimedEndTime: '2024-01-12T13:36:00+01:00',
            expectedEndTime: '2024-01-12T13:35:30+01:00',
            expectedStartTime: '2024-01-12T12:59:00+01:00',
            realtime: true,
            distance: 25116.17,
            duration: 2190,
            fromPlace: {
              name: 'Slependen stasjon',
            },
            toPlace: {
              name: 'Nyland stasjon',
            },
            toEstimatedCall: {
              destinationDisplay: {
                frontText: 'Lillestrøm',
              },
            },
            line: {
              publicCode: 'L1',
              name: 'Spikkestad-Oslo S-Lillestrøm',
            },
            authority: {
              name: 'Vy',
            },
            pointsOnLink: {
              points:
                'glolJ}ib_AC@aA^IBUJUHWFYHUFWBUBY?Y?YA[CYGg@K[IWIm@M]K[M[OYQYSYW[_@Y]U_@Ua@[m@Um@Yu@Oe@Me@Mk@Me@Ki@Ms@Ks@Gk@Im@Gw@Em@G_AEu@Co@QcEC]??Q}DEiACw@C}@AmAA_AAqAAcH?mHAoB?uCAiA?m@Am@As@C}@Cw@EcAEgAIkAG}@Gs@Gw@Iu@Gq@McAKu@Ku@Kk@Kk@Ik@Mm@Os@Mo@Ki@??[yAYqA[sAc@mBaAgE????kBeIi@_C]yAYmAc@kBWgAYoAaBkH{@oDmAmFUcAu@_D_@}A??u@gDi@yBEWOq@YkAUaA{@iDc@eBU{@WcAOo@Mm@Mq@Kk@Ig@Ii@Im@Io@I{@I_AEs@Es@Ew@CeACgAAy@?s@@w@@y@@o@By@B}@F}@F}@F{@Hy@J{@Ju@RqAz@iFRkAJm@Js@De@Fc@De@Dc@Dm@Fo@Bo@Ds@@k@Bk@@m@@k@?i@?m@?i@Ak@Au@Cq@Cq@Cu@Es@Gw@AI????Iy@Gi@Gg@Gi@Ig@Ig@Ie@Kg@G]Ka@K]W_AQm@Qi@c@qAQk@M]Ka@K_@Ke@Mg@I_@Ki@Kk@Ik@Im@Gi@Gm@Gm@Ek@Cm@Eo@Cy@Cw@Aq@Am@?_A?q@?{@HeTFyO@i@F}L?_A?o@?o@Ai@Ak@Ck@Ag@Ce@Ck@Cg@Ek@Gi@Ei@Iq@M_AO_AIg@SuAO{@CUw@gFa@mCm@kEMy@mByM????q@mEk@}DQkAQgA]qBUuAWsAUuAIc@g@uCQgAQiAk@aE??a@wC]gCSoASoAMy@Q}@UoAQw@Qy@Sw@U{@W}@Sq@Og@Qk@Oc@Wq@Si@Uk@i@sAUk@Ys@Um@Qe@Oe@Sm@Om@Sq@Oi@Qu@Oo@Ou@Mq@Mo@Kq@Ms@Ks@K{@[{Bo@aFWkBSaBUgBMy@My@Ku@Ku@Mw@O_AO}@Ms@Q}@UkACK????ESOm@Qq@Mi@Og@_@mASs@Uw@a@wAUy@]kAs@cC]kAQs@]oAYeAWiAyA{F}AgGwAuFYmAUy@Sw@a@aB{AcGuAqFyA}F[kAWaAS{@Qq@U{@GYQq@[mAMe@I]Kc@Ka@Ig@Ke@Gc@Ii@Gg@Io@Ee@Gw@Ek@Cm@Ag@Ak@As@As@?s@@s@Bu@@m@Dm@Bq@Fu@Do@Fq@Fo@Fg@Hq@Z{B??JaAFk@Dg@??Jg@DWHc@Jm@??Js@Hq@RsAPsAJu@NiALgAHmAHoADkABeA@iA?E?????u@?aAAs@Cs@Cm@Ce@A_@Ce@Ei@IaAKgAG{@YmDK_AI}@Is@Iu@IcAYcDEo@KiAKcAIs@Kq@MaAQgASoAO{@SeAWkAc@qBm@eC[iAQs@Oo@Om@Mm@Kg@Mo@Ko@Ku@Ks@Iu@I}@I_AGeAEw@GiAEcAIyBEs@Eq@GcAIy@Ek@Iw@Ii@Im@Ig@O{@Km@S_AUaAU}@[aAY{@Ww@u@yBQi@Wo@k@cBe@wAYy@Y{@sAwD{@eCSm@??Sm@I_@Ka@IYMc@Si@??Mi@IYI[EU??Oc@GQMa@i@{AmEoMe@qA[}@Qo@Oc@Oo@Ka@Mk@Kg@Mk@G[E[M_A??Ig@Ic@CO??AOCYC]Kw@OsAE_@Ec@Eo@Ac@Cg@Ao@A}@A_A@cA?}@Bw@@o@??D}@Dy@H_AHy@H{@Hk@Ju@L}@x@cFJq@????BMDQj@eD|@kFX}AZgBXcBf@qCVuAN}@Nu@Nu@R{@XqA`@iBLo@Lm@??VsANu@RgAJm@Jq@NaANmAR_BN_BNeBL_BHqADeAD}@DgABu@B{@@_A@y@@y@@cA?iAA{AAeAA_AAy@C_ACy@C_AMsDKkCGiB??KyCKiDE_BC{AAkA?eAAeA@oA@kA@gABwADoAD_BH{AFaAFiAHgAFaAl@}Jn@}JPoCLuBPeCLuALsALoALkANkATaBn@oE|@_GfAqH@KVcBNaAP}@f@kCTmAVsA??XyAb@aCLo@NcALw@Ho@Hq@Hw@HgAHaADu@Dw@H}Ab@_K????d@gKF_BBs@Dw@DaBBaABs@Bm@Bm@@_@B[B_@Bg@Do@B_@D]Fu@??Fs@Fq@J_AJw@Fk@Hk@Jo@Jq@F_@Lu@Nw@??ZcBPeA\\gBN_ANu@Lu@L{@Jm@Ly@L_ANkAHw@Hs@H_AL}AHqAHkAFsAF{ADoABmAHgDDuABgADcAFy@F_AF}@H_AH{@LqAJ{@??@[B]@]??F[DUFa@Fe@Hs@He@L{@Ny@??RuALw@Jk@Jg@Ji@\\{AR}@Ns@N{@L_AVcB??^gCf@cD????p@qE|@eGh@iD??j@uDb@yCn@aE`@oCNaANgALiAJiADi@Fo@Dw@LwBFiA??Fm@Hu@PcBLsARoB??Z}CH}@`@wDXcDb@}DH_A??Dc@B_@Dq@@]Fk@H}@??Z{C??Ba@D_@@WB]@WDm@??TqBViCTgCL{ALyANiBLaBJqAHuAD_AHkADgADgADeAFqB@[Bu@BoA@y@BcA@mA@uA@}A?aA?_A?{A?}HAsF?mJA_F?qH?qBAsC?aB?oC?{BAoC?eH?mD?sD?gBAoB?kA?qB?qA?}@?k@Ak@?m@Aq@Ai@Ag@As@Cw@Cs@Cs@Ew@Eu@Ci@Gq@Eu@G{@KmAQmBYkDO_BOkBQuBw@aJScCQqBS_CS{BKwAOyAa@eFEe@??C_@C[Cq@AUCY??I_AGu@KmAGq@Gw@KaAIy@Go@Io@Im@ES????G_@Ko@Ke@[sAMg@Mk@Ok@Og@Qi@Sm@Oc@Oc@O]O_@Qa@Sa@Qa@k@kAQ_@u@{A??]o@Uc@S_@]m@Wc@OWEGYe@QW[c@W]QWSUMQSSUYOOo@o@c@a@[W[UWS]U[SUM_@Ue@Wq@][QYQYM_Ag@}BoAsEaCwC}AcAi@s@_@c@Wc@Uk@Yk@[u@a@w@a@cFkCyEgC??gCsA_B{@w@_@e@S]O]OWKWKSGsBs@SGWI[K[I[I_@GUEWE[C[C[C[AU?[@Y?W@a@BWD]DUBwATy@NoB\\a@F[Fa@F_@DO@I@W@[@Y?YAUAYCWE]G_@IYK]Ma@O_@Sa@Si@[MIQKu@c@yD}B????]S}D_Ci@]c@Y_@Wa@[_@Y[[_@[e@g@WYSUSYW]W]U_@Wa@Yc@Yg@Yi@Wi@Wi@Uk@Ug@Qe@Si@Um@Qi@Qk@So@Qo@Mc@Kc@EQKa@Mk@Sy@Qw@ScASaAOu@Qy@QaAO{@O_AOaAMy@MgAM_AKgAMkAIaAI_AIeAIcAImAIwAs@iL_BoXMuBK}AG{@Em@Gs@Gq@I_AIu@E_@Gi@Kw@Iq@Iq@M}@M}@Oy@QgAQcAk@yCWoA]gBI_@??u@}DmDwQcEkTMm@QaA_@iBSaAOw@CI??',
            },
          },
          {
            id: null,
            mode: 'foot',
            aimedStartTime: '2024-01-12T13:35:30+01:00',
            aimedEndTime: '2024-01-12T14:09:19+01:00',
            expectedEndTime: '2024-01-12T14:09:19+01:00',
            expectedStartTime: '2024-01-12T13:35:30+01:00',
            realtime: false,
            distance: 2440.83,
            duration: 2029,
            fromPlace: {
              name: 'Nyland stasjon',
            },
            toPlace: {
              name: 'Destination',
            },
            toEstimatedCall: null,
            line: null,
            authority: null,
            pointsOnLink: {
              points:
                'kxzlJakjaA??~@`EPfAJf@BNUDA?K?GCIGMXIXc@tACNBLFJNZJNFFHHD@FBFANd@Pb@Ph@Lf@Pr@Pt@Nt@VrAhGp[DXL|@BPBPp@hDDVDZ?H@JA`@@D@DBDANEJCHEHEDGFWRIFGHGJGLENENCLCRUhBCPB@@?r@LB?xBZ|@Ll@FlAH^@R@J?BABCBCDIDN?D?H?H?B?DAF?FAHETDBJJBDBDDDBDD@B@B@B?B?B?B?BABCBABEBCBCDCBADA?E?C@C@CBCDEBAFAFAb@APAPAPCb@Id@Ih@MRGLCF?RBJ?N?LAPEt@Uj@STGPAFTLf@HVHRJTMp@ITAX@ZLlAJhA?rA\\bDVrAXlAh@r@Tl@Zb@f@d@^LnAzAj@|@h@jAd@jAEJUf@IN@@DLcApBmA`CIPKREREX@XBXSm@}@iCaAmCgAaE',
            },
          },
        ],
        systemNotices: [],
      },
      {
        aimedStartTime: '2024-01-12T12:54:07+01:00',
        aimedEndTime: '2024-01-12T14:10:09+01:00',
        expectedEndTime: '2024-01-12T14:10:09+01:00',
        expectedStartTime: '2024-01-12T12:54:07+01:00',
        duration: 4562,
        distance: 26424.149999999998,
        legs: [
          {
            id: null,
            mode: 'foot',
            aimedStartTime: '2024-01-12T12:54:07+01:00',
            aimedEndTime: '2024-01-12T13:05:00+01:00',
            expectedEndTime: '2024-01-12T13:05:00+01:00',
            expectedStartTime: '2024-01-12T12:54:07+01:00',
            realtime: false,
            distance: 727.21,
            duration: 653,
            fromPlace: {
              name: 'Origin',
            },
            toPlace: {
              name: 'IKEA Slependen',
            },
            toEstimatedCall: null,
            line: null,
            authority: null,
            pointsOnLink: {
              points:
                'o`nlJaac_AZ|@\\z@On@MAk@~@UZ@J@HEX@RLv@@DJVVd@LRNR\\^p@t@|@fA\\b@JTAHGVCJ@LBHDPP`@Tb@Xp@f@zAj@nBLd@Hj@@`ACz@@J?JDR@H?D?BAD?BEPAF?D?D@HBCNQDA@BBLDEHKd@a@BAD??O?M@KBKBGBIFGdAeA@?HIZ[??',
            },
          },
          {
            id: 'rO0ABXeRABhTQ0hFRFVMRURfVFJBTlNJVF9MRUdfVjMAAAAKMjAyNC0wMS0xMgAAADUAAAA7ABBSQjpOU1I6UXVheTo3OTQ0ABBSQjpOU1I6UXVheTo3MzczADtSQjpSVVQ6RGF0ZWRTZXJ2aWNlSm91cm5leTplNjBjM2M1ODE2ZjUxNGExZjk2NTdiZjEwYmQ3ZmJhNw==',
            mode: 'bus',
            aimedStartTime: '2024-01-12T13:05:00+01:00',
            aimedEndTime: '2024-01-12T13:21:00+01:00',
            expectedEndTime: '2024-01-12T13:21:00+01:00',
            expectedStartTime: '2024-01-12T13:05:00+01:00',
            realtime: true,
            distance: 15329.9,
            duration: 960,
            fromPlace: {
              name: 'IKEA Slependen',
            },
            toPlace: {
              name: 'Nationaltheatret',
            },
            toEstimatedCall: {
              destinationDisplay: {
                frontText: 'Oslo bussterminal',
              },
            },
            line: {
              publicCode: '250',
              name: 'Sætre - Slemmestad - Oslo bussterminal',
            },
            authority: {
              name: 'Ruter',
            },
            pointsOnLink: {
              points:
                '{lmlJa|a_AFEJKHEFEH?FEFKBM@MASDSLe@@KFSHWFYH[F[Nw@JSHWFMDKDIDABABEL@F@FBDBHHDJFPBP?P@RARERCNGNGFIFI@K@KAw@Sg@o@c@k@m@q@o@m@aA_Aq@o@s@s@k@m@c@e@e@i@]c@]g@U_@S_@Q_@Ue@Uc@]y@[{@_@gA[gA_@qAa@{A[sAmAyEs@uCaAgDwAmEqC{G}DaIgGkKmBiDcAcBcBqCqFeJc@q@U_@mAwB{@wA}@cBO[mA_CcAqB[q@Ys@k@uAQa@GOYs@o@iBe@wAg@{AY_Ag@iBYeAYeAS_A]}A_@_BYyAWqAQeAQcAO}@{@_GaAeIgAiKmAwLaAgJ{@qGmA{H]kBc@_CaAgFYgBMeAIk@Io@Gs@Gy@Gy@Ey@Cw@Ey@A}@Cu@C_BCkDAiACgAAaAE_AC{@Ew@Gy@G{@MsACQUaCuB}SmAoLOyAcBuP}@eIk@mEmA}Iy@_Iu@wLKaBKmAK_AK_AM_AS}ASmASkAMm@ScAq@_DY}A[_BKs@My@Iq@QkAOqAKcAE_@KiAIiAIiAGcAOaCa@sHS_DIwAGsAImAG{@Gu@KgAIcAK}@I_AK{@WsBaAuGcA}GaBaLwBwN{@gGKmAAk@CeCAoACs@E}@MiASsASwA_@yC]}B_@aCEc@Im@SuAM_AKq@CQEWOmAUwBOeBc@qFO_CKkBa@gFo@gFiBsL}A_KoBwMg@iFMuA[}FCY??Iu@QaASe@AGo@oBq@iBg@qBe@yCw@wEm@}Dq@mE_@eCO_A[uBO{@QaAUcAk@}BSw@Uw@e@cB}@sCeB_G}@yCWy@]wAI[[gAK]SaAOs@McAMyA_AyMSiCKyA??OsBKwAG}@Ea@UsBWuBUqBm@qES{AoAsJS}ASmASmAUkA_@mBg@_CYsAYyAYkBQkAMkAKcAKeAWcCIcA??EYKiAYyCUaCUoCSoBUuBKeAQyAW}BYyBc@gDa@_D[wBeC}PSqAMaAIg@Iy@Gm@Ei@Ek@EaAEcAG_BGoAEm@Ec@I{@Ee@EYE_@Mu@Kk@Ki@Mm@Qk@I[Sq@aAsDSm@W{@W}@U{@Sy@Qy@Mq@Ii@Ee@Ge@C_@Ea@Ce@Ci@K_BGwAOkCA[AO?Q@U@U@E@E?G?G?GAEAEAO@K?MBk@JsBFsA???KB]Ba@Dq@Dq@D]D{@LaBr@yFLy@PcAReAVaAPa@Pe@P[n@gAh@}@`@s@Tc@Vm@DOHUZqALq@PgAJ}@D}@DeABeA?eA?iA?q@Ay@Cm@Ew@GqAM}AEo@Aa@Ac@@c@Bk@Fk@D]DYLg@LYJYLSRWd@c@~@gA`AgAPUNSPYP]\\u@Rk@Pe@Le@Pu@Nw@Hc@Jy@LgAHsAF{@H}BF_AFy@NqAPkAVqA^mAf@{AVm@Zm@fB_DpJcP\\k@Tc@d@aAh@}AV{@d@uBZqAXsAJm@Lq@Hm@LaANsAJkAP}BRwCr@iL^eGT_DNmCHsADgA@i@@i@@y@?s@HwADgA?yA?i@Ac@A[A[Ca@E]Ky@_@iCG]EQESKUu@aBGMM[EQGYKc@CSESAO@M?O?MCKCKEGEEAAC?E[EYCY?Y?q@Ac@D{A@o@????Be@BaA@_A?a@?u@Ae@AUAKASCSEa@Ge@G]Ic@GWI[Uq@a@gAcAmCCGGEECCAGOACGOKUQKY{@EIu@kBUq@Oc@KWCEKUMWO_@KWAGGMiA}CiAwCEMGOUo@KWWq@Sk@AA',
            },
          },
          {
            id: null,
            mode: 'foot',
            aimedStartTime: '2024-01-12T13:21:00+01:00',
            aimedEndTime: '2024-01-12T13:24:57+01:00',
            expectedEndTime: '2024-01-12T13:24:57+01:00',
            expectedStartTime: '2024-01-12T13:21:00+01:00',
            realtime: false,
            distance: 234.7,
            duration: 237,
            fromPlace: {
              name: 'Nationaltheatret',
            },
            toPlace: {
              name: 'Nationaltheatret',
            },
            toEstimatedCall: null,
            line: null,
            authority: null,
            pointsOnLink: {
              points:
                'u~tlJ{}n`A??GYEKIS[w@GKMSOUGKAEAE?G?G@EAECGCGACAAACCGAC?AAECECGAEFOTo@ISEKAFKl@IJO^GRCAIRGRc@p@??',
            },
          },
          {
            id: 'rO0ABXeSABhTQ0hFRFVMRURfVFJBTlNJVF9MRUdfVjMAAAAKMjAyNC0wMS0xMgAAABAAAAAWABBSQjpOU1I6UXVheTo3MzMzABFSQjpOU1I6UXVheToxMTEzNAA7UkI6UlVUOkRhdGVkU2VydmljZUpvdXJuZXk6YzRjM2QwMjI1YWI5OGFhYjQ4NWNmZjAwZDExM2E0NzA=',
            mode: 'metro',
            aimedStartTime: '2024-01-12T13:27:00+01:00',
            aimedEndTime: '2024-01-12T13:37:00+01:00',
            expectedEndTime: '2024-01-12T13:37:00+01:00',
            expectedStartTime: '2024-01-12T13:27:00+01:00',
            realtime: true,
            distance: 4410.74,
            duration: 600,
            fromPlace: {
              name: 'Nationaltheatret',
            },
            toPlace: {
              name: 'Helsfyr',
            },
            toEstimatedCall: {
              destinationDisplay: {
                frontText: 'Mortensrud',
              },
            },
            line: {
              publicCode: '3',
              name: 'Kolsås - Mortensrud',
            },
            authority: {
              name: 'Ruter',
            },
            pointsOnLink: {
              points:
                'seulJ{co`AHMTm@FKBa@@KJu@NuAL{@Jw@Lu@Ls@Hk@Lo@?AVyAf@oCLs@Lw@Ho@Js@Fs@Hu@HwADaADkAD}@?WP{EDo@Fm@LiELeD??By@NmEBy@Du@Bk@JgAJcAL_APeAX{A\\aBd@iCL_AJ{@H{@Bi@@g@@e@FgCBcA?OHuD??@[JyE@uA@oAAiAGaAGy@Iy@M{@{@sEOeAIo@IiAEkAAeA?qAH}H??@i@D}DDqC@y@@e@B{@Be@H_ArAeMR}BFuA@yAEwAIsAO{ASqAWgAa@mAe@_Ag@m@mByB]k@[q@Yy@YkASkAQsAYkCKy@Ko@Ms@Ka@Me@Ma@O_@O_@Sa@Yi@uAeCa@u@??w@yAQc@Oa@K[K_@Ma@Mg@g@uBOk@Oi@Mc@Oa@O[Q[QYEc@Cs@Am@Am@@o@@g@@g@Bi@Di@De@Fc@Fc@Hg@Nm@Ja@La@N_@N]R_@RYPYVUTSRMPIPIl@WRITKNKNMLKNQPSRYLULUHSJUHUJYHUNc@Lg@Pk@Lg@Lg@P{@Ha@n@{CH]R}@??`@mBH_@Lq@DYDYD]Da@B]Dg@@_@Bi@@g@?c@?c@Ae@A_@Ck@Eo@?AWwCEq@Eq@Cu@Cw@Cc@Ao@?m@?k@?q@?y@@k@B{@j@oOD}@FgAHw@LuANgAn@uFHw@',
            },
          },
          {
            id: null,
            mode: 'foot',
            aimedStartTime: '2024-01-12T13:37:00+01:00',
            aimedEndTime: '2024-01-12T13:37:47+01:00',
            expectedEndTime: '2024-01-12T13:37:47+01:00',
            expectedStartTime: '2024-01-12T13:37:00+01:00',
            realtime: false,
            distance: 57.17,
            duration: 47,
            fromPlace: {
              name: 'Helsfyr',
            },
            toPlace: {
              name: 'Helsfyr T',
            },
            toEstimatedCall: null,
            line: null,
            authority: null,
            pointsOnLink: {
              points: '_utlJws|`ABBOtBPhA??',
            },
          },
          {
            id: 'rO0ABXeTABhTQ0hFRFVMRURfVFJBTlNJVF9MRUdfVjMAAAAKMjAyNC0wMS0xMgAAAAAAAAAJABFSQjpOU1I6UXVheToxMTE0OAARUkI6TlNSOlF1YXk6MTE0NDIAO1JCOlJVVDpEYXRlZFNlcnZpY2VKb3VybmV5OmM4MzJiNGVkODUyYmE0ZDQwMTc0YmM3ODRkNWZkZGVm',
            mode: 'bus',
            aimedStartTime: '2024-01-12T13:45:00+01:00',
            aimedEndTime: '2024-01-12T13:55:00+01:00',
            expectedEndTime: '2024-01-12T13:55:00+01:00',
            expectedStartTime: '2024-01-12T13:45:00+01:00',
            realtime: false,
            distance: 4589.89,
            duration: 600,
            fromPlace: {
              name: 'Helsfyr T',
            },
            toPlace: {
              name: 'Alfaset',
            },
            toEstimatedCall: {
              destinationDisplay: {
                frontText: 'Grorud T via Ikea',
              },
            },
            line: {
              publicCode: '66',
              name: 'Helsfyr T - Grorud T',
            },
            authority: {
              name: 'Ruter',
            },
            pointsOnLink: {
              points:
                'yttlJun|`A?@Jn@DZ?N?J?JC?CBCDCHAL?J@JBFDFD@DADCBG@I@GDUFWFs@?AFk@Bi@Bi@@W@S?]?e@CeA?c@Ci@Ae@C]CYKiAGe@CMEa@Cc@AEl@yGDq@Be@B_@Di@Bc@DM@O?OAQEKGGEAC?E@GGEGCICKCMAQiAsNIoA?AG{@Co@Am@Ai@@e@?g@@[Bi@\\eH??HcB@_@D{@DaA@m@@]@cABi@@Q@IFa@@ADEBK?K?KAGCECCCAG_@AQASA[Aa@CiDA]AqA?KA}@EuC?Q????As@?ACg@Ae@C}@GsAGeAMoCMoCC_@?g@@W?Q?CBa@BK@M?K?MAMEKGIGEA?E?GDEFCHI?GEECECCAAEEEGKGOQe@EKQSM_@ISK]M[GO??IQIMEMQWSYUW[YYSWO[KSEUEm@EaAGOCQCQEMEQIQMOMMOMQ[i@iAyB??CC]m@MSQWYa@QSY_@]a@A?Y[g@e@SQeAy@kAcAwAeAa@_@QMe@_@????UOUMQK[MQGECWGe@Ia@G}AOKA}C]k@Ii@GUC[EKKEGEEEIEOM_@AOK_AKaAIw@Ek@??CSGgAEy@Cs@AaAAs@Ay@As@?q@@yA@gDBsD@wD@_C?oDByD?_@@iB@gB@i@@m@@[@Y?EHg@@U@A@E@E?EAEAEACAAA?A?IIECGCOBG?C?[@[BYB_@FSDy@J??C@OBSEE?KAI?AEACCEEACAC@A@KIGKEGUa@KYQ_@o@oAeAuBiAyBS[MW]o@Wc@[c@[c@i@q@EG??c@i@[_@[]s@w@GIIIYc@EIGKIMGMAAAKAGCEEEEAC@A@KIGEEGQOYk@]s@[w@Um@Sk@IYIUESIYEWGYEYEWE_@Ge@Gk@Ec@C]C_@C_@A]GcBC_@A[Ca@EWCWE]G[G]Qy@Qy@e@yBS_AMq@GSCOEQCMKe@S_AAC',
            },
          },
          {
            id: null,
            mode: 'foot',
            aimedStartTime: '2024-01-12T13:55:00+01:00',
            aimedEndTime: '2024-01-12T14:10:09+01:00',
            expectedEndTime: '2024-01-12T14:10:09+01:00',
            expectedStartTime: '2024-01-12T13:55:00+01:00',
            realtime: false,
            distance: 1074.54,
            duration: 909,
            fromPlace: {
              name: 'Alfaset',
            },
            toPlace: {
              name: 'Destination',
            },
            toEstimatedCall: null,
            line: null,
            authority: null,
            pointsOnLink: {
              points:
                'egxlJeigaA@A?BRz@DZA@ILEDA@}@aEAECIACCGAE?IUiA?CAEMaA_Br@KBI?GBGHKNW^w@jA]f@ORSRQLMHMBAbD@fA@`ADtADv@s@?ODm@l@SPGJENGNEPI\\GGC?C?ORGZHLCJ?BV\\BDBH@J@NANAJEJEJUf@IN@@DLcApBmA`CIPKREREX@XBXSm@}@iCaAmCgAaE',
            },
          },
        ],
        systemNotices: [],
      },
      {
        aimedStartTime: '2024-01-12T12:54:07+01:00',
        aimedEndTime: '2024-01-12T14:13:28+01:00',
        expectedEndTime: '2024-01-12T14:13:28+01:00',
        expectedStartTime: '2024-01-12T12:54:07+01:00',
        duration: 4761,
        distance: 27352.32,
        legs: [
          {
            id: null,
            mode: 'foot',
            aimedStartTime: '2024-01-12T12:54:07+01:00',
            aimedEndTime: '2024-01-12T13:05:00+01:00',
            expectedEndTime: '2024-01-12T13:05:00+01:00',
            expectedStartTime: '2024-01-12T12:54:07+01:00',
            realtime: false,
            distance: 727.21,
            duration: 653,
            fromPlace: {
              name: 'Origin',
            },
            toPlace: {
              name: 'IKEA Slependen',
            },
            toEstimatedCall: null,
            line: null,
            authority: null,
            pointsOnLink: {
              points:
                'o`nlJaac_AZ|@\\z@On@MAk@~@UZ@J@HEX@RLv@@DJVVd@LRNR\\^p@t@|@fA\\b@JTAHGVCJ@LBHDPP`@Tb@Xp@f@zAj@nBLd@Hj@@`ACz@@J?JDR@H?D?BAD?BEPAF?D?D@HBCNQDA@BBLDEHKd@a@BAD??O?M@KBKBGBIFGdAeA@?HIZ[??',
            },
          },
          {
            id: 'rO0ABXeSABhTQ0hFRFVMRURfVFJBTlNJVF9MRUdfVjMAAAAKMjAyNC0wMS0xMgAAADUAAAA-ABBSQjpOU1I6UXVheTo3OTQ0ABFSQjpOU1I6UXVheToxMTk2OQA7UkI6UlVUOkRhdGVkU2VydmljZUpvdXJuZXk6ZTYwYzNjNTgxNmY1MTRhMWY5NjU3YmYxMGJkN2ZiYTc=',
            mode: 'bus',
            aimedStartTime: '2024-01-12T13:05:00+01:00',
            aimedEndTime: '2024-01-12T13:28:00+01:00',
            expectedEndTime: '2024-01-12T13:28:00+01:00',
            expectedStartTime: '2024-01-12T13:05:00+01:00',
            realtime: true,
            distance: 17846.09,
            duration: 1380,
            fromPlace: {
              name: 'IKEA Slependen',
            },
            toPlace: {
              name: 'Oslo bussterminal',
            },
            toEstimatedCall: {
              destinationDisplay: {
                frontText: 'Oslo bussterminal',
              },
            },
            line: {
              publicCode: '250',
              name: 'Sætre - Slemmestad - Oslo bussterminal',
            },
            authority: {
              name: 'Ruter',
            },
            pointsOnLink: {
              points:
                '{lmlJa|a_AFEJKHEFEH?FEFKBM@MASDSLe@@KFSHWFYH[F[Nw@JSHWFMDKDIDABABEL@F@FBDBHHDJFPBP?P@RARERCNGNGFIFI@K@KAw@Sg@o@c@k@m@q@o@m@aA_Aq@o@s@s@k@m@c@e@e@i@]c@]g@U_@S_@Q_@Ue@Uc@]y@[{@_@gA[gA_@qAa@{A[sAmAyEs@uCaAgDwAmEqC{G}DaIgGkKmBiDcAcBcBqCqFeJc@q@U_@mAwB{@wA}@cBO[mA_CcAqB[q@Ys@k@uAQa@GOYs@o@iBe@wAg@{AY_Ag@iBYeAYeAS_A]}A_@_BYyAWqAQeAQcAO}@{@_GaAeIgAiKmAwLaAgJ{@qGmA{H]kBc@_CaAgFYgBMeAIk@Io@Gs@Gy@Gy@Ey@Cw@Ey@A}@Cu@C_BCkDAiACgAAaAE_AC{@Ew@Gy@G{@MsACQUaCuB}SmAoLOyAcBuP}@eIk@mEmA}Iy@_Iu@wLKaBKmAK_AK_AM_AS}ASmASkAMm@ScAq@_DY}A[_BKs@My@Iq@QkAOqAKcAE_@KiAIiAIiAGcAOaCa@sHS_DIwAGsAImAG{@Gu@KgAIcAK}@I_AK{@WsBaAuGcA}GaBaLwBwN{@gGKmAAk@CeCAoACs@E}@MiASsASwA_@yC]}B_@aCEc@Im@SuAM_AKq@CQEWOmAUwBOeBc@qFO_CKkBa@gFo@gFiBsL}A_KoBwMg@iFMuA[}FCY??Iu@QaASe@AGo@oBq@iBg@qBe@yCw@wEm@}Dq@mE_@eCO_A[uBO{@QaAUcAk@}BSw@Uw@e@cB}@sCeB_G}@yCWy@]wAI[[gAK]SaAOs@McAMyA_AyMSiCKyA??OsBKwAG}@Ea@UsBWuBUqBm@qES{AoAsJS}ASmASmAUkA_@mBg@_CYsAYyAYkBQkAMkAKcAKeAWcCIcA??EYKiAYyCUaCUoCSoBUuBKeAQyAW}BYyBc@gDa@_D[wBeC}PSqAMaAIg@Iy@Gm@Ei@Ek@EaAEcAG_BGoAEm@Ec@I{@Ee@EYE_@Mu@Kk@Ki@Mm@Qk@I[Sq@aAsDSm@W{@W}@U{@Sy@Qy@Mq@Ii@Ee@Ge@C_@Ea@Ce@Ci@K_BGwAOkCA[AO?Q@U@U@E@E?G?G?GAEAEAO@K?MBk@JsBFsA???KB]Ba@Dq@Dq@D]D{@LaBr@yFLy@PcAReAVaAPa@Pe@P[n@gAh@}@`@s@Tc@Vm@DOHUZqALq@PgAJ}@D}@DeABeA?eA?iA?q@Ay@Cm@Ew@GqAM}AEo@Aa@Ac@@c@Bk@Fk@D]DYLg@LYJYLSRWd@c@~@gA`AgAPUNSPYP]\\u@Rk@Pe@Le@Pu@Nw@Hc@Jy@LgAHsAF{@H}BF_AFy@NqAPkAVqA^mAf@{AVm@Zm@fB_DpJcP\\k@Tc@d@aAh@}AV{@d@uBZqAXsAJm@Lq@Hm@LaANsAJkAP}BRwCr@iL^eGT_DNmCHsADgA@i@@i@@y@?s@HwADgA?yA?i@Ac@A[A[Ca@E]Ky@_@iCG]EQESKUu@aBGMM[EQGYKc@CSESAO@M?O?MCKCKEGEEAAC?E[EYCY?Y?q@Ac@D{A@o@????Be@BaA@_A?a@?u@Ae@AUAKASCSEa@Ge@G]Ic@GWI[Uq@a@gAcAmCCGGEECCAGOACGOKUQKY{@EIu@kBUq@Oc@KWCEKUMWO_@KWAGGMiA}CiAwCEMGOUo@KWWq@Sk@AA??EMIU]{@MYKOMOEIIKIGAK?GCGCGGIECM?GIEIGOWy@EKEKCGIKCCCCOQCCAA[a@QSQU_@c@QU[_@i@o@KMCEOQIKkAyAMQY]KMAAAAACKKCEcAmAUU?M?IAOGyGAg@AQHQDK`AuBx@gB@Eh@cAh@gA??`@y@JSHSHWFOHWJ_@Lg@Pw@Lu@H_@De@Dg@Ba@@s@?s@Eq@GuAEs@MsBGqAEm@E}@C}@AcBAgA?e@?]?Q?U@_@@k@HoANiAT}B@SHu@F]D[J_@H[j@uBBK@EDOFe@@EDYH[VaAJ]@CFODIFG`AwAZa@??@AHKLOFC?A@?DA@?FA@?B?H@??D@F@NBF?D?FADCFCHGl@m@\\[LOFGFEBEDCDE@APMFChC_ALCNEDDB@F?BADEBIBK@OAOJy@VwCBWL_BD[DEBI@I?IAKB]Fi@JsAD{@HiALoAJ_AFg@@G??LeBLyANkBBSBQHk@BEBG@I?I?G?IAGCIACCAECA?C?C@C@ADCDAHAH?H?H@HBF?F?NAL?RMRILEHABINATKhAIbAIbAO`BALE`@ABGt@KlAKvAAL',
            },
          },
          {
            id: null,
            mode: 'foot',
            aimedStartTime: '2024-01-12T13:28:00+01:00',
            aimedEndTime: '2024-01-12T13:31:42+01:00',
            expectedEndTime: '2024-01-12T13:31:42+01:00',
            expectedStartTime: '2024-01-12T13:28:00+01:00',
            realtime: false,
            distance: 213.19,
            duration: 222,
            fromPlace: {
              name: 'Oslo bussterminal',
            },
            toPlace: {
              name: 'Oslo bussterminal',
            },
            toEstimatedCall: null,
            line: null,
            authority: null,
            pointsOnLink: {
              points: 'sptlJm|s`A??LoAB[@EPqB@MBa@JD@@FQ??D@HDJsA@@B@DBB@DBDB@@Fu@LwAHwA??',
            },
          },
          {
            id: 'rO0ABXeTABhTQ0hFRFVMRURfVFJBTlNJVF9MRUdfVjMAAAAKMjAyNC0wMS0xMgAAAAAAAAAJABFSQjpOU1I6UXVheToxMTk2MAARUkI6TlNSOlF1YXk6MTA0MDIAO1JCOlJVVDpEYXRlZFNlcnZpY2VKb3VybmV5OmY4MDE0ZDY1OWI3ZWY3ODNhZTdjYjU4NjBiMWMyNzg3',
            mode: 'bus',
            aimedStartTime: '2024-01-12T13:35:00+01:00',
            aimedEndTime: '2024-01-12T13:50:00+01:00',
            expectedEndTime: '2024-01-12T13:50:00+01:00',
            expectedStartTime: '2024-01-12T13:35:00+01:00',
            realtime: false,
            distance: 6870.86,
            duration: 900,
            fromPlace: {
              name: 'Oslo bussterminal',
            },
            toPlace: {
              name: 'Trosterud',
            },
            toEstimatedCall: {
              destinationDisplay: {
                frontText: 'Lillestrøm',
              },
            },
            line: {
              publicCode: '110',
              name: 'Lillestrøm - Oslo bussterminal',
            },
            authority: {
              name: 'Ruter',
            },
            pointsOnLink: {
              points:
                '}ktlJynt`ADi@BQBSBQHk@BEBG@I?I?G?IAG@QBQBKH_@Ji@BNBN?DDUBGJm@BM?ARy@d@{Bt@mDZ{ANo@H[@E`@oBt@mDFUBM?CJe@DSFU@KHc@?GDe@Nq@TgAj@mC@A@KBMFa@x@wE@E??F_@l@iDXcB`@eCV_BDYV_@BE??LUBM?OOaBBa@VuC`@}D@KZwCb@iEZgDFk@BUKa@CKGWWqAe@{BOu@EMIc@C_@G[YwAAGM[EQI]EQMm@ScAG[Mq@??OaAKo@Ik@Is@Io@MqAQ_BC]YeCs@}GEi@Gi@KeAEY??E[Iu@Gc@Ga@G]Ic@Kc@i@aCa@aBMk@Kc@[sAIa@Kc@S_A?C??I[C_@AKAa@?W@G@E?GHSBMFYN_@Rm@FUDUBOBQ@O@Q@O?MD]@g@?_@BG@I@KAIAICIEEEAE?EDADADO?I?E?C?E@C@CBEFCBCDSd@EHEFCDEDCBE@E@A@C?I?EAIEECs@q@Uw@WcAe@qBeD_OS{ASgAOiAEy@Ca@?U?O@I@G?IAKCICECCCAA?C?CBEIKUKc@COG[Ge@OkAEU??AIGg@M_AIc@K]GUMc@Uo@q@gBo@yA{B_G_BeEeCyGiBaFeAqC]_Ae@qAY_AMa@??IWSu@Mi@Ke@Mk@Oq@Ki@[iB[cBk@iDqAyHc@eCMs@??Ii@i@wCKi@Ke@COMk@Ou@YsAk@oCoAcGo@{CWiAYiAcAmDqBeHgBiGqBcHc@cBc@eBUgASiAQoAIi@Gi@ScCKaBKmBMuBCw@CiB?{BAiBAuA?yA?sA@yB@gC@{D?{A?{A?qA?sAAuAAqACuACqAEwAEkACiAGmAGqAI{AKkBKsAKoAKqAMmAMqAGm@K{@MgAM{@UeBWaBSsAc@oCa@kCSwAYsBQqAQ}AQ{AMqAMkAQuBm@yGOcB]cDMoAYwBWmBc@mC_@qB_@mBi@sDSiAe@}BOu@Oq@YeAOk@Qg@Wu@Oe@Q[M[Wk@We@EI',
            },
          },
          {
            id: null,
            mode: 'foot',
            aimedStartTime: '2024-01-12T13:50:00+01:00',
            aimedEndTime: '2024-01-12T14:13:28+01:00',
            expectedEndTime: '2024-01-12T14:13:28+01:00',
            expectedStartTime: '2024-01-12T13:50:00+01:00',
            realtime: false,
            distance: 1694.97,
            duration: 1408,
            fromPlace: {
              name: 'Trosterud',
            },
            toPlace: {
              name: 'Destination',
            },
            toEstimatedCall: null,
            line: null,
            authority: null,
            pointsOnLink: {
              points:
                'sywlJw}haA?A?ECOAM@K?G@E@I@EBCBEFCPEBABCBC@E@G@K@YBSBSDKJQIMCCCCCAE?G@k@JG?IAKESMGCK?I?GDGFGLYt@_@|@Ul@c@bAOZCFAFAFAFAPAVCZAF?HCNGAI@IFIHILKTUh@iA`Cc@dAw@rBQd@M^Od@Mh@Mh@Mb@GPGNINGDI@i@b@YPC@OHQFUHUHu@TBz@BlALADAD?DBFBJHHPCHIx@ABK`AIh@CNIb@Ir@Y~CO~BAbD@fA@`ADtADv@s@?ODm@l@SPGJENGNEPI\\GGC?C?ORGZHLCJ?BV\\BDBH@J@NANAJEJEJUf@IN@@DLcApBmA`CIPKREREX@XBXSm@}@iCaAmCgAaE',
            },
          },
        ],
        systemNotices: [],
      },
      {
        aimedStartTime: '2024-01-12T12:54:07+01:00',
        aimedEndTime: '2024-01-12T14:13:45+01:00',
        expectedEndTime: '2024-01-12T14:13:45+01:00',
        expectedStartTime: '2024-01-12T12:54:07+01:00',
        duration: 4778,
        distance: 30258.55,
        legs: [
          {
            id: null,
            mode: 'foot',
            aimedStartTime: '2024-01-12T12:54:07+01:00',
            aimedEndTime: '2024-01-12T13:05:00+01:00',
            expectedEndTime: '2024-01-12T13:05:00+01:00',
            expectedStartTime: '2024-01-12T12:54:07+01:00',
            realtime: false,
            distance: 727.21,
            duration: 653,
            fromPlace: {
              name: 'Origin',
            },
            toPlace: {
              name: 'IKEA Slependen',
            },
            toEstimatedCall: null,
            line: null,
            authority: null,
            pointsOnLink: {
              points:
                'o`nlJaac_AZ|@\\z@On@MAk@~@UZ@J@HEX@RLv@@DJVVd@LRNR\\^p@t@|@fA\\b@JTAHGVCJ@LBHDPP`@Tb@Xp@f@zAj@nBLd@Hj@@`ACz@@J?JDR@H?D?BAD?BEPAF?D?D@HBCNQDA@BBLDEHKd@a@BAD??O?M@KBKBGBIFGdAeA@?HIZ[??',
            },
          },
          {
            id: 'rO0ABXeSABhTQ0hFRFVMRURfVFJBTlNJVF9MRUdfVjMAAAAKMjAyNC0wMS0xMgAAADUAAAA-ABBSQjpOU1I6UXVheTo3OTQ0ABFSQjpOU1I6UXVheToxMTk2OQA7UkI6UlVUOkRhdGVkU2VydmljZUpvdXJuZXk6ZTYwYzNjNTgxNmY1MTRhMWY5NjU3YmYxMGJkN2ZiYTc=',
            mode: 'bus',
            aimedStartTime: '2024-01-12T13:05:00+01:00',
            aimedEndTime: '2024-01-12T13:28:00+01:00',
            expectedEndTime: '2024-01-12T13:28:00+01:00',
            expectedStartTime: '2024-01-12T13:05:00+01:00',
            realtime: true,
            distance: 17846.09,
            duration: 1380,
            fromPlace: {
              name: 'IKEA Slependen',
            },
            toPlace: {
              name: 'Oslo bussterminal',
            },
            toEstimatedCall: {
              destinationDisplay: {
                frontText: 'Oslo bussterminal',
              },
            },
            line: {
              publicCode: '250',
              name: 'Sætre - Slemmestad - Oslo bussterminal',
            },
            authority: {
              name: 'Ruter',
            },
            pointsOnLink: {
              points:
                '{lmlJa|a_AFEJKHEFEH?FEFKBM@MASDSLe@@KFSHWFYH[F[Nw@JSHWFMDKDIDABABEL@F@FBDBHHDJFPBP?P@RARERCNGNGFIFI@K@KAw@Sg@o@c@k@m@q@o@m@aA_Aq@o@s@s@k@m@c@e@e@i@]c@]g@U_@S_@Q_@Ue@Uc@]y@[{@_@gA[gA_@qAa@{A[sAmAyEs@uCaAgDwAmEqC{G}DaIgGkKmBiDcAcBcBqCqFeJc@q@U_@mAwB{@wA}@cBO[mA_CcAqB[q@Ys@k@uAQa@GOYs@o@iBe@wAg@{AY_Ag@iBYeAYeAS_A]}A_@_BYyAWqAQeAQcAO}@{@_GaAeIgAiKmAwLaAgJ{@qGmA{H]kBc@_CaAgFYgBMeAIk@Io@Gs@Gy@Gy@Ey@Cw@Ey@A}@Cu@C_BCkDAiACgAAaAE_AC{@Ew@Gy@G{@MsACQUaCuB}SmAoLOyAcBuP}@eIk@mEmA}Iy@_Iu@wLKaBKmAK_AK_AM_AS}ASmASkAMm@ScAq@_DY}A[_BKs@My@Iq@QkAOqAKcAE_@KiAIiAIiAGcAOaCa@sHS_DIwAGsAImAG{@Gu@KgAIcAK}@I_AK{@WsBaAuGcA}GaBaLwBwN{@gGKmAAk@CeCAoACs@E}@MiASsASwA_@yC]}B_@aCEc@Im@SuAM_AKq@CQEWOmAUwBOeBc@qFO_CKkBa@gFo@gFiBsL}A_KoBwMg@iFMuA[}FCY??Iu@QaASe@AGo@oBq@iBg@qBe@yCw@wEm@}Dq@mE_@eCO_A[uBO{@QaAUcAk@}BSw@Uw@e@cB}@sCeB_G}@yCWy@]wAI[[gAK]SaAOs@McAMyA_AyMSiCKyA??OsBKwAG}@Ea@UsBWuBUqBm@qES{AoAsJS}ASmASmAUkA_@mBg@_CYsAYyAYkBQkAMkAKcAKeAWcCIcA??EYKiAYyCUaCUoCSoBUuBKeAQyAW}BYyBc@gDa@_D[wBeC}PSqAMaAIg@Iy@Gm@Ei@Ek@EaAEcAG_BGoAEm@Ec@I{@Ee@EYE_@Mu@Kk@Ki@Mm@Qk@I[Sq@aAsDSm@W{@W}@U{@Sy@Qy@Mq@Ii@Ee@Ge@C_@Ea@Ce@Ci@K_BGwAOkCA[AO?Q@U@U@E@E?G?G?GAEAEAO@K?MBk@JsBFsA???KB]Ba@Dq@Dq@D]D{@LaBr@yFLy@PcAReAVaAPa@Pe@P[n@gAh@}@`@s@Tc@Vm@DOHUZqALq@PgAJ}@D}@DeABeA?eA?iA?q@Ay@Cm@Ew@GqAM}AEo@Aa@Ac@@c@Bk@Fk@D]DYLg@LYJYLSRWd@c@~@gA`AgAPUNSPYP]\\u@Rk@Pe@Le@Pu@Nw@Hc@Jy@LgAHsAF{@H}BF_AFy@NqAPkAVqA^mAf@{AVm@Zm@fB_DpJcP\\k@Tc@d@aAh@}AV{@d@uBZqAXsAJm@Lq@Hm@LaANsAJkAP}BRwCr@iL^eGT_DNmCHsADgA@i@@i@@y@?s@HwADgA?yA?i@Ac@A[A[Ca@E]Ky@_@iCG]EQESKUu@aBGMM[EQGYKc@CSESAO@M?O?MCKCKEGEEAAC?E[EYCY?Y?q@Ac@D{A@o@????Be@BaA@_A?a@?u@Ae@AUAKASCSEa@Ge@G]Ic@GWI[Uq@a@gAcAmCCGGEECCAGOACGOKUQKY{@EIu@kBUq@Oc@KWCEKUMWO_@KWAGGMiA}CiAwCEMGOUo@KWWq@Sk@AA??EMIU]{@MYKOMOEIIKIGAK?GCGCGGIECM?GIEIGOWy@EKEKCGIKCCCCOQCCAA[a@QSQU_@c@QU[_@i@o@KMCEOQIKkAyAMQY]KMAAAAACKKCEcAmAUU?M?IAOGyGAg@AQHQDK`AuBx@gB@Eh@cAh@gA??`@y@JSHSHWFOHWJ_@Lg@Pw@Lu@H_@De@Dg@Ba@@s@?s@Eq@GuAEs@MsBGqAEm@E}@C}@AcBAgA?e@?]?Q?U@_@@k@HoANiAT}B@SHu@F]D[J_@H[j@uBBK@EDOFe@@EDYH[VaAJ]@CFODIFG`AwAZa@??@AHKLOFC?A@?DA@?FA@?B?H@??D@F@NBF?D?FADCFCHGl@m@\\[LOFGFEBEDCDE@APMFChC_ALCNEDDB@F?BADEBIBK@OAOJy@VwCBWL_BD[DEBI@I?IAKB]Fi@JsAD{@HiALoAJ_AFg@@G??LeBLyANkBBSBQHk@BEBG@I?I?G?IAGCIACCAECA?C?C@C@ADCDAHAH?H?H@HBF?F?NAL?RMRILEHABINATKhAIbAIbAO`BALE`@ABGt@KlAKvAAL',
            },
          },
          {
            id: null,
            mode: 'foot',
            aimedStartTime: '2024-01-12T13:28:00+01:00',
            aimedEndTime: '2024-01-12T13:28:32+01:00',
            expectedEndTime: '2024-01-12T13:28:32+01:00',
            expectedStartTime: '2024-01-12T13:28:00+01:00',
            realtime: false,
            distance: 24.25,
            duration: 32,
            fromPlace: {
              name: 'Oslo bussterminal',
            },
            toPlace: {
              name: 'Oslo bussterminal',
            },
            toEstimatedCall: null,
            line: null,
            authority: null,
            pointsOnLink: {
              points: 'sptlJm|s`A@@@SAAGCGC?A?W@A??',
            },
          },
          {
            id: 'rO0ABXeTABhTQ0hFRFVMRURfVFJBTlNJVF9MRUdfVjMAAAAKMjAyNC0wMS0xMgAAAAAAAAAMABFSQjpOU1I6UXVheToxMTk4NQARUkI6TlNSOlF1YXk6MTA5NDMAO1JCOlJVVDpEYXRlZFNlcnZpY2VKb3VybmV5OjU0ZTUxNjljOTBiOTVlZTA0Y2JkMTliOTkzNTA2MGMz',
            mode: 'bus',
            aimedStartTime: '2024-01-12T13:36:00+01:00',
            aimedEndTime: '2024-01-12T13:54:00+01:00',
            expectedEndTime: '2024-01-12T13:54:00+01:00',
            expectedStartTime: '2024-01-12T13:36:00+01:00',
            realtime: false,
            distance: 7593.08,
            duration: 1080,
            fromPlace: {
              name: 'Oslo bussterminal',
            },
            toPlace: {
              name: 'Veitvet',
            },
            toEstimatedCall: {
              destinationDisplay: {
                frontText: 'Lillestrøm',
              },
            },
            line: {
              publicCode: '380',
              name: 'Lillestrøm - Rv4 - Oslo bussterminal',
            },
            authority: {
              name: 'Ruter',
            },
            pointsOnLink: {
              points:
                'gqtlJg~s`A@[CMCIAKIMCWJwA@UBUBW@SBWBW@WBU@UBUBW@WBU@I@M@UBUBW@UBUDE^W@AZUNDH@D?@?D?FAB?BABBB@B?@?BABCBEBG@I?I?G?IAGCIACCAECA?C?C@C@ADGAE?MEc@UEAGAIBIFGBC@EDEDEHCLENCLCTYpDSfCId@ENELELGJEBCFKAI?KCGCIEKIYUWSmBaB_BuA]YYYSSY[QSGGOQEE??EEKMACEIEIKSAKCIEGECEAGKCGGKGQI]K[W_AyAiFGUQo@_@wAc@cBKUIUEOEOE_@I]Su@Wo@GSCMCOEW]sAQ_@CICMIY_AjAI@G?EACAECIGW_@cAiBqCeF}C}FOe@EIGIIOGIEGKOSO{BoCKWGKSYMKGEKISK{@gAGIGO??KSIOGKIOOUGK]]IOyAwC_@s@mAcCMc@GQEI_AgBGKCGIQEIAIAOAOAK?IAC?I?Wc@G{AKyBQIAWAOCYIIAiAIMCCAEC@]AKAGACACG?YCC?C@ABADAB?BIKIGKKc@[MK??m@c@[UIIQKYUeAq@eAm@YOYQYOIEKCEAICKAMAyEe@[EgAKm@G??[C_@EoAKg@Cm@CoAK_@Ce@E]CWCWEMAIAGCAEAIAIAIEIEIEEEEGAE@G@EBCBCFQEg@UiAo@gAk@}@q@SQa@a@UUm@y@_@q@g@aAEGo@yBs@{BIS??IS_@iAyAkEw@}BG[Ie@?OAQCOEUIQIOKGGCI?A?S[OYK[m@iBWu@??Ma@_@oAY}@q@sBm@cDSoAUsAWaBQiAYqBYsBK{BQ{AUcCCu@C}@Gs@MqAUeBYqCQgBSkBGk@??AIGm@QkAGa@a@qB{@{IyBeTUaCMyAY_DIiDCaBI}CIyCCyA?k@?_@@c@Fo@@C@E?E?I?ICGAEECAAC?C?EEGKK[I_@OiAKw@OiA??SeBs@{DqAgKgAiIk@qEKq@??Ge@MiAI{@GcAE}@CaAA{@AcA@cAB}@BmAHgAJkANsAJu@N_AVgA~@mDXgAR{@Lo@PmALkAHiAFeABoA@oA?yAAoAAo@C_@??A]KoBK}AMaBWyBi@aFIy@Iw@IcAG{AGuACcBByB?}B?cA??AcAEyCGoBI_BCi@AQIw@SeBQkAO{@SeA[sAu@{FeB}Mm@mEUmBGc@',
            },
          },
          {
            id: 'rO0ABXeTABhTQ0hFRFVMRURfVFJBTlNJVF9MRUdfVjMAAAAKMjAyNC0wMS0xMgAAACMAAAAoABFSQjpOU1I6UXVheToxMDk0MwARUkI6TlNSOlF1YXk6MTAzOTQAO1JCOlJVVDpEYXRlZFNlcnZpY2VKb3VybmV5OjQyM2U4OWFhYjNjOTM2MzUwOTlkNzJkOWFiYmVkY2M0',
            mode: 'bus',
            aimedStartTime: '2024-01-12T13:56:00+01:00',
            aimedEndTime: '2024-01-12T14:01:00+01:00',
            expectedEndTime: '2024-01-12T14:01:00+01:00',
            expectedStartTime: '2024-01-12T13:56:00+01:00',
            realtime: false,
            distance: 3132.07,
            duration: 300,
            fromPlace: {
              name: 'Veitvet',
            },
            toPlace: {
              name: 'Alfasetveien',
            },
            toEstimatedCall: {
              destinationDisplay: {
                frontText: 'Haugerud T',
              },
            },
            line: {
              publicCode: '25',
              name: 'Majorstuen-Haugerud',
            },
            authority: {
              name: 'Ruter',
            },
            pointsOnLink: {
              points:
                'uc{lJmkeaAc@gD_@sCQcAMs@WkAQu@Sy@y@sCcAwDcCaJi@kBUu@Sq@Wm@Wi@We@U]_@e@[]a@g@Y]W]SWUa@S_@Q]Qe@Wq@So@Qo@Og@Ou@Os@O_AMs@K{@KaAIu@IaAGkAEy@C}@GqA????]{J@gCAcAC{@Cq@Co@Gu@Iq@OqAGi@Is@AMAMAOAS?Y@E@E?E@EFCDCF?PBF@HCFAd@LRD??@?HBPNHBHBDBFBJBDBFBXDHFf@b@JLHHNPTXRZXd@`@r@^l@`@r@T`@PXPVTXNRVTZTVPTLZLXJVFVDVDz@JNBx@H^DRHH@l@B??J@X@RBH?LBHBT@`@TVN\\VVRXVTTLNNPX`@\\f@T`@Zn@Rd@lCnGR`@P^PZR\\PVJLNPNN??B@LLFDNF\\JXRFBFBFDDHDFLR?FBD@DBDD@DABC@C`@[TIH@ZErAL|@Jp@FlAHh@BH@RJHFHHJLDJBFFFD@FADCDEDKBKHIHGHCNMF?PARANANAPCd@Ib@Gh@K^Il@@PCjA[TG',
            },
          },
          {
            id: null,
            mode: 'foot',
            aimedStartTime: '2024-01-12T14:01:00+01:00',
            aimedEndTime: '2024-01-12T14:13:45+01:00',
            expectedEndTime: '2024-01-12T14:13:45+01:00',
            expectedStartTime: '2024-01-12T14:01:00+01:00',
            realtime: false,
            distance: 935.85,
            duration: 765,
            fromPlace: {
              name: 'Alfasetveien',
            },
            toPlace: {
              name: 'Destination',
            },
            toEstimatedCall: null,
            line: null,
            authority: null,
            pointsOnLink: {
              points:
                'ilylJgxgaA??KN@Dj@STGPAFTLf@HVHRJTMp@ITAX@ZLlAJhA?rA\\bDVrAXlAh@r@Tl@Zb@f@d@^LnAzAj@|@h@jAd@jAEJUf@IN@@DLcApBmA`CIPKREREX@XBXSm@}@iCaAmCgAaE',
            },
          },
        ],
        systemNotices: [],
      },
      {
        aimedStartTime: '2024-01-12T12:54:07+01:00',
        aimedEndTime: '2024-01-12T14:14:09+01:00',
        expectedEndTime: '2024-01-12T14:14:09+01:00',
        expectedStartTime: '2024-01-12T12:54:07+01:00',
        duration: 4802,
        distance: 26616.219999999998,
        legs: [
          {
            id: null,
            mode: 'foot',
            aimedStartTime: '2024-01-12T12:54:07+01:00',
            aimedEndTime: '2024-01-12T13:05:00+01:00',
            expectedEndTime: '2024-01-12T13:05:00+01:00',
            expectedStartTime: '2024-01-12T12:54:07+01:00',
            realtime: false,
            distance: 727.21,
            duration: 653,
            fromPlace: {
              name: 'Origin',
            },
            toPlace: {
              name: 'IKEA Slependen',
            },
            toEstimatedCall: null,
            line: null,
            authority: null,
            pointsOnLink: {
              points:
                'o`nlJaac_AZ|@\\z@On@MAk@~@UZ@J@HEX@RLv@@DJVVd@LRNR\\^p@t@|@fA\\b@JTAHGVCJ@LBHDPP`@Tb@Xp@f@zAj@nBLd@Hj@@`ACz@@J?JDR@H?D?BAD?BEPAF?D?D@HBCNQDA@BBLDEHKd@a@BAD??O?M@KBKBGBIFGdAeA@?HIZ[??',
            },
          },
          {
            id: 'rO0ABXeSABhTQ0hFRFVMRURfVFJBTlNJVF9MRUdfVjMAAAAKMjAyNC0wMS0xMgAAADUAAAA-ABBSQjpOU1I6UXVheTo3OTQ0ABFSQjpOU1I6UXVheToxMTk2OQA7UkI6UlVUOkRhdGVkU2VydmljZUpvdXJuZXk6ZTYwYzNjNTgxNmY1MTRhMWY5NjU3YmYxMGJkN2ZiYTc=',
            mode: 'bus',
            aimedStartTime: '2024-01-12T13:05:00+01:00',
            aimedEndTime: '2024-01-12T13:28:00+01:00',
            expectedEndTime: '2024-01-12T13:28:00+01:00',
            expectedStartTime: '2024-01-12T13:05:00+01:00',
            realtime: true,
            distance: 17846.09,
            duration: 1380,
            fromPlace: {
              name: 'IKEA Slependen',
            },
            toPlace: {
              name: 'Oslo bussterminal',
            },
            toEstimatedCall: {
              destinationDisplay: {
                frontText: 'Oslo bussterminal',
              },
            },
            line: {
              publicCode: '250',
              name: 'Sætre - Slemmestad - Oslo bussterminal',
            },
            authority: {
              name: 'Ruter',
            },
            pointsOnLink: {
              points:
                '{lmlJa|a_AFEJKHEFEH?FEFKBM@MASDSLe@@KFSHWFYH[F[Nw@JSHWFMDKDIDABABEL@F@FBDBHHDJFPBP?P@RARERCNGNGFIFI@K@KAw@Sg@o@c@k@m@q@o@m@aA_Aq@o@s@s@k@m@c@e@e@i@]c@]g@U_@S_@Q_@Ue@Uc@]y@[{@_@gA[gA_@qAa@{A[sAmAyEs@uCaAgDwAmEqC{G}DaIgGkKmBiDcAcBcBqCqFeJc@q@U_@mAwB{@wA}@cBO[mA_CcAqB[q@Ys@k@uAQa@GOYs@o@iBe@wAg@{AY_Ag@iBYeAYeAS_A]}A_@_BYyAWqAQeAQcAO}@{@_GaAeIgAiKmAwLaAgJ{@qGmA{H]kBc@_CaAgFYgBMeAIk@Io@Gs@Gy@Gy@Ey@Cw@Ey@A}@Cu@C_BCkDAiACgAAaAE_AC{@Ew@Gy@G{@MsACQUaCuB}SmAoLOyAcBuP}@eIk@mEmA}Iy@_Iu@wLKaBKmAK_AK_AM_AS}ASmASkAMm@ScAq@_DY}A[_BKs@My@Iq@QkAOqAKcAE_@KiAIiAIiAGcAOaCa@sHS_DIwAGsAImAG{@Gu@KgAIcAK}@I_AK{@WsBaAuGcA}GaBaLwBwN{@gGKmAAk@CeCAoACs@E}@MiASsASwA_@yC]}B_@aCEc@Im@SuAM_AKq@CQEWOmAUwBOeBc@qFO_CKkBa@gFo@gFiBsL}A_KoBwMg@iFMuA[}FCY??Iu@QaASe@AGo@oBq@iBg@qBe@yCw@wEm@}Dq@mE_@eCO_A[uBO{@QaAUcAk@}BSw@Uw@e@cB}@sCeB_G}@yCWy@]wAI[[gAK]SaAOs@McAMyA_AyMSiCKyA??OsBKwAG}@Ea@UsBWuBUqBm@qES{AoAsJS}ASmASmAUkA_@mBg@_CYsAYyAYkBQkAMkAKcAKeAWcCIcA??EYKiAYyCUaCUoCSoBUuBKeAQyAW}BYyBc@gDa@_D[wBeC}PSqAMaAIg@Iy@Gm@Ei@Ek@EaAEcAG_BGoAEm@Ec@I{@Ee@EYE_@Mu@Kk@Ki@Mm@Qk@I[Sq@aAsDSm@W{@W}@U{@Sy@Qy@Mq@Ii@Ee@Ge@C_@Ea@Ce@Ci@K_BGwAOkCA[AO?Q@U@U@E@E?G?G?GAEAEAO@K?MBk@JsBFsA???KB]Ba@Dq@Dq@D]D{@LaBr@yFLy@PcAReAVaAPa@Pe@P[n@gAh@}@`@s@Tc@Vm@DOHUZqALq@PgAJ}@D}@DeABeA?eA?iA?q@Ay@Cm@Ew@GqAM}AEo@Aa@Ac@@c@Bk@Fk@D]DYLg@LYJYLSRWd@c@~@gA`AgAPUNSPYP]\\u@Rk@Pe@Le@Pu@Nw@Hc@Jy@LgAHsAF{@H}BF_AFy@NqAPkAVqA^mAf@{AVm@Zm@fB_DpJcP\\k@Tc@d@aAh@}AV{@d@uBZqAXsAJm@Lq@Hm@LaANsAJkAP}BRwCr@iL^eGT_DNmCHsADgA@i@@i@@y@?s@HwADgA?yA?i@Ac@A[A[Ca@E]Ky@_@iCG]EQESKUu@aBGMM[EQGYKc@CSESAO@M?O?MCKCKEGEEAAC?E[EYCY?Y?q@Ac@D{A@o@????Be@BaA@_A?a@?u@Ae@AUAKASCSEa@Ge@G]Ic@GWI[Uq@a@gAcAmCCGGEECCAGOACGOKUQKY{@EIu@kBUq@Oc@KWCEKUMWO_@KWAGGMiA}CiAwCEMGOUo@KWWq@Sk@AA??EMIU]{@MYKOMOEIIKIGAK?GCGCGGIECM?GIEIGOWy@EKEKCGIKCCCCOQCCAA[a@QSQU_@c@QU[_@i@o@KMCEOQIKkAyAMQY]KMAAAAACKKCEcAmAUU?M?IAOGyGAg@AQHQDK`AuBx@gB@Eh@cAh@gA??`@y@JSHSHWFOHWJ_@Lg@Pw@Lu@H_@De@Dg@Ba@@s@?s@Eq@GuAEs@MsBGqAEm@E}@C}@AcBAgA?e@?]?Q?U@_@@k@HoANiAT}B@SHu@F]D[J_@H[j@uBBK@EDOFe@@EDYH[VaAJ]@CFODIFG`AwAZa@??@AHKLOFC?A@?DA@?FA@?B?H@??D@F@NBF?D?FADCFCHGl@m@\\[LOFGFEBEDCDE@APMFChC_ALCNEDDB@F?BADEBIBK@OAOJy@VwCBWL_BD[DEBI@I?IAKB]Fi@JsAD{@HiALoAJ_AFg@@G??LeBLyANkBBSBQHk@BEBG@I?I?G?IAGCIACCAECA?C?C@C@ADCDAHAH?H?H@HBF?F?NAL?RMRILEHABINATKhAIbAIbAO`BALE`@ABGt@KlAKvAAL',
            },
          },
          {
            id: null,
            mode: 'foot',
            aimedStartTime: '2024-01-12T13:28:00+01:00',
            aimedEndTime: '2024-01-12T13:31:42+01:00',
            expectedEndTime: '2024-01-12T13:31:42+01:00',
            expectedStartTime: '2024-01-12T13:28:00+01:00',
            realtime: false,
            distance: 213.19,
            duration: 222,
            fromPlace: {
              name: 'Oslo bussterminal',
            },
            toPlace: {
              name: 'Oslo bussterminal',
            },
            toEstimatedCall: null,
            line: null,
            authority: null,
            pointsOnLink: {
              points: 'sptlJm|s`A??LoAB[@EPqB@MBa@JD@@FQ??D@HDJsA@@B@DBB@DBDB@@Fu@LwAHwA??',
            },
          },
          {
            id: 'rO0ABXeTABhTQ0hFRFVMRURfVFJBTlNJVF9MRUdfVjMAAAAKMjAyNC0wMS0xMgAAAAAAAAANABFSQjpOU1I6UXVheToxMTk2MAARUkI6TlNSOlF1YXk6MTE0NDIAO1JCOlJVVDpEYXRlZFNlcnZpY2VKb3VybmV5OjZlMjFkOWY5OGIyYmQ4MDg4YjlhN2M4ZjUzNGExOTg2',
            mode: 'bus',
            aimedStartTime: '2024-01-12T13:40:00+01:00',
            aimedEndTime: '2024-01-12T13:59:00+01:00',
            expectedEndTime: '2024-01-12T13:59:00+01:00',
            expectedStartTime: '2024-01-12T13:40:00+01:00',
            realtime: false,
            distance: 6755.19,
            duration: 1140,
            fromPlace: {
              name: 'Oslo bussterminal',
            },
            toPlace: {
              name: 'Alfaset',
            },
            toEstimatedCall: {
              destinationDisplay: {
                frontText: 'Kjeller',
              },
            },
            line: {
              publicCode: '100',
              name: 'Kjeller - Oslo bussterminal',
            },
            authority: {
              name: 'Ruter',
            },
            pointsOnLink: {
              points:
                '}ktlJynt`ADi@BQBSBQHk@BEBG@I?I?G?IAG@QBQBKH_@Ji@BNBN?DDUBGJm@BM?ARy@d@{Bt@mDZ{ANo@H[@E`@oBt@mDFUBM?CJe@DSFU@KHc@?GDe@Nq@TgAj@mC@A@KBMFa@x@wE@E??F_@l@iDXcB`@eCV_BDYV_@BE??LUBM?OOaBBa@VuC`@}D@KZwCb@iEZgDFk@BUKa@CKGWWqAe@{BOu@EMIc@C_@G[YwAAGM[EQI]EQMm@ScAG[Mq@??OaAKo@Ik@Is@Io@MqAQ_BC]YeCs@}GEi@Gi@KeAEY??E[Iu@Gc@Ga@G]Ic@Kc@i@aCa@aBMk@Kc@[sAIa@Kc@S_A?C??I[C_@AKAa@?W@G@E?GHSBMFYN_@Rm@FUDUBOBQ@O@Q@O?MD]@g@?_@BG@I@KAIAICIEEEAE?EDADADO?I?E?C?E@C@CBEFCBCDSd@EHEFCDEDCBE@E@A@C?I?EAIEECs@q@Uw@WcAe@qBeD_OS{ASgAOiAEy@Ca@?U?O@I@G?IAKCICECCCAA?C?CBEIKUKc@COG[Ge@OkAEU??AIGg@M_AIc@K]GUMc@Uo@q@gBo@yA{B_G_BeEeCyGiBaFeAqC]_Ae@qAY_AMa@??IWSu@Mi@Ke@Mk@Oq@Ki@[iB[cBk@iDqAyHc@eCMs@??Ii@i@wCKi@Ke@COMk@Ou@YsAk@oCoAcGBUGk@Ce@Ao@Ao@AWA[AQC]CWIe@COOgAMo@OaAGWcA}FCMUgA??AESaASu@WaA[gAUu@]cAWy@W{@K]wAsEIYKWMa@Qo@K_@I[CIAMCKAQ@M?Q@Y@QA]KKEGEEEIEOM_@AOK_AKaAIw@Ek@??CSGgAEy@Cs@AaAAs@Ay@As@?q@@yA@gDBsD@wD@_C?oDByD?_@@iB@gB@i@@m@@[@Y?EHg@@U@A@E@E?EAEAEACAAA?A?IIECGCOBG?C?[@[BYB_@FSDy@J??C@OBSEE?KAI?AEACCEEACAC@A@KIGKEGUa@KYQ_@o@oAeAuBiAyBS[MW]o@Wc@[c@[c@i@q@EG??c@i@[_@[]s@w@GIIIYc@EIGKIMGMAAAKAGCEEEEAC@A@KIGEEGQOYk@]s@[w@Um@Sk@IYIUESIYEWGYEYEWE_@Ge@Gk@Ec@C]C_@C_@A]GcBC_@A[Ca@EWCWE]G[G]Qy@Qy@e@yBS_AMq@GSCOEQCMKe@S_AAC',
            },
          },
          {
            id: null,
            mode: 'foot',
            aimedStartTime: '2024-01-12T13:59:00+01:00',
            aimedEndTime: '2024-01-12T14:14:09+01:00',
            expectedEndTime: '2024-01-12T14:14:09+01:00',
            expectedStartTime: '2024-01-12T13:59:00+01:00',
            realtime: false,
            distance: 1074.54,
            duration: 909,
            fromPlace: {
              name: 'Alfaset',
            },
            toPlace: {
              name: 'Destination',
            },
            toEstimatedCall: null,
            line: null,
            authority: null,
            pointsOnLink: {
              points:
                'egxlJeigaA@A?BRz@DZA@ILEDA@}@aEAECIACCGAE?IUiA?CAEMaA_Br@KBI?GBGHKNW^w@jA]f@ORSRQLMHMBAbD@fA@`ADtADv@s@?ODm@l@SPGJENGNEPI\\GGC?C?ORGZHLCJ?BV\\BDBH@J@NANAJEJEJUf@IN@@DLcApBmA`CIPKREREX@XBXSm@}@iCaAmCgAaE',
            },
          },
        ],
        systemNotices: [],
      },
      {
        aimedStartTime: '2024-01-12T12:56:02+01:00',
        aimedEndTime: '2024-01-12T14:14:09+01:00',
        expectedEndTime: '2024-01-12T14:14:09+01:00',
        expectedStartTime: '2024-01-12T12:56:02+01:00',
        duration: 4687,
        distance: 26193.91,
        legs: [
          {
            id: null,
            mode: 'foot',
            aimedStartTime: '2024-01-12T12:56:02+01:00',
            aimedEndTime: '2024-01-12T13:00:09+01:00',
            expectedEndTime: '2024-01-12T13:00:09+01:00',
            expectedStartTime: '2024-01-12T12:56:02+01:00',
            realtime: false,
            distance: 291.84,
            duration: 247,
            fromPlace: {
              name: 'Origin',
            },
            toPlace: {
              name: 'Slependen',
            },
            toEstimatedCall: null,
            line: null,
            authority: null,
            pointsOnLink: {
              points: 'o`nlJaac_AZ|@\\z@On@MAk@~@UZ@J@HEX@RLv@@DJVVd@LRNR\\^p@t@|@fA??',
            },
          },
          {
            id: 'rO0ABXeRABhTQ0hFRFVMRURfVFJBTlNJVF9MRUdfVjMAAAAKMjAyNC0wMS0xMgAAABkAAAAeABBSQjpOU1I6UXVheTo3OTI4ABBSQjpOU1I6UXVheTo3MjYzADtSQjpSVVQ6RGF0ZWRTZXJ2aWNlSm91cm5leToyOWIyODMyMDdlZDg3YmRmZjJlYWM4ODdiMTNlZGE2OA==',
            mode: 'bus',
            aimedStartTime: '2024-01-12T13:00:00+01:00',
            aimedEndTime: '2024-01-12T13:07:00+01:00',
            expectedEndTime: '2024-01-12T13:07:09+01:00',
            expectedStartTime: '2024-01-12T13:00:09+01:00',
            realtime: true,
            distance: 3475.48,
            duration: 420,
            fromPlace: {
              name: 'Slependen',
            },
            toPlace: {
              name: 'Sandvika bussterminal',
            },
            toEstimatedCall: {
              destinationDisplay: {
                frontText: 'Sandvika',
              },
            },
            line: {
              publicCode: '265',
              name: 'Sandvika - Nesøya - Sandvika',
            },
            authority: {
              name: 'Ruter',
            },
            pointsOnLink: {
              points:
                'yzmlJelb_A[a@QUo@y@WYIKIKQUGKCCEIIOQ]GKEKAGCEAEAICQMi@@I@MAOCKGIK_@E[AYCY?[?_@Dc@Di@LaAJs@B_@BYBY@k@AgAAa@Gi@CUEWG_@EOCMe@{ASq@GUGUMk@EUCSKs@G_@Ek@K_BGiAGg@G_@E[IYEWISO_@KQGKIIMQMKQOWSYSe@]WQOQIKQSOSU]Yg@QYOW??S[]m@y@wAq@mAa@s@Ua@Q_@MUQ[Wo@Yo@M[Ug@Q[CGCCCECEOQOSCKYWMIECCAKEMAIAOAS?I?KBM@]LWLCHe@PODMDQDG@G?K@M?I?IAMAMAKEKCICIE[MCKYOIEECCC?IAGAGCECECCA?C?A?EKCICGCICKIWEKCIUa@??aEaI[k@S]EKEKCIAGGMGK?GAOCMGKGEG?GBGHCHAJAJKLML}@jAe@\\EMGGGAEk@AS?S@QBOFO~AoDzAoDz@wBP]HBHCFIDM@Q?QAOEMGIICIBOOOSSQQ_@Q_@???AO[O]o@eBWu@Ws@k@iBe@aBMYEIIMGEEAEAG@GDIFUV??UZKHSHKBO?SCQGKGKMOQKYK_@E]C[ASA[A}BB[@_@@]?M?MAKEKEECAE?EBCFCFALMPU\\WLUPYV[\\e@l@s@lAq@nAmAnBY\\_@Zc@^[V]@WJCCEAE?C@EFCHCL?F?D@F@F@FBFBBD@D?PX\\l@b@nBRDFDFNd@hB^xALb@d@nBBL?LALCJCFEBE@E?EGEIi@_CI_@[kA]uA',
            },
          },
          {
            id: null,
            mode: 'foot',
            aimedStartTime: '2024-01-12T13:07:09+01:00',
            aimedEndTime: '2024-01-12T13:09:59+01:00',
            expectedEndTime: '2024-01-12T13:09:59+01:00',
            expectedStartTime: '2024-01-12T13:07:09+01:00',
            realtime: false,
            distance: 189.05,
            duration: 170,
            fromPlace: {
              name: 'Sandvika bussterminal',
            },
            toPlace: {
              name: 'Sandvika stasjon',
            },
            toEstimatedCall: null,
            line: null,
            authority: null,
            pointsOnLink: {
              points: 'm{plJ}uf_A??nAbD@DB?@DH?L?LOVYS_As@_E@?',
            },
          },
          {
            id: 'rO0ABXeDABhTQ0hFRFVMRURfVFJBTlNJVF9MRUdfVjMAAAAKMjAyNC0wMS0xMgAAAAEAAAAEAA9SQjpOU1I6UXVheTo5OTMAD1JCOk5TUjpRdWF5OjQ3NwAvUkI6TlNCOkRhdGVkU2VydmljZUpvdXJuZXk6MzE5X0FTUi1MSE1fMjQtMDEtMTI=',
            mode: 'rail',
            aimedStartTime: '2024-01-12T13:15:00+01:00',
            aimedEndTime: '2024-01-12T13:27:00+01:00',
            expectedEndTime: '2024-01-12T13:27:10+01:00',
            expectedStartTime: '2024-01-12T13:15:00+01:00',
            realtime: true,
            distance: 12466.92,
            duration: 730,
            fromPlace: {
              name: 'Sandvika stasjon',
            },
            toPlace: {
              name: 'Nationaltheatret stasjon',
            },
            toEstimatedCall: {
              destinationDisplay: {
                frontText: 'Lillehammer',
              },
            },
            line: {
              publicCode: 'RE10',
              name: 'Drammen-Oslo S-Lillehammer',
            },
            authority: {
              name: 'Vy',
            },
            pointsOnLink: {
              points:
                '{xplJ{yf_AAEo@kC_@yA]uAo@aC_@wAYgAc@gBWiAWmAa@aBGYQu@q@uCe@oB??e@sB[yAUeAOs@UkA]cBQcAOw@O}@My@o@}Dk@sDuA{I{B{NqAoIe@yCq@{DcAoFs@aDq@iCs@mCw@eCi@cBs@qByAsD}DqI}CgGiAyB}@gB{@kBu@cBs@_By@qBu@kBm@_B{@cCo@kBs@wBq@uBcAiDq@aCe@iBe@kBg@qBe@oBk@iCe@wBc@wB_@sBY{Ae@iCm@qDi@oD_@eCi@}De@uDi@aFi@gF_@eEi@_Hq@wJeCy]k@iIqNarBc@yGWaFUqEMuCMeDIeDI}CGiCGkCCcCEwDGcGs@ax@EsGCwBAqB@_A?w@@o@B{@Bk@@o@Du@Do@Fw@Fq@Fs@Fm@Fi@Ju@Z{B\\iC`@{CJq@Js@NmAPsAHw@JeAHmALgBF}ABsABsA?G?????u@?_AAuAAy@Ai@Ac@Cq@A_@Ci@Ck@Ei@G{@IgAOgBOqB[}D??YcDEo@KiAKcAIs@Kq@MaAQgASoAO{@SeAWkAc@qBm@eC[iAQs@Oo@Om@Mm@Kg@Mo@Ko@Ku@Ks@Iu@I}@I_AGeAEw@GiAEcAIyBEs@Eq@GcAIy@Ek@Iw@Ii@Im@Ig@O{@Km@S_AUaAU}@[aAY{@Ww@u@yBQi@Wo@k@cBe@wAYy@Y{@??a@sAOg@uA_F_@oAYeAi@mBi@iBi@gBe@wA[}@qDoKc@qAY_Aa@sASw@Qu@Oo@Om@Ms@O{@Mq@Ii@MaAKw@Gk@Ee@C]Ee@Cc@Ci@Co@Cs@?[Aa@??AaA?o@?i@@]@q@@m@D}@F{@H}@Hy@Ju@PeARmA~@cFLo@????BMp@wD^uBN}@PcARmARwAZ_CPuAPiAPcARgAVuATmAHa@Lq@Lo@ReALo@Lm@??VsANu@RgAJm@Jq@NaANmAR_BN_BNeBL_BHqADeAD}@DgABu@B{@@_A@y@@y@@cA?iAA{AAeAA_AAy@C_ACy@C_AMsDKkCGiB??KyCKiDE_BC{AAkA?eAAeA@oA@kA@gABwADoAD_BH{AFaAFiAHgAFaAl@}Jn@}JPoCLuBPeCLuALsALoALkANkATaBn@oE|@_GfAqH@KVcBNaAP}@f@kCTmAVsA??XyAb@aCLo@NcALw@Ho@Hq@Hw@HgAHaADu@Dw@H}Ab@_K??',
            },
          },
          {
            id: null,
            mode: 'foot',
            aimedStartTime: '2024-01-12T13:27:10+01:00',
            aimedEndTime: '2024-01-12T13:32:00+01:00',
            expectedEndTime: '2024-01-12T13:32:00+01:00',
            expectedStartTime: '2024-01-12T13:27:10+01:00',
            realtime: false,
            distance: 326.35,
            duration: 290,
            fromPlace: {
              name: 'Nationaltheatret stasjon',
            },
            toPlace: {
              name: 'Nationaltheatret',
            },
            toEstimatedCall: null,
            line: null,
            authority: null,
            pointsOnLink: {
              points: '_fulJefn`A??l@yJNqD@OCSCWWeCCQKcAEOEQ]f@SCt@cB??',
            },
          },
          {
            id: 'rO0ABXeSABhTQ0hFRFVMRURfVFJBTlNJVF9MRUdfVjMAAAAKMjAyNC0wMS0xMgAAAAsAAAARABBSQjpOU1I6UXVheTo3MzMzABFSQjpOU1I6UXVheToxMTEzNAA7UkI6UlVUOkRhdGVkU2VydmljZUpvdXJuZXk6NzQzMTQyMWUzM2FlNDU2YmMzOTQ5OWEyMTAyN2M2Mzg=',
            mode: 'metro',
            aimedStartTime: '2024-01-12T13:34:00+01:00',
            aimedEndTime: '2024-01-12T13:44:00+01:00',
            expectedEndTime: '2024-01-12T13:44:00+01:00',
            expectedStartTime: '2024-01-12T13:34:00+01:00',
            realtime: true,
            distance: 4410.74,
            duration: 600,
            fromPlace: {
              name: 'Nationaltheatret',
            },
            toPlace: {
              name: 'Helsfyr',
            },
            toEstimatedCall: {
              destinationDisplay: {
                frontText: 'Ellingsrudåsen',
              },
            },
            line: {
              publicCode: '2',
              name: 'Østerås - Ellingsrudåsen',
            },
            authority: {
              name: 'Ruter',
            },
            pointsOnLink: {
              points:
                'seulJ{co`AHMTm@FKBa@@KJu@NuAL{@Jw@Lu@Ls@Hk@Lo@?AVyAf@oCLs@Lw@Ho@Js@Fs@Hu@HwADaADkAD}@?WP{EDo@Fm@LiELeD??By@NmEBy@Du@Bk@JgAJcAL_APeAX{A\\aBd@iCL_AJ{@H{@Bi@@g@@e@FgCBcA?OHuD??@[JyE@uA@oAAiAGaAGy@Iy@M{@{@sEOeAIo@IiAEkAAeA?qAH}H??@i@D}DDqC@y@@e@B{@Be@H_ArAeMR}BFuA@yAEwAIsAO{ASqAWgAa@mAe@_Ag@m@mByB]k@[q@Yy@YkASkAQsAYkCKy@Ko@Ms@Ka@Me@Ma@O_@O_@Sa@Yi@uAeCa@u@??w@yAQc@Oa@K[K_@Ma@Mg@g@uBOk@Oi@Mc@Oa@O[Q[QYEc@Cs@Am@Am@@o@@g@@g@Bi@Di@De@Fc@Fc@Hg@Nm@Ja@La@N_@N]R_@RYPYVUTSRMPIPIl@WRITKNKNMLKNQPSRYLULUHSJUHUJYHUNc@Lg@Pk@Lg@Lg@P{@Ha@n@{CH]R}@??`@mBH_@Lq@DYDYD]Da@B]Dg@@_@Bi@@g@?c@?c@Ae@A_@Ck@Eo@?AWwCEq@Eq@Cu@Cw@Cc@Ao@?m@?k@?q@?y@@k@B{@j@oOD}@FgAHw@LuANgAn@uFHw@',
            },
          },
          {
            id: null,
            mode: 'foot',
            aimedStartTime: '2024-01-12T13:44:00+01:00',
            aimedEndTime: '2024-01-12T13:44:32+01:00',
            expectedEndTime: '2024-01-12T13:44:32+01:00',
            expectedStartTime: '2024-01-12T13:44:00+01:00',
            realtime: false,
            distance: 33.93,
            duration: 32,
            fromPlace: {
              name: 'Helsfyr',
            },
            toPlace: {
              name: 'Helsfyr T',
            },
            toEstimatedCall: null,
            line: null,
            authority: null,
            pointsOnLink: {
              points: '_utlJws|`ABBAb@i@G??',
            },
          },
          {
            id: 'rO0ABXeTABhTQ0hFRFVMRURfVFJBTlNJVF9MRUdfVjMAAAAKMjAyNC0wMS0xMgAAAAYAAAANABFSQjpOU1I6UXVheToxMTE0MgARUkI6TlNSOlF1YXk6MTE0NDIAO1JCOlJVVDpEYXRlZFNlcnZpY2VKb3VybmV5OjZlMjFkOWY5OGIyYmQ4MDg4YjlhN2M4ZjUzNGExOTg2',
            mode: 'bus',
            aimedStartTime: '2024-01-12T13:49:00+01:00',
            aimedEndTime: '2024-01-12T13:59:00+01:00',
            expectedEndTime: '2024-01-12T13:59:00+01:00',
            expectedStartTime: '2024-01-12T13:49:00+01:00',
            realtime: false,
            distance: 3925.06,
            duration: 600,
            fromPlace: {
              name: 'Helsfyr T',
            },
            toPlace: {
              name: 'Alfaset',
            },
            toEstimatedCall: {
              destinationDisplay: {
                frontText: 'Kjeller',
              },
            },
            line: {
              publicCode: '100',
              name: 'Kjeller - Oslo bussterminal',
            },
            authority: {
              name: 'Ruter',
            },
            pointsOnLink: {
              points:
                'wvtlJsr|`AAIGg@M_AIc@K]GUMc@Uo@q@gBo@yA{B_G_BeEeCyGiBaFeAqC]_Ae@qAY_AMa@??IWSu@Mi@Ke@Mk@Oq@Ki@[iB[cBk@iDqAyHc@eCMs@??Ii@i@wCKi@Ke@COMk@Ou@YsAk@oCoAcGBUGk@Ce@Ao@Ao@AWA[AQC]CWIe@COOgAMo@OaAGWcA}FCMUgA??AESaASu@WaA[gAUu@]cAWy@W{@K]wAsEIYKWMa@Qo@K_@I[CIAMCKAQ@M?Q@Y@QA]KKEGEEEIEOM_@AOK_AKaAIw@Ek@??CSGgAEy@Cs@AaAAs@Ay@As@?q@@yA@gDBsD@wD@_C?oDByD?_@@iB@gB@i@@m@@[@Y?EHg@@U@A@E@E?EAEAEACAAA?A?IIECGCOBG?C?[@[BYB_@FSDy@J??C@OBSEE?KAI?AEACCEEACAC@A@KIGKEGUa@KYQ_@o@oAeAuBiAyBS[MW]o@Wc@[c@[c@i@q@EG??c@i@[_@[]s@w@GIIIYc@EIGKIMGMAAAKAGCEEEEAC@A@KIGEEGQOYk@]s@[w@Um@Sk@IYIUESIYEWGYEYEWE_@Ge@Gk@Ec@C]C_@C_@A]GcBC_@A[Ca@EWCWE]G[G]Qy@Qy@e@yBS_AMq@GSCOEQCMKe@S_AAC',
            },
          },
          {
            id: null,
            mode: 'foot',
            aimedStartTime: '2024-01-12T13:59:00+01:00',
            aimedEndTime: '2024-01-12T14:14:09+01:00',
            expectedEndTime: '2024-01-12T14:14:09+01:00',
            expectedStartTime: '2024-01-12T13:59:00+01:00',
            realtime: false,
            distance: 1074.54,
            duration: 909,
            fromPlace: {
              name: 'Alfaset',
            },
            toPlace: {
              name: 'Destination',
            },
            toEstimatedCall: null,
            line: null,
            authority: null,
            pointsOnLink: {
              points:
                'egxlJeigaA@A?BRz@DZA@ILEDA@}@aEAECIACCGAE?IUiA?CAEMaA_Br@KBI?GBGHKNW^w@jA]f@ORSRQLMHMBAbD@fA@`ADtADv@s@?ODm@l@SPGJENGNEPI\\GGC?C?ORGZHLCJ?BV\\BDBH@J@NANAJEJEJUf@IN@@DLcApBmA`CIPKREREX@XBXSm@}@iCaAmCgAaE',
            },
          },
        ],
        systemNotices: [],
      },
      {
        aimedStartTime: '2024-01-12T12:56:02+01:00',
        aimedEndTime: '2024-01-12T14:17:28+01:00',
        expectedEndTime: '2024-01-12T14:17:28+01:00',
        expectedStartTime: '2024-01-12T12:56:02+01:00',
        duration: 4886,
        distance: 28471.820000000003,
        legs: [
          {
            id: null,
            mode: 'foot',
            aimedStartTime: '2024-01-12T12:56:02+01:00',
            aimedEndTime: '2024-01-12T13:00:09+01:00',
            expectedEndTime: '2024-01-12T13:00:09+01:00',
            expectedStartTime: '2024-01-12T12:56:02+01:00',
            realtime: false,
            distance: 291.84,
            duration: 247,
            fromPlace: {
              name: 'Origin',
            },
            toPlace: {
              name: 'Slependen',
            },
            toEstimatedCall: null,
            line: null,
            authority: null,
            pointsOnLink: {
              points: 'o`nlJaac_AZ|@\\z@On@MAk@~@UZ@J@HEX@RLv@@DJVVd@LRNR\\^p@t@|@fA??',
            },
          },
          {
            id: 'rO0ABXeRABhTQ0hFRFVMRURfVFJBTlNJVF9MRUdfVjMAAAAKMjAyNC0wMS0xMgAAABkAAAAeABBSQjpOU1I6UXVheTo3OTI4ABBSQjpOU1I6UXVheTo3MjYzADtSQjpSVVQ6RGF0ZWRTZXJ2aWNlSm91cm5leToyOWIyODMyMDdlZDg3YmRmZjJlYWM4ODdiMTNlZGE2OA==',
            mode: 'bus',
            aimedStartTime: '2024-01-12T13:00:00+01:00',
            aimedEndTime: '2024-01-12T13:07:00+01:00',
            expectedEndTime: '2024-01-12T13:07:09+01:00',
            expectedStartTime: '2024-01-12T13:00:09+01:00',
            realtime: true,
            distance: 3475.48,
            duration: 420,
            fromPlace: {
              name: 'Slependen',
            },
            toPlace: {
              name: 'Sandvika bussterminal',
            },
            toEstimatedCall: {
              destinationDisplay: {
                frontText: 'Sandvika',
              },
            },
            line: {
              publicCode: '265',
              name: 'Sandvika - Nesøya - Sandvika',
            },
            authority: {
              name: 'Ruter',
            },
            pointsOnLink: {
              points:
                'yzmlJelb_A[a@QUo@y@WYIKIKQUGKCCEIIOQ]GKEKAGCEAEAICQMi@@I@MAOCKGIK_@E[AYCY?[?_@Dc@Di@LaAJs@B_@BYBY@k@AgAAa@Gi@CUEWG_@EOCMe@{ASq@GUGUMk@EUCSKs@G_@Ek@K_BGiAGg@G_@E[IYEWISO_@KQGKIIMQMKQOWSYSe@]WQOQIKQSOSU]Yg@QYOW??S[]m@y@wAq@mAa@s@Ua@Q_@MUQ[Wo@Yo@M[Ug@Q[CGCCCECEOQOSCKYWMIECCAKEMAIAOAS?I?KBM@]LWLCHe@PODMDQDG@G?K@M?I?IAMAMAKEKCICIE[MCKYOIEECCC?IAGAGCECECCA?C?A?EKCICGCICKIWEKCIUa@??aEaI[k@S]EKEKCIAGGMGK?GAOCMGKGEG?GBGHCHAJAJKLML}@jAe@\\EMGGGAEk@AS?S@QBOFO~AoDzAoDz@wBP]HBHCFIDM@Q?QAOEMGIICIBOOOSSQQ_@Q_@???AO[O]o@eBWu@Ws@k@iBe@aBMYEIIMGEEAEAG@GDIFUV??UZKHSHKBO?SCQGKGKMOQKYK_@E]C[ASA[A}BB[@_@@]?M?MAKEKEECAE?EBCFCFALMPU\\WLUPYV[\\e@l@s@lAq@nAmAnBY\\_@Zc@^[V]@WJCCEAE?C@EFCHCL?F?D@F@F@FBFBBD@D?PX\\l@b@nBRDFDFNd@hB^xALb@d@nBBL?LALCJCFEBE@E?EGEIi@_CI_@[kA]uA',
            },
          },
          {
            id: null,
            mode: 'foot',
            aimedStartTime: '2024-01-12T13:07:09+01:00',
            aimedEndTime: '2024-01-12T13:09:59+01:00',
            expectedEndTime: '2024-01-12T13:09:59+01:00',
            expectedStartTime: '2024-01-12T13:07:09+01:00',
            realtime: false,
            distance: 189.05,
            duration: 170,
            fromPlace: {
              name: 'Sandvika bussterminal',
            },
            toPlace: {
              name: 'Sandvika stasjon',
            },
            toEstimatedCall: null,
            line: null,
            authority: null,
            pointsOnLink: {
              points: 'm{plJ}uf_A??nAbD@DB?@DH?L?LOVYS_As@_E@?',
            },
          },
          {
            id: 'rO0ABXeDABhTQ0hFRFVMRURfVFJBTlNJVF9MRUdfVjMAAAAKMjAyNC0wMS0xMgAAAAEAAAAFAA9SQjpOU1I6UXVheTo5OTMAD1JCOk5TUjpRdWF5OjU3MQAvUkI6TlNCOkRhdGVkU2VydmljZUpvdXJuZXk6MzE5X0FTUi1MSE1fMjQtMDEtMTI=',
            mode: 'rail',
            aimedStartTime: '2024-01-12T13:15:00+01:00',
            aimedEndTime: '2024-01-12T13:31:00+01:00',
            expectedEndTime: '2024-01-12T13:31:00+01:00',
            expectedStartTime: '2024-01-12T13:15:00+01:00',
            realtime: true,
            distance: 14113.62,
            duration: 960,
            fromPlace: {
              name: 'Sandvika stasjon',
            },
            toPlace: {
              name: 'Oslo S',
            },
            toEstimatedCall: {
              destinationDisplay: {
                frontText: 'Lillehammer',
              },
            },
            line: {
              publicCode: 'RE10',
              name: 'Drammen-Oslo S-Lillehammer',
            },
            authority: {
              name: 'Vy',
            },
            pointsOnLink: {
              points:
                '{xplJ{yf_AAEo@kC_@yA]uAo@aC_@wAYgAc@gBWiAWmAa@aBGYQu@q@uCe@oB??e@sB[yAUeAOs@UkA]cBQcAOw@O}@My@o@}Dk@sDuA{I{B{NqAoIe@yCq@{DcAoFs@aDq@iCs@mCw@eCi@cBs@qByAsD}DqI}CgGiAyB}@gB{@kBu@cBs@_By@qBu@kBm@_B{@cCo@kBs@wBq@uBcAiDq@aCe@iBe@kBg@qBe@oBk@iCe@wBc@wB_@sBY{Ae@iCm@qDi@oD_@eCi@}De@uDi@aFi@gF_@eEi@_Hq@wJeCy]k@iIqNarBc@yGWaFUqEMuCMeDIeDI}CGiCGkCCcCEwDGcGs@ax@EsGCwBAqB@_A?w@@o@B{@Bk@@o@Du@Do@Fw@Fq@Fs@Fm@Fi@Ju@Z{B\\iC`@{CJq@Js@NmAPsAHw@JeAHmALgBF}ABsABsA?G?????u@?_AAuAAy@Ai@Ac@Cq@A_@Ci@Ck@Ei@G{@IgAOgBOqB[}D??YcDEo@KiAKcAIs@Kq@MaAQgASoAO{@SeAWkAc@qBm@eC[iAQs@Oo@Om@Mm@Kg@Mo@Ko@Ku@Ks@Iu@I}@I_AGeAEw@GiAEcAIyBEs@Eq@GcAIy@Ek@Iw@Ii@Im@Ig@O{@Km@S_AUaAU}@[aAY{@Ww@u@yBQi@Wo@k@cBe@wAYy@Y{@??a@sAOg@uA_F_@oAYeAi@mBi@iBi@gBe@wA[}@qDoKc@qAY_Aa@sASw@Qu@Oo@Om@Ms@O{@Mq@Ii@MaAKw@Gk@Ee@C]Ee@Cc@Ci@Co@Cs@?[Aa@??AaA?o@?i@@]@q@@m@D}@F{@H}@Hy@Ju@PeARmA~@cFLo@????BMp@wD^uBN}@PcARmARwAZ_CPuAPiAPcARgAVuATmAHa@Lq@Lo@ReALo@Lm@??VsANu@RgAJm@Jq@NaANmAR_BN_BNeBL_BHqADeAD}@DgABu@B{@@_A@y@@y@@cA?iAA{AAeAA_AAy@C_ACy@C_AMsDKkCGiB??KyCKiDE_BC{AAkA?eAAeA@oA@kA@gABwADoAD_BH{AFaAFiAHgAFaAl@}Jn@}JPoCLuBPeCLuALsALoALkANkATaBn@oE|@_GfAqH@KVcBNaAP}@f@kCTmAVsA??XyAb@aCLo@NcALw@Ho@Hq@Hw@HgAHaADu@Dw@H}Ab@_K????d@gKF_BBs@Dw@DaBBaABs@Bm@Bm@@_@B[B_@Bg@Do@B_@D]Fu@??Fs@Fq@J_AJw@Fk@Hk@Jo@Jq@F_@Lu@Nw@??ZcBPeA\\gBN_ANu@Lu@L{@Jm@Ly@L_ANkAHw@Hs@H_AL}AHqAHkAFsAF{ADoABmAHgDDuABgADcAFy@F_AF}@H_AH{@LqAJ{@??@[B]@]??F[DUFa@Fe@Hs@He@L{@Ny@??P_AJg@Lk@Lk@Rs@Po@J_@H[J]Nk@Lo@Pq@Jm@Lm@Lw@??^iCf@_D??',
            },
          },
          {
            id: null,
            mode: 'foot',
            aimedStartTime: '2024-01-12T13:31:00+01:00',
            aimedEndTime: '2024-01-12T13:38:59+01:00',
            expectedEndTime: '2024-01-12T13:38:59+01:00',
            expectedStartTime: '2024-01-12T13:31:00+01:00',
            realtime: false,
            distance: 436.66,
            duration: 479,
            fromPlace: {
              name: 'Oslo S',
            },
            toPlace: {
              name: 'Bjørvika',
            },
            toEstimatedCall: null,
            line: null,
            authority: null,
            pointsOnLink: {
              points:
                '_gtlJuqs`A??FHER?BCH\\ZZXZXXXBG?EDSb@{B@@@OB@D?F?B?b@yCBMDSXqALFP@BAD?DCDEBGBKBKBIB?@?NJHHLNHH@@@@?@?@BB?BAFADNJp@`@NJ@K?CFc@?G??CC?A?A@A@A?A?ACC????AAAAFc@FYF]AA',
            },
          },
          {
            id: 'rO0ABXeSABhTQ0hFRFVMRURfVFJBTlNJVF9MRUdfVjMAAAAKMjAyNC0wMS0xMgAAAAEAAAAFABBSQjpOU1I6UXVheTo3MTY5ABFSQjpOU1I6UXVheToxMDQwMgA7UkI6UlVUOkRhdGVkU2VydmljZUpvdXJuZXk6YzA3MTEwYjI3YjM5N2VmZmUwMTRjZDgxYTEzNzhhODI=',
            mode: 'bus',
            aimedStartTime: '2024-01-12T13:43:00+01:00',
            aimedEndTime: '2024-01-12T13:54:00+01:00',
            expectedEndTime: '2024-01-12T13:54:00+01:00',
            expectedStartTime: '2024-01-12T13:43:00+01:00',
            realtime: false,
            distance: 8270.2,
            duration: 660,
            fromPlace: {
              name: 'Bjørvika',
            },
            toPlace: {
              name: 'Trosterud',
            },
            toEstimatedCall: {
              destinationDisplay: {
                frontText: 'Blystadlia',
              },
            },
            line: {
              publicCode: '300',
              name: 'Blystadlia - Ahus - Oslo bussterminal',
            },
            authority: {
              name: 'Ruter',
            },
            pointsOnLink: {
              points:
                'wwslJe~s`ADU`@oCF[`@mCRuAFa@Hg@XeBPiAFc@Fc@ZsBPgAHk@PJr@`@lHhEJFxEnCLHPHPJNFNDHBH@PBP@ZCPCNEPEFANGPIRKZQv@a@pCyAVMHCDABAB?BAD?DFDBD@DADCDGBK@KXQ\\ID?Z?VGBGDI\\s@b@gAl@oB^yAXwARkAPuAL_AJaALwBHwBBmBAoBEuCKmBWcDMoAKqA}BeToAsLg@mEi@wFm@iF[wBi@cCi@sBg@yAe@eAKQk@cAeDyD[]IKSU_BgBs@y@{@cAe@k@k@m@a@e@a@g@uAkB]e@_@m@a@w@Wq@So@WaAM{@QkAM{AKeBMwCQiGGuBG_BKyAMiAOw@Os@Ws@[q@_@i@]]]UYMWEYCW?UDSDUJSLULWT}AtA[VWPYLUFG@]DUCMCMG[SWWSYUa@Qc@Uw@WcAe@qBeD_OS{ASgAOiAEy@Ca@?U?O@I@G?IAKCICECCCAA?C?CBEIKUKc@COG[Ge@OkAEU??AIGg@M_AIc@K]GUMc@Uo@q@gBo@yA{B_G_BeEeCyGiBaFeAqC]_Ae@qAY_AMa@??IWSu@Mi@Ke@Mk@Oq@Ki@[iB[cBk@iDqAyHc@eCMs@??Ii@i@wCKi@Ke@COMk@Ou@YsAk@oCoAcGo@{CWiAYiAcAmDqBeHgBiGqBcHc@cBc@eBUgASiAQoAIi@Gi@ScCKaBKmBMuBCw@CiB?{BAiBAuA?yA?sA@yB@gC@{D?{A?{A?qA?sAAuAAqACuACqAEwAEkACiAGmAGqAI{AKkBKsAKoAKqAMmAMqAGm@K{@MgAM{@UeBWaBSsAc@oCa@kCSwAYsBQqAQ}AQ{AMqAMkAQuBm@yGOcB]cDMoAYwBWmBc@mC_@qB_@mBi@sDSiAe@}BOu@Oq@YeAOk@Qg@Wu@Oe@Q[M[Wk@We@EI',
            },
          },
          {
            id: null,
            mode: 'foot',
            aimedStartTime: '2024-01-12T13:54:00+01:00',
            aimedEndTime: '2024-01-12T14:17:28+01:00',
            expectedEndTime: '2024-01-12T14:17:28+01:00',
            expectedStartTime: '2024-01-12T13:54:00+01:00',
            realtime: false,
            distance: 1694.97,
            duration: 1408,
            fromPlace: {
              name: 'Trosterud',
            },
            toPlace: {
              name: 'Destination',
            },
            toEstimatedCall: null,
            line: null,
            authority: null,
            pointsOnLink: {
              points:
                'sywlJw}haA?A?ECOAM@K?G@E@I@EBCBEFCPEBABCBC@E@G@K@YBSBSDKJQIMCCCCCAE?G@k@JG?IAKESMGCK?I?GDGFGLYt@_@|@Ul@c@bAOZCFAFAFAFAPAVCZAF?HCNGAI@IFIHILKTUh@iA`Cc@dAw@rBQd@M^Od@Mh@Mh@Mb@GPGNINGDI@i@b@YPC@OHQFUHUHu@TBz@BlALADAD?DBFBJHHPCHIx@ABK`AIh@CNIb@Ir@Y~CO~BAbD@fA@`ADtADv@s@?ODm@l@SPGJENGNEPI\\GGC?C?ORGZHLCJ?BV\\BDBH@J@NANAJEJEJUf@IN@@DLcApBmA`CIPKREREX@XBXSm@}@iCaAmCgAaE',
            },
          },
        ],
        systemNotices: [],
      },
      {
        aimedStartTime: '2024-01-12T12:41:00+01:00',
        aimedEndTime: '2024-01-12T14:18:50+01:00',
        expectedEndTime: '2024-01-12T14:18:50+01:00',
        expectedStartTime: '2024-01-12T12:41:00+01:00',
        duration: 5870,
        distance: 35115.93,
        legs: [
          {
            id: null,
            mode: 'foot',
            aimedStartTime: '2024-01-12T12:41:00+01:00',
            aimedEndTime: '2024-01-12T12:45:07+01:00',
            expectedEndTime: '2024-01-12T12:45:07+01:00',
            expectedStartTime: '2024-01-12T12:41:00+01:00',
            realtime: false,
            distance: 291.84,
            duration: 247,
            fromPlace: {
              name: 'Origin',
            },
            toPlace: {
              name: 'Slependen',
            },
            toEstimatedCall: null,
            line: null,
            authority: null,
            pointsOnLink: {
              points: 'o`nlJaac_AZ|@\\z@On@MAk@~@UZ@J@HEX@RLv@@DJVVd@LRNR\\^p@t@|@fA??',
            },
          },
          {
            id: 'rO0ABXeRABhTQ0hFRFVMRURfVFJBTlNJVF9MRUdfVjMAAAAKMjAyNC0wMS0xMgAAAA8AAAAUABBSQjpOU1I6UXVheTo3OTI4ABBSQjpOU1I6UXVheTo3MjY1ADtSQjpSVVQ6RGF0ZWRTZXJ2aWNlSm91cm5leTpiYzAwOGYwN2YxYzRhMDQ1YzE5ZjEwY2Q3MTQyZDliMQ==',
            mode: 'bus',
            aimedStartTime: '2024-01-12T12:43:00+01:00',
            aimedEndTime: '2024-01-12T12:48:00+01:00',
            expectedEndTime: '2024-01-12T12:50:07+01:00',
            expectedStartTime: '2024-01-12T12:45:07+01:00',
            realtime: true,
            distance: 3232.21,
            duration: 300,
            fromPlace: {
              name: 'Slependen',
            },
            toPlace: {
              name: 'Sandvika bussterminal',
            },
            toEstimatedCall: {
              destinationDisplay: {
                frontText: 'Sandvika',
              },
            },
            line: {
              publicCode: '270',
              name: 'Asker - Sandvika (- Fornebu)',
            },
            authority: {
              name: 'Ruter',
            },
            pointsOnLink: {
              points:
                'yzmlJelb_A[a@QUo@y@WYIKIKQUGKCCEIIOQ]GKEKAGCEAEAICQMi@@I@MAOCKGIK_@E[AYCY?[?_@Dc@Di@LaAJs@B_@BYBY@k@AgAAa@Gi@CUEWG_@EOCMe@{ASq@GUGUMk@EUCSKs@G_@Ek@K_BGiAGg@G_@E[IYEWISO_@KQGKIIMQMKQOWSYSe@]WQOQIKQSOSU]Yg@QYOW??S[]m@y@wAq@mAa@s@Ua@Q_@MUQ[Wo@Yo@M[Ug@Q[CGCCCECEOQOSCKYWMIECCAKEMAIAOAS?I?KBM@]LWLCHe@PODMDQDG@G?K@M?I?IAMAMAKEKCICIE[MCKYOIEECCC?IAGAGCECECCA?C?A?EKCICGCICKIWEKCIUa@??aEaI[k@S]EKEKCIAGGMGK?GAOCMGKGEG?GBGHCHAJAJKLML}@jAe@\\EMGGGAMDEJEPAPBV[jAYzA]nB]bBs@lCYv@E@EBEHCJCL?L@F?D@DEVCNEP{@jCm@|A??O\\MZKRIRCDKGMKOQ?A?AAAAC?AAECUCUAM?A??C_@AIASCY]}E?MC]AKEm@AOCa@ASAKAUCUCQCQAMAKIk@_@cCMy@Eg@?C@C?GAEAEAAEOM_AU{ASuAKi@K_@IUUUMOI?{@aBWi@M_@I[GSK]Qw@UcAYuAQy@IYIUKUGq@EY@MLWnA_BNQBBD@D?PX\\l@b@nBRDFDFNd@hB^xABJ',
            },
          },
          {
            id: null,
            mode: 'foot',
            aimedStartTime: '2024-01-12T12:50:07+01:00',
            aimedEndTime: '2024-01-12T12:52:07+01:00',
            expectedEndTime: '2024-01-12T12:52:07+01:00',
            expectedStartTime: '2024-01-12T12:50:07+01:00',
            realtime: false,
            distance: 125.59,
            duration: 120,
            fromPlace: {
              name: 'Sandvika bussterminal',
            },
            toPlace: {
              name: 'Sandvika stasjon',
            },
            toEstimatedCall: null,
            line: null,
            authority: null,
            pointsOnLink: {
              points: '}xplJ{pf_A?@@DB?@DH?L?LOVYS_A_AqD??',
            },
          },
          {
            id: 'rO0ABXeEABhTQ0hFRFVMRURfVFJBTlNJVF9MRUdfVjMAAAAKMjAyNC0wMS0xMgAAAAYAAAARAA9SQjpOU1I6UXVheTo5OTAAD1JCOk5TUjpRdWF5OjYwNAAwUkI6TlNCOkRhdGVkU2VydmljZUpvdXJuZXk6MjEzNF9BU1ItTExTXzI0LTAxLTEy',
            mode: 'rail',
            aimedStartTime: '2024-01-12T13:02:00+01:00',
            aimedEndTime: '2024-01-12T13:37:00+01:00',
            expectedEndTime: '2024-01-12T13:37:20+01:00',
            expectedStartTime: '2024-01-12T13:02:00+01:00',
            realtime: true,
            distance: 24463.5,
            duration: 2120,
            fromPlace: {
              name: 'Sandvika stasjon',
            },
            toPlace: {
              name: 'Grorud stasjon',
            },
            toEstimatedCall: {
              destinationDisplay: {
                frontText: 'Lillestrøm',
              },
            },
            line: {
              publicCode: 'L1',
              name: 'Spikkestad-Oslo S-Lillestrøm',
            },
            authority: {
              name: 'Vy',
            },
            pointsOnLink: {
              points:
                'qyplJcyf_AkBeIi@_C]yAYmAc@kBWgAYoAaBkH{@oDmAmFUcAu@_D_@}A??u@gDi@yBEWOq@YkAUaA{@iDc@eBU{@WcAOo@Mm@Mq@Kk@Ig@Ii@Im@Io@I{@I_AEs@Es@Ew@CeACgAAy@?s@@w@@y@@o@By@B}@F}@F}@F{@Hy@J{@Ju@RqAz@iFRkAJm@Js@De@Fc@De@Dc@Dm@Fo@Bo@Ds@@k@Bk@@m@@k@?i@?m@?i@Ak@Au@Cq@Cq@Cu@Es@Gw@AI????Iy@Gi@Gg@Gi@Ig@Ig@Ie@Kg@G]Ka@K]W_AQm@Qi@c@qAQk@M]Ka@K_@Ke@Mg@I_@Ki@Kk@Ik@Im@Gi@Gm@Gm@Ek@Cm@Eo@Cy@Cw@Aq@Am@?_A?q@?{@HeTFyO@i@F}L?_A?o@?o@Ai@Ak@Ck@Ag@Ce@Ck@Cg@Ek@Gi@Ei@Iq@M_AO_AIg@SuAO{@CUw@gFa@mCm@kEMy@mByM????q@mEk@}DQkAQgA]qBUuAWsAUuAIc@g@uCQgAQiAk@aE??a@wC]gCSoASoAMy@Q}@UoAQw@Qy@Sw@U{@W}@Sq@Og@Qk@Oc@Wq@Si@Uk@i@sAUk@Ys@Um@Qe@Oe@Sm@Om@Sq@Oi@Qu@Oo@Ou@Mq@Mo@Kq@Ms@Ks@K{@[{Bo@aFWkBSaBUgBMy@My@Ku@Ku@Mw@O_AO}@Ms@Q}@UkACK????ESOm@Qq@Mi@Og@_@mASs@Uw@a@wAUy@]kAs@cC]kAQs@]oAYeAWiAyA{F}AgGwAuFYmAUy@Sw@a@aB{AcGuAqFyA}F[kAWaAS{@Qq@U{@GYQq@[mAMe@I]Kc@Ka@Ig@Ke@Gc@Ii@Gg@Io@Ee@Gw@Ek@Cm@Ag@Ak@As@As@?s@@s@Bu@@m@Dm@Bq@Fu@Do@Fq@Fo@Fg@Hq@Z{B??JaAFk@Dg@??Jg@DWHc@Jm@??Js@Hq@RsAPsAJu@NiALgAHmAHoADkABeA@iA?E?????u@?aAAs@Cs@Cm@Ce@A_@Ce@Ei@IaAKgAG{@YmDK_AI}@Is@Iu@IcAYcDEo@KiAKcAIs@Kq@MaAQgASoAO{@SeAWkAc@qBm@eC[iAQs@Oo@Om@Mm@Kg@Mo@Ko@Ku@Ks@Iu@I}@I_AGeAEw@GiAEcAIyBEs@Eq@GcAIy@Ek@Iw@Ii@Im@Ig@O{@Km@S_AUaAU}@[aAY{@Ww@u@yBQi@Wo@k@cBe@wAYy@Y{@sAwD{@eCSm@??Sm@I_@Ka@IYMc@Si@??Mi@IYI[EU??Oc@GQMa@i@{AmEoMe@qA[}@Qo@Oc@Oo@Ka@Mk@Kg@Mk@G[E[M_A??Ig@Ic@CO??AOCYC]Kw@OsAE_@Ec@Eo@Ac@Cg@Ao@A}@A_A@cA?}@Bw@@o@??D}@Dy@H_AHy@H{@Hk@Ju@L}@x@cFJq@????BMDQj@eD|@kFX}AZgBXcBf@qCVuAN}@Nu@Nu@R{@XqA`@iBLo@Lm@??VsANu@RgAJm@Jq@NaANmAR_BN_BNeBL_BHqADeAD}@DgABu@B{@@_A@y@@y@@cA?iAA{AAeAA_AAy@C_ACy@C_AMsDKkCGiB??KyCKiDE_BC{AAkA?eAAeA@oA@kA@gABwADoAD_BH{AFaAFiAHgAFaAl@}Jn@}JPoCLuBPeCLuALsALoALkANkATaBn@oE|@_GfAqH@KVcBNaAP}@f@kCTmAVsA??XyAb@aCLo@NcALw@Ho@Hq@Hw@HgAHaADu@Dw@H}Ab@_K????d@gKF_BBs@Dw@DaBBaABs@Bm@Bm@@_@B[B_@Bg@Do@B_@D]Fu@??Fs@Fq@J_AJw@Fk@Hk@Jo@Jq@F_@Lu@Nw@??ZcBPeA\\gBN_ANu@Lu@L{@Jm@Ly@L_ANkAHw@Hs@H_AL}AHqAHkAFsAF{ADoABmAHgDDuABgADcAFy@F_AF}@H_AH{@LqAJ{@??@[B]@]??F[DUFa@Fe@Hs@He@L{@Ny@??RuALw@Jk@Jg@Ji@\\{AR}@Ns@N{@L_AVcB??^gCf@cD????p@qE|@eGh@iD??j@uDb@yCn@aE`@oCNaANgALiAJiADi@Fo@Dw@LwBFiA??Fm@Hu@PcBLsARoB??Z}CH}@`@wDXcDb@}DH_A??Dc@B_@Dq@@]Fk@H}@??Z{C??Ba@D_@@WB]@WDm@??TqBViCTgCL{ALyANiBLaBJqAHuAD_AHkADgADgADeAFqB@[Bu@BoA@y@BcA@mA@uA@}A?aA?_A?{A?}HAsF?mJA_F?qH?qBAsC?aB?oC?{BAoC?eH?mD?sD?gBAoB?kA?qB?qA?}@?k@Ak@?m@Aq@Ai@Ag@As@Cw@Cs@Cs@Ew@Eu@Ci@Gq@Eu@G{@KmAQmBYkDO_BOkBQuBw@aJScCQqBS_CS{BKwAOyAa@eFEe@??C_@C[Cq@AUCY??I_AGu@KmAGq@Gw@KaAIy@Go@Io@Im@ES????G_@Ko@Ke@[sAMg@Mk@Ok@Og@Qi@Sm@Oc@Oc@O]O_@Qa@Sa@Qa@k@kAQ_@u@{A??]o@Uc@S_@]m@Wc@OWEGYe@QW[c@W]QWSUMQSSUYOOo@o@c@a@[W[UWS]U[SUM_@Ue@Wq@][QYQYM_Ag@}BoAsEaCwC}AcAi@s@_@c@Wc@Uk@Yk@[u@a@w@a@cFkCyEgC??gCsA_B{@w@_@e@S]O]OWKWKSGsBs@SGWI[K[I[I_@GUEWE[C[C[C[AU?[@Y?W@a@BWD]DUBwATy@NoB\\a@F[Fa@F_@DO@I@W@[@Y?YAUAYCWE]G_@IYK]Ma@O_@Sa@Si@[MIQKu@c@yD}B????]S}D_Ci@]c@Y_@Wa@[_@Y[[_@[e@g@WYSUSYW]W]U_@Wa@Yc@Yg@Yi@Wi@Wi@Uk@Ug@Qe@Si@Um@Qi@Qk@So@Qo@Mc@Kc@EQKa@Mk@Sy@Qw@ScASaAOu@Qy@QaAO{@O_AOaAMy@MgAM_AKgAMkAIaAI_AIeAIcAImAIwAs@iL_BoXMuBK}AG{@Em@Gs@Gq@I_AIu@E_@Gi@Kw@Iq@Iq@M}@M}@Oy@QgAQcAk@yCWoA]gBI_@??u@}DmDwQcEkTMm@QaA_@iBSaAOw@CI????Kg@]aBQs@Qs@Oo@Oi@Ok@Mi@Sq@Qm@Sw@]iAgBeGe@_Be@aBW{@Qs@Qk@Ok@Mi@Kc@Mk@Qu@Mk@Ow@Mm@Kq@Kg@Mu@Kq@Ik@Ks@Ky@EWGe@YuBOeAKw@UuAEY??I]ESGSK_@EOK]Mc@GWOg@Kc@GQ??So@ESGQGYIUUo@??IQ[q@Q_@Sa@U_@w@oAY_@UYMQ??',
            },
          },
          {
            id: null,
            mode: 'foot',
            aimedStartTime: '2024-01-12T13:37:20+01:00',
            aimedEndTime: '2024-01-12T13:39:50+01:00',
            expectedEndTime: '2024-01-12T13:39:50+01:00',
            expectedStartTime: '2024-01-12T13:37:20+01:00',
            realtime: false,
            distance: 144.57,
            duration: 150,
            fromPlace: {
              name: 'Grorud stasjon',
            },
            toPlace: {
              name: 'Stjerneblokkveien',
            },
            toEstimatedCall: null,
            line: null,
            authority: null,
            pointsOnLink: {
              points: '}|{lJaomaA??c@m@Y[He@ACEEEEACFSHSLUR[V[DGBEBE@G@E@I?I?I?K?C??',
            },
          },
          {
            id: 'rO0ABXeTABhTQ0hFRFVMRURfVFJBTlNJVF9MRUdfVjMAAAAKMjAyNC0wMS0xMgAAADYAAAA7ABFSQjpOU1I6UXVheToxMDMyNgARUkI6TlNSOlF1YXk6MTA3MjcAO1JCOlJVVDpEYXRlZFNlcnZpY2VKb3VybmV5OjZiZjc4NDcxYThmNmMxZTA3YzUyZmM0MmU1NmM5YmI0',
            mode: 'bus',
            aimedStartTime: '2024-01-12T13:45:00+01:00',
            aimedEndTime: '2024-01-12T13:50:00+01:00',
            expectedEndTime: '2024-01-12T13:50:00+01:00',
            expectedStartTime: '2024-01-12T13:45:00+01:00',
            realtime: false,
            distance: 2078.2,
            duration: 300,
            fromPlace: {
              name: 'Stjerneblokkveien',
            },
            toPlace: {
              name: 'Grorud T',
            },
            toEstimatedCall: {
              destinationDisplay: {
                frontText: 'Grorud T',
              },
            },
            line: {
              publicCode: '79',
              name: 'Grorud T - Åsbråten',
            },
            authority: {
              name: 'Ruter',
            },
            pointsOnLink: {
              points:
                'w{{lJ_zmaA?FCd@@N?J@RA?KNKHKJKJQVMTGNGTERERc@dCKXEPGTINCBCDAFMZQZINQZKHMFQHQF??A?QDS?Y@gBFsBH]BO@G?O@K@A?m@B??M?e@BI@G@IDGFGJIPGPETGVEVY~AE\\APCR?N?T@TBJDLDNDJ`@v@NXDLHRJZJ\\d@lBH\\DZB^?V?ZAVEXEPIXGNIPKRKNORONKHGBCBMFSJMDQHIB??GDEBIFMJKJOTGFGLMVK\\CJAFEVEVC\\?JARGAA?E@MBMFLGLCDAH@?`@?v@E^CRADCHCFCDCBEDEBGFG?GCKGMIKKIKIKKSIQGQGWEOAGEWAUE]IsAG_AAUCYCUCOCKEKGGGGGGKEMAcACM?IAGCCAGEIGGKGMEOEQEUC[MaCC[CYEWESIQGMIIIEKCK?M@w@JOBI@G@o@FUB????C?WBi@BS?SGGAIAGAA?C?_@D[BS@S@W?S@MAG@I@I@mA?U??]Ag@?kA?a@M@QBK@I@BrB?V@R',
            },
          },
          {
            id: null,
            mode: 'foot',
            aimedStartTime: '2024-01-12T13:50:00+01:00',
            aimedEndTime: '2024-01-12T13:50:20+01:00',
            expectedEndTime: '2024-01-12T13:50:20+01:00',
            expectedStartTime: '2024-01-12T13:50:00+01:00',
            realtime: false,
            distance: 12.76,
            duration: 20,
            fromPlace: {
              name: 'Grorud T',
            },
            toPlace: {
              name: 'Grorud T',
            },
            toEstimatedCall: null,
            line: null,
            authority: null,
            pointsOnLink: {
              points: '}a~lJ_qlaA??CDC?A?C@A?C??E??',
            },
          },
          {
            id: 'rO0ABXeTABhTQ0hFRFVMRURfVFJBTlNJVF9MRUdfVjMAAAAKMjAyNC0wMS0xMgAAAAAAAAAIABFSQjpOU1I6UXVheToxMDcyNAARUkI6TlNSOlF1YXk6MTAyNTkAO1JCOlJVVDpEYXRlZFNlcnZpY2VKb3VybmV5OmVhYzk2YWEyZjg0ZmY1ZTdmNDEzYTk5YjRmY2RlMzBk',
            mode: 'bus',
            aimedStartTime: '2024-01-12T14:07:00+01:00',
            aimedEndTime: '2024-01-12T14:14:00+01:00',
            expectedEndTime: '2024-01-12T14:14:00+01:00',
            expectedStartTime: '2024-01-12T14:07:00+01:00',
            realtime: false,
            distance: 4457.24,
            duration: 420,
            fromPlace: {
              name: 'Grorud T',
            },
            toPlace: {
              name: 'Postnord',
            },
            toEstimatedCall: {
              destinationDisplay: {
                frontText: 'Helsfyr',
              },
            },
            line: {
              publicCode: '68',
              name: 'Helsfyr T - Grorud T via Alfaset',
            },
            authority: {
              name: 'Ruter',
            },
            pointsOnLink: {
              points:
                'mb~lJcplaA???TH?H?F?DAH?JHL@?W?]Ag@?kA?a@?_C?OAS?O?}@?O@K@IBEBG@IHEFALENCHAFAH?F?HBDDDFDFBHFZBJBTBb@NrHHlDF|BBx@D|@Dp@D~@HvAJzAFt@Fz@LrALpAPxARdBRxAVzAPbAN~@XtAR~@Pv@Pt@Nl@ZjATx@Ph@FPPjALp@BJ??Jd@HNFNd@nAJVNTRTRRb@\\b@t@^n@x@tAr@dApAnBhA`BT\\PVRXRZ^b@NN??XZb@\\`@ZVLX`@\\XTNd@Vt@\\\\L\\NRJPFVFR@R@TAZCTCH?HB@BBD@BDBD@DADCDG@E@E?E@EFCDC@?D?PBF@HCFAd@LRD??@?HBPNHBHBDBFBJBDBFBXDHFf@b@JLHHNPTXRZXd@`@r@^l@`@r@T`@PXPVTXNRVTZTVPTLZLXJVFVDVDz@JNBx@H^DRHH@l@B??J@X@RBH?LBHBT@`@TVN\\VVRXVTTLNNPX`@\\f@T`@Zn@Rd@lCnGR`@P^PZR\\PVJLNPNN??B@LLFDNF\\JXRFBFBFDDHDFLR?FBD@DBDD@DABC@C`@[TIH@ZErAL|@Jp@FlAHh@BH@RJHFHHJLDJBFFFD@FADCDEDKBKHIHGHCNMF?PARANANAPCd@Ib@Gh@K^Il@@PCjA[TG??`A[JALCH?RBBDBBBVBT@HBPDb@?TBbABlBFvBBf@Fr@Dh@Ht@Jr@Np@Lj@Nd@N`@Rb@Td@PZh@x@NP~@nAzAjBHLVZRVNVI\\Sd@Q^GN??KTcApBmA`CIPKREREX@XBXDPdAxCJ\\l@bB',
            },
          },
          {
            id: null,
            mode: 'foot',
            aimedStartTime: '2024-01-12T14:14:00+01:00',
            aimedEndTime: '2024-01-12T14:18:50+01:00',
            expectedEndTime: '2024-01-12T14:18:50+01:00',
            expectedStartTime: '2024-01-12T14:14:00+01:00',
            realtime: false,
            distance: 310.02,
            duration: 290,
            fromPlace: {
              name: 'Postnord',
            },
            toPlace: {
              name: 'Destination',
            },
            toEstimatedCall: null,
            line: null,
            authority: null,
            pointsOnLink: {
              points: 'q|xlJmweaACFA?m@aBBEDKK]eAyCEQSm@}@iCaAmCgAaE',
            },
          },
        ],
        systemNotices: [],
      },
    ],
  },
};

it('renders without crashing', () => {
  render(
    <ItineraryListContainer
      tripQueryResult={tripQueryResult as unknown as QueryType}
      selectedTripPatternIndexes={[0]}
      setSelectedTripPatternIndexes={() => {}}
      pageResults={() => {}}
      loading={false}
    />,
  );
});
