/**
 * Copyright 2019 MobiledgeX, Inc. All rights and licenses reserved.
 * MobiledgeX, Inc. 156 2nd Street #408, San Francisco, CA 94105
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

const http = require('http');
const WebSocket = require('ws');
const url = require('url');

const uuidv4 = require('uuid/v4');
const util = require('util')

const server = http.createServer();
const wsServer = new WebSocket.Server({ noServer: true });


// Server instance level variables:
const SIMULATELAGTIMEINMS = 0; // In milliseconds.
var connectedClients = {};
var games = {};
var nameMap = {};

// HTTP Server ==> WebSocket upgrade handling:
server.on('upgrade', function upgrade(request, socket, head) {
  console.log('upgrade http to wss: url='+request.url);
  var parsedUrl = url.parse(request.url, true, true);
  const pathname = parsedUrl.pathname
  console.log("pathname="+pathname);
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

  // Connection level variables:
  let uuidPlayer = uuidv4();
  let playerKey = "";

  console.log("got a request!");
  var parsedUrl = url.parse(request.url, true, true);
  //roomid value is passed as a query parameter in the URL.
  //Example: ws://localhost:3000/?roomid=GamerX
  let roomId = parsedUrl.query.roomid
  console.log("roomId="+roomId);

  let sessionMsg = {};
  sessionMsg.type = "register";
  console.log("request.headers="+JSON.stringify(request.headers));
  console.log("request.url="+request.url);
  let playerkey = request.headers['sec-websocket-key'];
  sessionMsg.sessionId = playerkey;
  sessionMsg.uuidPlayer = uuidPlayer;
  //console.log("Session Message:" + JSON.stringify(sessionMsg));
  ws.send(JSON.stringify(sessionMsg));

  playerKey = sessionMsg.sessionId;
  nameMap[uuidPlayer] = playerKey;
  console.log("Player key: " + sessionMsg.sessionId);

  // Update websocket connections:
  connectedClients[playerkey] = ws;

  let gameId = createOrJoinMatch(uuidPlayer, roomId);
  console.log("player: " + uuidPlayer + " has gameId: " + gameId);

  // Handle all messages from users here. The C# websocket embeds all text in
  ws.on('message', function(msgStr) {
    //console.log("got a message: <%s>", msgStr);
    gameMessage = JSON.parse(msgStr);
    //console.log("gameMessage: %o", gameMessage);
    switch(gameMessage.type) {
      case "scoreEvent":
        let scoreEvent = gameMessage;
        updateScore(uuidPlayer, scoreEvent);
        break;
      case "moveEvent":
        let moveEvent = gameMessage;
        updateObject(moveEvent);
        break;
      case "contactEvent":
        let contactEvent = gameMessage;
        updateContactEvent(contactEvent);
        break;
      case "gameState":
        let gameState = gameMessage;
        updateGameStates(uuidPlayer, gameState);
        break;
      case "resign":
        break;
      case "gameRestart":
        console.log("gameMessage: %o", gameMessage);
        let gameRestartRequest = gameMessage;
        gameRestart(gameRestartRequest);
        break;
    }
  });
  ws.on('close', function(connection) {
    console.log("closing...");
    // close user connection
    console.log("Disconnecting user: " + uuidPlayer);
    delete nameMap[uuidPlayer];
    delete connectedClients[playerKey];

    // Move other player back to waiting state:
    let game = games[gameId];
    delete games[gameId];
    if (game !== undefined && game.players !== undefined) {
      console.log("Players: %o", game.players);
      for (let i = 0; i < game.players.length; i++) {
        let player = game.players[i];
        if (nameMap[player] === undefined) {
          continue;
        }
        if (player != uuidPlayer) {
          console.log("Moving " + player + " to waiting state.");
          // Replace the running game with the simplified "waiting state" version.
          games[gameId] = {gameId: gameId, player1: player};
          let connection = getConnectionFromAlias(player);
          let message = "Other player left game '" + gameId + "'. Waiting for second player...";
          console.log(message);
          connection.send(JSON.stringify({type: "notification", notificationText: message}));
          connection.send(JSON.stringify({type: "resign"}));
        }
      }
    }
    console.log("Player %s disconnected.", uuidPlayer);
    //console.log("Connection NameMap: %o, connectedClients: %o, nameMap, Object.keys(connectedClients));
  });
});

// Attach broadcaster, to all clients. No broadcast by default.
var count = 0;
wsServer.broadcast = function(data) {
  // This client set is retrivable by session.
  for(let i in connectedClients)
    connectedClients[i].send(data);
};
setInterval(function() {
  let msg = {};
  msg.type = "qotd";
  msg.qotd = "This is madness: " + count++;
  wsServer.broadcast(JSON.stringify(msg));
}, 10000 );

function updateScore(uuidPlayer, scoreEvent) {
  let gameId = scoreEvent.gameId;
  if (gameId == null) {
    return;
  }

  let game = games[gameId];

  console.log("ScoreEvent: %o", scoreEvent)

  // Check if player matches:
  let found = false;
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
      console.log("Thy game is Pong?");
  }

  console.log("Server sees scores {P1: %d, P2: %d}", game.playerScore1, game.playerScore2)

  // Update other player(s) of new score:
  game.players.forEach(function(player) {
    if (uuidPlayer != player) { // No need to send to the originator.
      let playerKey = nameMap[player];
      if (playerKey !== undefined) {
        connection = connectedClients[playerKey];
        if (connection) {
          // Address to uuid of player:
          let se = {type: "scoreEvent", uuid: player, side: scoreEvent.side, playerScore1: game.playerScore1, playerScore2: game.playerScore2};
          connection.send(JSON.stringify(se));
        }
      }
    }
  });

  // Start the next round in match.
  nextRound(game);
}

function updateContactEvent(item) {
  let game = games[item.gameId];

  // Only ball contact events:
  if (item.objectType == "Ball") {
    console.log("Ball collision event to update: %o", item)
    let last = game.gameStates.length - 1;
    let gs = game.gameStates[last];
    gs.balls.forEach(function(ball) {
      if (ball.uuid == item.uuid) {
        ball.position = item.position;
        ball.velocity = item.velocity;
      }
    });

    item.sequence = ++gs.sequence;

    // Pure echo, if non-matching player.
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

function updateObject(item) {
  let game = games[item.gameId];
  if (game.gameStates == null) {
    // console.log("updateObject received for non-running game");
    return;
  }

  // To update an item, you need: item type, item uuid, position, velocity:
  // Echo only, even to self to allow client to check differences.
  if (item.objectType == "Ball") {
    // Keep a copy on the server to inspect...
    //console.log("XXXX Game to update: %o", game)
    let last = game.gameStates.length - 1;
    let gs = game.gameStates[last];
    ++gs.sequence;

    gs.balls.forEach(function(ball) {
      if (ball.uuid == item.uuid) {
        ball.position = item.position;
        ball.velocity = item.velocity;
      }
    });
    item.sequence = gs.sequence;
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
    let playerKey = nameMap[player];
    if (playerKey !== undefined) {
      connection = connectedClients[playerKey];
      if (connection && connection.readyState === connection.OPEN) {
        connection.send(JSON.stringify(objectToUpdate));
      } else {
        console.log("Client connection is invalid for [%s].");
      }
  }
  });
}

// Assuming 2 players.
function updateGameStates(alias, playerGameState) {
  console.log("updateGameStates");

  let gameId = playerGameState.gameId;
  console.log("Looking for gameId: %s", gameId);
  let servergame = games[gameId];

  if (servergame === undefined) {
    console.log("Game not found: " + servergame);
  return;
  }

  console.log("Player Alias's Game state" + JSON.stringify(playerGameState));

  let currentPlayerIndex = playerIndex(alias, playerGameState);
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
  let uuidOtherPlayer = ""
  let otherPlayerIdx = -1;
  for (let idx = 0; idx < servergame.players.length; idx++) {
    let anAlias = servergame.players[idx];
    if (anAlias != alias) {
      uuidOtherPlayer = anAlias;
      otherPlayerIdx = idx;
      break;
    }
  }

  let player1Alias = alias;
  let player2Alias = uuidOtherPlayer;
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
  let last = servergame.gameStates.length - 1;
  let serverGameState = servergame.gameStates[last];
  //console.log("Found Game. Size: %d Last state: %o", servergame['gameStates'].length, JSON.stringify(serverGameState));

  // Update Server State:
  let newServerGameState = newGameState(gameId, serverGameState,
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

// dictionary...
function newEmptyGameState(gameId, uuidPlayer, uuidOtherPlayer) {
  let gameState = {}
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
  let idx = -1;
  for(let i = 0; i < servergame.players.length; i++) {
    if (servergame.players[i].uuid == uuidPlayer) {
      idx = i;
      break;
    }
  }
  return idx;
}

function playerIndex(uuidPlayer, gameState) {
  let idx = -1;
  for(let i = 0; i < gameState.players.length; i++) {
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
  let seq = serverGameState.sequence+1;

  let gameState = {};
  gameState.type = "gameState";
  gameState.source = "server";
  gameState.gameId = gameId;
  gameState.sequence = seq;

  gameState.currentPlayer = uuidPlayer;
  gameState.players = [];

  // Latest data is the current player. Server uses player's data.
  let playerIdx = playerIndex(uuidPlayer, uuidPlayerGameState);
  let player1 = uuidPlayerGameState.players[playerIdx];
  console.log("Player1 (client) index: %d, value: %o", playerIdx, player1);
  if (player1.uuid !== undefined || player.uuid != ""){
    let cp1 = newPlayer(player1);
    gameState.players.push(cp1);
  }

  // Other player is the last known server state.
  let otherPlayerIdx = playerServerIndex(uuidOtherPlayer, serverGameState);
  let player2 = serverGameState.players[otherPlayerIdx];
  console.log("Player2 (server) index: %d, value: %o, for %s, in %o", otherPlayerIdx, player2, uuidOtherPlayer, serverGameState);
  if (player2.uuid !== undefined || player.uuid != ""){
    let cp2 = newPlayer(player2);
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

function randRange(min, max) {
  return (Math.random() * (max - min)) + min
}

function randomBall(ballId) {
  let rand = randRange(-1, 1);
  let updown = randRange(-1, 1);
  let velocity = null;
  if (rand < 0) {
    velocity = { x: 3, y: updown * 2}
  } else {
    velocity = { x: -3, y: updown * 2}
  }
  let position = { x: 0, y: 0 }
  ball = {uuid: ballId, position: position, velocity: velocity}

  return ball;
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
  let playerKey = nameMap[uuidPlayer];
  let connection = connectedClients[playerKey];

  return connection;
}

function createOrJoinMatch(uuidPlayer, roomId) {
  console.log("Player %s looking for room with roomID %s", uuidPlayer, roomId);
  gameId = roomId;
  // TODO: gameId = roomId + "-" + uuidv4(); This requires reworking the code flow.

  if (gameId === undefined || gameId == null) {
    console.log("Bad state!");
    return;
  }

  let connection = getConnectionFromAlias(uuidPlayer);
  game = games[gameId];
  if(game == null) {
    // Create a simplified instance of the game object to hold the gameId and the playerId
    games[gameId] = {gameId: gameId, player1: uuidPlayer};
    let message = "Created game '" + gameId + "'. Waiting for second player...";
    console.log(message);
    connection.send(JSON.stringify({type: "notification", notificationText: message}));
    return gameId;
  }

  uuidOtherPlayer = games[gameId].player1;
  console.log("Room already exists. uuidOtherPlayer="+uuidOtherPlayer);
  if(games[gameId].player2 != null) {
    let message = "Room " + gameId + " is already full.";
    console.log(message);
    connection.send(JSON.stringify({type: "notification", notificationText: message}));
    return;
  }

  // Check for connections to players:
  console.log("uuidPlayer: %s, uuidOtherPlayer: %s, gameId: %s", uuidPlayer, uuidOtherPlayer, gameId)

  let otherConnection = null;
  if (uuidOtherPlayer !== undefined)
    otherConnection = getConnectionFromAlias(uuidOtherPlayer);
  if (otherConnection === undefined || otherConnection == null) {
    console.log("Other player is not connected: " + uuidOtherPlayer);
    return null;
  }

  // A game is a pair of players, and a "recording" of all game states until a winner is found.

  let gameState = newEmptyGameState(gameId, uuidPlayer, uuidOtherPlayer);
  let gameStates = [];
  gameStates.push(gameState);
  games[gameId] = {
    gameId: gameId,
    sequence: 0,
    players: [uuidOtherPlayer, uuidPlayer],
    player1: uuidOtherPlayer,
    player2: uuidPlayer,
    playerScore1: 0,
    playerScore2: 0,
    gameStates: gameStates
  };
  console.log("Games running: %o", games);
  console.log("First GameState: %o", games[gameId].gameStates);

  // push this gameState to the clients to initialize their game.
  let ballId = gameState['balls'][0].uuid;
  connection.send(JSON.stringify({type: "gameJoin", gameId: gameId, ballId: ballId, side: 0, uuidOtherPlayer: uuidOtherPlayer}));
  otherConnection.send(JSON.stringify({type: "gameJoin", gameId: gameId, ballId: ballId, side: 1, uuidOtherPlayer: uuidPlayer}));

  // Start the game. Frist game is at sequence 0.
  let sendGameState = games[gameId].gameStates[0];

  // Assign player side (0)
  connection.send(JSON.stringify(sendGameState));
  otherConnection.send(JSON.stringify(sendGameState));

  // GameState is actually ignored right now.
  // Send the ball off in a uniform location with a reset (in case there's a pre-game...)
  let ball = randomBall(ballId);
  let restartGame = {
    type: "gameRestart",
    gameId: gameId,
    balls: [ball]
  }
  connection.send(JSON.stringify(restartGame));
  otherConnection.send(JSON.stringify(restartGame));

  console.log("Created a game: " + JSON.stringify(games[gameId]));
  return gameId;
}

// For this game, the ball just starts in the middle.
function nextRound(game) {
  let gameId = game.gameId;
  if (gameId == null || gameId == undefined) {
    console.log("Unknown game!")
    return;
  }
  if (game.gameStates.length == 0) {
    console.log("Not gameState!");
    return;
  }

  let idx = game.gameStates.length-1;
  let gameState = game.gameStates[idx];
  let ballId = gameState.balls[0].uuid;

  let ball = randomBall(ballId);
  let nextRound = {
    type: "nextRound",
    gameId: game.gameId,
    balls: [ball]
  }
  gameState.sequence = 0;

  game.players.forEach(function(player) {
    let connection = getConnectionFromAlias(player);
    if (connection === undefined) {
      console.log("XXXXXXXXXXXXX Bad connection!");
    } else {
      connection.send(JSON.stringify(nextRound));
    }
  });
}

function gameRestart(gameRestartRequest) {
  let gameId = gameRestartRequest.gameId;
  if (gameId == null || gameId == undefined) {
    console.log("Unknown game!")
    return;
  }
  game = games[gameId];
  if (game.gameStates.length == 0) {
    console.log("Not gameState!");
    return;
  }

  let idx = game.gameStates.length-1;
  let gameState = game.gameStates[idx];
  gameState.sequence = 0;
  let ballId = gameState.balls[0].uuid;

  let ball = randomBall(ballId);
  let gameRestart = {
    type: "gameRestart",
    gameId: game.gameId,
    balls: [ball]
  }

  // Make sure player belongs...

  game.players.forEach(function(player) {
    let connection = getConnectionFromAlias(player);
    if (connection === undefined) {
      console.log("XXXXXXXXXXXXX Bad connection!");
    } else {
      connection.send(JSON.stringify(gameRestart));
    }
  });
}


server.listen(3000);
console.log("Listening for player connections on port 3000");
