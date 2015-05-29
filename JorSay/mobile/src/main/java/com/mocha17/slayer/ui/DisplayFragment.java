package com.mocha17.slayer.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mocha17.slayer.R;
import com.mocha17.slayer.etc.Constants;
import com.mocha17.slayer.etc.Logger;

/**
 * Created by Chaitanya on 5/2/15.
 */
public class DisplayFragment extends Fragment {
    TextView tv;
    private final String ADDED = "Notification posted";
    private final String REMOVED = "Notification removed";
    public DisplayFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        tv = (TextView)rootView.findViewById(R.id.tv);
        tv.setMovementMethod(new ScrollingMovementMethod());
        Logger.v("Displayed fragment");
        return rootView;
    }

    public void updateUI(Intent intent) {
        if (intent.getBooleanExtra(Constants.KEY_ADDED, false) == true) {
            tv.append(ADDED+": ");
        } else {
            tv.append(REMOVED+": ");
        }
        tv.append(intent.getStringExtra(Constants.KEY_DETAILS)+"\n\n");
    }
}
