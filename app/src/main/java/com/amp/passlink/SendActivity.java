package com.amp.passlink;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.amp.passlink.Networking.Endpoints;
import com.bumptech.glide.Glide;
import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Response;
import com.leocardz.link.preview.library.LinkPreviewCallback;
import com.leocardz.link.preview.library.SourceContent;
import com.leocardz.link.preview.library.TextCrawler;
import com.wang.avi.AVLoadingIndicatorView;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SendActivity extends AppCompatActivity {

    private static final int RC_BARCODE_CAPTURE = 9001;

    private TextView link_text, preview_title, preview_link, preview_description;
    private CardView cardView;
    private ImageView preview_image;
    private AVLoadingIndicatorView progress_bar;
    private Button send_button;
    private LinearLayout loading_layout;
    private String textBody = "";
    private TextView title;
    private boolean share_intent_handle_flag = false;

    private LinkPreviewCallback linkPreviewCallback;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send);

        try {
            getSupportActionBar().hide();
        } catch (Exception e) {
            e.printStackTrace();
        }


        title = (TextView) findViewById(R.id.title_tv);
        link_text = (TextView) findViewById(R.id.share_link);
        preview_title = (TextView) findViewById(R.id.preview_title);
        preview_link = (TextView) findViewById(R.id.preview_link);
        preview_description = (TextView) findViewById(R.id.preview_description);
        cardView = (CardView) findViewById(R.id.card_view);
        preview_image = (ImageView) findViewById(R.id.preview_image);
        progress_bar = (AVLoadingIndicatorView) findViewById(R.id.progress_bar);
        send_button = (Button) findViewById(R.id.send_button);
        loading_layout = (LinearLayout) findViewById(R.id.loading_layout);
        send_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ConnectivityManager cm =
                        (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                final NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                boolean isConnected = activeNetwork != null &&
                        activeNetwork.isConnectedOrConnecting();

                if (!isConnected) {
                    Snackbar.make(findViewById(R.id.coordinatorLayout),
                            "No Internet connection.", Snackbar.LENGTH_SHORT).show();
                    return;
                }


                if (textBody.isEmpty()) {
                    Snackbar.make(findViewById(R.id.coordinatorLayout),
                            "No data to send.", Snackbar.LENGTH_SHORT).show();
                } else {
                    Intent intent = new Intent(SendActivity.this, BarCodeActivity.class);
                    intent.putExtra("action", "send");
                    startActivityForResult(intent, RC_BARCODE_CAPTURE);
                }
            }
        });


        linkPreviewCallback = new LinkPreviewCallback() {
            @Override
            public void onPre() {
                // Any work that needs to be done before generating the preview. Usually inflate
                // your custom preview layout here.
                progress_bar.setVisibility(View.VISIBLE);
                loading_layout.setVisibility(View.VISIBLE);
                progress_bar.show();
            }

            @Override
            public void onPos(SourceContent sourceContent, boolean b) {
                // Populate your preview layout with the results of sourceContent.
                Log.d("", "");
                cardView.setVisibility(View.VISIBLE);
                progress_bar.hide();
                progress_bar.setVisibility(View.GONE);
                loading_layout.setVisibility(View.GONE);
                preview_title.setText(sourceContent.getTitle());
                preview_description.setText(sourceContent.getDescription());
                preview_link.setText(sourceContent.getCannonicalUrl());
                List<String> images = sourceContent.getImages();
                if (images != null && !images.isEmpty()) {
                    preview_image.setVisibility(View.VISIBLE);
                    Glide.with(SendActivity.this).load(images.get(0)).into(preview_image);
                } else {
                    preview_image.setVisibility(View.GONE);
                }
            }
        };

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                handleSendText(intent); // Handle text being sent
            }
        } else {

            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = clipboard.getPrimaryClip();
            if (clip != null) {
                ClipData.Item item = clip.getItemAt(0);
                String url = item.coerceToText(this).toString();
                if (url.isEmpty()) {
                    Snackbar mySnackbar = Snackbar.make(findViewById(R.id.coordinatorLayout),
                            "No Copied text found.", Snackbar.LENGTH_SHORT);
                    mySnackbar.show();
                }

                textBody = url;
                link_text.setText(textBody);

                if (Patterns.WEB_URL.matcher(url).matches()) {
                    TextCrawler textCrawler = new TextCrawler();
                    textCrawler.makePreview(linkPreviewCallback, url);
                    title.setText("Copied Link");
                } else {
                    title.setText("Copied Text");
                }
            }

        }

    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onRestart() {
        super.onRestart();

        if (share_intent_handle_flag) {
            share_intent_handle_flag = false;
            return;
        }

        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = clipboard.getPrimaryClip();
        if (clip != null) {
            ClipData.Item item = clip.getItemAt(0);
            String url = item.coerceToText(this).toString();
            if (textBody.equals(url)) {
                return;
            }
            if (url.isEmpty()) {
                Snackbar mySnackbar = Snackbar.make(findViewById(R.id.coordinatorLayout),
                        "No Copied text found.", Snackbar.LENGTH_SHORT);
                mySnackbar.show();
            }

            textBody = url;
            link_text.setText(textBody);

            if (Patterns.WEB_URL.matcher(url).matches()) {
                TextCrawler textCrawler = new TextCrawler();
                textCrawler.makePreview(linkPreviewCallback, url);
                title.setText("Copied Link");
            } else {
                title.setText("Copied Text");
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    private void handleSendText(Intent intent) {
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
            // Update UI to reflect text being shared
            if (Patterns.WEB_URL.matcher(sharedText).matches()) {
                title.setText("Shared Link");
            } else {
                title.setText("Shared Text");
            }
            textBody = sharedText;
            if (!sharedText.contains(" ")) {
               /* final String URL_REGEX = "^((https?|ftp)://|(www|ftp)\\.)?[a-z0-9-]+(\\.[a-z0-9-]+)+([/?].*)?$";
                Pattern p = Pattern.compile(URL_REGEX);
                Matcher m = p.matcher(sharedText);*/

                if (sharedText.contains(".")) {
                    //possible url
                    if (!textBody.startsWith("www.") && !textBody.startsWith("http://") && !textBody.startsWith("https://")) {
                        textBody = "http://" + textBody;
                    }
                }
            }
            link_text.setText(sharedText);
            Intent Bar_intent = new Intent(SendActivity.this, BarCodeActivity.class);
            Bar_intent.putExtra("action", "send");
            startActivityForResult(Bar_intent, RC_BARCODE_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        share_intent_handle_flag = true;

        if (requestCode == RC_BARCODE_CAPTURE) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    String result = data.getStringExtra("result");
                    sendDataToServer(result);
                    Log.d("Amal", "data to send activity  " + result);
                } else {
                    Log.d("Amal", "No barcode captured, intent data is null");
                }
            } else {

            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void sendDataToServer(String key) {

        JsonObject json = new JsonObject();
        json.addProperty("code", key);
        json.addProperty("value", textBody);

        Ion.with(this)
                .load(Endpoints.updateUrl)
                .setJsonObjectBody(json)
                .asString()
                .withResponse()
                .setCallback(new FutureCallback<Response<String>>() {
                    @Override
                    public void onCompleted(Exception e, Response<String> result) {
                        // do stuff with the result or error
                        Log.d("", "");
                        if (result.getHeaders().code() == 200) {
                            Snackbar snackbar = Snackbar.make(findViewById(R.id.coordinatorLayout),
                                    "Sent successfully", Snackbar.LENGTH_SHORT);
                            View sbView = snackbar.getView();
                            sbView.setBackgroundColor(getResources().getColor(R.color.dark_green_500));
                            snackbar.show();
                        }
                    }
                });
    }

}
