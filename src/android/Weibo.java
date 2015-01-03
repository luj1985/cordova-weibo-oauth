package com.ihavekey.weibo.oauth;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;

import com.sina.weibo.sdk.auth.AuthInfo;
import com.sina.weibo.sdk.auth.Oauth2AccessToken;
import com.sina.weibo.sdk.auth.WeiboAuthListener;
import com.sina.weibo.sdk.auth.sso.SsoHandler;
import com.sina.weibo.sdk.exception.WeiboException;


public class Weibo extends CordovaPlugin {
    
    private SsoHandler mSsoHandler = null;

    @Override
    public boolean execute(String action, JSONArray args,
            CallbackContext callbackContext) throws JSONException {
        if (action.equals("login")) {
            AuthInfo authInfo = this.createAuthInfo();
            this.login(authInfo, callbackContext);
            return true;
        }
        return false;
    }
    
    private AuthInfo createAuthInfo() {
        Activity activity = this.cordova.getActivity();
        String ns = activity.getPackageName();
        Resources resources = activity.getResources();

        int resKey = resources.getIdentifier("APP_KEY", "string", ns);
        int resSecret = resources.getIdentifier("APP_SECRET", "string", ns);
        int resScope = resources.getIdentifier("APP_SCOPE", "string", ns);

        String key = activity.getString(resKey);
        String secret = activity.getString(resSecret);
        // scope is optional
        String scope = resScope == 0 ? "" : activity.getString(resScope);

        AuthInfo authInfo = new AuthInfo(activity, key, secret, scope);
        return authInfo;
    }

    private void login(AuthInfo authInfo, final CallbackContext callbackContext) {
        Activity activity = this.cordova.getActivity();
        mSsoHandler = new SsoHandler(activity, authInfo);
        this.cordova.setActivityResultCallback(this);
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mSsoHandler.authorize(new AuthListener(callbackContext));
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (mSsoHandler != null) {
            mSsoHandler.authorizeCallBack(requestCode, resultCode, data);
        }
    }

    class AuthListener implements WeiboAuthListener {
        private CallbackContext context;

        public AuthListener(CallbackContext context) {
            this.context = context;
        }

        @Override
        public void onComplete(Bundle values) {
            Oauth2AccessToken accessToken = Oauth2AccessToken
                    .parseAccessToken(values);
            if (accessToken != null && accessToken.isSessionValid()) {
                String uid = accessToken.getUid();
                String token = accessToken.getToken();

                JSONObject res = new JSONObject();
                try {
                    res.put("uid", uid);
                    res.put("token", token);
                    context.success(res);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        @Override
        public void onWeiboException(WeiboException e) {
            Log.e("Cordova-Weibo", e.getMessage(), e);
        }

        @Override
        public void onCancel() {
            context.error(0);
        }
    }
}
