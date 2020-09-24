const { Socket } = require("dgram");
const app = require("express")();
const session = require("express-session");
const http = require("http").createServer(app);
const io = require('socket.io')(http);
const VLCApp = require("./VLC");
const { VLC } = require('node-vlc-http');
const { Console } = require("console");
var cookieParser = require('cookie-parser');
const Staff = require("./Models/Staff");
const Request = require("./Models/Request");
const QueueSong = require("./Models/QueueSong");
var bodyParser = require('body-parser');
const { request } = require("http");
const { render } = require("ejs");

var currentSong = "";

setInterval(()=>{
    vlcObj.library(playlistObj=>{
        if(playlistObj[0].children.length > 0){
            playlistObj[0].children.forEach(element => {
                if(element.hasOwnProperty('current')){
                    if(element.name !== currentSong){

                        currentSong = element.name;
                        console.log(currentSong);
                        mediaNamespace.emit('new song on', {currentSong: currentSong});
                    }
                }
            });
        }
    });
},1000);

const mediaNamespace = io.of('/mediaNameSpace');

mediaNamespace.on('connection', socket => {
    console.log('client connected to media namespace');
});
app.use(bodyParser.urlencoded({ extended: true }));

const vlcObj = new VLCApp('localhost',8080,'test');

app.set('views', __dirname + '/Templates');
app.engine('html',require('ejs').renderFile);
app.use(cookieParser());

app.use(session({
    key: 'user_sid',
    secret: 'thisissuchasecret',
    resave: false,
    saveUninitialized: false,
    cookie: {
        expires: 60000
    }
}));

var sessionChecker = (req, res, next) => {
    if (req.session.user && req.cookies.user_sid) {
        res.redirect('/media');
    } else {
        next();
    }    
};

app.get('/', (req, res) => {
    res.redirect('/login');
});

app.route('/login').get(sessionChecker,(req,res)=>{
    if(req.session.user && req.cookies.user_sid){
        res.redirect('/media');
    }else
        res.sendFile(__dirname+'/Templates/login.html');
}).post((req,res)=>{
    var username = req.body.uname;
    var password = req.body.psw;
    Staff.findOne({where: {uname: username}}).then((user)=>{
        if(!user){
            res.redirect('/login');
        }else if(!user.validPassword(password)){
            res.redirect('/login');
        }else{
            req.session.user = user.uname;
            res.redirect('/media');
        }
    });
});

app.get('/media',(req,res)=>{
    if(req.session.user && req.cookies.user_sid){
        vlcObj.library(playlistObj=>{
            res.render('media.html',{songs: playlistObj,  user: req.session.user});
        });
    }else{
        res.redirect('/login');
    }
});

app.get('/logout', (req, res) => {
    if (req.session.user && req.cookies.user_sid) {
        res.clearCookie('user_sid');
        res.redirect('/');
    } else {
        res.redirect('/login');
    }
});

app.route('/addToQueue').get((req, res) => {
    vlcObj.add(req.query.uri,()=>{
        setTimeout(()=>{
            res.redirect('/media');
        },1000)
    });
});

app.route('/removeFromQueue').get((req, res) => {
    vlcObj.remove(req.query.id,()=>{
        setTimeout(()=>{
            res.redirect('/media');
        },1000)
    });
});

http.listen(5000, ()=>{
    console.log('listening on port 5000');
});


