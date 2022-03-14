package com.bonushub.crdb.india.view.base;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

import com.bonushub.crdb.india.R;
import com.bonushub.crdb.india.utils.ingenico.DialogUtil;

public abstract class BaseActivity extends AppCompatActivity {

    protected Handler uiHandler = new Handler(Looper.getMainLooper());

    protected <T extends View> T bindViewById(int id) {
        return (T) findViewById(id);
    }

    public void showException(final String message) {
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                DialogUtil.showMessage(BaseActivity.this, getString(R.string.exception_prompt), message, new DialogUtil.OnConfirmListener() {
                    @Override
                    public void onConfirm() {
                    }
                });
            }
        });
    }

    public void finishWithInfo(String info) {
        DialogUtil.showMessage(this, getString(R.string.error_prompt), info, new DialogUtil.OnConfirmListener() {
            @Override
            public void onConfirm() {
                finish();
            }
        });
    }

    protected void startActivity(Class<?> cls) {
        startActivity(new Intent(this, cls));
        overridePendingTransition(R.anim.dync_in_from_right, R.anim.dync_out_to_left);
    }
}