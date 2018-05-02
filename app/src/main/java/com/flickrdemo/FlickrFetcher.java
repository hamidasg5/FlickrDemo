package com.flickrdemo;

import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class FlickrFetcher
{
    private static final String TAG = "FlickrFetcher";
    private static final String API_KEY = "704850b52056d831ee5c61e918c851f7";
    private static final String SECRET_KEY = "d19b5a241ce36d8c";
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

    public byte[] getUrlBytes(String spec) throws IOException
    {
        URL url = new URL(spec);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

        try
        {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            InputStream inputStream = urlConnection.getInputStream();

            if (urlConnection.getResponseCode() != HttpURLConnection.HTTP_OK)
            {
                throw new IOException(urlConnection.getResponseMessage() + ": with " + spec);
            }

            int bytesRead;
            byte[] buffer = new byte[1024];
            while ((bytesRead = inputStream.read(buffer)) > 0)
            {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.close();

            return outputStream.toByteArray();
        }
        finally
        {
            urlConnection.disconnect();
        }
    }

    private String getUrlString(String spec) throws IOException
    {
        return new String(getUrlBytes(spec));
    }

    public List<FlickrItem> getRecent(int page)
    {
        String url = getUrl(GET_RECENT_METHOD, null, page);
        return fetchItems(url);
    }

    public List<FlickrItem> getPopular(int page)
    {
        String url = getUrl(GET_POPULAR_METHOD, null, page);
        return fetchItems(url);
    }

    public List<FlickrItem> searchPhotos(String query, int page)
    {
        String url = getUrl(SEARCH_METHOD, query, page);
        return fetchItems(url);
    }

    private List<FlickrItem> fetchItems(String url)
    {
        List<FlickrItem> items = new ArrayList<>();

        try
        {
            Log.i(TAG, url);
            String jsonString = getUrlString(url);
            JSONObject root = new JSONObject(jsonString);
            parseItems(items, root);
        }
        catch (IOException ioe)
        {
            Log.e(TAG, "Failed to fetch: ", ioe);
        }
        catch (JSONException je)
        {
            Log.e(TAG, "Failed to parse json: ", je);
        }

        return items;
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

    private void parseItems(List<FlickrItem> items, JSONObject root) throws IOException, JSONException
    {
        JSONObject photos = root.getJSONObject("photos");
        JSONArray photoArray = photos.getJSONArray("photo");

        for (int i = 0; i < photoArray.length(); i++)
        {
            JSONObject photo = photoArray.getJSONObject(i);

            FlickrItem item = new FlickrItem();
            item.setId(photo.getString("id"));
            item.setCaption(photo.getString("title"));
            item.setOwner(photo.getString("owner"));

            if (photo.has("url_s"))
            {
                item.setUrl(photo.getString("url_s"));
                items.add(item);
            }
        }
    }
}
