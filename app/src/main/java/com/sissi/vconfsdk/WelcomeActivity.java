package com.sissi.vconfsdk;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;

import com.sissi.vconfsdk.base.RequestAgent;
import com.sissi.vconfsdk.startup.StartManager;

public class WelcomeActivity extends AppCompatActivity
        implements StartManager.OnStartupResultListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
    }

    @Override
    protected void onResume() {
        super.onResume();

        StartManager startManager = (StartManager) RequestAgent.instance(StartManager.class);
        startManager.startup(0, this);
    }

    @Override
    public void onStartupSuccess() {
        new Handler().postDelayed(() -> startActivity(new Intent(this, LoginActivity.class)), 4000);
    }

    @Override
    public void onStartupFailed(int errCode) {

    }
}
