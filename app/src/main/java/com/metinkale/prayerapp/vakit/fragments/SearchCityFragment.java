/*
 * Copyright (c) 2013-2017 Metin Kale
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metinkale.prayerapp.vakit.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.metinkale.prayer.R;
import com.metinkale.prayerapp.MainActivity;
import com.metinkale.prayerapp.utils.FileChooser;
import com.metinkale.prayerapp.utils.PermissionUtils;
import com.metinkale.prayerapp.vakit.times.sources.CalcTimes;
import com.metinkale.prayerapp.vakit.times.Cities;
import com.metinkale.prayerapp.vakit.times.Entry;
import com.metinkale.prayerapp.vakit.times.Source;
import com.metinkale.prayerapp.vakit.times.sources.WebTimes;

import net.steamcrafted.materialiconlib.MaterialMenuInflater;

import java.io.File;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class SearchCityFragment extends MainActivity.MainFragment implements OnItemClickListener, OnQueryTextListener, LocationListener, OnClickListener, CompoundButton.OnCheckedChangeListener {
    private MyAdapter mAdapter;
    private FloatingActionButton mFab;
    private MenuItem mSearchItem;
    @Nullable
    private Cities mCities = Cities.get();
    private SwitchCompat mAutoLocation;
    private Location mLocation;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.vakit_addcity, container, false);
        mAutoLocation = v.findViewById(R.id.autoLocation);
        mAutoLocation.setOnCheckedChangeListener(this);


        ColorStateList trackStates = new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{}
                },
                new int[]{
                        Color.WHITE,
                        Color.LTGRAY
                }
        );
        mAutoLocation.setThumbTintList(trackStates);
        mAutoLocation.setThumbTintMode(PorterDuff.Mode.MULTIPLY);


        trackStates = new ColorStateList(
                new int[][]{
                        new int[]{}
                },
                new int[]{
                        getResources().getColor(R.color.colorPrimaryDark)}
        );
        mAutoLocation.setTrackTintList(trackStates);
        mAutoLocation.setTrackTintMode(PorterDuff.Mode.MULTIPLY);


        mFab = v.findViewById(R.id.search);
        mFab.setOnClickListener(this);
        ListView listView = v.findViewById(R.id.listView);
        listView.setFastScrollEnabled(true);
        listView.setOnItemClickListener(this);
        listView.addFooterView(View.inflate(getActivity(), R.layout.vakit_addcity_addcsv, null));
        mAdapter = new MyAdapter(getActivity());
        listView.setAdapter(mAdapter);

        TextView legacy = v.findViewById(R.id.legacySwitch);
        legacy.setText(R.string.oldAddCity);
        legacy.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                back();
                moveToFrag(new SelectCityFragment());

            }
        });

        return v;
    }


    @Override
    public void onResume() {
        super.onResume();
        checkLocation();
    }

    @SuppressWarnings("MissingPermission")
    @Override
    public void onPause() {
        if (PermissionUtils.get(getActivity()).pLocation) {
            LocationManager lm = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
            lm.removeUpdates(this);
        }
        super.onPause();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (PermissionUtils.get(getActivity()).pLocation) {
            checkLocation();
        }
    }

    @SuppressWarnings("MissingPermission")
    public void checkLocation() {
        if (PermissionUtils.get(getActivity()).pLocation) {
            LocationManager lm = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);

            Location loc = null;
            List<String> providers = lm.getProviders(true);
            for (String provider : providers) {
                Location last = lm.getLastKnownLocation(provider);
                // one hour==1meter in accuracy
                if ((last != null) && ((loc == null) || ((last.getAccuracy() - (last.getTime() / (1000 * 60 * 60))) < (loc.getAccuracy() - (loc.getTime() / (1000 * 60 * 60)))))) {
                    loc = last;
                }
            }

            if (loc != null)
                onLocationChanged(loc);

            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_COARSE);
            criteria.setAltitudeRequired(false);
            criteria.setBearingRequired(false);
            criteria.setCostAllowed(false);
            criteria.setSpeedRequired(true);
            String provider = lm.getBestProvider(criteria, true);
            if (provider != null) {
                lm.requestSingleUpdate(provider, this, null);

            }

        } else {
            PermissionUtils.get(getActivity()).needLocation(getActivity());
        }
    }


    @Override
    public void onClick(View view) {
        if (view == mFab) {
            MenuItemCompat.collapseActionView(mSearchItem);
            MenuItemCompat.expandActionView(mSearchItem);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        MaterialMenuInflater.with(getActivity(), inflater)
                .setDefaultColor(0xFFFFFFFF)
                .inflate(R.menu.search, menu);
        mSearchItem = menu.findItem(R.id.menu_search);
        SearchView mSearchView = (SearchView) MenuItemCompat.getActionView(mSearchItem);
        mSearchView.performClick();
        mSearchView.setOnQueryTextListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long index) {
        Entry i = mAdapter.getItem(pos);
        if (i != null) if (i.getSource() == Source.Calc) {
            Bundle bdl = new Bundle();
            bdl.putString("city", i.getName());
            bdl.putDouble("lat", i.getLat());
            bdl.putDouble("lng", i.getLng());
            bdl.putBoolean("autoCity", mAutoLocation.isChecked());
            CalcTimes.add(getActivity(), bdl);
        } else {
            WebTimes.add(i.getSource(), i.getName(), i.getKey(), i.getLat(), i.getLng()).setAutoLocation(mAutoLocation.isChecked());
            back();
        }

    }

    @Override
    public boolean onQueryTextSubmit(@Nullable String query) {

        mCities.search(query == null ? query : query.trim().replace(" ", "+"), new Cities.Callback<List<Entry>>() {
            @Override
            public void onResult(@Nullable List<Entry> items) {
                mAutoLocation.setChecked(false);
                if ((items != null) && !items.isEmpty()) {
                    mAdapter.clear();
                    mAdapter.addAll(items);
                }
                mAdapter.notifyDataSetChanged();
            }
        });


        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {

        return false;
    }


    @Override
    public void onLocationChanged(@NonNull Location loc) {
        mLocation = loc;
        if ((mAdapter.getCount() <= 1)) {
            autoLocation();
        }


    }

    private void autoLocation() {
        mAdapter.clear();
        Entry item = new Entry();
        item.setName("GPS");
        item.setCountry("");
        item.setSource(Source.Calc);
        item.setLat(mLocation.getLatitude());
        item.setLng(mLocation.getLongitude());
        mAdapter.add(item);
        mAdapter.notifyDataSetChanged();

        mCities.search(item.getLat(), item.getLng(), new Cities.Callback<List<Entry>>() {
            @Override
            public void onResult(@Nullable List<Entry> items) {
                if ((items != null) && !items.isEmpty()) {
                    mAdapter.clear();
                    mAdapter.addAll(items);
                }
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    public void addFromCSV(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.addFromCSV)
                .setItems(R.array.addFromCSV, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        if (which == 0) {
                            FileChooser chooser = new FileChooser(getActivity());
                            chooser.setExtension("csv");
                            chooser.showDialog();
                            chooser.setFileListener(new FileChooser.FileSelectedListener() {
                                @Override
                                public void fileSelected(File file) {
                                    String name = file.getName();
                                    if (name.contains("."))
                                        name = name.substring(0, name.lastIndexOf("."));

                                    WebTimes.add(Source.CSV, name, file.toURI().toString(), 0, 0);
                                    back();
                                }
                            });
                        } else {
                            AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                            final EditText editText = new EditText(getActivity());
                            editText.setHint("http(s)://example.com/prayertimes.csv");
                            alert.setView(editText);
                            alert.setTitle(R.string.csvFromURL);
                            alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    String url = editText.getText().toString();
                                    String name = url.substring(url.lastIndexOf("/") + 1);
                                    if (name.contains("."))
                                        name = name.substring(0, name.lastIndexOf("."));
                                    WebTimes.add(Source.CSV, name, url, 0, 0);
                                    back();
                                }
                            });
                            alert.setNegativeButton(R.string.cancel, null);
                            alert.show();

                        }
                    }
                });
        builder.show();
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if (b) autoLocation();
        mAdapter.notifyDataSetChanged();

    }


    private class MyAdapter extends ArrayAdapter<Entry> {

        MyAdapter(@NonNull Context context) {
            super(context, 0, 0);

        }

        @Override
        public void addAll(@NonNull Collection<? extends Entry> collection) {
            super.addAll(collection);
            sort(new Comparator<Entry>() {
                @Override
                public int compare(Entry e1, Entry e2) {
                    return e1.getSource().ordinal() - e2.getSource().ordinal();
                }
            });
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            ViewHolder vh;
            if (convertView == null) {
                convertView = View.inflate(parent.getContext(), R.layout.vakit_addcity_row, null);
                vh = new ViewHolder();
                vh.city = convertView.findViewById(R.id.city);
                vh.country = convertView.findViewById(R.id.country);
                vh.sourcetxt = convertView.findViewById(R.id.sourcetext);
                vh.source = convertView.findViewById(R.id.source);
                vh.gpsIcon = convertView.findViewById(R.id.gps);
                convertView.setTag(vh);
            } else {
                vh = (ViewHolder) convertView.getTag();
            }
            Entry i = getItem(position);

            vh.city.setText(i.getName());

            vh.country.setText(i.getCountry());

            vh.sourcetxt.setText(i.getSource().name);
            if (i.getSource().drawableId == 0) {
                vh.source.setVisibility(View.GONE);
            } else {
                vh.source.setImageResource(i.getSource().drawableId);
                vh.source.setVisibility(View.VISIBLE);
            }

            if (mAutoLocation.isChecked()) vh.gpsIcon.setVisibility(View.VISIBLE);
            else vh.gpsIcon.setVisibility(View.GONE);
            return convertView;
        }

        class ViewHolder {
            TextView country;
            TextView city;
            TextView sourcetxt;
            ImageView source;
            ImageView gpsIcon;
        }

    }

}
