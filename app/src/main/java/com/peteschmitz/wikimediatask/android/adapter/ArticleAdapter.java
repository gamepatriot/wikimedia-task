package com.peteschmitz.wikimediatask.android.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.peteschmitz.wikimediatask.android.R;

/**
 * Created by Pete on 3/15/14.
 */
public class ArticleAdapter extends ArrayAdapter<String> {

    private Context mContext;

    public ArticleAdapter(Context context){
        this(context, null);
    }

    public ArticleAdapter(Context context, String[] items){
        super(context, R.layout.grid_item, items);

        mContext = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null){
            convertView = View.inflate(mContext, R.layout.grid_item, null);
        }

        return convertView;
    }
}
