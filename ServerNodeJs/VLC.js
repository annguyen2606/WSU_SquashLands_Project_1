
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
        this.queueSize = 5;
        let shuffle = function (array) {
            for (let i = array.length - 1; i > 0; i--) {
              let j = Math.floor(Math.random() * (i + 1));
              [array[i], array[j]] = [array[j], array[i]];
            }
        }

        this.vlcClient = new VLC({host: this.host, password: this.password, username:""});
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

    add(uri, callBack){
        console.log(typeof uri);
        this.vlcClient.addToQueue(uri);
        callBack();
    }

    remove(id, callBack){
        var request = http.get({host:this.host, path: "/requests/status.xml?command=pl_delete&id=" + id, port: this.port, auth: `:${this.password}` },res=>{
            console.log(`removing song ID ${id} from queue`);
        })
        request.end();
        callBack()
    }

    addAndPlay(uri, callBack){
        var request = http.get({host:this.host, path: "/requests/status.xml?command=in_play&input=" + encodeURI(uri), port: this.port, auth: `:${this.password}` },res=>{
            console.log(`Adding file ${uri} into queue`);
        })
        request.end();
        callBack();
    }
}