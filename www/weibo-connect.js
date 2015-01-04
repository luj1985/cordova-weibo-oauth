var exec = require('cordova/exec');
var weiboPlugin = {
  authorize : function(s, f) {
    exec(s, f, "WeiboConnect", "authorize", []);
  },
  isInstalled : function(s, f) {
    exec(s, f, "WeiboConnect", "isInstalled", []);
  }
}
module.exports = weiboPlugin;
