package com.choliy.igor.flickrgallery.fragment;

import android.app.Dialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.choliy.igor.flickrgallery.FlickrConstants;
import com.choliy.igor.flickrgallery.R;
import com.choliy.igor.flickrgallery.adapter.HistoryAdapter;
import com.choliy.igor.flickrgallery.async.HistoryLoader;
import com.choliy.igor.flickrgallery.data.FlickrLab;
import com.choliy.igor.flickrgallery.event.HistoryStartEvent;
import com.choliy.igor.flickrgallery.event.HistoryTitleEvent;
import com.choliy.igor.flickrgallery.model.HistoryItem;
import com.choliy.igor.flickrgallery.util.DialogUtils;
import com.choliy.igor.flickrgallery.util.InfoUtils;
import com.choliy.igor.flickrgallery.util.NavUtils;
import com.choliy.igor.flickrgallery.util.PrefUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class HistoryFragment extends DialogFragment implements
        LoaderManager.LoaderCallbacks<List<HistoryItem>> {

    private HistoryAdapter mHistoryAdapter;
    private List<HistoryItem> mSavedHistory;

    @BindView(R.id.rv_history) RecyclerView mRvHistory;
    @BindView(R.id.btn_history_clear) TextView mBtnClear;
    @BindView(R.id.layout_no_history) LinearLayout mNoHistory;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(Boolean.TRUE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_history, container, Boolean.FALSE);
        ButterKnife.bind(this, view);
        getActivity()
                .getSupportLoaderManager()
                .restartLoader(HistoryLoader.HISTORY_LOADER_ID, null, this);
        return view;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new Dialog(getActivity(), getTheme()) {
            @Override
            public void onBackPressed() {
                closeHistoryDialog();
            }
        };
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public Loader<List<HistoryItem>> onCreateLoader(int id, Bundle args) {
        return new HistoryLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<List<HistoryItem>> loader, List<HistoryItem> historyItems) {
        setupUi(historyItems);
    }

    @Override
    public void onLoaderReset(Loader<List<HistoryItem>> loader) {
    }

    @OnClick({R.id.btn_history_start, R.id.btn_history_clear, R.id.btn_history_close})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_history_start:
                closeHistoryDialog();
                EventBus.getDefault().post(new HistoryStartEvent(Boolean.TRUE));
                break;
            case R.id.btn_history_clear:
                DialogUtils.clearDialog(getActivity(), new SaveHistoryAsyncTask());
                break;
            case R.id.btn_history_close:
                closeHistoryDialog();
                break;
        }
    }

    @Subscribe
    public void onEvent(HistoryTitleEvent event) {
        closeHistoryDialog();
    }

    private void setupUi(List<HistoryItem> historyItems) {
        mHistoryAdapter = new HistoryAdapter(historyItems);
        mRvHistory.setAdapter(mHistoryAdapter);
        mRvHistory.setHasFixedSize(Boolean.TRUE);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());

        // set current property to false for avoid listItem width issue
        layoutManager.setAutoMeasureEnabled(Boolean.FALSE);
        mRvHistory.setLayoutManager(layoutManager);
        ItemTouchHelper touchHelper = new ItemTouchHelper(new OnHistorySwipeCallback());
        touchHelper.attachToRecyclerView(mRvHistory);
        checkHistory();
    }

    private void checkHistory() {
        if (mHistoryAdapter.getItemCount() > 0) {
            mNoHistory.setVisibility(View.GONE);
            mBtnClear.setVisibility(View.VISIBLE);
        } else {
            mNoHistory.setVisibility(View.VISIBLE);
            mBtnClear.setVisibility(View.INVISIBLE);
        }
    }

    private void restoreHistory() {
        String text = getActivity().getString(R.string.dialog_restore_cleaned);
        Snackbar snackbar = Snackbar.make(mBtnClear, text, Snackbar.LENGTH_LONG);
        snackbar.setAction(R.string.dialog_undo_btn, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new SaveHistoryAsyncTask().execute(Boolean.FALSE);
            }
        });
        snackbar.show();
    }

    private void restoreSingleHistory(final int position, final HistoryItem item) {
        String text = getActivity().getString(R.string.dialog_restore_removed);
        Snackbar snackbar = Snackbar.make(mBtnClear, text, Snackbar.LENGTH_LONG);
        snackbar.setAction(R.string.dialog_undo_btn, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mHistoryAdapter.restoreItem(position, item);
                FlickrLab.getInstance(getActivity()).addHistory(item, Boolean.TRUE);
                InfoUtils.showShortShack(mBtnClear, getString(R.string.dialog_restore_restored_single));
                checkHistory();
            }
        });
        snackbar.show();
    }

    private void closeHistoryDialog() {
        NavUtils.sIsHistoryDialogShown = Boolean.FALSE;
        getDialog().dismiss();
    }

    public class SaveHistoryAsyncTask extends AsyncTask<Boolean, Void, Void> {

        private boolean mClearHistoryBase;

        @Override
        protected Void doInBackground(Boolean... bool) {
            mClearHistoryBase = bool[FlickrConstants.INT_ZERO];
            if (mClearHistoryBase) {
                FlickrLab.getInstance(getActivity()).clearAllHistory();
                PrefUtils.setStoredQuery(getActivity(), null);
            } else {
                FlickrLab.getInstance(getActivity()).restoreHistory(mSavedHistory);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (mClearHistoryBase) {
                mSavedHistory = mHistoryAdapter.getHistory();
                mHistoryAdapter.updateHistory(new ArrayList<HistoryItem>());
                restoreHistory();
            } else {
                mHistoryAdapter.updateHistory(mSavedHistory);
                InfoUtils.showShortShack(mBtnClear, getString(R.string.dialog_restore_restored));
            }
            checkHistory();
        }
    }

    private class OnHistorySwipeCallback extends ItemTouchHelper.SimpleCallback {

        private int mPosition;

        OnHistorySwipeCallback() {
            super(FlickrConstants.INT_ZERO, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        }

        @Override
        public boolean onMove(
                RecyclerView recyclerView,
                RecyclerView.ViewHolder viewHolder,
                RecyclerView.ViewHolder target) {
            return Boolean.FALSE;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            mPosition = viewHolder.getAdapterPosition();
            HistoryItem item = mHistoryAdapter.removeItem(mPosition);
            FlickrLab.getInstance(getActivity()).deleteHistory(item.getId());
            restoreSingleHistory(mPosition, item);
            checkHistory();
        }
    }
}