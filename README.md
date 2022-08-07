# Games used:
1) single-card-game
2) double-card-game

## Limitations:
1) No persistence is used. All the player level info is store in runtime cache

## Design decision:
1) The design uses Akka Http Websockets mapped to Akka acctors for the Dealer and the Players.
2) Rationale behind the design decision is to have a virtual replica of an actual game table headed by the dealer and played by the players

## How to run:
Run the app by running the main file 'com.gameserver.GameServer' manually
        OR
By running '.gradlew' and then running 'gradle run'
        OR
By building 'gradle clean build' and then runninng the shadow jar file 'java -jar game-server-all.jar' from build/lib

## How to test:
1) Go to https://dwst.github.io/
2) Type /connect ws://localhost:8080/game-server/v1/ws to connect. Follow the server messages and the game begins
3) Open multiple tabs of https://dwst.github.io/ so as add players
