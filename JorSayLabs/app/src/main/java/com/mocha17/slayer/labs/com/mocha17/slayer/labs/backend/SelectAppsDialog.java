package com.mocha17.slayer.labs.com.mocha17.slayer.labs.backend;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.Toast;

import com.mocha17.slayer.labs.R;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Chaitanya on 5/31/15.
 */
public class SelectAppsDialog extends DialogFragment {

    private Context context;
    //https://developer.android.com/reference/android/support/v7/widget/RecyclerView.html
    private RecyclerView appsList;
    private RecyclerView.Adapter appsListAdapter;
    //LayoutManager is where the magic happens
    //http://developer.android.com/reference/android/support/v7/widget/RecyclerView.LayoutManager.html
    //"By changing the LayoutManager a RecyclerView can be used to implement a standard vertically
    //scrolling list, a uniform grid, staggered grids, horizontally scrolling collections and more."
    private RecyclerView.LayoutManager layoutManager;

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

        //Populate appInfos
        appInfos = new LinkedList<AppInfo>();
        PackageManager pm = context.getPackageManager();

        //TODO do this loop in an AsyncTask - and till we get data display spinner etc. we are getting 'too much work on main thread.'
        for (ApplicationInfo applicationInfo : pm.getInstalledApplications(0)) {
            android.util.Log.v("CK", "applicationInfo.name: " + applicationInfo.loadLabel(pm));
            Drawable icon = pm.getApplicationIcon(applicationInfo);
            icon.setBounds(10, 10, 10, 10);
            appInfos.add(new AppInfo(applicationInfo.packageName, applicationInfo.loadLabel(pm).toString(),
                    icon, false));
        }

        //android.util.Log.v("CK", "AppInfos: " + appInfos);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.dialog_select_apps, container, false);

        appsList = (RecyclerView) view.findViewById(R.id.select_apps);

        //Improves performance.
        //We are not querying packageManager for updates while this dialog is being displayed and
        //dynamically updating the list etc. Not necessary.
        appsList.setHasFixedSize(true);

        //Use a LinearLayoutManager to get ListView-like behavior
        appsList.setLayoutManager(new LinearLayoutManager(context));

        //Adapter
        //AppListAdater will work on the class member appInfos - instead of getting a List via Constructor
        AppsListAdapter appsListAdapter = new AppsListAdapter();
        appsList.setAdapter(appsListAdapter);

        return view;
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
            //parent.removeView(view);
            //android.util.Log.v("CK", "onCreateViewHolder removeView");
            //((ViewGroup)view.getParent()).removeView(view);
            //CheckedTextView appRow = (CheckedTextView)view.findViewById(R.id.app_row);
            return appsListViewHolder;
        }

        //This is invoked bu LayoutManager to replace the contents of a view
        //Similar to getView from the ListView world
        @Override
        public void onBindViewHolder(final AppsListViewHolder viewHolder, final int position) {
            if (position <= appInfos.size()) {
                final AppInfo app = appInfos.get(position);
                viewHolder.row.setText(app.name);
                //setCompoundDrawablesWithIntrinsicBounds (int left, int top, int right, int bottom)
                //Drawable d = context.getResources().getDrawable(app.icon, )

                viewHolder.row.setCompoundDrawablesWithIntrinsicBounds(app.icon, null, null, null);
                viewHolder.row.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        boolean newChecked = !viewHolder.row.isChecked(); //toggle
                        viewHolder.row.setChecked(newChecked); //toggle
                        app.selected = newChecked;
                        Toast.makeText(context, app.name + (app.selected? " selected":" not selected"), Toast.LENGTH_SHORT).show();
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
