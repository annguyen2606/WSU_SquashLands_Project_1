

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
}