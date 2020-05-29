from flask import Flask, render_template, request, redirect, url_for, session, send_from_directory, send_file
from flask_socketio import SocketIO, emit, send
import sqlite3 as sql
import time
import vlcCont as VLC
import datetime
import json
import atexit
from flask_bcrypt import Bcrypt
import random
import threading
import os
import csv
import re

app = Flask(__name__)
bcrypt = Bcrypt(app)
socketio = SocketIO(app)

app.secret_key = 'squashiesjukebox'

# Keeps track of logged in users
activeUsers = []

# Keeps track of if there are no user requests in queue
queueEmpty = False

# Timer for regular announcements
videoAnnouncementTimer = 20
mobilePIN = "None"

bufferForPlaylist = []

# Current playing song
currentSong = ""

queueSize = 5

mobileAnnouncementText = "this taht "

player = VLC.VLC()

class AlreadyInQueue(Exception): pass

videoAnnouncements = []

# Grab the paths for all video announcement files
pathForAnnouncements = os.getcwd().replace('\\', '/') + '/Video Announcements/'
for root, dirs, files in os.walk(pathForAnnouncements):
    for filename in files:
        if '.mp4' in filename:
            video = 'file:///'+pathForAnnouncements+filename
            video = video.replace(' ', '%20')
            videoAnnouncements.append(video)

# file:///C:/Users/user/Desktop/PX%20Test/Video%20Announcements/J%20Balvin,%20Willy%20William%20-%20Mi%20Gente%20(Official%20Video)-wnJ6LuUFpMo.mkv



def background():
    global currentSong
    while True:
        queue = player.playlist(True)
        media = player.playlist(False)
        status = player.status()
        state = status['root']['state']
        timeStr = status['root']['time']
        dictTmp = [state, timeStr]
        socketio.emit('sync status', dictTmp)
        if queue == "empty" or "@ro" in queue:
            # use random.shuffle() with media library and add to queue
            toBeShuffled = media
            random.shuffle(toBeShuffled)

            #Repopulate queue with songs
            for i,song in enumerate(toBeShuffled):
                if i < 4:
                    if queue != 'empty':
                        if queue['@name'] != song['@name']:
                            player.add(song["@uri"])
                    else:
                        player.add(song["@uri"])
                    if i == 3:
                        time.sleep(1)
                        socketio.emit("sync for repopulated queue", player.playlist(True))
            global queueEmpty
            queueEmpty = True
            
            queue = queue = player.playlist(True)
        for song in player.playlist(True):
            if "@current" in song:
                tmp = []
                try:
                    if "@id" in song:
                        if (song['@id'] != currentSong) and (currentSong != ""):
                            player.remove(currentSong)
                            currentSong = song["@id"]
                            tmp.append(song['@uri'])
                        tmp.append(status)
                        socketio.emit("respond to sync", tmp)
                        
                        if (state == 'stopped') and (song['@uri'].find('/Video%20Announcements/') != -1):
                            player.next()
                except TypeError:
                    print(song)
                    print(currentSong)

        
#Timer for Video Announcements
def videoTimer():
    global videoAnnouncementTimer
    mins = 0
    # Loop until mins = x
    while mins <= videoAnnouncementTimer:    
        # Sleep for a minute
        time.sleep(60)
        # Increment the minute total
        mins += 1

    # Do function
    randVidAnnou()
    
    socketio.emit("sync for repopulated queue", player.playlist(True))

    #Restart timer
    videoTimer()

# Queues up video announcement
def randVidAnnou():
 

    random.shuffle(videoAnnouncements)

    player.add(videoAnnouncements[0])
    



#Removes non user requested queue songs
def removeBufferInPlaylist():
    queue = player.playlist(True)
    for song in queue:
        if "@current" not in song:
            player.remove(song["@id"])
    global queueEmpty
    queueEmpty = False
        

#When the application is run, tests to see if table exists
def staffTable():
    con = sql.connect("database.db")
    cur = con.cursor()
    cur.execute('''SELECT count(name) FROM sqlite_master WHERE type='table' AND name='staff' ''' )

    if cur.fetchone()[0]==1:
        print("Staff table exists")
    else:
        print("Staff table doesn't exist")

        cur = con.cursor()

        cur.execute('CREATE TABLE [staff] (id INTEGER PRIMARY KEY, uname TEXT, userType TEXT, pwhash TEXT)')

        admin = bcrypt.generate_password_hash('admin')

        cur.execute("INSERT INTO [staff] (uname, userType, pwhash) VALUES (?,?,?)",("admin", "admin", admin))

        con.commit()
        print("Staff table created")

    con.close()

# When the application is run and there is no table for song requests
def newRequests():

    con = sql.connect("database.db")

    cur = con.cursor()
    cur.execute('''SELECT count(name) FROM sqlite_master WHERE type='table' AND name='requests' ''')

    if cur.fetchone()[0]!=1 :
        print("Request table doesn't exist")
        con.execute('CREATE TABLE [requests] (patronName TEXT, songName TEXT, email TEXT)')
        print("Request table created")

        con.commit()
    else:

        print("Request table exists")

    con.close()

# When the application is run and there is no table for queued songs
def newTable():

    con = sql.connect("database.db")

    cur = con.cursor()
    cur.execute('''SELECT count(name) FROM sqlite_master WHERE type='table' AND name='queued' ''')

    if cur.fetchone()[0]==1 : 
        print('Queued table exists.')
    else:
        print('Queued table does not exist.')

        con.execute('CREATE TABLE [queued] (songName TEXT, queuer TEXT, [timeStamp] DATETIME)')
        print("Queued table created successfully")

        con.commit()
    con.close()
 


#Login
@app.route("/", methods=['GET','POST'])
def index():

    if request.method == 'POST':

        con = sql.connect("database.db")
        cur = con.cursor()

        cur.execute('''SELECT uname, userType, pwhash FROM staff''')
        rows = cur.fetchall();

        # checks form data and compares to registered users
        for row in rows:

            if row[0] == request.form['uname']:

                if bcrypt.check_password_hash(row[2], request.form['psw']) == True:

                    if request.form['uname'] not in activeUsers:

                        activeUsers.append(request.form['uname'])
                        session['username'] = request.form['uname']
                        session['userType'] = row[1]
                        print(request.form['uname'] + " logged in")


                        with open('log.txt', "a+") as file:
                            stamp = datetime.datetime.now()
                            file.write( str(stamp)[:16] + " " + request.form['uname'] + " logged in" + "\n")


                        return redirect(url_for('media'))

                    else:

                        error = "User already logged in"
                        return render_template('login.html', error = error)

                else:

                    error = "Invalid Username/Password"
                    return render_template('login.html', error = error)

        error = "Invalid Username/Password"
        return render_template('login.html', error = error)

        

    if 'username' in session:
        return redirect(url_for('media'))
    return render_template('login.html')


#Media page
@app.route("/Media")
def media():

    if 'username' not in session:
        return redirect(url_for('index'))

    songs = player.playlist(False)
    queue = player.playlist(True)

    return render_template('media.html', songs = songs, queue = queue, player = player)

#Adds songs from media library to playlist/queue
@app.route('/Songs')
def addToPlaylist():
    media = player.playlist(False)
    queue = player.playlist(True)


    global queueEmpty

    chosenSong = ""

    if "id" in request.args:
        argID = request.args["id"]
        queuer = session["username"]
    else:
        argID = request.args["mobileID"]
        queuer = request.args["queuer"]

    try:

        for song in media:
            if song["@id"] == argID:
                location = song["@uri"]
                chosenSong = song

                if '@ro' not in queue:
                    for track in queue:
                        if track["@name"] == song["@name"]:
                            raise AlreadyInQueue
                
                try:
                    stamp = datetime.datetime.now()
                    stamp = str(stamp)[:16]

                    with sql.connect("database.db") as con:
                        cur = con.cursor()
                        cur.execute("INSERT INTO [queued] (songName, queuer,[timeStamp]) VALUES (?,?,?)", (song["@name"], queuer, stamp) )
                        con.commit()
                except:
                    con.rollback()
                    print("Failed to add "+ song["@name"]+" to DB")
                finally:
                    con.close()

                if queueEmpty == True:
                    removeBufferInPlaylist()
                
                queue = player.playlist(True)

                if queueEmpty == False:
                    if len(queue) >= queueSize and '@ro' not in queue:

                        if 'id' in request.args:
                            return render_template('media.html', error = "Queue full", songs = media, queue = queue)
                        else:
                            print('Queue full')
                            chosenSong['@ro'] = '202'
                            return json.dumps(chosenSong)

                    else:
                        player.add(location)
                else:

                    if 'id' in request.args:
                        return render_template('media.html', error = "Queue full", songs = media, queue = queue)
                    else:
                        print('Queue full')
                        chosenSong['@ro'] = '202'
                        return json.dumps(chosenSong)


                with open('log.txt', "a+") as file:
                    stamp = datetime.datetime.now()
                    file.write( str(stamp)[:16] + " " + track['@name'] + " added to queue by " + queuer + "\n")
                time.sleep(2.5)
                queue = player.playlist(True)
                socketio.emit("sync for repopulated queue", queue)
                print(song["@name"],"added to playlist")
                break

    except AlreadyInQueue:
        if 'id' in request.args:
            return render_template('media.html', error = "Song already in queue", songs = media, queue = queue)
        else:
            print('Already in queue')
            chosenSong['@ro'] = '201'
            return json.dumps(chosenSong)

    if "id" in request.args:

        return redirect(url_for('media'))
    else:
        chosenSong['@ro'] = '200'
        return json.dumps(chosenSong)

#Removes song from playlist
@app.route('/Queue/<songID>')
def removeFromQueue(songID):
    media = player.playlist(True)
    songs = player.playlist(False)
    if "@ro" in media:

        if "@current" in media:
            print("Cannot remove "+ media["@name"] +" from playlist, because the song is currently playing")
            return render_template('media.html', error = "Cannot remove song because song is playing", songs = songs, queue = media)
        else:
            player.remove(songID)
            print(media["@name"]+" removed from playlist")
            socketio.emit("web remove song from queue", media["@uri"])
            time.sleep(2)
            render_template('media.html', songs = player.playlist(False), queue = player.playlist(True))
    else:

        for song in media:
            if song["@id"] == songID:
                if "@current" in song:
                    error = "Cannot remove song from playlist, because the song is currently playing"
                    return render_template('media.html', error = error, songs = songs, queue = media)
                else:
                    player.remove(songID)
                    print(song["@name"]+" removed from playlist")
                    socketio.emit("web remove song from queue", song["@uri"])
                    time.sleep(2)
                    render_template('media.html', songs = player.playlist(False), queue = player.playlist(True))
                    

    with open('log.txt', "a+") as file:
        stamp = datetime.datetime.now()
        for song in media:
            if song['@id'] == songID:
                file.write( str(stamp)[:16] + " " + session['username'] + " removed "+ song['@name'] + " from the queue\n")

    return render_template('media.html', songs = player.playlist(False), queue = player.playlist(True))

# Query for queued songs
@app.route('/Statistics', methods = ['GET','POST'])
def statistics():

    if 'username' not in session:
        return redirect(url_for('index'))

    with sql.connect("database.db") as con:
            cur = con.cursor()
            cur.execute("SELECT * FROM queued")
            rows= cur.fetchall()

    media = player.playlist(False)
    songs = []

    for song in media:
        songs.append(song['@name'])


    if request.method == 'POST':

        con = sql.connect("database.db")
        cur = con.cursor()

        date1 = request.form['date1']
        date2 = request.form['date2']

        date1 = date1.replace('T', ' ')
        date2 = date2.replace('T', ' ')


        # Checks date validity
        if date1 < date2:


            if 'songs' in request.form:
                
                cur.execute("SELECT * FROM queued WHERE [timeStamp] >= '%s' AND [timeStamp] <='%s' AND songName = '%s'" % (date1, date2, request.form['songs']))
                rows = cur.fetchall()

                # Creates csv file of query data
                with open('CSV/statsResults.csv', mode='wt', newline='') as statsCSV:
                    stats_writer = csv.writer(statsCSV, delimiter=',', quotechar='"', quoting=csv.QUOTE_MINIMAL)
                    stats_writer.writerow(['Song Name', 'Queuer', 'Timestamp'])

                    for result in rows:
                        stats_writer.writerow([result[0], result[1], result[2]])

                if request.form["query"] == "count":
                    session['data'] = rows
                    rows.reverse()
                    return render_template('statistics.html', rows=rows, songs=songs, count=len(rows))
            else:

                cur.execute("SELECT * FROM queued WHERE [timeStamp] >= '%s' AND [timeStamp] <='%s' " % (date1, date2))
                rows = cur.fetchall()

                # Creates csv file of query data
                with open('CSV/statsResults.csv', mode='wt', newline='') as statsCSV:
                    stats_writer = csv.writer(statsCSV, delimiter=',', quotechar='"', quoting=csv.QUOTE_MINIMAL)
                    stats_writer.writerow(['Song Name', 'Queuer', 'Timestamp'])

                    for result in rows:
                        stats_writer.writerow([result[0], result[1], result[2]])

                if request.form["query"] == "count":
                    session['data'] = rows
                    rows.reverse()
                    return render_template('statistics.html', rows=rows, songs=songs, count=len(rows))


    # Creates csv file of query data
    with open('CSV/statsResults.csv', mode='wt', newline='') as statsCSV:
        stats_writer = csv.writer(statsCSV, delimiter=',', quotechar='"', quoting=csv.QUOTE_MINIMAL)
        stats_writer.writerow(['Song Name', 'Queuer', 'Timestamp'])

        for result in rows:
            stats_writer.writerow([result[0], result[1], result[2]])

    rows.reverse()
    return render_template('statistics.html', rows=rows, songs=songs)

# Announcements
@app.route('/Announcements')
def announcements():

    if 'username' not in session:
        return redirect(url_for('index'))

    global videoAnnouncementTimer
    global mobileAnnouncementText

    return render_template('announcements.html', announcementVids=videoAnnouncements, videoAnnouncementTimer=videoAnnouncementTimer, mobileAnnouncementText = mobileAnnouncementText)

# Play emergency announcement
@app.route('/Announcements/<announcement>')
def playAnnouncement(announcement):
    if 'username' not in session:
        return redirect(url_for('index'))

    try:
        global currentSong
        player.addPlaying(videoAnnouncements[int(announcement)])
    except:
        return 'failed to play announcement'

    return redirect(url_for('announcements'))

# Requests
@app.route('/SongRequests')
def songRequests():

    with sql.connect("database.db") as con:
            cur = con.cursor()
            cur.execute("SELECT * FROM requests")
            requests = cur.fetchall()


    # Creates csv file of query data
    with open('CSV/requests.csv', mode='wt', newline='') as requestCSV:
        request_writer = csv.writer(requestCSV, delimiter=',', quotechar='"', quoting=csv.QUOTE_MINIMAL)
        request_writer.writerow(['Patron Name ', 'Song Name', 'Email'])

        for result in requests:
            request_writer.writerow([result[0], result[1], result[2]])

    return render_template('requests.html', requests = requests)

#Remove requests
@app.route('/Requests/<record>')
def deleteRequests(record):
    patron, song, email = record.split('|')

    with sql.connect("database.db") as con:
            cur = con.cursor()
            cur.execute("DELETE FROM requests WHERE patronName = '%s' AND songName = '%s' AND email = '%s'" % (patron,song, email))
            con.commit()

    return redirect(url_for('songRequests'))



#settings
@app.route('/Settings')
def settings():

    if 'username' not in session:
        return redirect(url_for('index'))

    return render_template('settings.html', queueSize=queueSize, mobilePIN = mobilePIN)

# Create new account
@app.route('/Settings/NewAccount', methods = ['GET','POST'])
def newAccount():

    if 'username' not in session:
        return redirect(url_for('index'))

    if request.method == 'POST':

        with sql.connect("database.db") as con:
            cur = con.cursor()
            cur.execute("SELECT uname FROM staff")
            staff= cur.fetchall()

            for user in staff:
                if request.form['uname'] == user[0]:

                    print('Username already exists')
                    error = "Username already exists"
                    return render_template('newAccount.html', error=error)

        if request.form['psw1'] == request.form['psw2']:
            newUser = request.form['uname']
            newUserType = request.form['usertype']
            newUserPsw = bcrypt.generate_password_hash(request.form['psw2'])
            try:
                cur = con.cursor()
                cur.execute("INSERT INTO [staff] (uname, userType, pwhash) VALUES (?,?,?)", (newUser, newUserType, newUserPsw) )
                con.commit()
            except:
                con.rollback()
                print('There was an error with the database')
            finally:
                con.close()
                print('New User added: ' + newUser)

                with open('log.txt', "a+") as file:
                    stamp = datetime.datetime.now()
                    file.write( str(stamp)[:16] + " " + session['username'] + " created an account ("+newUser+", "+newUserType+")" + "\n")

            return render_template('settings.html', msg='Account created!')

        else:
            print('Passwords do not match')
            con.close()
            error = "Passwords do not match"
            return render_template('newAccount.html', error=error)

    return render_template('newAccount.html')


# List users to change passwords
@app.route('/Settings/ChangePassword')
def changePassword():

    if 'username' not in session:
        return redirect(url_for('index'))

    with sql.connect('database.db') as con:
        cur = con.cursor()
        cur.execute("SELECT * FROM staff")
        users = cur.fetchall()

    if 'msg' in request.args:
        return render_template('changePassword.html',users = users, msg=request.args['msg'])

    return render_template('changePassword.html',users = users)

# List users to change passwords
@app.route('/Settings/ChangePassword/<user>', methods = ['GET','POST'])
def changePasswordUser(user):

    if 'username' not in session:
        return redirect(url_for('index'))

    with sql.connect('database.db') as con:
        cur = con.cursor()
        cur.execute("SELECT * FROM staff")
        users = cur.fetchall()
        for staff in users:
            if staff[1] == user:
                user = staff


    if request.method =='POST':
        if request.form['psw1'] == request.form['psw2']:
            newpwhash = bcrypt.generate_password_hash(request.form['psw2'])
            with sql.connect('database.db') as con:
                cur = con.cursor()
                cur.execute('''UPDATE staff SET pwhash = "%s" WHERE uname = "%s" ''' % (newpwhash.decode(),user[1]))
                con.commit()

            with open('log.txt', "a+") as file:
                    stamp = datetime.datetime.now()
                    file.write( str(stamp)[:16] + " " + session['username'] + " changed password for "+ user[1] + "\n")

            return redirect(url_for('changePassword',msg='Password changed'))
        else:
            print('Passwords do not match')

    return render_template('changePasswordUser.html',user = user)

#List users to delete
@app.route('/Settings/RemoveAccount')
def removeUsers():

    if 'username' not in session:
        return redirect(url_for('index'))

    with sql.connect('database.db') as con:
        cur = con.cursor()
        cur.execute("SELECT * FROM staff")
        users = cur.fetchall()

    if 'user' in request.args:

        with sql.connect('database.db') as con:
            cur = con.cursor()
            cur.execute("DELETE FROM staff WHERE id = '%s'" % (request.args['user']))
            con.commit()

        with open('log.txt', "a+") as file:
            stamp = datetime.datetime.now()
            for user in users:
                if user[0] == int(request.args['user']):
                    file.write( str(stamp)[:16] + " " + session['username'] + " removed account for "+ user[1] + "\n")

        return render_template('settings.html',msg='User deleted')


    return render_template('removeAccount.html',users = users)

#Log
@app.route('/Settings/Log')
def log():

    if 'username' not in session:
        return redirect(url_for('index'))

    try:
        with open('log.txt', "r") as file:
            log = file.readlines()
            log.reverse()
    except:
        open('log.txt', "x")
        return redirect(url_for('log'))

    return render_template('log.html',log=log)

#Handles song requests
@app.route('/Requests')
def requests():

    if len(request.args) == 3 and 'patronName' in request.args and 'songName' in request.args and 'emailAddress' in request.args:
        patronName = request.args['patronName']
        songName = request.args['songName']
        email = request.args['emailAddress']

        with sql.connect('database.db') as con:
                cur = con.cursor()
                cur.execute("INSERT INTO [requests] (patronName, songName, email) VALUES (?,?,?)", (patronName, songName, email) )
                con.commit()

        response = {'patronName':'200', 'songName':songName, 'emailAddress':email}
        return json.dumps(response)

    return 'Doesnt Works'

# Calls when video timer variable is changed on website
@app.route('/ChangeVideoTimer/<time>')
def changeVidTimer(time):

    global videoAnnouncementTimer
    videoAnnouncementTimer = int(time)
    print('Video timer changed to '+str(videoAnnouncementTimer)+' mins')
    return 'Timer changed to go off every ' + str(videoAnnouncementTimer) + ' seconds'

# Calls when queue size variable is changed on website
@app.route('/ChangeQueueSize/<size>')
def changeQueueSize(size):

    global queueSize
    queueSize = int(size)
    print('Queue size changed to '+str(queueSize))
    socketio.emit("web change queue size", queueSize)
    return 'Timer changed to go off every ' + str(videoAnnouncementTimer) + ' seconds'

@app.route('/GenerateNewPIN/<pinNumber>')
def generateNewPin(pinNumber):

    global mobilePIN
    mobilePIN = pinNumber
    print('Mobile PIN changed to '+str(mobilePIN))
    sync_pin_number()
    return mobilePIN


#Sends play command to VLC
@app.route('/PlayVLC')
def playVLC():
    player.play()
    return 'play'

#Sends pause command to VLC
@app.route('/PauseVLC')
def pauseVLC():
    player.pause()
    return 'pause'

#Sends previous command to VLC
@app.route('/PreviousVLC')
def previousVLC():
    player.previous()
    return 'previous'

#Sends next command to VLC
@app.route('/NextVLC')
def nextVLC():
    player.next()
    return 'next'

#Returns the media library
@app.route('/mediaLibrary.json')
def mediaLibrary():
    if len(request.args) == 1 and 'queue' in request.args and request.args['queue'] == "true":
        mediaLibrary = json.dumps(player.playlist(True))
    else:
        mediaLibrary = json.dumps(player.playlist(False))

    return mediaLibrary

# Returns currently playing song
@app.route('/CurrentSong')
def currentSong():
    songs = player.playlist(True)

    if '@ro' in songs:
        return songs['@name']

    for song in songs:
            if '@current' in song:
                return song['@name']
    
    return 'Nothing playing'

# Upon request user is prompted with file download
@app.route('/DownloadCSVStats')
def downloadCSVStats():

    return send_file(os.getcwd()+'\\CSV\\statsResults.csv', as_attachment=True, cache_timeout=1)

@app.route('/DownloadCSVRequests')    
def downloadCSVRequests():

    return send_file(os.getcwd()+'\\CSV\\requests.csv', as_attachment=True, cache_timeout=1)

# Logs the user out of the system
@app.route('/logout')
def logout():
    if 'username' in session:
        if session['username'] in activeUsers:
            activeUsers.remove(session['username'])
        session.pop('username', None)
    return redirect(url_for('index'))

@app.route('/ChangeMobileAnnouncement/<newAnnouncement>')
def web_change_mobile_announcement(newAnnouncement):
    if 'username' not in session:
        return redirect(url_for('index'))
    change_mobile_announcement_text(newAnnouncement)
    socketio.emit('web modify mobile announcement text', newAnnouncement)
    return newAnnouncement

@socketio.on('sync library and queue')
def respond_to_sync_library(data):
    print(data)
    queue = player.playlist(True)
    playList = player.playlist(False)
    tmp = [queue,playList, queueSize]
    emit("respond to sync with library and queue", tmp)

@socketio.on('connect')
def on_connect():
    if request.args.get('fail'):
        print("failed")
        return False
    print("connected")
    socketio.emit("connected")

@socketio.on('get pin number')
def sync_pin_number():
    socketio.emit('sync pin number', mobilePIN)
            
@socketio.on('request request list')
def request_request_list():
    with sql.connect("database.db") as con:
        cur = con.cursor()
        cur.execute("SELECT * FROM requests")
        requests = cur.fetchall()
        tmp = []
        for row in requests:
            request_as_dict = {'patronName': row[0], 'songName': row[1], 'email' : row[2]}
            tmp.append(request_as_dict)
        requests = json.dumps(tmp, ensure_ascii=False)
        socketio.emit('respond request request list', requests)

@socketio.on('add song to queue from tablet')
def add_song_to_queue(data):
    jsonObj = json.loads(data)
    print(jsonObj["@name"])
    stamp = datetime.datetime.now()
    stamp = str(stamp)[:16]
    with sql.connect("database.db") as con:
        cur = con.cursor()  
        cur.execute("INSERT INTO [queued] (songName, queuer,[timeStamp]) VALUES (?,?,?)", (jsonObj["@name"], "Tablet", stamp) )
        con.commit()
    player.add(jsonObj["@uri"])
    time.sleep(0.5)
    socketio.emit("sync for repopulated queue", player.playlist(True))
    
@socketio.on('add request from tablet')
def add_request(data):
    jsonObj = json.loads(data)
    with sql.connect('database.db') as con:
                cur = con.cursor()
                cur.execute("INSERT INTO [requests] (patronName, songName, email) VALUES (?,?,?)", (jsonObj["patronName"], jsonObj["songName"],jsonObj["email"]) )
                con.commit()
                cur.execute("SELECT * FROM requests")
                requests = cur.fetchall()
                tmp = []
                for row in requests:
                    request_as_dict = {'patronName': row[0], 'songName': row[1], 'email' : row[2]}
                    tmp.append(request_as_dict)
                jsonObj = json.dumps(tmp, ensure_ascii=False)
    con.close()
    socketio.emit("respond add request from tablet", jsonObj)
    
@socketio.on('remove song from queue')
def remove_song_from_queue(data):
    jsonObj = json.loads(data)
    queue = player.playlist(True)
    for song in queue:
        if jsonObj["@id"] == song["@id"]:
            player.remove(jsonObj["@id"])
            print("remove " + jsonObj["@name"])
    socketio.emit("removed song from queue", jsonObj["@id"])
            
@socketio.on('modify queue')
def tablet_modify_queue(data):
    jsonObj = json.loads(data)
    newQueueTmp = []
    for i in range(len(jsonObj)):
        if i > 0:
            newQueueTmp.append(jsonObj[i])
    for song in newQueueTmp:
        player.remove(song["@id"])
    
    for song in newQueueTmp:
        player.add(song["@uri"])
    time.sleep(1)
    socketio.emit("sync for repopulated queue", player.playlist(True))
    print("queue modified!")
    
@socketio.on('tablet modify mobile announcement text')
def change_mobile_announcement_text(data):
    global mobileAnnouncementText
    strTmp = str(data)
    mobileAnnouncementText = strTmp
    print(mobileAnnouncementText)
    socketio.emit("broadcast modified announcement", strTmp)
    
@socketio.on('request statistics log')
def request_statistics_log():
    with sql.connect('database.db') as con:
                cur = con.cursor()
                cur.execute("SELECT songName, queuer, strftime('%d-%m-%Y', timeStamp) as timeStamp FROM queued")
                queueds = cur.fetchall()
                tmp = []
                for row in queueds:
                    queued_as_dict = {'songName': row[0], 'queuer': row[1], 'timeStamp': row[2]}
                    tmp.append(queued_as_dict)
                jsonObj = json.dumps(tmp, ensure_ascii=False)
    con.close()
    socketio.emit("respond request statistic log", jsonObj)
    
@socketio.on('tablet request pause play')
def tablet_request_pause():
    pauseVLC()
        
@socketio.on('tablet request next')
def tablet_request_pause():
    nextVLC()
    
    
def exit_handler():
    print("Squashies Jukebox shutting down...")
    
if __name__ == "__main__":
    # Checks if tables exist in db
    newTable()
    staffTable()
    newRequests()

    videoT = threading.Thread(target=videoTimer)
    videoT.daemon=True
    videoT.start()

    
    back = threading.Thread(target=background)
    back.daemon=True
    back.start()
    atexit.register(exit_handler)
    socketio.run(app, host = '0.0.0.0', port = 5000, debug = False)
