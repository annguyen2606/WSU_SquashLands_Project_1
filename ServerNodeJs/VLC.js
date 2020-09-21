
var basicAuth = require("basic-auth-connect");
const vlc = require("vlc-client");
const request = require('request');
const http = require('http');
module.exports = class VLC{

    constructor(host,port,password){
        this.host = host;
        this.port = port;
        this.password = password;

        let spawn = require('child_process').spawn;
        let vlcBackEnd = spawn('VLC',[
            '-I',
            'qt',
            '--extraintf',
            'http',
            '--http-host',
            `${this.host}`,
            '--http-port',
            `${this.port}`,
            '--http-password',
            `${this.password}`
        ]);
    }

    playlist(callBack){     
        var request = http.get({host:this.host, path: "/requests/playlist.json", port: this.port, auth: `:${this.password}` },res=>{
            var a = '';
            res.setEncoding("utf-8");
            res.on('data', a = (chunk)=>{
                var obj = JSON.parse(chunk);
                var media = obj.children[1].children;
                let tmp = Array.from(media);
                callBack(tmp);
            });
        })
        request.end();
    }
}