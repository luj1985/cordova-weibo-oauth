var exec = require('cordova/exec');
var weiboPlugin = {
  login : function(success, error) {
    exec(success, error, "WeiboConnect", "login", []);
  }
}
module.exports = weiboPlugin;
