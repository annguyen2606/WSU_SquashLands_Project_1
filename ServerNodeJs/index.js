const { Socket } = require("dgram");

const app = require("express")();
const http = require("http").createServer(app);
const io = require('socket.io')(http);
const VLC = require("./VLC");
const { Console } = require("console");

const vlcObj = new VLC('localhost',8080,'test');


vlcObj.playlist(playlistObj=>{
    var returnedData = playlistObj;
    returnedData.forEach(element => {
        console.log(element);
    });
})

app.get('/', (req, res) => {
    res.sendFile(__dirname+'/media.html');
});

io.on('connection', (socket) => {
    console.log('A client connected');
});

http.listen(5000, ()=>{
    console.log('listening on port 5000');
});


