
const http = require('http');
const queryString = require('querystring');
const { VLC } = require('node-vlc-http');
module.exports = class VLCApp{

    constructor(host,port,password,callBack){
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

    library(callBack){     
        var request = http.get({host:this.host, path: "/requests/playlist.json", port: this.port, auth: `:${this.password}` },res=>{
            var a = '';
            res.setEncoding("utf-8");
            res.on('data', a = (chunk)=>{
                var obj = JSON.parse(chunk);
                var media = obj.children;
                let tmp = Array.from(media);
                callBack(tmp);
            });
        })
        request.end();
    }

    getClient(){
        if(typeof this.httpclient !== 'undefined')
            return this.httpclient;
        else
            return new VLC({host:this.host, port: this.port,username:"", password: this.password});
    }
}