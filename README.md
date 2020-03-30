# monkeyzone
MonkeyZone is a multi-player demo game provided by the jME core developer team.

    Download source code (Github Repository)

    Watch pre-alpha video footage (YouTube Video)

    Read "MonkeyZone – a jME3 game from the core" (news article)

    Related forum thread: Open Game Finder

This open-source demo:

    showcases one possible way to implement a game with jME3, and

    helps the jME team verify the jME3 API in terms of usability.

The game idea is based on “BattleZone” arcade game from the 1980s, a first-person shooter the with real-time strategy elements. The game was written using the jMonkeyEngine SDK, and it’s based off the BasicGame project template. It took us one week to create a playable pre-alpha, including networking. The project design follows best practices that make it possible to edit maps, vehicles, etc, in jMonkeyEngine SDK without having to change the code – This allows 3D graphic designers to contribute models more easily. (If you feel like contributing assets or working on parts of the game code, drop us a note!)

Implementation

MonkeyZone is a multi-player game with a physics simulation. Both, clients and server, run the physics simulation. The clients send input data from the player group to the server, where they control the entities, and also broadcast to the clients. Additionally, the server sends regular syncronization data for all objects in the game to prevent drifting. When a human user or an AI performs an action (presses a button), the actual logic is done on the server. The results are broadcast as data messages to the entities. When the entity is controlled by an AI, the actual AI code (that determines where the entity should move, and when it should perform an action) is executed on the client. _The way MonkeyZone is implemented is just one of the many possible ways to do a game like this in jME. Some things might be done more efficiently, some might be done in another way completely. MonkeyZone tries to do things the way that are most appropriate to implement the game at hand and it shows nicely how jME3 and the standard Java <abbr title="Application Programming Interface">API</abbr>++ can make game development easier and faster. Also note that the way MonkeyZone is designed is not scalable to a MMO style game, it will only work in a FPS style environment where the whole game world can be loaded at once._

More info in the wiki: https://wiki.jmonkeyengine.org/jme3/advanced/monkey_zone.html
