package com.flickrdemo;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class HomeActivity extends AppCompatActivity
{
    private static final String TAG = "HomeActivity";

    private static final int ITEMS_PER_PAGE = 100;

    private int mNumColumns = 3;
    private int mPage = 1;

    private RecyclerView mPhotoRecyclerView;
    private GridLayoutManager mLayoutManager;
    private PhotoAdapter mPhotoAdapter;
    private TextView mNoInternetTextView;
    private ProgressBar mLoadingProgressBar;

    private FlickrFetcher mFlickrFetcher;
    private FlickrFetcher.FlickrMethod mFlickrMethod = FlickrFetcher.FlickrMethod.GET_RECENT;
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mFlickrFetcher = new FlickrFetcher();
        mHandler = new Handler(Looper.getMainLooper());

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
                        mLoadingProgressBar.setVisibility(View.VISIBLE);
                        mFlickrFetcher.requestItems(mFlickrMethod, mResponseCallback, mPage, null);
                    }
                }
            }
        });
        mPhotoRecyclerView.setVisibility(View.GONE);

        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        if (activeNetwork.isConnectedOrConnecting())
        {
            mFlickrFetcher.requestItems(mFlickrMethod, mResponseCallback, mPage, null);
        }
        else
        {
            Toast.makeText(this, "No network access!", Toast.LENGTH_LONG).show();
        }
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
                    mPhotoAdapter.setFlickrItems(new ArrayList<FlickrItem>());
                    mPage = 1;
                    mFlickrMethod = FlickrFetcher.FlickrMethod.SEARCH;
                    mFlickrFetcher.requestItems(mFlickrMethod, mResponseCallback, mPage, s);
                    mPhotoRecyclerView.setVisibility(View.GONE);
                    mLoadingProgressBar.setVisibility(View.VISIBLE);
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
        switch (item.getItemId())
        {
            case R.id.menu_item_recent_photos:
                if (mFlickrMethod != FlickrFetcher.FlickrMethod.GET_RECENT)
                {
                    mPage = 1;
                    mFlickrMethod = FlickrFetcher.FlickrMethod.GET_RECENT;
                    mPhotoAdapter.setFlickrItems(new ArrayList<FlickrItem>());
                    mFlickrFetcher.requestItems(mFlickrMethod, mResponseCallback, mPage, null);
                    mPhotoRecyclerView.setVisibility(View.GONE);
                    mLoadingProgressBar.setVisibility(View.VISIBLE);
                }
                return true;
            case R.id.menu_item_popular_photos:
                if (mFlickrMethod != FlickrFetcher.FlickrMethod.GET_POPULAR)
                {
                    mPage = 1;
                    mFlickrMethod = FlickrFetcher.FlickrMethod.GET_POPULAR;
                    mPhotoAdapter.setFlickrItems(new ArrayList<FlickrItem>());
                    mFlickrFetcher.requestItems(mFlickrMethod, mResponseCallback, mPage, null);
                    mPhotoRecyclerView.setVisibility(View.GONE);
                    mLoadingProgressBar.setVisibility(View.VISIBLE);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    Callback mResponseCallback = new Callback() {
        @Override
        public void onFailure(Call call, IOException e)
        {
            Log.e(TAG, e.toString());
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mPhotoRecyclerView.setVisibility(View.GONE);
                    mNoInternetTextView.setVisibility(View.VISIBLE);
                    mLoadingProgressBar.setVisibility(View.GONE);
                }
            });
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException
        {
            String json = response.body().string();
            final List<FlickrItem> flickrItems = mFlickrFetcher.parseItems(json);

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (flickrItems.size() == 0 && mPage == 1)
                    {
                        mPhotoRecyclerView.setVisibility(View.GONE);
                        mNoInternetTextView.setVisibility(View.VISIBLE);
                        mLoadingProgressBar.setVisibility(View.GONE);
                        return;
                    }

                    mPhotoAdapter.addToList(flickrItems);
                    Log.i(TAG, "Page updated");

                    mPhotoAdapter.notifyDataSetChanged();
                    mLoadingProgressBar.setVisibility(View.GONE);
                    if (mPhotoRecyclerView.getVisibility() == View.GONE)
                    {
                        mPhotoRecyclerView.setVisibility(View.VISIBLE);
                        mNoInternetTextView.setVisibility(View.GONE);
                    }
                }
            });
        }
    };

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
            photoHolder.bindItem(item);
            Glide.with(HomeActivity.this)
                    .asBitmap()
                    .load(item.getUrl())
                    .apply(new RequestOptions().placeholder(R.drawable.image_placeholder))
                    .into(photoHolder.mImageView);
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
