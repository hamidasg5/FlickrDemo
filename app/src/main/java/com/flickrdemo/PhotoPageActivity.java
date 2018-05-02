package com.flickrdemo;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

public class PhotoPageActivity extends AppCompatActivity
{
    private static final String TAG = "PhotoPageActivity";

    private WebView mWebView;
    private Uri mUri;
    private ProgressBar mProgressBar;

    public static Intent newIntent(Context context, Uri photoPageUri)
    {
        Intent intent = new Intent(context, PhotoPageActivity.class);
        intent.setData(photoPageUri);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_page);

        final Toolbar mToolbar = findViewById(R.id.photo_page_toolbar);
        setSupportActionBar(mToolbar);

        mWebView = findViewById(R.id.photo_page);
        mUri = getIntent().getData();

        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.setWebChromeClient(new WebChromeClient(){
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress == 100)
                {
                    mProgressBar.setVisibility(View.GONE);
                }
                else
                {
                    mProgressBar.setVisibility(View.VISIBLE);
                    mProgressBar.setProgress(newProgress);
                }
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                mToolbar.setTitle(title);
            }
        });
        mWebView.setWebViewClient(new WebViewClient());
        mWebView.loadUrl(mUri.toString());

        mProgressBar = findViewById(R.id.progressBar);


    }
}
