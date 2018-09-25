package com.sissi.vconfsdk;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import com.sissi.vconfsdk.utils.KLog;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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

}
