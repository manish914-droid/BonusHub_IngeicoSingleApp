package com.bonushub.crdb.india.view.base;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.widget.ScrollView;
import android.widget.TextView;

import com.usdk.apiservice.aidl.BaseError;


public abstract class BaseDeviceActivity extends BaseActivity {

    private TextView tvOutput;
    private ScrollView scrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            onCreateView(savedInstanceState);
        } catch (Exception e) {
            finishWithInfo(e.getMessage());
            return;
        }

      //  scrollView = bindViewById(R.id.scrollView);
      //  tvOutput = bindViewById(R.id.tvOutput);

      //  View btnClearMsg = bindViewById(R.id.btnClearMsg);

    }

    protected abstract void onCreateView(Bundle savedInstanceState) throws Exception;

    protected void handleException(Exception e) {
        e.printStackTrace();
        showException(e.getClass().getSimpleName() + " : " + e.getMessage());
    }

    protected void outputRedText(String text) {
        outputText(Color.RED, text);
    }

    protected void outputBlackText(String text) {
        outputText(Color.BLACK, text);
    }

    protected void outputBlueText(String text) {
        outputText(Color.BLUE, text);
    }

    protected void outputText(String text) {
        int orange = Color.rgb(0xEF, 0xB3, 0x36);
        outputText(orange, text);
    }

    protected void outputText(final int color, final String text) {
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                String appendText = text + "\n";
                tvOutput.append(getColorText(color, appendText));

                uiHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                    }
                }, 50);
            }
        });
    }

    private static SpannableStringBuilder getColorText(int color, String text) {
        SpannableStringBuilder ssb = new SpannableStringBuilder(text);
        ForegroundColorSpan colorSpan = new ForegroundColorSpan(color);
        ssb.setSpan(colorSpan, 0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return ssb;
    }

    public String getErrorDetail(int error) {
        String message = getErrorMessage(error);
        if (error < 0) {
            return message + "[" + error + "]";
        }
        return message + String.format("[0x%02X]", error);
    }

    public String getErrorMessage(int error) {
        String message;
        switch (error) {
            case BaseError.SERVICE_CRASH: message = "SERVICE_CRASH"; break;
            case BaseError.REQUEST_EXCEPTION: message = "REQUEST_EXCEPTION"; break;
            case BaseError.ERROR_CANNOT_EXECUTABLE: message = "ERROR_CANNOT_EXECUTABLE"; break;
            case BaseError.ERROR_INTERRUPTED: message = "ERROR_INTERRUPTED"; break;
            case BaseError.ERROR_HANDLE_INVALID: message = "ERROR_HANDLE_INVALID"; break;
            default:
                message = "Unknown error";
        }
        return message;
    }

}