const { Socket } = require("dgram");

const app = require("express")();
const http = require("http").createServer(app);
const io = require('socket.io')(http);
const vlc = require("vlc-client");
const VLC = require("./VLC");

const vlcObj = new VLC('localhost',8080,'test');

const vlcClient = new vlc.Client({ip: '0.0.0.0', port: 8080, password:'test'}); 
app.get('/', (req, res) => {
    res.sendFile(__dirname+'/media.html');
});

io.on('connection', (socket) => {
    console.log('A client connected');
});

http.listen(5000, ()=>{
    console.log('listening on port 3000');
});