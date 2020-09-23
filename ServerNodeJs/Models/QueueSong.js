var Sequelize = require('sequelize');

var sequelize = new Sequelize({dialect: 'sqlite', storage: './database.db'});

var QueueSong = sequelize.define('queued_song', {
  Song_Name: {
      type: Sequelize.STRING,
      unique: true,
      allowNull: false
  },Queuer: {
      type: Sequelize.STRING,
      allowNull: false
  }
});


// create all the defined tables in the specified database.
sequelize.sync()
    .then(() => console.log('queued_songs table has been successfully created, if one doesn\'t exist'))
    .catch(error => console.log('This error occured', error));


module.exports = QueueSong
