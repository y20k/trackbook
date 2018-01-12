/**
 * DropdownHelper.java
 * Implements a dropdown menu
 * The dropdown menu used to select tracks
 *
 * This file is part of
 * TRACKBOOK - Movement Recorder for Android
 *
 * Copyright (c) 2016-18 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 *
 * Trackbook uses osmdroid - OpenStreetMap-Tools for Android
 * https://github.com/osmdroid/osmdroid
 */


package org.y20k.trackbook.helpers;

import android.app.Activity;
import android.content.res.Resources;
import android.database.DataSetObserver;
import android.support.annotation.Nullable;
import android.support.v7.widget.ThemedSpinnerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.y20k.trackbook.R;
import org.y20k.trackbook.core.TrackBundle;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


/**
 * DropdownHelper class
 */
public class DropdownAdapter extends BaseAdapter implements ThemedSpinnerAdapter, TrackbookKeys {

    /* Define log tag */
    private static final String LOG_TAG = DropdownAdapter.class.getSimpleName();


    /* Main class variables */
    private final Activity mActivity;
    private final ThemedSpinnerAdapter.Helper mDropdownAdapterHelper;
    private List<TrackBundle> mTrackBundleList;


    /* Constructor */
    public DropdownAdapter(Activity activity) {
        // store activity
        mActivity = activity;

        // fill list with track bundles
        initializeTrackBundleList();

        // create an adapter helper
        mDropdownAdapterHelper = new ThemedSpinnerAdapter.Helper(activity);
    }


    @Override
    public void setDropDownViewTheme(@Nullable Resources.Theme theme) {
        mDropdownAdapterHelper.setDropDownViewTheme(theme);
    }


    @Nullable
    @Override
    public Resources.Theme getDropDownViewTheme() {
        return mDropdownAdapterHelper.getDropDownViewTheme();
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // getView -> collapsed view of dropdown
        View view = convertView;
        if (view == null) {
            LayoutInflater inflater = mDropdownAdapterHelper.getDropDownViewInflater();
            view = inflater.inflate(R.layout.custom_dropdown_item_collapsed, parent, false);
        }
        ((TextView) view).setText(getItem(position).getTrackName());
        return view;
    }


    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        // getDropDownView -> expanded view of dropdown
        View view = convertView;
        if (view == null) {
            LayoutInflater inflater = mDropdownAdapterHelper.getDropDownViewInflater();
//            view = inflater.inflate(R.layout.custom_dropdown_item_expanded, parent, false);
            view = inflater.inflate(R.layout.support_simple_spinner_dropdown_item, parent, false);
        }
        ((TextView) view).setText(getItem(position).getTrackName());
        return view;
    }


    @Override
    public void registerDataSetObserver(DataSetObserver dataSetObserver) {

    }


    @Override
    public void unregisterDataSetObserver(DataSetObserver dataSetObserver) {

    }


    @Override
    public int getCount() {
        return mTrackBundleList.size();
    }


    @Override
    public TrackBundle getItem(int i) {
        return mTrackBundleList.get(i);
    }


    @Override
    public long getItemId(int i) {
        return 0;
    }


    @Override
    public boolean hasStableIds() {
        return false;
    }


    @Override
    public int getItemViewType(int i) {
        return 0;
    }


    @Override
    public int getViewTypeCount() {
        return 1;
    }


    @Override
    public boolean isEmpty() {
        return mTrackBundleList.size() == 0;
    }


    /* Refreshes the adapter data */
    public void refresh() {
        // re-initialize the adapter's array list
        initializeTrackBundleList();
    }


    /* Initializes list of track bundles */
    private void initializeTrackBundleList() {

        // get list of files from storage
        StorageHelper storageHelper = new StorageHelper(mActivity);
        File files[] = storageHelper.getListOfTrackbookFiles();

        // fill list with track bundles
        mTrackBundleList = new ArrayList<>();
        for (File file : files) {
            mTrackBundleList.add(new TrackBundle(file));
        }

    }

}
