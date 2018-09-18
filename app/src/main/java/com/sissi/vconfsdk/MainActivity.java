package com.sissi.vconfsdk;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import com.sissi.vconfsdk.base.engine.Requester;
import com.sissi.vconfsdk.login.LoginManager;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void login(View view) {
        LoginManager loginManager = (LoginManager) Requester.instance(LoginManager.class);
        loginManager.login("server", "account", "passwd", null);
    }
}
