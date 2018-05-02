package com.flickrdemo;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.content.AsyncTaskLoader;

import java.util.ArrayList;
import java.util.List;

public class FetchItemsTask extends AsyncTaskLoader
{
    public enum Method
    {
        SEARCH, GET_RECENT, GET_POPULAR
    }

    private int mPage;
    private Method mMethod;
    private String mExtra;

    FetchItemsTask(Context context, int page)
    {
        super(context);
        mPage = page;
        mMethod = Method.GET_RECENT;
        mExtra = "";
    }

    @Nullable
    @Override
    public List<FlickrItem> loadInBackground()
    {
        switch (mMethod)
        {
            case GET_RECENT:
                return new FlickrFetcher().getRecent(mPage);
            case GET_POPULAR:
                return new FlickrFetcher().getPopular(mPage);
            case SEARCH:
                return new FlickrFetcher().searchPhotos(mExtra, mPage);
            default:
                return new ArrayList<>();
        }
    }

    public void setPage(int page)
    {
        mPage = page;
    }

    public void setMethod(Method method) {
        mMethod = method;
    }

    public Method getMethod() {
        return mMethod;
    }

    public void setExtra(String extra)
    {
        if (extra!=null && !extra.trim().equals(""))
        {
            mExtra = extra;
        }
    }
}
