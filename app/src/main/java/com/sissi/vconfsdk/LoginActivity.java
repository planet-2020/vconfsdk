package com.sissi.vconfsdk;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.sissi.vconfsdk.base.RequestAgent;
import com.sissi.vconfsdk.login.LoginManager;
import com.sissi.vconfsdk.login.MemberStateManager;
import com.sissi.vconfsdk.utils.KLog;

public class LoginActivity extends AppCompatActivity
        implements LoginManager.OnLoginResultListener, MemberStateManager.OnMemberStateChangedListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
    }

    @Override
    protected void onStart() {
        super.onStart();
        KLog.p("-->");
    }

    @Override
    protected void onResume() {
        super.onResume();
        KLog.p("-->");
    }

    @Override
    protected void onPause() {
        super.onPause();
        KLog.p("-->");
    }

    @Override
    protected void onStop() {
        super.onStop();
        KLog.p("-->");
    }

    public void login(View view) {
        LoginManager loginManager = (LoginManager) RequestAgent.instance(LoginManager.class);
        loginManager.login("server", "account", "passwd", this);

        MemberStateManager memberStateManager = (MemberStateManager) RequestAgent.instance(MemberStateManager.class);
        memberStateManager.addOnMemberStateChangedListener(this);
    }

    @Override
    public void onLoginSuccess() {
        KLog.p("####");
        startActivity(new Intent(this, MainActivity.class));
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
