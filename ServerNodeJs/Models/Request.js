var Sequelize = require('sequelize');
var bcrypt = require('bcrypt');

var sequelize = new Sequelize({dialect: 'sqlite', storage: './database.db'});


var Request = sequelize.define('request', {
  Patron_name: {
      type: Sequelize.STRING,
      unique: true,
      allowNull: false
  },Song_Name: {
      type: Sequelize.STRING,
      allowNull: false
  },Email: {
      type: Sequelize.STRING,
      unique: false,
      allowNull: false
  } 
});

// create all the defined tables in the specified database.
sequelize.sync()
    .then(() => console.log('requests table has been successfully created, if one doesn\'t exist'))
    .catch(error => console.log('This error occured', error));


module.exports = Request;
