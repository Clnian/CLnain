package com.example.clnain;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;

public class SplashActivity extends Activity {
    private int seconds = 6;
    private boolean skipping = false;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        goToMain();
    }

    private void goToMain() {
        final TextView timeView = findViewById(R.id.tv_count);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (seconds == 0 || skipping) {
                    startActivity(new Intent(SplashActivity.this, MainActivity.class));
                    finish();
                } else {
                    timeView.setText(seconds + "S");
                    seconds--;
                    handler.postDelayed(this, 1000);
                }
            }
        }, 1000);
    }

    // 点击取消按钮跳过等待
    public void onClickCancel(View view) {
        skipping = true;
    }

    // 避免内存泄漏
    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}