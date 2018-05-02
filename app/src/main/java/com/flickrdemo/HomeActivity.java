package com.flickrdemo;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<List<FlickrItem>>
{
    private static final String TAG = "HomeActivity";

    private static final int FLICKR_FETCHER_LOADER_ID = 1;
    private static final int ITEMS_PER_PAGE = 9;

    private int mNumColumns = 3;
    private int mPage = 1;

    private RecyclerView mPhotoRecyclerView;
    private GridLayoutManager mLayoutManager;
    private PhotoAdapter mPhotoAdapter;
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;
    private TextView mNoInternetTextView;
    private ProgressBar mLoadingProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Toolbar toolbar = findViewById(R.id.home_toolbar);
        setSupportActionBar(toolbar);

        mNoInternetTextView = findViewById(R.id.no_internet_connection);
        mNoInternetTextView.setVisibility(View.GONE);

        mLoadingProgressBar = findViewById(R.id.loading_content_progress);
        mLoadingProgressBar.setVisibility(View.VISIBLE);

        mPhotoRecyclerView = findViewById(R.id.photo_recycler_view);
        mLayoutManager = new GridLayoutManager(this, mNumColumns);
        mPhotoRecyclerView.setLayoutManager(mLayoutManager);
        mPhotoAdapter = new PhotoAdapter();
        mPhotoRecyclerView.setAdapter(mPhotoAdapter);
        mPhotoRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                DisplayMetrics displayMetrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                float factor = displayMetrics.density;
                int screenWidth = displayMetrics.widthPixels;
                int widthDp = (int)(screenWidth / factor);
                mNumColumns = widthDp / 120;
                mLayoutManager.setSpanCount(mNumColumns);
            }
        });
        mPhotoRecyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                int lastIndex = mLayoutManager.findLastCompletelyVisibleItemPosition();
                if (mPhotoAdapter.getFlickrItems().size()-10 < lastIndex)
                {
                    if (mPage * ITEMS_PER_PAGE - 10 < lastIndex)
                    {
                        mPage = mPage + 1;
                        FetchItemsTask fetchItemsTask = (FetchItemsTask) getSupportLoaderManager().getLoader(FLICKR_FETCHER_LOADER_ID);
                        if (fetchItemsTask != null)
                        {
                            mLoadingProgressBar.setVisibility(View.VISIBLE);
                            fetchItemsTask.setPage(mPage);
                            fetchItemsTask.forceLoad();
                        }
                    }
                }
            }
        });
        mPhotoRecyclerView.setVisibility(View.GONE);

        getSupportLoaderManager().initLoader(FLICKR_FETCHER_LOADER_ID, null, this).forceLoad();

        Handler responseHandler = new Handler();
        mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);
        mThumbnailDownloader.setThumbnailDownloadListener(new ThumbnailDownloader.ThumbnailDownloadListener<PhotoHolder>() {
            @Override
            public void onThumbnailDownloaded(PhotoHolder target, Bitmap thumbnail) {
                Drawable drawable = new BitmapDrawable(getResources(), thumbnail);
                target.bindDrawable(drawable);
            }
        });
        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        mThumbnailDownloader.clearQueue();
        mThumbnailDownloader.quit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.activity_home, menu);

        MenuItem searchItem = menu.findItem(R.id.menu_item_search_photos);
        final SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                if (!s.trim().equals(""))
                {
                    FetchItemsTask fetchItemsTask = (FetchItemsTask) getSupportLoaderManager().getLoader(FLICKR_FETCHER_LOADER_ID);
                    if (fetchItemsTask != null)
                    {
                        mPhotoAdapter.setFlickrItems(new ArrayList<FlickrItem>());
                        fetchItemsTask.setMethod(FetchItemsTask.Method.SEARCH);
                        fetchItemsTask.setExtra(s);
                        fetchItemsTask.setPage(1);
                        fetchItemsTask.forceLoad();
                        mPage = 1;
                        mPhotoRecyclerView.setVisibility(View.GONE);
                        mLoadingProgressBar.setVisibility(View.VISIBLE);
                    }
                }
                else
                {
                    Toast.makeText(HomeActivity.this, "search query is empty", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        FetchItemsTask fetchItemsTask = (FetchItemsTask) getSupportLoaderManager().getLoader(FLICKR_FETCHER_LOADER_ID);
        switch (item.getItemId())
        {
            case R.id.menu_item_recent_photos:
                if (fetchItemsTask != null)
                {
                    if (fetchItemsTask.getMethod() != FetchItemsTask.Method.GET_RECENT)
                    {
                        fetchItemsTask.setPage(1);
                        fetchItemsTask.setMethod(FetchItemsTask.Method.GET_RECENT);
                        mPhotoAdapter.setFlickrItems(new ArrayList<FlickrItem>());
                        fetchItemsTask.forceLoad();
                        mPage = 1;
                        mPhotoRecyclerView.setVisibility(View.GONE);
                        mLoadingProgressBar.setVisibility(View.VISIBLE);
                    }
                }
                return true;
            case R.id.menu_item_popular_photos:
                if (fetchItemsTask != null)
                {
                    if (fetchItemsTask.getMethod() != FetchItemsTask.Method.GET_POPULAR)
                    {
                        fetchItemsTask.setPage(1);
                        fetchItemsTask.setMethod(FetchItemsTask.Method.GET_POPULAR);
                        mPhotoAdapter.setFlickrItems(new ArrayList<FlickrItem>());
                        fetchItemsTask.forceLoad();
                        mPage = 1;
                        mPhotoRecyclerView.setVisibility(View.GONE);
                        mLoadingProgressBar.setVisibility(View.VISIBLE);
                    }
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @NonNull
    @Override
    public Loader<List<FlickrItem>> onCreateLoader(int id, @Nullable Bundle args)
    {
        return new FetchItemsTask(this, mPage);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<List<FlickrItem>> loader, List<FlickrItem> data)
    {
        if (data.size() == 0 && mPage == 1)
        {
            mPhotoRecyclerView.setVisibility(View.GONE);
            mNoInternetTextView.setVisibility(View.VISIBLE);
            mLoadingProgressBar.setVisibility(View.GONE);
            return;
        }

        mPhotoAdapter.addToList(data);
        Log.i(TAG, "Page updated");

        mPhotoAdapter.notifyDataSetChanged();
        mLoadingProgressBar.setVisibility(View.GONE);
        if (mPhotoRecyclerView.getVisibility() == View.GONE)
        {
            mPhotoRecyclerView.setVisibility(View.VISIBLE);
            mNoInternetTextView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<List<FlickrItem>> loader) {}

    private class PhotoHolder extends RecyclerView.ViewHolder implements View.OnClickListener
    {
        private ImageView mImageView;
        private FlickrItem mFlickrItem;

        PhotoHolder(View itemView)
        {
            super(itemView);
            itemView.setOnClickListener(this);
            mImageView = itemView.findViewById(R.id.item_image_view);
            mImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        }

        void bindDrawable(Drawable drawable)
        {
            mImageView.setImageDrawable(drawable);
        }

        void bindItem(FlickrItem flickrItem)
        {
            mFlickrItem = flickrItem;
        }

        @Override
        public void onClick(View v)
        {
            //Intent intent = new Intent(Intent.ACTION_VIEW, mFlickrItem.getPhotoPageUri());
            Intent intent = PhotoPageActivity.newIntent(HomeActivity.this, mFlickrItem.getPhotoPageUri());
            startActivity(intent);
        }
    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder>
    {
        private List<FlickrItem> mFlickrItems;

        PhotoAdapter()
        {
            mFlickrItems = new ArrayList<>();
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup viewGroup, int viewType)
        {
            View view = LayoutInflater.from(HomeActivity.this).inflate(R.layout.item_home_list, viewGroup, false);
            return new PhotoHolder(view);
        }

        @Override
        public void onBindViewHolder(PhotoHolder photoHolder, int position)
        {
            FlickrItem item = mFlickrItems.get(position);

            Drawable placeholder = getDrawable(R.drawable.image_placeholder);
            photoHolder.bindDrawable(placeholder);
            mThumbnailDownloader.queueThumbnail(photoHolder, item.getUrl());
            photoHolder.bindItem(item);
        }

        @Override
        public int getItemCount()
        {
            return mFlickrItems.size();
        }

        void addToList(List<FlickrItem> newItems)
        {
            mFlickrItems.addAll(newItems);
        }

        public List<FlickrItem> getFlickrItems()
        {
            return mFlickrItems;
        }

        public void setFlickrItems(List<FlickrItem> flickrItems) {
            mFlickrItems = flickrItems;
        }
    }
}
