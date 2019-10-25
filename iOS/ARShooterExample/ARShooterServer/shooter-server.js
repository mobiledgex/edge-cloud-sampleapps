var io = require('socket.io')(1337) // Listen on port 1337

var scoreInGameMap = new Map(); // Map gameID to Map of usernames and score in that game

io.on('connection', function(socket) {

    console.log((new Date()) + "Connection from origin " + socket + "."); 

    socket.on("login", function(gameID, username) {
        console.log("gamid: " + gameID + ", username " + username);
        if (!scoreInGameMap.has(gameID)) {
            var scores = {};
            scoreInGameMap.set(gameID, scores);
        }
        var scores = scoreInGameMap.get(gameID);
        if (username in scores) {
            socket.emit("repeatUsername", "Username already being used. Choose a different one.");
        } else {
            scores[username] = 0;
            scoreInGameMap.set(gameID, scores);
            socket.username = username;
            socket.gameID = gameID;
            socket.join(gameID, function(err) {
                io.in(gameID).emit("otherUsers", scores);  // send self all other usernames and score
            });
        }
    });

    socket.on("bullet", function(gameID, bullet) {
        socket.to(gameID).emit("bullet", bullet);
    });

    socket.on("worldMap", function(gameID, worldMap) {
        // Reset scores when someone sends worldMap
        scores = scoreInGameMap.get(gameID);
        const usernames = Object.keys(scores);
        for (const username of usernames) {
            scores[username] = 0
        }
        scoreInGameMap.set(gameID, scores);
        io.in(gameID).emit("otherUsers", scores);
        // Send world map
        socket.to(gameID).emit("worldMap", worldMap);
    });

    socket.on("score", function(gameID, username) {
        // console.log("score");
    });

    socket.on("error", function(err) {
        console.log("Caught flash policy server socket error: ");
        console.log(err.stack);
    });
    
    socket.on('disconnect', function(reason) {
        var username = socket.username;
        var gameID = socket.gameID;
        var gameScores = scoreInGameMap.get(gameID);
        delete gameScores[username];
        scoreInGameMap.set(gameID, gameScores);
        io.in(gameID).emit("otherUsers", gameScores);
        console.log(reason);
    });
});
