package com.peteschmitz.wikimediatask.android.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.peteschmitz.wikimediatask.android.R;
import com.peteschmitz.wikimediatask.android.network.WikiArticleImageURLTask;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Pete on 3/15/14.
 */
public class ArticleAdapter extends ArrayAdapter<String> {

    private int[] mColors;
    private Context mContext;
    private HashMap<String, String> mArticleImageMap = new HashMap<String, String>();
    private Set<String> mQueuedImageURLRequests = new HashSet<String>();

    public ArticleAdapter(Context context){
        this(context, null);
    }

    public ArticleAdapter(Context context, String[] items){
        super(context, R.layout.grid_item, items);

        mContext = context;

        setColors();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;

        if (convertView == null){

            convertView = View.inflate(mContext, R.layout.grid_item, null);

            holder = new ViewHolder();
            holder.imageContainer = (ImageView) convertView.findViewById(R.id.image_container);
            holder.imageCaption = (TextView) convertView.findViewById(R.id.image_caption);
            holder.imagePlaceholder = (LinearLayout) convertView.findViewById(R.id.image_placeholder);
            holder.callback = new Callback() {
                @Override
                public void onSuccess() {
                    holder.imagePlaceholder.setBackgroundResource(R.color.white);
                }

                @Override
                public void onError() {
                    showPlaceholder(holder);
                }
            };
            convertView.setTag(holder);

        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        // Get target article
        final String article = getItem(position);

        holder.activeArticle = article;
        holder.index = position;

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

                        if (holder.activeArticle.equals(article)){
                            loadViewImage(holder);
                        }
                    }
                }
                    .execute();

                // Add article to queue
                mQueuedImageURLRequests.add(article);
            }

            showPlaceholder(holder);

        } else {
            loadViewImage(holder);
        }

        holder.imageCaption.setText(holder.activeArticle);

        return convertView;
    }

    private void loadViewImage(ViewHolder holder) {
        String url = mArticleImageMap.get(holder.activeArticle);

        // Cancel load if url is null/empty
        if (TextUtils.isEmpty(url)){
            showPlaceholder(holder);
            return;
        }

        Picasso.with(mContext)
                .load(url)
                .fit()
                .centerCrop()
                .into(holder.imageContainer, holder.callback);
    }

    private void showPlaceholder(ViewHolder holder) {
        holder.imageContainer.setImageBitmap(null);
        holder.imagePlaceholder.setVisibility(View.VISIBLE);
        holder.imagePlaceholder.setBackgroundColor(getColor(holder.index));
    }

    private void setColors(){
        mColors = mContext.getResources().getIntArray(R.array.placeholder_colors);
    }

    private int getColor(int position){
        return mColors[position % mColors.length];
    }

    private class ViewHolder{
        public String activeArticle;
        public ImageView imageContainer;
        public TextView imageCaption;
        public LinearLayout imagePlaceholder;
        public Callback callback;
        public int index;
    }
}
