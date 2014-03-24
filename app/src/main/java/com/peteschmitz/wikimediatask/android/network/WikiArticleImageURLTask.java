package com.peteschmitz.wikimediatask.android.network;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.peteschmitz.wikimediatask.android.R;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

/**
 * Task used to query for a Wikipedia article's main image.
 * <p/>
 * Created by Pete on 3/15/14.
 */
public class WikiArticleImageURLTask extends AsyncTask<Void, Void, String> {

    private static final String TAG = "wmt image task";

    private static final int MAX_IMAGES = 30;

    private final String mDecodedArticleName;
    private Context mContext;
    private final String[] mPriorityWords = new String[]{};
    private final String[] mBlackListedWords = new String[]{"padlock"};
    private final String[] mSupportedImageExtensions = new String[]{
            ".jpg", ".gif", ".png", ".bmp"
    };

    public WikiArticleImageURLTask(Context context, String decodedArticleName) {
        mDecodedArticleName = decodedArticleName;
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
        String imageFileName = "";

        // Quick image search
        JSONObject queryObject = performQuickImageSearch(query);
        if (queryObject != null) {
            imageFileName = chooseBestImage(getQuickSearchImages(queryObject), query);
        }

        // Full image search
        if (TextUtils.isEmpty(imageFileName)) {
            queryObject = performFullImageSearch(query);

            if (queryObject != null){
                imageFileName = chooseBestImage(getFullSearchImages(queryObject), query);
            }
        }

        // Cancel if both quick and full searches failed
        if (TextUtils.isEmpty(imageFileName)) {
            return "";
        }

        // Replaces spaces with underscores
        imageFileName = imageFileName.replaceAll(" ", "_");

        // Generate an MD5 hash of the file name
        String hash = getHash(imageFileName);

        // Encode file name to valid URL format
        try {
            imageFileName = URLEncoder.encode(imageFileName, "ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // Build image URL
        String url = mContext.getResources().getString(R.string.wiki_commons) +
                hash.charAt(0) + "/" +
                hash.substring(0, 2) + "/" +
                imageFileName;

        // Check if url is valid; change to lang if it isn't
        if (!URLExists(url)) {
            Log.d("logd", "switch to lang: " + query);
            url = mContext.getResources().getString(R.string.wiki_lang) +
                    hash.charAt(0) + "/" +
                    hash.substring(0, 2) + "/" +
                    imageFileName;
        }

        return url;
    }

    private ArrayList<String> getQuickSearchImages(JSONObject query) {
        ArrayList<String> images = new ArrayList<String>();
        try {
            JSONArray imageArray = query.getJSONObject("parse").getJSONArray("images");

            for (int i = 0; i < imageArray.length(); i++) {
                String imageName = imageArray.get(i).toString();

                // Skip image if extension is unsupported
                if (!imageExtensionIsSupported(imageName)) continue;

                // Skip image if it contains a blacklisted word
                if (imageContainsBlackListedWord(imageName)) continue;

                images.add(imageName);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return images;
    }

    private boolean imageContainsBlackListedWord(String imageName) {

        for (String blackWord : mBlackListedWords){
            if (imageName.toLowerCase().contains(blackWord)) return true;
        }

        return false;
    }

    private ArrayList<String> getFullSearchImages(JSONObject query) {
        ArrayList<String> images = new ArrayList<String>();
        try {
            JSONObject pages = query.getJSONObject("query").getJSONObject("pages");
            JSONArray imageArray = pages.getJSONObject((String) pages.keys().next()).getJSONArray("images");

            for (int i = 0; i < imageArray.length(); i++) {
                String imageName = imageArray.getJSONObject(i).getString("title").split(":")[1];

                // Skip image if extension is unsupported
                if (!imageExtensionIsSupported(imageName)) continue;

                // Skip image if it contains a blacklisted word
                if (imageContainsBlackListedWord(imageName)) continue;

                images.add(imageName);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return images;
    }

    /**
     * Search wikipedia for images of supplied article query. Only the first section
     * is queried for images. Returns a string {@code JSONObject} of the query.
     */
    private JSONObject performQuickImageSearch(String articleQuery) {
        articleQuery = Uri.encode(articleQuery);

        String getRequest =
                mContext.getString(R.string.wiki_api) +
                        "&action=parse" +
                        "&page=" + articleQuery +
                        "&prop=images" +
                        "&format=json" +
                        "&section=0";

        String networkResponse = performHTTPRequest(getRequest);

        try {
            return new JSONObject(networkResponse);
        } catch (JSONException e) {
            return null;
        }
    }

    /**
     * Search wikipedia for all images (up to {@code MAX_IMAGES}) for the supplied article query.
     * This full query should only be used when {@link #performQuickImageSearch(String)} doesn't
     * provide a sufficient result.
     */
    private JSONObject performFullImageSearch(String articleQuery) {
        articleQuery = Uri.encode(articleQuery);

        String getRequest =
                mContext.getString(R.string.wiki_api) +
                        "&action=query" +
                        "&titles=" + articleQuery +
                        "&prop=images" +
                        "&format=json" +
                        "&imlimit=" + MAX_IMAGES;

        String networkResponse = performHTTPRequest(getRequest);

        try {
            return new JSONObject(networkResponse);
        } catch (JSONException e) {
            return null;
        }
    }

    private static String performHTTPRequest(String getRequest) {
        StringBuilder stringBuilder = new StringBuilder();
        HttpClient client = new DefaultHttpClient();
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
    private static String getHash(String base) {

        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");

            byte[] crypt = digest.digest(base.getBytes());
            return toHex(crypt);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 not supported");
        }
    }

    private String chooseBestImage(ArrayList<String> images, String query) {

        if (images == null || images.isEmpty()) return "";

        ArrayList<String> imageNames = new ArrayList<String>();
        ArrayList<String> queryNames = new ArrayList<String>();

        // Split query words; omit words that are 1 or 2 chars
        for (String queryName : query.split(" ")){
            if (queryName.length() > 2) queryNames.add(queryName.toLowerCase());
        }

        // Collect relevant image names (anything that contains a query word)
        lp:
        for (String imageName : images) {
            for (String queryName : queryNames){
                if (imageName.toLowerCase().contains(queryName)) {
                    imageNames.add(imageName);
                    continue lp;
                }
            }

        }

        // Use priority words to determine best image
        for (String priorityWord : mPriorityWords) {
            for (String imageName : imageNames) {
                if (imageName.toLowerCase().contains(priorityWord)) {
                    return imageName;
                }
            }
        }

        return imageNames.isEmpty() ? images.get(0) : imageNames.get(0);
    }

    private boolean imageExtensionIsSupported(String imageName) {
        for (String extension : mSupportedImageExtensions) {
            if (imageName.toLowerCase().contains(extension)) {
                return true;
            }
        }
        return false;
    }

    private static boolean URLExists(String url) {
        HttpClient client = new DefaultHttpClient();
        HttpHead head = new HttpHead(url);

        try {
            HttpResponse response = client.execute(head);
            return response.getStatusLine().getStatusCode() != HttpStatus.SC_NOT_FOUND;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    private static String toHex(byte[] data) {
        char[] chars = new char[data.length * 2];
        for (int i = 0; i < data.length; i++) {
            chars[i * 2] = HEX_DIGITS[(data[i] >> 4) & 0xf];
            chars[i * 2 + 1] = HEX_DIGITS[data[i] & 0xf];
        }
        return new String(chars);
    }
}
