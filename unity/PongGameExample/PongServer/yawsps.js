var http = require('http');
var WebSocket = require('ws');

const uuidv4 = require('uuid/v4');
var util = require('util')

const server = http.createServer();
const wsServer = new WebSocket.Server({ noServer: true });

// HTTP Server ==> WebSocket upgrade handling:
server.on('upgrade', function upgrade(request, socket, head) {
  const pathname = request.url

  if (pathname === '/') {
    wsServer.handleUpgrade(request, socket, head, function done(ws) {
      wsServer.emit('connection', ws, request);
      console.log('A client connected.');
    });
  } else {
    socket.destroy();
  }
});

// Websocket connection handler:
wsServer.on('connection', function connection(ws, request) {
  var uuidPlayer = uuidv4();
  var playerKey = "";

  console.log("got a request!");

  var sessionMsg = {};
  sessionMsg.type = "register";
  var playerkey = request.headers['sec-websocket-key'];
  sessionMsg.sessionId = playerkey;
  sessionMsg.uuidPlayer = uuidPlayer;
  //console.log("Session Message:" + JSON.stringify(sessionMsg));
  ws.send(JSON.stringify(sessionMsg));

  playerKey = sessionMsg.sessionId;
  nameMap[uuidPlayer] = playerKey;
  console.log("Player key: " + sessionMsg.sessionId);

  // Update websocket connections:
  connectedClients[playerkey] = ws;

  // Add to lobby.
  lobby.set(uuidPlayer, 1);

  var gameId = createMatch(uuidPlayer);
  if (gameId == null) {
      // No game to run yet.
  }

  // Handle all messages from users here. The C# websocket embeds all text in
  ws.on('message', function(msgStr) {
    //console.log("got a message: <%s>", msgStr);
    gameMessage = JSON.parse(msgStr);
    console.log("gameMessage: %o", gameMessage);
    switch(gameMessage.type) {
      case "scoreEvent":
        var scoreEvent = gameMessage;
        updateScore(uuidPlayer, scoreEvent);
        break;
      case "moveEvent":
        var moveEvent = gameMessage;
        updateObject(moveEvent);
        break;
      case "collisionEvent":
        // Like a move event, but wall collision related.
        break;
      case "gameState":
        var gameState = gameMessage;
        updateGameStates(uuidPlayer, gameState);
        break;
      case "resign":
        break;
      case "restart":
        break;
    }
  });
  ws.on('close', function(connection) {
    console.log("closing...");
    // close user connection
    console.log("Disconnecting user: " + uuidPlayer);
    delete nameMap[uuidPlayer];
    delete connectedClients[playerKey];
    lobby.delete(uuidPlayer);

    // Move other player back to lobby:
    var game = games[gameId];
    if (game !== undefined) {
      console.log("Players: %o", game.players);
      for (var i = 0; i < game.players.length; i++) {
        var player = game.players[i];
        if (player != uuidPlayer) {
          console.log("re-adding: " + player)
          lobby.set(player, 1); // kick back to lobby
        }
      }
    }
    console.log("Player %s disconnected.", uuidPlayer);
    //console.log("Connection NameMap: %o, connectedClients: %o, lobby: %o", nameMap, Object.keys(connectedClients), lobby);
  });
});


// We'll need some way to do sessions to reconnect, and match them up with "activity"
const SIMULATELAGTIMEINMS = 0; // In milliseconds.
var connectedClients = {};
var games = {};
var lobby = new Map(); // Mob Map.

var nameMap = {};

// Attach broadcaster, to all clients. No broadcast by default?
var count = 0;
wsServer.broadcast = function(data) {
  // This client set is retrivable by session.
  for(var i in connectedClients)
    connectedClients[i].send(data);
};
setInterval(function() {
  var msg = {};
  msg.type = "qotd";
  msg.qotd = "This is madness: " + count++;
  wsServer.broadcast(JSON.stringify(msg));
}, 10000 );

function updateScore(uuidPlayer, scoreEvent) {
  var gameId = scoreEvent.gameId;

  var game = games[gameId];

  console.log("ScoreEvent: %o", scoreEvent)

  // Check if player matches:
  var found = false;
  game.players.forEach(function(player) {
    if (player == uuidPlayer) {
      found = true;
    }
  });
  if (!found) {
    console.log("Who are you [%s]? known player uuids: [%o]", scoreEvent.uuid, game.players)
    return;
  }

  // Pure echo. We're just going to mirror and relay this server side for the player. Server can't verify here.
  switch (scoreEvent.side) {
    case 0:
      game.playerScore1 = scoreEvent.playerScore1;
      game.playerScore2 = scoreEvent.playerScore2;
      break;
    case 1:
      game.playerScore1 = scoreEvent.playerScore1;
      game.playerScore2 = scoreEvent.playerScore2;
      break;
    default:
      console.log("Thy game is Pong?")
  }

  console.log("Server sees scores {P1: %d, P2: %d}", game.playerScore1, game.playerScore2)


  // Update other player(s) of new score:
  game.players.forEach(function(player) {
    if (uuidPlayer != player) { // No need to send to the originator.
      var playerKey = nameMap[player];
      if (playerKey !== undefined) {
        connection = connectedClients[playerKey];
        if (connection) {
          // Address to uuid of player:
          var se = {type: "scoreEvent", uuid: player, side: scoreEvent.side, playerScore1: game.playerScore1, playerScore2: game.playerScore2};
          connection.send(JSON.stringify(se));
        }
      }
    }
  });
}

function updateObject(item) {
  var game = games[item.gameId];

  // To update an item, you need: item type, item uuid, position, velocity:
  // Echo only, even to self to allow client to check differences.
  if (item.objectType == "Ball") {
    // Keep a copy on the server to inspect...
    //console.log("XXXX Game to update: %o", game)
    var last = game.gameStates.length - 1;
    var gs = game.gameStates[last];
    gs.balls.forEach(function(ball) {
      if (ball.uuid == item.uuid) {
        ball.position = item.position;
        ball.velocity = item.velocity;
      }
    });
    // Pure echo.
    if (SIMULATELAGTIMEINMS > 0) {
      // Network lag simulation:
      setTimeout(function(){
        updateClients(game, item);
      }, SIMULATELAGTIMEINMS);
    } else {
      updateClients(game, item);
    }
  }
  else if (item.objectType == "Player") {
    // Pure echo.
    if (SIMULATELAGTIMEINMS > 0) {
        // Network lag simulation:
        setTimeout(function(){
          updateClients(game, item);
      }, SIMULATELAGTIMEINMS);
    } else {
      updateClients(game, item);
    }
  }
}

function updateClients(game, objectToUpdate) {
  game.players.forEach(function(player) {
    // Pure echo (no storage).
    var playerKey = nameMap[player];
    if (playerKey !== undefined) {
      connection = connectedClients[playerKey];
      if (connection) {
        connection.send(JSON.stringify(objectToUpdate));
      }
  }
  });
}

// Assuming 2 players.
function updateGameStates(alias, playerGameState) {
	console.log("updateGameStates");

  var gameId = playerGameState.gameId;
  console.log("Looking for gameId: %s", gameId);
	var servergame = games[gameId];

	if (servergame === undefined) {
		console.log("Game not found: " + servergame);
		return;
  }

  console.log("Player Alias's Game state" + JSON.stringify(playerGameState));

  var currentPlayerIndex = playerIndex(alias, playerGameState);
  if (currentPlayerIndex == -1) {
    console.log("Who are you?");
    return;
  }

	if (playerGameState.currentPlayer != alias) {
		console.log("You are not you!");
		return;
	}

  // First 2 are players.
  // Lookup on the server state, who the other player is:
  var uuidOtherPlayer = ""
  var otherPlayerIdx = -1;
  for (var idx = 0; idx < servergame.players.length; idx++) {
    var anAlias = servergame.players[idx];
    if (anAlias != alias) {
      uuidOtherPlayer = anAlias;
      otherPlayerIdx = idx;
      break;
    }
  }

	var player1Alias = alias;
	var player2Alias = uuidOtherPlayer;
  console.log("Player1: " + player1Alias);
  console.log("Player2: " + player2Alias);

	// look up sessions, so we have connections for both:
	player1key = nameMap[player1Alias];
	player2key = nameMap[player2Alias];

	p1Connection = connectedClients[player1key];
	p2Connection = connectedClients[player2key];
	if (p1Connection === undefined) {
		console.log("Bad connection for player1!");
    // TODO: Lookup game, and retry until rejoin timeout.
		return;
	}

	if (p2Connection === undefined) {
    console.log("Bad connection for player2!");
    console.log("Lookup key: " + player2key);
    console.log("player2Alais: " + player2Alias);
	  return;
	}

  // lookup Gameid to find the last server playerGameState.
  var last = servergame.gameStates.length - 1;
  var serverGameState = servergame.gameStates[last];
	//console.log("Found Game. Size: %d Last state: %o", servergame['gameStates'].length, JSON.stringify(serverGameState));

  // Update Server State:
  var newServerGameState = newGameState(gameId, serverGameState,
                                        alias, playerGameState,
                                        player2Alias, uuidOtherPlayer);

  // Append to game record:
  servergame.gameStates.push(newServerGameState);
  if (servergame.gameStates > 20) { // Keep some previous game snapshots.
    servergame.gameStates.shift();
  }

  //console.log("Pushed gamestate: " + JSON.stringify(newServerGameState));

	// Let the clients know:
  newServerGameState.currentPlayer = alias;
	p1Connection.send(JSON.stringify(newServerGameState));
  newServerGameState.currentPlayer = player2Alias;
	p2Connection.send(JSON.stringify(newServerGameState));

}

// JSON dictionary...
function newEmptyGameState(gameId, uuidPlayer, uuidOtherPlayer) {
  var gameState = {}
  gameState.type = "gameState";
  gameState.source = "server";
  gameState.gameId = gameId;
  gameState.sequence = 0;

  gameState.currentPlayer = uuidPlayer;
  gameState.players = [];
  gameState.players.push({uuid: uuidPlayer, position: {x: 0, y: 0, z: 0}, velocity: {x: 0, y: 0, z: 0}});
  gameState.players.push({uuid: uuidOtherPlayer, position: {x: 0, y: 0, z: 0}, velocity: {x: 0, y: 0, z: 0}});

  gameState.balls = [];
  gameState.balls.push(newBall());

  gameState.playerScore1 = 0;
  gameState.playerScore2 = 0;
	return gameState
}

function playerServerIndex(uuidPlayer, servergame) {
  var idx = -1;
  for(var i = 0; i < servergame.players.length; i++) {
    if (servergame.players[i].uuid == uuidPlayer) {
      idx = i;
      break;
    }
  }
  return idx;
}

function playerIndex(uuidPlayer, gameState) {
  var idx = -1;
  for(var i = 0; i < gameState.players.length; i++) {
    if (gameState.players[i].uuid == uuidPlayer) {
      idx = i;
      break;
    }
  }
  return idx;
}

function newGameState(gameId, serverGameState,
        uuidPlayer, uuidPlayerGameState,
        uuidOtherPlayer) {
	// Update Server view of the game:
  states = serverGameState.gameStates;
	var seq = serverGameState.sequence+1;

  var gameState = {};
  gameState.type = "gameState";
  gameState.source = "server";
  gameState.gameId = gameId;
  gameState.sequence = seq;

  gameState.currentPlayer = uuidPlayer;
  gameState.players = [];

  // Latest data is the current player. Server uses player's data.
  var playerIdx = playerIndex(uuidPlayer, uuidPlayerGameState);
  var player1 = uuidPlayerGameState.players[playerIdx];
  console.log("Player1 (client) index: %d, value: %o", playerIdx, player1);
  if (player1.uuid !== undefined || player.uuid != ""){
    var cp1 = newPlayer(player1);
    gameState.players.push(cp1);
  }

  // Other player is the last known server state.
  var otherPlayerIdx = playerServerIndex(uuidOtherPlayer, serverGameState);
  var player2 = serverGameState.players[otherPlayerIdx];
  console.log("Player2 (server) index: %d, value: %o, for %s, in %o", otherPlayerIdx, player2, uuidOtherPlayer, serverGameState);
  if (player2.uuid !== undefined || player.uuid != ""){
    var cp2 = newPlayer(player2);
    gameState.players.push(cp2);
  }

  // Ball(s):
  gameState.balls = [];
  serverGameState.balls.forEach(function(ball) {
    gameState.balls.push(copyBall(ball));
  });

  gameState.playerScore1 = serverGameState.playerScore1;
  gameState.playerScore2 = serverGameState.playerScore2;

	return gameState;
}

function copyBall(ball) {
  //console.log("Cloning Ball Data: " + JSON.stringify(ball))
  return {
    uuid: player.uuid,
    position: {
      x: player.position.x,
      y: player.position.y,
      z: player.position.z
    },
    velocity: {
      x: player.velocity.x,
      y: player.velocity.y,
      z: player.velocity.z
    }
  };
}

function newBall() {
  return {
    uuid: uuidv4(),
    position: {
      x: 0,
      y: 0,
      z: 0
    },
    velocity: {
      x: 0,
      y: 0,
      z: 0
    }
  };
}

function newPlayer(player) {
  //console.log("Cloning Player Data: " + JSON.stringify(player))
  return {
    uuid: player.uuid,
    position: {
      x: player.position.x,
      y: player.position.y,
      z: player.position.z
    },
    velocity: {
      x: player.velocity.x,
      y: player.velocity.y,
      z: player.velocity.z
    }
  };
}


function getConnectionFromAlias(uuidPlayer) {
  console.log("looking for uuidPlayer connection: " + uuidPlayer);
  console.log("nameMap: %o", nameMap);
  console.log("connections: %o", nameMap);
  var playerKey = nameMap[uuidPlayer];
  var connection = connectedClients[playerKey];

  return connection;
}

function createMatch(uuidPlayer) {
	if (lobby.size < 2) {
		return null;
	}

  // Check for connections to players:
  console.log("Lobby: %o, %s", lobby, uuidPlayer)
  var u = lobby.keys().next();
  if (lobby[uuidPlayer]) {
    lobby.delete(u.value); // Remove player from lobby.
  }

  // Pick some other player in lobby:
  var uuidOtherPlayer = lobby.keys().next().value;
  lobby.delete(uuidOtherPlayer);

  if (uuidOtherPlayer !== undefined)
  var otherConnection = getConnectionFromAlias(uuidOtherPlayer);
  if (otherConnection == undefined) {
    console.log("Other player is not connected: " + uuidOtherPlayer);
    return null;
  }
  var connection = getConnectionFromAlias(uuidPlayer);

	// Great. Create a game with whoever is free in line:
	var gameId = uuidv4();
	// A game is a pair of players, and a "recording" of all game states until a winner is found.

	var gameState = newEmptyGameState(gameId, uuidPlayer, uuidOtherPlayer);
	var gameStates = [];
	gameStates.push(gameState);
	games[gameId] = {
		gameId: gameId,
    sequence: 0,
    players: [uuidPlayer, uuidOtherPlayer],
    player1: uuidPlayer,
    player2: uuidOtherPlayer,
    playerScore1: 0,
    playerScore2: 0,
		gameStates: gameStates
	};
  console.log("Games running: %o", games);
  console.log("First GameState: %o", games[gameId].gameStates);

  // push this gameState to the clients to initialize their game.
  var ballId = gameState['balls'][0].uuid;
  connection.send(JSON.stringify({type: "gameJoin", gameId: gameId, ballId: ballId, side: 0, uuidOtherPlayer: uuidOtherPlayer}));
  otherConnection.send(JSON.stringify({type: "gameJoin", gameId: gameId, side: 1, uuidOtherPlayer: uuidPlayer}));

  // Start the game. Frist game is at sequence 0.
  var sendGameState = games[gameId].gameStates[0];

  // Assign player side (0)
  connection.send(JSON.stringify(sendGameState));
  otherConnection.send(JSON.stringify(sendGameState));

  console.log("Created a game: " + JSON.stringify(games[gameId]));
  return gameId;
}

server.listen(3000);
