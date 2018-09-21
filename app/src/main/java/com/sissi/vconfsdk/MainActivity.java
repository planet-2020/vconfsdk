package com.sissi.vconfsdk;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import com.sissi.vconfsdk.base.Requester;
import com.sissi.vconfsdk.login.LoginManager;
import com.sissi.vconfsdk.login.MemberStateManager;
import com.sissi.vconfsdk.utils.KLog;

public class MainActivity extends Activity implements LoginManager.OnLoginResultListener, MemberStateManager.OnMemberStateChangedListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void login(View view) {
        LoginManager loginManager = (LoginManager) Requester.instance(LoginManager.class);
        loginManager.login("server", "account", "passwd", this);

        MemberStateManager memberStateManager = (MemberStateManager) Requester.instance(MemberStateManager.class);
        memberStateManager.addOnMemberStateChangedListener(this);
    }

    @Override
    public void onLoginSuccess() {
        KLog.p("####");
    }

    @Override
    public void onLoginFailed(int errorCode) {
        KLog.p("####");
    }

    @Override
    public void onLoginTimeout() {
        KLog.p("####");
    }

    @Override
    public void onMemberStateChanged() {
        KLog.p("####");
    }
}
