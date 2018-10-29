package com.amp.passlink;

import android.app.Activity;
import android.app.ProgressDialog;
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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amp.passlink.Utils.SharedPreferencesUtils;
import com.bumptech.glide.Glide;
import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Response;
import com.leocardz.link.preview.library.LinkPreviewCallback;
import com.leocardz.link.preview.library.SourceContent;
import com.leocardz.link.preview.library.TextCrawler;
import com.splunk.mint.Mint;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    private static final int RC_BARCODE_CAPTURE = 9001;

    @BindView(R.id.scan_button)
    LinearLayout scan_button;

    @BindView(R.id.set_up_card)
    CardView set_up_card;

    @BindView(R.id.sync_check_ll)
    LinearLayout sync_check_ll;

    @BindView(R.id.send_button)
    Button send_button;

    @BindView(R.id.preview_ll)
    LinearLayout preview_ll;

    @BindView(R.id.preview_card_view)
    CardView preview_card_view;

    @BindView(R.id.connectNewButton)
    TextView connectNewButton;

    @BindView(R.id.preview_title)
    TextView preview_title;

    @BindView(R.id.preview_link)
    TextView preview_link;

    @BindView(R.id.preview_description)
    TextView preview_description;

    @BindView(R.id.preview_image)
    ImageView preview_image;

    private LinkPreviewCallback linkPreviewCallback;
    private String copiedText = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        Mint.initAndStartSession(this.getApplication(), "49fa363d");
        try {
            getSupportActionBar().hide();
        } catch (Exception e) {
            e.printStackTrace();
        }

       /* send_button = (Button) findViewById(R.id.send_button);
        receive_button = (Button) findViewById(R.id.receive_button);
        send_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, SendActivity.class));
            }
        });
        receive_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, RecieveActivity.class));
            }
        });*/

        linkPreviewCallback = new LinkPreviewCallback() {
            @Override
            public void onPre() {
                // Any work that needs to be done before generating the preview. Usually inflate
                // your custom preview layout here.
                /*progress_bar.setVisibility(View.VISIBLE);
                loading_layout.setVisibility(View.VISIBLE);
                progress_bar.show();*/
                preview_card_view.setVisibility(View.VISIBLE);
                preview_link.setText(copiedText);
                preview_title.setText("Copied text");
            }

            @Override
            public void onPos(SourceContent sourceContent, boolean b) {
                // Populate your preview layout with the results of sourceContent.


                // progress_bar.hide();
                //  progress_bar.setVisibility(View.GONE);
                //  loading_layout.setVisibility(View.GONE);
                preview_title.setText(sourceContent.getTitle());
                preview_description.setText(sourceContent.getDescription());
                preview_link.setText(sourceContent.getCannonicalUrl());
                List<String> images = sourceContent.getImages();
                if (images != null && !images.isEmpty()) {
                    preview_image.setVisibility(View.VISIBLE);
                    Glide.with(MainActivity.this).load(images.get(0)).into(preview_image);
                } else {
                    preview_image.setVisibility(View.GONE);
                }
            }
        };

        String uniqueID = (String) SharedPreferencesUtils.getParam(this, SharedPreferencesUtils.SHARED_PREF_UNIQUEID, "");
        if (!uniqueID.isEmpty()) {
            set_up_card.setVisibility(View.GONE);
            preview_ll.setVisibility(View.VISIBLE);
            // send_button.setVisibility(View.VISIBLE);
            connectNewButton.setVisibility(View.VISIBLE);
        }

        scan_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, BarCodeActivity.class);
                intent.putExtra("action", "receive");
                startActivityForResult(intent, RC_BARCODE_CAPTURE);
            }
        });

        send_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (copiedText.isEmpty())
                    Toast.makeText(MainActivity.this, "No copied link to send.", Toast.LENGTH_LONG).show();
                else {
                    sendDataToServer();
                }
            }

        });

    }

    private void sendDataToServer() {
        String uniqueID = (String) SharedPreferencesUtils.getParam(this, SharedPreferencesUtils.SHARED_PREF_UNIQUEID, "");
        JsonObject json = new JsonObject();
        json.addProperty("uniqueID", uniqueID);
        json.addProperty("type", "MOBILE");
        json.addProperty("url", copiedText);

        Ion.with(this)
                .load("https://ribbon-peak.glitch.me/send")
                .setJsonObjectBody(json)
                .asString()
                .withResponse()
                .setCallback(new FutureCallback<Response<String>>() {
                    @Override
                    public void onCompleted(Exception e, Response<String> result) {
                        // do stuff with the result or error

                        if (result.getHeaders().code() == 200) {
                            Log.d("amal", "send response: 200");

                        } else {
                            Toast.makeText(MainActivity.this, "Sync Failed. Try again.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        String uniqueID = (String) SharedPreferencesUtils.getParam(this, SharedPreferencesUtils.SHARED_PREF_UNIQUEID, "");
        if (!uniqueID.isEmpty()) {
            checkClipboard();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == RC_BARCODE_CAPTURE) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    String result = data.getStringExtra("result");
                    Log.d("amal", "result: " + result);
                    sendSetUpToServer(result);
                } else {
                    Log.d("Amal", "No barcode captured, intent data is null");
                }
            } else {

            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void sendSetUpToServer(final String uniqueId) {

        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Syncing...");
        progressDialog.show();

        JsonObject json = new JsonObject();
        json.addProperty("uniqueID", uniqueId);
        json.addProperty("deviceID", (String) SharedPreferencesUtils.getParam(this, SharedPreferencesUtils.SHARED_PREF_REGID, ""));

        Ion.with(this)
                .load("https://ribbon-peak.glitch.me/setup")
                .setJsonObjectBody(json)
                .asString()
                .withResponse()
                .setCallback(new FutureCallback<Response<String>>() {
                    @Override
                    public void onCompleted(Exception e, Response<String> result) {
                        // do stuff with the result or error
                        progressDialog.dismiss();
                        if (result.getHeaders().code() == 200) {
                            Log.d("amal", "response: 200");
                            SharedPreferencesUtils.setParam(MainActivity.this, SharedPreferencesUtils.SHARED_PREF_UNIQUEID, uniqueId);
                            set_up_card.setVisibility(View.GONE);
                            sync_check_ll.setVisibility(View.VISIBLE);
                            sync_check_ll.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    sync_check_ll.setVisibility(View.GONE);
                                    preview_ll.setVisibility(View.VISIBLE);
                                    //  send_button.setVisibility(View.VISIBLE);
                                    connectNewButton.setVisibility(View.VISIBLE);
                                    checkClipboard();
                                }
                            }, 1500);
                        } else {
                            Toast.makeText(MainActivity.this, "Sync Failed. Try again.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void checkClipboard() {
        ClipboardManager myClipboard;
        myClipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clipData = myClipboard.getPrimaryClip();
        if (clipData != null) {
            ClipData.Item item = clipData.getItemAt(0);
            String text = item.getText().toString();
            if (Patterns.WEB_URL.matcher(text).matches()) {
                copiedText = text;
                TextCrawler textCrawler = new TextCrawler();
                textCrawler.makePreview(linkPreviewCallback, text);
            } else {
                if (!(boolean) SharedPreferencesUtils.getParam(this, SharedPreferencesUtils.SHARED_PREF_FIRSTSEND, false)) {
                    send_button.setText("Send a test link");
                    copiedText = "first_send";
                } else
                    Toast.makeText(this, "Couldn't detect any copied link.", Toast.LENGTH_LONG).show();
            }
        }
    }

}
