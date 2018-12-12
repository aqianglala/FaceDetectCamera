package com.example.myfacedetectcamera.activities;

import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.example.myfacedetectcamera.BaseApplication;
import com.example.myfacedetectcamera.R;
import com.example.myfacedetectcamera.adapters.UserListAdapter;
import com.example.myfacedetectcamera.model.UserModel;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;

public class ManagerActivity extends AppCompatActivity implements UserListAdapter.OnDeleteClickListener,
        SwipeRefreshLayout.OnRefreshListener {

    private static final String mTag = ManagerActivity.class.getSimpleName();
    private RecyclerView mRecyclerView;
    private List<UserModel> mData = new ArrayList<>();
    private UserListAdapter mAdapter;
    private boolean isLoading;
    private boolean isRefresh;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private int lastVisibleItem = 0;
    private LinearLayoutManager mLayoutManager;
    private int mPageSize = 10;
    private int mPageIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager);


        mAdapter = new UserListAdapter(mData);
        mAdapter.setOnDeleteClickListener(this);

        mLayoutManager = new LinearLayoutManager(this);

        mRecyclerView = findViewById(R.id.recyclerView);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE && lastVisibleItem + 1 == mAdapter.getItemCount()) {
                    loadMore();
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                lastVisibleItem = mLayoutManager.findLastVisibleItemPosition();
            }
        });

        mSwipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                if (!isLoading) {
                    isLoading = true;
                    mSwipeRefreshLayout.setRefreshing(true);
                    getUser(0);
                }
            }
        });
    }

    private void getUser(int pageIndex) {
        String ip = BaseApplication.getIp();
        if (TextUtils.isEmpty(ip)) {
            showToast("请先设置ip！");
            stopLoading();
            return;
        }
        String url = "http://" + ip + ":7070/App/user/page/" + pageIndex + "/" + mPageSize;
        OkHttpUtils
                .get()
                .url(url)
                .build()
                .execute(new StringCallback() {
                    @Override
                    public void onError(Call call, Exception e, int id) {
                        Log.e(mTag, e.getMessage());
                        stopLoading();
                    }

                    @Override
                    public void onResponse(String response, int id) {
                        Log.e(mTag, response);
                        List<UserModel> data = new Gson().fromJson(response, new TypeToken<List<UserModel>>() {
                        }.getType());
                        if (data != null && data.size() > 0) {
                            if (isRefresh) {
                                mData.clear();
                            }
                            mData.addAll(data);
                            mAdapter.notifyDataSetChanged();
                        }
                        stopLoading();
                    }
                });
    }

    private void deleteUser(String userId, final int pos) {
        if (TextUtils.isEmpty(userId)) {
            showToast("userId为空！");
            return;
        }
        String ip = BaseApplication.getIp();
        if (TextUtils.isEmpty(ip)) {
            showToast("请先设置ip！");
            return;
        }
        String url = "http://" + ip + ":7070/App/user/delete/" + userId;
        OkHttpUtils
                .post()
                .url(url)
                .addParams("userId", userId)
                .build()
                .execute(new StringCallback() {
                    @Override
                    public void onError(Call call, Exception e, int id) {
                        Log.e(mTag, e.getMessage());
                        showToast(e.getMessage());
                    }

                    @Override
                    public void onResponse(String response, int id) {
                        Log.e(mTag, response);
                        if ("true".equals(response)) {
                            mAdapter.removeData(pos);
                        } else {
                            showToast("删除失败！");
                        }
                    }
                });
    }

    @Override
    public void delete(UserModel model, int pos) {
        deleteUser(model.getUserId(), pos);
    }

    private void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRefresh() {
        if (!isLoading) {
            isLoading = true;
            isRefresh = true;
            mPageIndex = 0;
            getUser(0);
        }
    }

    private void loadMore() {
        if (!isLoading) {
            isLoading = true;
            isRefresh = false;
            mSwipeRefreshLayout.setRefreshing(true);
            getUser(++mPageIndex);
        }
    }

    private void stopLoading() {
        isLoading = false;
        mSwipeRefreshLayout.setRefreshing(false);
    }
}
