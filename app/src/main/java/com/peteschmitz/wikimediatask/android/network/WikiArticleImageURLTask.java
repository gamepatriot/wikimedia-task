package com.peteschmitz.wikimediatask.android.network;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.peteschmitz.wikimediatask.android.R;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Task used to query for a Wikipedia article's main image.
 *
 * Created by Pete on 3/15/14.
 */
public class WikiArticleImageURLTask extends AsyncTask<Void, Void, String> {

    private static final String TAG = "wmt image task";

    private final String mDecodedArticleName;
    private Context mContext;

    public WikiArticleImageURLTask(Context context, String decodedArticleName){
        mDecodedArticleName =decodedArticleName;
        mContext = context;
    }

    @Override
    protected String doInBackground(Void... params) {
        return getArticleImageUrl(mDecodedArticleName);
    }

    /**
     * Retrieve the URL of an article image from the wiki-commons website. Note: supplied query
     * should be decoded.
     */
    private String getArticleImageUrl(String query) {
        String result = "";

        try {
            JSONObject queryObject = new JSONObject(performWikiImageSearch(query)).getJSONObject("query-continue");

                if (queryObject != null){
                    JSONObject imageObject = queryObject.getJSONObject("images");

                    if (imageObject != null){

                        // Get file name
                        String fileName = imageObject.getString("imcontinue").split("\\|")[1];

                        // Get md5 of file name
                        String hash = getHash(fileName);

                        // Build image URL

                        return mContext.getResources().getString(R.string.wiki_commons) +
                                hash.charAt(0) + "/" +
                                hash.substring(0, 2) + "/" +
                                fileName;
                    }
                }


        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
        }

        return result;
    }

    /**
     * Search wikipedia for supplied article query,  and then find image file names relating to
     * the article. Returns a {@code JSONObject} string.
     */
    private String performWikiImageSearch(String articleQuery) {
        articleQuery = Uri.encode(articleQuery);

        StringBuilder stringBuilder = new StringBuilder();
        HttpClient client = new DefaultHttpClient();
        String getRequest = mContext.getString(R.string.wiki_api) + "?&action=query&titles=" + articleQuery + "&prop=images&format=json";
        HttpGet get = new HttpGet(getRequest);

        try {
            HttpResponse response = client.execute(get);
            StatusLine statusLine = response.getStatusLine();
            int statusCode = statusLine.getStatusCode();
            if (statusCode == HttpStatus.SC_OK) {

                HttpEntity entity = response.getEntity();
                InputStream content = entity.getContent();
                BufferedReader reader = new BufferedReader(new InputStreamReader(content));
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }

            } else {
                Log.e(TAG, "Http status invalid, code: " + statusCode);
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }

        return stringBuilder.toString();
    }

    /**
     * Convenience function; get MD5 of a string
     */
    private static String getHash(String base){

        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");

            byte[] crypt = digest.digest(base.getBytes());
            BigInteger bInt = new BigInteger(1, crypt);
            return bInt.toString(16);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 not supported");
        }
    }
}
