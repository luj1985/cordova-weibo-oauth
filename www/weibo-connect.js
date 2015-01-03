var exec = require('cordova/exec');
var weiboPlugin = {
  login : function() {
    exec(success, error, "WeiboConnect", "login");
  }
}
module.exports = weiboPlugin;
