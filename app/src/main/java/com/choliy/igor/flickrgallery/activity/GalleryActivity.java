package com.choliy.igor.flickrgallery.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.choliy.igor.flickrgallery.interfaces.FlickrConstants;
import com.choliy.igor.flickrgallery.R;
import com.choliy.igor.flickrgallery.data.FlickrLab;
import com.choliy.igor.flickrgallery.fragment.GalleryFragment;
import com.choliy.igor.flickrgallery.interfaces.OnHistoryDialogClickListener;
import com.choliy.igor.flickrgallery.model.HistoryItem;
import com.choliy.igor.flickrgallery.util.AnimUtils;
import com.choliy.igor.flickrgallery.util.FlickrUtils;
import com.choliy.igor.flickrgallery.util.NavUtils;
import com.choliy.igor.flickrgallery.util.PrefUtils;
import com.choliy.igor.flickrgallery.util.TimeUtils;

import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;

public class GalleryActivity extends BroadcastActivity implements
        NavigationView.OnNavigationItemSelectedListener,
        OnHistoryDialogClickListener {

    private FragmentManager mFragmentManager;
    private boolean mShowSearchType;

    @BindView(R.id.toolbar_gallery) Toolbar mToolbar;
    @BindView(R.id.search_view) SearchView mSearchView;
    @BindView(R.id.drawer_layout) DrawerLayout mDrawerLayout;
    @BindView(R.id.nav_view) NavigationView mNavigationView;

    public static Intent newIntent(Context context) {
        return new Intent(context, GalleryActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);
        PrefUtils.isFirstStart(this);
        ButterKnife.bind(this);
        setupUi(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(FlickrConstants.TOOLBAR_KEY, mShowSearchType);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START))
            mDrawerLayout.closeDrawer(GravityCompat.START);
        else super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == NavUtils.REQUEST_CODE && resultCode == RESULT_OK) {
            AnimUtils.animateToolbarVisibility(this, Boolean.TRUE);
            mFragmentManager
                    .beginTransaction()
                    .replace(R.id.fragment_container, new GalleryFragment())
                    .commit();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        NavUtils.onNavDrawerClicked(this, item.getItemId());
        mDrawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onStartClick() {
        AnimUtils.animateToolbarVisibility(this, Boolean.TRUE);
        AnimUtils.animateToolbarType(this, mSearchView, mShowSearchType = Boolean.TRUE);
    }

    @Override
    public void onHistoryClick(String historyTitle) {
        PrefUtils.setStoredQuery(this, historyTitle);
        AnimUtils.animateToolbarVisibility(this, Boolean.TRUE);
        AnimUtils.animateToolbarType(this, mSearchView, mShowSearchType = Boolean.TRUE);
    }

    public void onToolbarClick(View view) {
        switch (view.getId()) {
            case R.id.toolbar_icon_menu:
                mDrawerLayout.openDrawer(mNavigationView);
                break;
            case R.id.toolbar_icon_back:
                AnimUtils.animateToolbarType(this, mSearchView, mShowSearchType = Boolean.FALSE);
                break;
            case R.id.toolbar_icon_search:
                AnimUtils.animateToolbarType(this, mSearchView, mShowSearchType = Boolean.TRUE);
                break;
        }
    }

    private void setupUi(Bundle bundle) {
        mFragmentManager = getSupportFragmentManager();
        Fragment fragment = mFragmentManager.findFragmentById(R.id.fragment_container);
        if (fragment == null) {
            fragment = GalleryFragment.newInstance();
            mFragmentManager
                    .beginTransaction()
                    .add(R.id.fragment_container, fragment)
                    .commit();
        }

        mNavigationView.setNavigationItemSelectedListener(this);
        setSupportActionBar(mToolbar);
        setupSearchView();
        setupDevDate();

        if (bundle != null) {
            mShowSearchType = bundle.getBoolean(FlickrConstants.TOOLBAR_KEY);
            AnimUtils.animateToolbarType(this, mSearchView, mShowSearchType);
        }

        if (NavUtils.sIsHistoryDialogShown) NavUtils.showHistory(this);
        if (NavUtils.sIsAboutDialogShown) NavUtils.aboutDialog(this);
    }

    private void setupSearchView() {
        final ImageView closeIcon = (ImageView) mSearchView.findViewById(R.id.search_close_btn);
        TextView hintText = (TextView) mSearchView.findViewById(R.id.search_src_text);
        hintText.setHintTextColor(ContextCompat.getColor(this, R.color.textColorLightGray));

        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    closeIcon.setClickable(Boolean.FALSE);
                    closeIcon.setImageResource(FlickrConstants.INT_ZERO);
                } else {
                    closeIcon.setClickable(Boolean.TRUE);
                    closeIcon.setImageResource(R.drawable.ic_close);
                }
                return Boolean.TRUE;
            }

            @Override
            public boolean onQueryTextSubmit(String query) {
                PrefUtils.setStoredQuery(GalleryActivity.this, query);
                HistoryItem item = new HistoryItem(
                        query,
                        TimeUtils.getDate(),
                        TimeUtils.getTime(GalleryActivity.this));
                FlickrLab.getInstance(GalleryActivity.this).addHistory(item, Boolean.FALSE);

                AnimUtils.animateToolbarType(GalleryActivity.this, mSearchView, query.isEmpty());
                mFragmentManager
                        .beginTransaction()
                        .replace(R.id.fragment_container, GalleryFragment.newInstance())
                        .commit();
                return Boolean.TRUE;
            }
        });

        mSearchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String query = PrefUtils.getStoredQuery(GalleryActivity.this);
                mSearchView.setQuery(query, Boolean.FALSE);
            }
        });

        closeIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSearchView.setQuery(FlickrConstants.STRING_EMPTY, Boolean.FALSE);
                FlickrUtils.showInfo(view, getString(R.string.text_search_query));
                PrefUtils.setStoredQuery(GalleryActivity.this, null);
            }
        });
    }

    private void setupDevDate() {
        View header = mNavigationView.getHeaderView(FlickrConstants.INT_ZERO);
        TextView developer = (TextView) header.findViewById(R.id.nav_text_developer);
        int year = Calendar.getInstance().get(Calendar.YEAR);
        String text = getString(R.string.dialog_developer, String.valueOf(year));
        developer.setText(text);
    }
}