package com.peteschmitz.wikimediatask.android.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import com.peteschmitz.wikimediatask.android.R;
import com.peteschmitz.wikimediatask.android.network.WikiArticleImageURLTask;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Pete on 3/15/14.
 */
public class ArticleAdapter extends ArrayAdapter<String> {

    private Context mContext;
    private HashMap<String, String> mArticleImageMap = new HashMap<String, String>();
    private Set<String> mQueuedImageURLRequests = new HashSet<String>();

    public ArticleAdapter(Context context){
        this(context, null);
    }

    public ArticleAdapter(Context context, String[] items){
        super(context, R.layout.grid_item, items);

        mContext = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;

        if (convertView == null){

            convertView = View.inflate(mContext, R.layout.grid_item, null);

            holder = new ViewHolder();
            holder.imageContainer = (ImageView) convertView.findViewById(R.id.image_container);
            convertView.setTag(holder);

        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        // Get target article
        final String article = getItem(position);
        holder.activeArticle = article;

        // Load article image URL
        if (!mArticleImageMap.containsKey(article)){

            // Only make request if article isn't already queued
            if (!mQueuedImageURLRequests.contains(article)){

                // Perform async retrieval of URL
                new WikiArticleImageURLTask(mContext, article){

                    @Override
                    protected void onPostExecute(String s) {

                        // Remove from queue
                        mQueuedImageURLRequests.remove(article);

                        // Insert result to map
                        mArticleImageMap.put(article, s);

                        notifyDataSetInvalidated();
                    }
                }
                    .execute();

                // Add article to queue
                mQueuedImageURLRequests.add(article);
            }

        } else {
            loadViewImage(holder);
        }

        return convertView;
    }

    private void loadViewImage(ViewHolder holder) {
        String url = mArticleImageMap.get(holder.activeArticle);

        // Cancel load if url is null/empty
        // TODO: Show placeholder instead
        if (TextUtils.isEmpty(url)) return;

        Picasso.with(mContext)
                .load(url)
                .fit()
                .centerCrop()
                .into(holder.imageContainer);
    }

    private class ViewHolder{
        public String activeArticle;
        public ImageView imageContainer;
    }
}
