package com.android.updating;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by sayagodshala on 3/3/2015.
 */
public class UpdatingSDKAlertDialog extends Dialog {

    public interface UpdatingSDKAlertDialogListener {
        public void onButton1Action();

        public void onButton2Action();
    }

    private UpdatingSDKAlertDialogListener mListener;

    private String BUTTON1_TEXT = "";
    private String BUTTON2_TEXT = "";

    private Context mContext;

    private Button button_one, button_two;
    private TextView text_updating_sdk_message,
            text_updating_sdk_developer_message;

    private JSONObject responseObj;

    public UpdatingSDKAlertDialog(Context context, JSONObject response) {
        super(context, R.style.UpdatingAlertDialog);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.setContentView(R.layout.view_updating_dialog);

        mContext = context;
        responseObj = response;

    }

    public UpdatingSDKAlertDialog(Context context, JSONObject response,
                                  String button1Text, String button2Text,
                                  UpdatingSDKAlertDialogListener listener) {
        this(context, response);

        mListener = listener;

        BUTTON1_TEXT = button1Text;
        BUTTON2_TEXT = button2Text;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        Window window = getWindow();
        lp.copyFrom(window.getAttributes());
        lp.width = LayoutParams.MATCH_PARENT;
        lp.height = LayoutParams.WRAP_CONTENT;
        window.setAttributes(lp);

        initializeViews();
        defaultConfiguration();
        setEventForViews();

    }

    private void initializeViews() {

        button_one = (Button) findViewById(R.id.button_one);
        button_two = (Button) findViewById(R.id.button_two);

        text_updating_sdk_message = (TextView) findViewById(R.id.text_updating_sdk_message);
        text_updating_sdk_developer_message = (TextView) findViewById(R.id.text_updating_sdk_developer_message);

    }

    private void defaultConfiguration() {

        // text_updating_sdk_developer_message.setVisibility(View.GONE);

        text_updating_sdk_developer_message.setVisibility(View.GONE);

        try {

            if (responseObj.has("updateMessage")) {
                text_updating_sdk_message.setText(responseObj
                        .getString("updateMessage"));
            } else {
                text_updating_sdk_message
                        .setText("A new update for this app is available!");
            }

            if (responseObj.has("developerMessage")) {
                if (!responseObj.getString("developerMessage").equalsIgnoreCase("")) {
                    text_updating_sdk_developer_message.setText(responseObj
                            .getString("developerMessage"));
                    text_updating_sdk_developer_message.setVisibility(View.VISIBLE);
                } else {
                    text_updating_sdk_developer_message.setVisibility(View.GONE);
                }
            }


            if (!responseObj.getBoolean("updateAvailable")) {
                text_updating_sdk_message.setVisibility(View.GONE);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        button_one.setText(BUTTON1_TEXT);
        button_two.setText(BUTTON2_TEXT);

        if (BUTTON2_TEXT.equalsIgnoreCase("")) {
            button_two.setVisibility(View.GONE);
        }

    }

    private void setEventForViews() {
        button_one.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onButton1Action();
                dismiss();
            }
        });
        button_two.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onButton2Action();
                dismiss();
            }
        });
    }

}
