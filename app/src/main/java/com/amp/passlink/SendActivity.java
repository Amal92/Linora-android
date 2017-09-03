package com.amp.passlink;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.amp.passlink.Networking.Endpoints;
import com.bumptech.glide.Glide;
import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.leocardz.link.preview.library.LinkPreviewCallback;
import com.leocardz.link.preview.library.SourceContent;
import com.leocardz.link.preview.library.TextCrawler;
import com.wang.avi.AVLoadingIndicatorView;

import java.util.List;

public class SendActivity extends AppCompatActivity {

    private static final int RC_BARCODE_CAPTURE = 9001;

    private TextView link_text, preview_title, preview_link, preview_description;
    private CardView cardView;
    private ImageView preview_image;
    private AVLoadingIndicatorView progress_bar;
    private Button send_button;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send);
        link_text = (TextView) findViewById(R.id.share_link);
        preview_title = (TextView) findViewById(R.id.preview_title);
        preview_link = (TextView) findViewById(R.id.preview_link);
        preview_description = (TextView) findViewById(R.id.preview_description);
        cardView = (CardView) findViewById(R.id.card_view);
        preview_image = (ImageView) findViewById(R.id.preview_image);
        progress_bar = (AVLoadingIndicatorView) findViewById(R.id.progress_bar);
        send_button = (Button) findViewById(R.id.send_button);
        send_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SendActivity.this, BarCodeActivity.class);

                startActivityForResult(intent, RC_BARCODE_CAPTURE);
            }
        });


        LinkPreviewCallback linkPreviewCallback = new LinkPreviewCallback() {
            @Override
            public void onPre() {
                // Any work that needs to be done before generating the preview. Usually inflate
                // your custom preview layout here.
                progress_bar.setVisibility(View.VISIBLE);
                progress_bar.show();
            }

            @Override
            public void onPos(SourceContent sourceContent, boolean b) {
                // Populate your preview layout with the results of sourceContent.
                Log.d("", "");
                cardView.setVisibility(View.VISIBLE);
                progress_bar.hide();
                progress_bar.setVisibility(View.GONE);
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

        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

        ClipData clip = clipboard.getPrimaryClip();
        if (clip != null) {
            ClipData.Item item = clip.getItemAt(0);
            String url = item.coerceToText(this).toString();
            link_text.setText(url);
            send_button.setEnabled(true);
            if (Patterns.WEB_URL.matcher(url).matches()) {
                TextCrawler textCrawler = new TextCrawler();
                textCrawler.makePreview(linkPreviewCallback, url);
            }
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_BARCODE_CAPTURE) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    String result=data.getStringExtra("result");
                    sendDataToServer(result);
                    Log.d("Amal","data to send activit  "+result);

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
        json.addProperty("value", link_text.getText().toString());

        Ion.with(this)
                .load(Endpoints.updateUrl)
                .setJsonObjectBody(json)
                .asJsonObject()
                .setCallback(new FutureCallback<JsonObject>() {
                    @Override
                    public void onCompleted(Exception e, JsonObject result) {
                        // do stuff with the result or error
                        Log.d("","");
                    }
                });
    }


}
