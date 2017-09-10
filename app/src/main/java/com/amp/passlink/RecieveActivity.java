package com.amp.passlink;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class RecieveActivity extends AppCompatActivity {

    private static final int RC_BARCODE_CAPTURE = 9001;
    private TextView title_tv, share_link;
    private Button try_again;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recieve);

        try {
            getSupportActionBar().hide();
        } catch (Exception e) {
            e.printStackTrace();
        }

        title_tv = (TextView) findViewById(R.id.title_tv);
        share_link = (TextView) findViewById(R.id.share_link);
        try_again = (Button) findViewById(R.id.try_again);
        try_again.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(RecieveActivity.this, BarCodeActivity.class);
                intent.putExtra("action", "receive");
                startActivityForResult(intent, RC_BARCODE_CAPTURE);
            }
        });

        Intent intent = new Intent(RecieveActivity.this, BarCodeActivity.class);
        intent.putExtra("action", "receive");
        startActivityForResult(intent, RC_BARCODE_CAPTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_BARCODE_CAPTURE) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    String result = data.getStringExtra("result");
                    share_link.setText(result);
                    if (Patterns.WEB_URL.matcher(result).matches()) {
                        title_tv.setText("Received Link");
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(Uri.parse(result));
                        startActivity(i);
                    } else {
                        title_tv.setText("Received Text");
                    }

                } else {

                }
            } else {

            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

}
