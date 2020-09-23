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
        expires: 600000
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
            res.render('media.html',songs = playlistObj);
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

http.listen(5000, ()=>{
    console.log('listening on port 5000');
});


