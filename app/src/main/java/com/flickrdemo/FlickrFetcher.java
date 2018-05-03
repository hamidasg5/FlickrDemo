package com.flickrdemo;

import android.net.Uri;
import android.util.Log;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class FlickrFetcher
{
    private static final String TAG = "FlickrFetcher";
    private static final String API_KEY = "704850b52056d831ee5c61e918c851f7";
    //private static final String SECRET_KEY = "d19b5a241ce36d8c";
    private static final String GET_RECENT_METHOD = "flickr.photos.getRecent";
    private static final String GET_POPULAR_METHOD = "flickr.photos.getPopular";
    private static final String SEARCH_METHOD = "flickr.photos.search";
    private static final Uri ENDPOINT = Uri
            .parse("https://api.flickr.com/services/rest/")
            .buildUpon()
            .appendQueryParameter("api_key", API_KEY)
            .appendQueryParameter("format", "json")
            .appendQueryParameter("nojsoncallback", "1")
            .appendQueryParameter("extras", "url_s")
            .appendQueryParameter("safe_search", String.valueOf(1))
            .build();

    public enum FlickrMethod
    {
        SEARCH, GET_RECENT, GET_POPULAR
    }

    private OkHttpClient mClient = new OkHttpClient();
    private Gson gson = new Gson();

    public void requestItems(FlickrMethod flickrMethod, Callback callback, int page, String extra)
    {
        switch (flickrMethod)
        {
            case GET_RECENT:
                String recentUrl = getUrl(GET_RECENT_METHOD, null, page);
                requestUrlBytes(recentUrl, callback);
                break;
            case GET_POPULAR:
                String popularUrl = getUrl(GET_POPULAR_METHOD, null, page);
                requestUrlBytes(popularUrl, callback);
                break;
            case SEARCH:
                String searchUrl = getUrl(SEARCH_METHOD, (extra==null) ? "flickr" : extra, page);
                requestUrlBytes(searchUrl, callback);
                break;
        }
    }

    public List<FlickrItem> parseItems(String jsonString)
    {
        List<FlickrItem> items = new ArrayList<>();
        try
        {
            JSONObject root = new JSONObject(jsonString);
            JSONObject photos = root.getJSONObject("photos");
            JSONArray photoArray = photos.getJSONArray("photo");
            items = Arrays.asList(gson.fromJson(photoArray.toString(), FlickrItem[].class));
        }
        catch (JSONException je)
        {
            Log.e(TAG, "Failed to parse json: ", je);
        }
        return items;
    }

    private void requestUrlBytes(String url, Callback callback)
    {
        Request request = new Request.Builder().url(url).build();
        mClient.newCall(request).enqueue(callback);
    }

    private String getUrl(String method, String extra, int page)
    {
        Uri.Builder builder = ENDPOINT.buildUpon();
        builder.appendQueryParameter("method", method);
        builder.appendQueryParameter("page", String.valueOf(page));

        if (method.equals(GET_POPULAR_METHOD))
        {
            builder.appendQueryParameter("user", "user");
        }
        else if (method.equals(SEARCH_METHOD))
        {
            builder.appendQueryParameter("text", extra);
        }
        return builder.build().toString();
    }
}
