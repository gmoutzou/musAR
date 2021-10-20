package com.gmoutzou.musar.gui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.gmoutzou.musar.R;

public class SettingsFragment extends Fragment {

    public static final String MY_PREFERENCES = "mypref";
    public static final String HOST_KEY = "host";
    public static final String PORT_KEY = "port";
    public static final String DEFAULT_HOST= "10.0.0.33";
    public static final String DEFAULT_PORT= "1099";

    private SharedPreferences sharedpreferences;
    private View parentView;
    private EditText etHost;
    private EditText etPort;
    private Button saveButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        parentView = inflater.inflate(R.layout.settings, container, false);
        setupPreferences();
        return parentView;
    }

    private void setupPreferences() {
        final MainActivity parentActivity = (MainActivity) getActivity();
        etHost = parentView.findViewById(R.id.et_host);
        etPort = parentView.findViewById(R.id.et_port);
        saveButton = parentView.findViewById(R.id.btn_save);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                savePreferences();
                parentActivity.changeFragment(new HomeFragment());
            }
        });
        sharedpreferences = getActivity()
                .getApplicationContext()
                .getSharedPreferences(MY_PREFERENCES, Context.MODE_PRIVATE);
        if (sharedpreferences.contains(HOST_KEY) && !sharedpreferences.getString(HOST_KEY, "").equals("")) {
            etHost.setText(sharedpreferences.getString(HOST_KEY, ""));
        } else {
            etHost.setText(DEFAULT_HOST);
        }
        if (sharedpreferences.contains(PORT_KEY) && !sharedpreferences.getString(PORT_KEY, "").equals("")) {
            etPort.setText(sharedpreferences.getString(PORT_KEY, ""));
        } else {
            etPort.setText(DEFAULT_PORT);
        }
    }

    private void savePreferences() {
        String h = etHost.getText().toString();
        String p = etPort.getText().toString();
        if (!h.equals("") && !p.equals("")) {
            SharedPreferences.Editor editor = sharedpreferences.edit();
            editor.putString(HOST_KEY, h);
            editor.putString(PORT_KEY, p);
            editor.commit();
        }
    }
}
