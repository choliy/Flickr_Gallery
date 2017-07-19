package com.choliy.igor.flickrgallery.adapter;

import android.content.Context;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.choliy.igor.flickrgallery.R;
import com.choliy.igor.flickrgallery.model.GalleryItem;
import com.choliy.igor.flickrgallery.util.PrefUtils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.PictureHolder> {

    private Context mContext;
    private List<GalleryItem> mItems;
    private OnPictureClickListener mListener;

    public interface OnPictureClickListener {

        void onRequestedLastItem(int position);

        void onPictureClicked(String pictureTitle, String itemUri);
    }

    public GalleryAdapter(
            Context context,
            List<GalleryItem> items,
            OnPictureClickListener listener) {

        mContext = context;
        mItems = items;
        mListener = listener;
    }

    @Override
    public PictureHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(setupLayout(), parent, Boolean.FALSE);
        return new PictureHolder(view);
    }

    @Override
    public void onBindViewHolder(PictureHolder holder, int position) {
        holder.bindItem(position);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public void updateItems(List<GalleryItem> items) {
        mItems = items;
        notifyDataSetChanged();
    }

    private int setupLayout() {
        int layout;
        String savedStyle = PrefUtils.getStyleSettings(mContext);
        String cardStyle = mContext.getString(R.string.pref_grid_style_value_3);
        boolean newVersion = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;

        if (savedStyle.equals(cardStyle) && newVersion)
            layout = R.layout.list_item_card_new;
        else if (savedStyle.equals(cardStyle) && !newVersion)
            layout = R.layout.list_item_card_old;
        else if (newVersion)
            layout = R.layout.list_item_simple_new;
        else
            layout = R.layout.list_item_simple_old;

        return layout;
    }

    class PictureHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        @BindView(R.id.gallery_image_item) ImageView mPicture;

        PictureHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            String pictureTitle = mItems.get(getAdapterPosition()).getTitle();
            String itemUri = mItems.get(getAdapterPosition()).getItemUri();
            mListener.onPictureClicked(pictureTitle, itemUri);
        }

        private void bindItem(int position) {
            //mProgressView.smoothToShow();
            String url = mItems.get(position).getSmallPictureUrl();
            boolean animationOn = PrefUtils.getAnimationSettings(mContext);
            if (animationOn) {
                Glide.with(mContext)
                        .load(url)
                        .animate(R.anim.anim_picture)
                        .into(mPicture);
            } else Glide.with(mContext).load(url).into(mPicture);

            // Callback on the end of the list
            if (position == getItemCount() - 1)
                mListener.onRequestedLastItem(getItemCount());
        }
    }
}