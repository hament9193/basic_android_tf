package com.assg.basictfstyletransfer;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;


import java.io.IOException;
import java.io.InputStream;

/**
 * Created by hamentchoudhary on 29/11/17.
 */

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.VH>{
    Context mContext;
    RCallback callback;
    int count;


    public RecyclerAdapter(Context callback, int count) {
        this.count = count;
        this.callback = (RCallback)callback;
        this.mContext = callback;
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recycler, parent, false);

        return new VH(itemView);
    }

    @Override
    public void onBindViewHolder(VH holder, final int position) {
        holder.imageView.setImageDrawable(getDrawable(position));

        holder.imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callback.applyStyle(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return count;
    }

    class VH extends RecyclerView.ViewHolder{

        ImageView imageView;

        public VH(View itemView) {
            super(itemView);
            imageView = (ImageView) itemView.findViewById(R.id.item_recycler);
        }
    }

    public interface RCallback{
        void applyStyle(int position);
    }

    public Drawable getDrawable(int position){
        try {
            // get input stream
            InputStream ims = mContext.getAssets().open("thumbnails/style" + position + ".jpg");
            // load image as Drawable
            Drawable d = Drawable.createFromStream(ims, null);
            // set image to ImageView
            return d;
        }
        catch(IOException ex) {
            return null;
        }
    }
}
