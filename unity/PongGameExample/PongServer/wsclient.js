const WebSocket = require('ws');

// FIXME: This test client does not use FindCloudlet to get this URL.
//var server = 'ws://ponggamehackathonapp-tcp.mobiledgexponggamehackathonapp10.bonn-mexdemo.tdg.mobiledgex.net:3000';
var server = 'ws://ponggamehackathonapp-tcp.mobiledgexponggamehackathonapp10.bonn-mexdemo.tdg.mobiledgex.net:3000';
var ws = new WebSocket(server);
ws.onopen = function(){
  console.log('Connected to %s!', server);
}

var uuidPlayer = "";
var clientSession = "";
var gameId = "";
var side = -1;
var uuidOtherPlayer = "";
var gameSequence = 0;
var gameState = null;


const STATE = {
    LOBBY: 'lobby',
    JOINED: 'joining',
    INGAME: 'ingame',
    LOST: 'lost',
    WON: 'won'
}
var STATUS = STATE.LOBBY;



function newGameState(gameId, gameSequence, uuidPlayer, uuidOtherPlayer) {
    var gameState = {};
    gameState.type = "gameState";
    gameState.source = "client";
    gameState.gameId = gameId;
    gameState.sequence = gameSequence;

    gameState.currentPlayer = uuidPlayer;
    gameState.side = -1; // Unknown side.
    gameState.players = [];
    gameState.players[0] = {uuid: uuidPlayer, position: {x: 0, y: 0, z: 0}, velocity: {x: 0, y: 0, z: 0}};
    gameState.players[1] = {uuid: uuidOtherPlayer, position: {x: 0, y: 0, z: 0}, velocity: {x: 0, y: 0, z: 0}};

    gameState.balls = [];
    gameState.balls.push({position: {x: 0, y: 0, z: 0}, velocity: {x: 0, y: 0, z: 0}});


    return gameState
}

// For messages from server:
function updateWithServerScore(scoreEvent) {
    // This is sent from the server.
    if (gameState == null) {
        console.log("Score update to no game: GameId: %d", scoreEvent.gameId);
    }

    // Server wins. Always (!):
    gameState.playerScore1 = scoreEvent.playerScore1;
    gameState.playerScore2 = scoreEvent.playerScore2;
    console.log("GameState update with scores: Player1: %d, Player2: %d", gameState.playerScore1, gameState.playerScore2);
}

function updatePosition(moveItem) {
    console.log("moveItem: %o", moveItem);
    // One item move message at a time for now.

    // blind update:
    if (moveItem.uuid == gameState.balls[0].uuid) {
        gameState.balls[0].position = moveItem.position;
        gameState.balls[0].velocity = moveItem.velocity;
        return;
    }

    // Players?
    if (moveItem.uuid == uuidPlayer) {
        side = gameState.side;
        if (moveItem.side == gameState.side) {
            // Server tells current client where it thinks the client is. This isn't an actual client, so just print diff:
            var player = null;
            if (gameState.player[0].uuid == uuidPlayer) {
                player = gameState.player[0];

            } else if (gameState.player[1].uuid == uuidPlayer) {
                player = gameState.player[1];
            }
            var ex = moveItem.position.x - player.position.x;
            var ey = moveItem.position.y - player.position.y;
            var evx = moveItem.velocity.x - player.velocity.x;
            var evy = moveItem.velocity.y - player.velocity.y;
            console.log("Server versus local difference: (%d, %d)", ex, ey);
            console.log("Server versus local velocity difference: (%d, %d)", evx, evy);
        }
    } else {
        // Other player moved. Just update and print:
        var player = null;
        var idx = -1;
        if (gameState.players[0].uuid == uuidOtherPlayer) {
            player = gameState.players[0];
            idx = 0;
        } else if (gameState.players[1].uuid == uuidOtherPlayer) {
            player = gameState.players[1];
            idx = 1;
        }
        if (idx == -1) {
            console.log("Error: Invalid gameState gameId: [%d] Can't find other player to update. Sequence: [%d]", moveItem.gameId, gameSequence);
        } else {
            player.position = moveItem.position;
            player.velocity = moveItem.velocity;
        }
    }
}

ws.addEventListener("message", function(event) {

  //console.log("Got: " + event.data);
  jsonObj = JSON.parse(event.data);
  switch (jsonObj.type) {
	  case "register":
	    clientSession = jsonObj.sessionId;
		uuidPlayer = jsonObj.uuidPlayer;
	    console.log("Our sessionId: " + clientSession);
		console.log("Our new alias: " + uuidPlayer);
        STATUS = STATE.LOBBY;
	    break;
	  case "qotd":
		console.log("qotd: <" + jsonObj.qotd + ">");
		break;
	  case "gameJoin":
		STATUS = 1;
        gameId = jsonObj.gameId;
        side = jsonObj.side;
        uuidOtherPlayer = jsonObj.uuidOtherPlayer;
		console.log("We're told to join gameId: %s, side: %d, other player: %s", gameId, side, uuidOtherPlayer);
        STATUS = STATE.JOINED;
	    break;
	  case "gameStart":
         // Run game!

         break;
      case "scoreEvent":
        var scoreEvent = jsonObj;
        updateWithServerScore(scoreEvent);
        console.log("New score: " )
        break;
      case "moveEvent":
        moveItem = jsonObj;
        updatePosition(moveItem);
        break;
	  case "resign":
	    // Quit!
	    break;
	  case "restart":
	    // Restart!
	    break;
      case "gameState":
        var gameStateReceived = jsonObj;
        //console.log("GameState Recieved: %o", gameState)
        if (gameStateReceived.sequence == 0) {
            console.log("Allow updates.")
            STATUS = STATE.INGAME;
        }

        if (gameStateReceived.sequence > gameSequence) {
            gameSequence = gameStateReceived.sequence;
        }
        if (gameId != gameStateReceived.gameId) {
            console.log("Game Id Mismatch!");
            // disconnect
            break;
        }
        // "other" player in the gameState
        if (uuidPlayer == gameStateReceived.players[0]) {
            uuidOtherPlayer = gameStateReceived.players[1];
        }
        else if (uuidPlayer == gameStateReceived.players[1]) {
            uuidOtherPlayer = gameStateReceived.players[0];
        }
	    console.log("gameState" + JSON.stringify(jsonObj));
		console.log("gameId: " + gameStateReceived.gameId);
		console.log("sequence: " + gameSequence);
        // Dummy update:
        gameState = gameStateReceived;
	    break;

	  default:
	    console.log("Unknown message type: " + jsonObj.type);
  }

});

sendMessage = (text = 'test') => {
  console.log('Sent: ' + text)
  ws.send(text);
}

/*
var count = 0;
setInterval(function() {
  if (STATUS == STATE.INGAME){
    var gameState = newGameState(gameId, gameSequence, uuidPlayer, uuidOtherPlayer);
    ws.send(JSON.stringify(gameState));
  }
}, 2000 );
*/
