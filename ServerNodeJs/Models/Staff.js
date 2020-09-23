var Sequelize = require('sequelize');
var bcrypt = require('bcrypt');
var sequelize = new Sequelize({dialect: 'sqlite', storage: './database.db'});

var Staff = sequelize.define('staff', {
    uname: {
        type: Sequelize.STRING,
        unique: true,
        allowNull: false
    },pwhash: {
        type: Sequelize.STRING,
        allowNull: false
    },userType: {
        type: Sequelize.STRING,
        unique: false,
        allowNull: false
    } 
}, {
    hooks: {
      beforeCreate: (staff) => {
        const salt = bcrypt.genSaltSync();
        staff.pwhash = bcrypt.hashSync(staff.pwhash, salt);
      }
    }  
});

Staff.prototype.validPassword = function (password) {
  return bcrypt.compareSync(password, this.pwhash);
}

// create all the defined tables in the specified database.
sequelize.sync()
    .then(() => console.log('staffs table has been successfully created, if one doesn\'t exist'))
    .catch(error => console.log('This error occured', error));

// export User model for use in other files.
module.exports = Staff;
