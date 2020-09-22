var Sequelize = require('sequelize');
var bcrypt = require('bcrypt');


// create a sequelize instance with our local postgres database information.
var sequelize = new Sequelize('./database.db');

// setup User model and its fields.
var Staff = sequelize.define('staffs', {
    id: {
        type: Sequelize.INTEGER,
        unique:true,
        allowNull:false
    },uname: {
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
        staff.password = bcrypt.hashSync(staff.password, salt);
      }
    },
    instanceMethods: {
      validPassword: function(password) {
        return bcrypt.compareSync(password, this.password);
      }
    }    
});

// create all the defined tables in the specified database.
sequelize.sync()
    .then(() => console.log('users table has been successfully created, if one doesn\'t exist'))
    .catch(error => console.log('This error occured', error));

// export User model for use in other files.
module.exports = Staff;