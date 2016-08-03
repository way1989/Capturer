package com.way.capture.activity;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.way.capture.R;

public class FeedbackActivity extends BaseActivity {
    private EditText mEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mEditText = (EditText) findViewById(R.id.inputText);
    }

    public void onPost(View view) {
        String message = mEditText.getText().toString();
        if (TextUtils.isEmpty(message) || message.length() < 10) {
            Snackbar.make(view, R.string.feedback_text_empty, Snackbar.LENGTH_SHORT).show();
            return;
        }
        Snackbar.make(view, R.string.feedback_send_succeed, Snackbar.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
