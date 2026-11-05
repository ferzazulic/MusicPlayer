# CarMusic – muzički player za Android head unit (Opel Astra H)

Offline player za muziku sa interne memorije / SD / USB, napravljen za Android multimediju u autu.

## Funkcije

- **Biblioteka**: pjesme, albumi, izvođači, folderi, red reprodukcije, pretraga, sortiranje
- **Now Playing** prilagođen autu (landscape, velike kontrole), animirani prelazi omota, ambijentalna pozadina iz boja omota, spektar animacija, tajmer za spavanje
- **Komande na volanu** preko Android MediaSession (sljedeća / prethodna / play-pause), dugi pritisak = premotavanje, opcionalno dupli pritisak = sljedeća, dijagnostika primljenih tastera
- **CID ekran (Astra H)** – info o pjesmi na gornjem ekranu:
  - *Head unit CAN-box* (zadano): standardni `com.android.music.metachanged` / `playstatechanged` broadcasti + MediaSession metadata, koje CAN-box dekoderi (Raise / Hiworld / Simple Soft…) prosljeđuju na fabrički CID
  - *USB CAN adapter* (eksperimentalno): direktno slanje teksta na MS-CAN (95,2 kbit/s, OBD pin 3 = CAN-H, pin 11 = CAN-L) preko SLCAN adaptera (CANable, Lawicel, Arduino+MCP2515). Raspored poruke je rekonstruisan iz community istraživanja i CAN ID-ovi se mogu mijenjati u postavkama
- **Postavke kao kod premium playera**: 5-pojasni EQ s presetima, bas boost, virtualizer, loudness, fade na play/pause, preskakanje tišine, brzina, nastavak reprodukcije nakon paljenja, auto-play, pokretanje pri bootu, filtriranje kratkih fajlova i foldera
- **Teme inspirisane fabričkim sistemima VAG grupacije**: VW Discover Pro (MIB2), RNS 510, GTI, R; Audi MMI 3G, Virtual Cockpit, RS; Škoda Amundsen, Electric; SEAT Media System, CUPRA; Porsche PCM + bonus Astra CID i Midnight. Teme mijenjaju paletu, oblike, font, stil progress bara i uzorak pozadine – sve uz animirani prelaz
- **Accent boja**: preseti ili vlastita boja (nijansa / zasićenost / svjetlina)

## Build

APK se automatski gradi na GitHub Actions (tab **Actions → Build APK → Artifacts**).
Lokalno: `./gradlew assembleRelease` (potreban Android SDK, JDK 17).
Release APK je potpisan debug ključem, pa se može direktno instalirati na head unit.

## Napomene za Astru H

- Na head unitu u postavkama (Car settings / Factory / "Default music player" / "Steering wheel") odaberi CarMusic kao muzički player ako komande na volanu ne dolaze do aplikacije – stranica *Komande na volanu → Dijagnostika* pokazuje šta aplikacija prima.
- Da li će tekst doći na CID u CAN-box načinu zavisi od firmvera head unita; na nekim jedinicama CAN-box prikazuje samo ugrađeni player. U tom slučaju ostaje USB CAN opcija.
