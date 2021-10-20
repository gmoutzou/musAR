package com.gmoutzou.musar.gui.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.gmoutzou.musar.R;

public class LocalDataAdapter extends RecyclerView.Adapter<LocalDataAdapter.ViewHolder> {
    private int[] images = {R.drawable.avatar_1, R.drawable.avatar_2, R.drawable.avatar_3,
                            R.drawable.avatar_4, R.drawable.avatar_5, R.drawable.avatar_6,
                            R.drawable.avatar_7, R.drawable.avatar_8, R.drawable.avatar_9,
                            R.drawable.avatar_10, R.drawable.avatar_11, R.drawable.avatar_12};
    private BannerCallbackInterface bannerCallbackInterface;

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image_over, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.imageView.setImageResource(images[position]);
        holder.imageView.setTag(position);
    }

    @Override
    public int getItemCount() {
        return images.length;
    }

    public void registerCallbackInterface(BannerCallbackInterface bannerCallbackInterface) {
        this.bannerCallbackInterface = bannerCallbackInterface;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        ViewHolder(View itemView) {
            super(itemView);
            imageView = (ImageView) itemView.findViewById(R.id.image);
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    bannerCallbackInterface.onBannerItemClick(v.getTag().toString());
                }
            });
        }
    }
}
