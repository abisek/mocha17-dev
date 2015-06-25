package com.mocha17.slayer.settings;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.mocha17.slayer.R;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Created by Chaitanya on 5/31/15.
 */
public class SelectAppsDialog extends DialogFragment implements View.OnClickListener {

    private Context context;
    private SharedPreferences defaultSharedPreferences;
    private SharedPreferences.Editor editor;

    //https://developer.android.com/reference/android/support/v7/widget/RecyclerView.html
    private RecyclerView appsList;
    private ProgressBar appsListLoading;
    private Button ok, cancel;
    private CheckedTextView allApps;

    private List<AppInfo> appInfos;
    private Set<String> selectedPackages;
    private AppsLoaderTask appsLoaderTask;
    private enum UIState {
        ALL_APPS,
        APPS_LIST_LOADING_START,
        APPS_LIST_LOADING_SUCCESS,
        APPS_LIST_LOADING_FAILURE,
        APPS_LIST_LOADING_CANCELLED;
    };

    static SelectAppsDialog newInstance() {
        return new SelectAppsDialog();
    }

    //public Constructor
    public SelectAppsDialog() {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;

        defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        editor = defaultSharedPreferences.edit();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.dialog_select_apps, container, false);

        appsList = (RecyclerView) view.findViewById(R.id.select_apps_list);
        //Improves performance.
        //We are not querying packageManager for updates while this dialog is being displayed and
        //dynamically updating the list etc. Not necessary.
        appsList.setHasFixedSize(true);
        //Use a LinearLayoutManager to get ListView-like behavior
        //This is where the magic happens
        //developer.android.com/reference/android/support/v7/widget/RecyclerView.LayoutManager.html
        //"By changing the LayoutManager a RecyclerView can be used to implement a standard
        //vertically scrolling list, a uniform grid, staggered grids, horizontally scrolling
        //collections and more."
        appsList.setLayoutManager(new LinearLayoutManager(context));

        appsListLoading = (ProgressBar) view.findViewById(R.id.select_apps_progress);
        appsListLoading.setIndeterminate(true);

        ok = (Button)view.findViewById(R.id.ok_button);
        ok.setOnClickListener(this);

        cancel = (Button)view.findViewById(R.id.cancel_button);
        cancel.setOnClickListener(this);

        allApps = (CheckedTextView)view.findViewById(R.id.all_apps);
        allApps.setOnClickListener(this);

        //Update UI based on savedInstanceState (if available) and the info in SharedPreferences

        //Get previously selected apps
        //The set returned by getPreferenceValue cannot be reliably modified.
        //We will copy it into a modifiable one as needed.
        Set<String> prevSelected = defaultSharedPreferences.getStringSet(
                getString(R.string.pref_key_apps), new HashSet<String>());

        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean(getString(R.string.pref_key_all_apps))) {
                //All apps are selected by the user, populated selectedApps from SharedPreferences
                selectedPackages = new HashSet<>(prevSelected);
                updateUI(UIState.ALL_APPS);
            } else {
                //User has potentially selected some apps. Restore the UI apppropriately.
                String [] tempSelectedPackages =
                        savedInstanceState.getStringArray(getString(R.string.pref_key_apps));
                if (tempSelectedPackages != null) {
                    selectedPackages = new HashSet<>();
                    for (String tempSelectedPackage : tempSelectedPackages) {
                        selectedPackages.add(tempSelectedPackage);
                    }
                } else {
                    //We do not have saved selected packages, use the set from sharedPreferences
                    selectedPackages = new HashSet<>(prevSelected);
                }
                updateUI(UIState.APPS_LIST_LOADING_START);
            }
        } else { //When we don't have a saved state, set UI state based on SharedPreferences data
            selectedPackages = new HashSet<>(prevSelected);

            if (defaultSharedPreferences.getBoolean(getString(R.string.pref_key_all_apps), false)) {
                updateUI(UIState.ALL_APPS);
            } else {
                updateUI(UIState.APPS_LIST_LOADING_START);
            }
        }
        return view;
    }

    @Override
    public void onSaveInstanceState (Bundle outState) {
        //Store if 'all apps' is selected
        outState.putBoolean(getString(R.string.pref_key_all_apps), allApps.isChecked());
        if (!allApps.isChecked()) {
            if (selectedPackages != null && !selectedPackages.isEmpty()) {
                //Store currently selected packages
                outState.putStringArray(getString(R.string.pref_key_apps),
                        selectedPackages.toArray(new String[selectedPackages.size()]));
            }
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ok_button:
                editor.putStringSet(getString(R.string.pref_key_apps), selectedPackages).apply();
                editor.putBoolean(
                        getString(R.string.pref_key_all_apps), allApps.isChecked()).apply();
                /*fall through*/
            case R.id.cancel_button:
                if (appsLoaderTask!= null && !appsLoaderTask.isCancelled()) {
                    appsLoaderTask.cancel(true);
                }
                getDialog().dismiss();
                break;
            case R.id.all_apps:
                allApps.toggle();
                if (allApps.isChecked()) {
                    updateUI(UIState.ALL_APPS);
                } else {
                    updateUI(UIState.APPS_LIST_LOADING_START);
                }
                break;
        }
    }

    private void updateUI(UIState uiState) {
        //We want to ignore UI state updates when the Task is cancelled - most likely the user is
        //moving away from this Dialog; but we could enter this method when this task isn't
        //initialized and hasn't started. Hence the null check.
        if (appsLoaderTask!= null && appsLoaderTask.isCancelled()) {
            return;
        }

        switch(uiState) {
            case ALL_APPS:
                allApps.setChecked(true);
                appsList.setVisibility(View.INVISIBLE);
                appsListLoading.setVisibility(View.INVISIBLE);
                break;
            case APPS_LIST_LOADING_START:
                appsList.setVisibility(View.INVISIBLE); //continues to take space
                appsListLoading.setVisibility(View.VISIBLE);
                /* We could have chosen to avoid running this Task again if it was done, opting to
                show the data immediately. We are not doing that because triggering it again updates
                selected state correctly. If user selects appA while list is displayed, then selects
                all apps and then unselectes all apps, in the list displayed, appA should be shown
                as selected. */
                if (appsLoaderTask == null || appsLoaderTask.isDone()) {
                    appsLoaderTask = new AppsLoaderTask();
                    appsLoaderTask.execute();
                }
                break;
            case APPS_LIST_LOADING_SUCCESS:
                //Handle the race condition when allApps is toggled on while the AsyncTask was still
                //executing. Do not make the views visible if allApps is checked.
                if (!allApps.isChecked()) {
                    appsListLoading.setVisibility(View.GONE);
                    appsList.setVisibility(View.VISIBLE);

                    //Adapter for the List
                    //works on the class member appInfos instead of getting a List via Constructor
                    AppsListAdapter appsListAdapter = new AppsListAdapter();
                    appsList.setAdapter(appsListAdapter);
                }
                break;
            case APPS_LIST_LOADING_FAILURE:
                Toast.makeText(context, R.string.select_apps_error,
                        Toast.LENGTH_SHORT).show();
                getDialog().dismiss();
                break;
            case APPS_LIST_LOADING_CANCELLED:
            default:
                //do nothing
                break;
        }
    }

    /** AsyncTask for loading list of apps asyncronously */
    private class AppsLoaderTask extends AsyncTask<Void, Void, UIState> {
        private int iconWidth, iconHeight;
        private Canvas canvas;
        private boolean done;

        AppsLoaderTask() {
            Resources resources = context.getResources();
            iconWidth = iconHeight = (int) resources.getDimension(android.R.dimen.app_icon_size);
            canvas = new Canvas();
        }

        @Override
        protected UIState doInBackground(Void... params) {
            //Populate appInfos
            appInfos = new LinkedList<AppInfo>();

            PackageManager pm = context.getPackageManager();
            List<ApplicationInfo> installedApps = pm.getInstalledApplications(0);
            if (installedApps.isEmpty()) {
                //highly unlikely
                return UIState.APPS_LIST_LOADING_FAILURE;
            }
            for (ApplicationInfo applicationInfo : installedApps) {
                if (isCancelled()) {
                    //When an AsyncTask is cancelled, onPostExecute isn't called, instead,
                    //onCancelled() is called. We have nothing to do once this task is cancelled,
                    // so.. we return a value that's gonna be ignored anyway.
                    //We do have a isCancelled() check before we do UI update
                    return UIState.APPS_LIST_LOADING_CANCELLED;
                }
                Drawable icon = pm.getApplicationIcon(applicationInfo);
                appInfos.add(new AppInfo(applicationInfo.packageName,
                        applicationInfo.loadLabel(pm).toString(), getSizeAdjustedDrawable(icon),
                        selectedPackages.contains(applicationInfo.packageName)));
            }

            //Sort first by checked state and then alphabetically - display selected apps at top
            Collections.sort(appInfos);

            return UIState.APPS_LIST_LOADING_SUCCESS;
        }

        @Override
        protected void onPostExecute(UIState result) {
            updateUI(result);
            if (result == UIState.APPS_LIST_LOADING_SUCCESS) {
                done = true;
            }
        }

        boolean isDone() {
            return done;
        }

        //The code in this method comes from AOSP:
        //https://github.com/android/platform_frameworks_base/blob/master/policy/src/com/android/
        // internal/policy/impl/IconUtilities.java
        Drawable getSizeAdjustedDrawable(Drawable icon) {
            int sourceWidth = icon.getIntrinsicWidth();
            int sourceHeight = icon.getIntrinsicHeight();
            int width = iconWidth, height = iconHeight;
            if (sourceWidth > 0 && sourceHeight > 0) {
                // There are intrinsic sizes.
                if (iconWidth < sourceWidth || iconHeight < sourceHeight) {
                    // It's too big, scale it down.
                    final float ratio = (float) sourceWidth / sourceHeight;
                    if (sourceWidth > sourceHeight) {
                        height = (int) (iconWidth / ratio);
                    } else if (sourceHeight > sourceWidth) {
                        width = (int) (iconHeight * ratio);
                    }
                } else if (sourceWidth < iconWidth && sourceHeight < iconHeight) {
                    // It's small, use the size they gave us.
                    width = sourceWidth;
                    height = sourceHeight;
                }
            }

            final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            canvas.setBitmap(bitmap);

            icon.setBounds(0, 0, width, height);
            icon.draw(canvas);
            return new BitmapDrawable(context.getResources(), bitmap);
        }
    }

    private class AppsListAdapter extends RecyclerView.Adapter<AppsListAdapter.AppsListViewHolder> {

        //Reference to the views for each data item
        class AppsListViewHolder extends RecyclerView.ViewHolder
                implements View.OnClickListener {
            CheckedTextView row;
            public AppsListViewHolder(View v) {
                super(v);
                row = (CheckedTextView) v.findViewById(R.id.app_row);
                row.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                int position = getPosition();
                if (v instanceof CheckedTextView) { //we got ViewHolder row
                    ((CheckedTextView) v).toggle();
                    AppInfo appInfo = appInfos.get(position);
                    //Update 'selected apps' list.
                    if (selectedPackages.contains(appInfo.packageName)) {
                        selectedPackages.remove(appInfo.packageName);
                    } else {
                        selectedPackages.add(appInfo.packageName);
                    }
                }
            }
        }

        //This is invoked by LayoutManager to create new views
        @Override
        public AppsListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.row_apps_list, parent, false);
            AppsListViewHolder appsListViewHolder = new AppsListViewHolder(view);
            return appsListViewHolder;
        }

        //This is invoked bu LayoutManager to replace the contents of a view
        //Similar to getView from the ListView world
        @Override
        public void onBindViewHolder(final AppsListViewHolder viewHolder, final int position) {
            if (position <= appInfos.size()) {
                final AppInfo app = appInfos.get(position);
                viewHolder.row.setText(app.name);
                viewHolder.row.setCompoundDrawablesWithIntrinsicBounds(
                        app.icon/*left*/, null, null, null);
                viewHolder.row.setChecked(selectedPackages.contains(app.packageName));
            }
        }

        @Override
        public int getItemCount() {
            return appInfos.size();
        }
    }
}