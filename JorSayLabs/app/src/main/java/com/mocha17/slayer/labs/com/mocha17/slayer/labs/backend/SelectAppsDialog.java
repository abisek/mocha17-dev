package com.mocha17.slayer.labs.com.mocha17.slayer.labs.backend;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.mocha17.slayer.labs.R;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Chaitanya on 5/31/15.
 */
public class SelectAppsDialog extends DialogFragment {

    private Context context;
    //https://developer.android.com/reference/android/support/v7/widget/RecyclerView.html
    private RecyclerView appsList;
    private ProgressBar appsListLoading;

    private RecyclerView.LayoutManager layoutManager;

    private enum UI_STATE {
        APPS_LIST_LOADING_START,
        APPS_LIST_LOADING_SUCCESS,
        APPS_LIST_LOADING_FAILURE;
    };

    List<AppInfo> appInfos;
    public static SelectAppsDialog newInstance() {
        return new SelectAppsDialog();
    }

    //public Constructor
    public SelectAppsDialog() {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        //context = activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getActivity();

        //Start loading apps list
        new AppsLoaderTask().execute();
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
        //http://developer.android.com/reference/android/support/v7/widget/RecyclerView.LayoutManager.html
        //"By changing the LayoutManager a RecyclerView can be used to implement a standard vertically
        //scrolling list, a uniform grid, staggered grids, horizontally scrolling collections and more."
        appsList.setLayoutManager(new LinearLayoutManager(context));

        appsListLoading = (ProgressBar) view.findViewById(R.id.select_apps_progress);
        appsListLoading.setIndeterminate(true);

        updateUI(UI_STATE.APPS_LIST_LOADING_START);
        return view;
    }

    private void updateUI(UI_STATE uiState) {
        switch(uiState) {
            case APPS_LIST_LOADING_START:
                appsList.setVisibility(View.INVISIBLE); //continues to take space
                appsListLoading.setVisibility(View.VISIBLE);
                break;
            case APPS_LIST_LOADING_SUCCESS:
                appsListLoading.setVisibility(View.GONE);

                appsList.setVisibility(View.VISIBLE);
                //Adapter for the List
                //AppListAdater will work on the class member appInfos - instead of getting a List via Constructor
                AppsListAdapter appsListAdapter = new AppsListAdapter();
                appsList.setAdapter(appsListAdapter);
                break;
            case APPS_LIST_LOADING_FAILURE:
                Toast.makeText(context, R.string.select_apps_error, Toast.LENGTH_SHORT).show();
                getDialog().dismiss();
                break;
        }
    }

    private class AppsLoaderTask extends AsyncTask<Void, Void, UI_STATE> {
        private int iconWidth, iconHeight;
        private Canvas canvas;

        AppsLoaderTask() {
            Resources resources = context.getResources();
            iconWidth = iconHeight = (int) resources.getDimension(android.R.dimen.app_icon_size);
            canvas = new Canvas();
        }
        @Override
        protected UI_STATE doInBackground(Void... params) {
            //Populate appInfos
            appInfos = new LinkedList<AppInfo>();

            PackageManager pm = context.getPackageManager();
            List<ApplicationInfo> installedApps = pm.getInstalledApplications(0);

            if (installedApps.isEmpty()) {
                //highly unlikely
                return UI_STATE.APPS_LIST_LOADING_FAILURE;
            }
            for (ApplicationInfo applicationInfo : installedApps) {
                Drawable icon = pm.getApplicationIcon(applicationInfo);
                appInfos.add(new AppInfo(applicationInfo.packageName, applicationInfo.loadLabel(pm).toString(),
                        getSizeAdjustedDrawable(icon), false));
            }
            Collections.sort(appInfos);

            return UI_STATE.APPS_LIST_LOADING_SUCCESS;
        }

        @Override
        protected void onPostExecute(UI_STATE result) {
            updateUI(result);
        }

        //The code in this method comes from AOSP:
        //https://github.com/android/platform_frameworks_base/blob/master/policy/src/com/android/internal/policy/impl/IconUtilities.java
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

            final Bitmap bitmap = Bitmap.createBitmap(width, height,
                    Bitmap.Config.ARGB_8888);
            canvas.setBitmap(bitmap);

            icon.setBounds(0, 0, width, height);
            icon.draw(canvas);
            return new BitmapDrawable(getResources(), bitmap);
        }
    }

    private class AppsListAdapter extends RecyclerView.Adapter<AppsListAdapter.AppsListViewHolder> {

        //Reference to the views for each data item
        public class AppsListViewHolder extends RecyclerView.ViewHolder {
            CheckedTextView row;
            public AppsListViewHolder(View v) {
                super(v);
                row = (CheckedTextView) v.findViewById(R.id.app_row);
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
                viewHolder.row.setCompoundDrawablesWithIntrinsicBounds(app.icon, null, null, null);
                viewHolder.row.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        boolean newChecked = !viewHolder.row.isChecked(); //toggle
                        viewHolder.row.setChecked(newChecked);
                        app.selected = newChecked;
                        //Toast.makeText(context, app.name + (app.selected? " selected":" not selected"), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return appInfos.size();
        }
    }

}

    //@TargetApi(Build.VERSION_CODES.LOLLIPOP)
   /* private SelectAppsDialog(Context context) {
        super();
        init();
    }*/

    /*private SelectAppsDialog(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private SelectAppsDialog(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private SelectAppsDialog(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    @Override
    protected void onBindDialogView(View view) {
        appsList = (ListView) view.findViewById(R.id.appsList);
        appsList.setAdapter(new ArrayAdapter<String>(getContext(),
                android.R.layout.simple_list_item_multiple_choice));
        //appsList.getAdapter().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE); //enable multiple choice
    }

    private void init() {
        setDialogLayoutResource(R.layout.dialog_select_apps);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);


    }*/
//}
