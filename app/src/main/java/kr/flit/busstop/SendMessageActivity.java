package kr.flit.busstop;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class SendMessageActivity extends AppCompatActivity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_message);
        webView = (WebView)findViewById(R.id.webView);

        setupWebSettings(webView);

        String url = "http://busstop-chat.meteor.com/sender";
//        webView.setWebChromeClient(new WebChromeClient(){
//
//                                      oncom
//                                   }
//        );
//

        webView.setWebViewClient(new WebViewClient(){
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                findViewById(R.id.progress).setVisibility(View.GONE);
            }
        });
        webView.loadUrl(url);
    }


    private void setupWebSettings(WebView webview) {
        WebSettings s = webview.getSettings();

//    	s.setUseWideViewPort(false);
        s.setJavaScriptEnabled(true);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setDomStorageEnabled(true);
        s.setAppCacheEnabled(true);
        s.setDatabaseEnabled(true);
//        s.setPluginState(WebSettings.PluginState.ON);

        s.setSupportMultipleWindows(true);

    }
}
