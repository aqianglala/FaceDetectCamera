package com.example.myfacedetectcamera.activities;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.example.myfacedetectcamera.MainActivity;
import com.example.myfacedetectcamera.R;

public class WebActivity extends AppCompatActivity {

    private static final String KEY_URL = "key_url";
    private WebView mWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);
        String url = getIntent().getStringExtra(KEY_URL);
        if (TextUtils.isEmpty(url)) {
            Toast.makeText(this, "url为空！", Toast.LENGTH_SHORT).show();
            return;
        }
        mWebView = new WebView(this);
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setSupportZoom(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setDefaultTextEncodingName("utf-8");
        webSettings.setLoadsImagesAutomatically(true);

        //调用JS方法.安卓版本大于17,加上注解 @JavascriptInterface
        webSettings.setJavaScriptEnabled(true);

        mWebView.loadUrl(url);
        FrameLayout root = findViewById(R.id.fl_content);
        root.addView(mWebView);
    }

    public static void start(Context context, String url) {
        Intent intent = new Intent(context, WebActivity.class);
        intent.putExtra(KEY_URL, url);
        context.startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mWebView != null) {
            mWebView.setVisibility(View.GONE);
            mWebView.removeAllViews();
            mWebView.destroy();
        }
    }
}
