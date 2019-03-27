package com.zzy.minibo.Fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.sina.weibo.sdk.auth.AccessTokenKeeper;
import com.sina.weibo.sdk.auth.Oauth2AccessToken;
import com.zzy.minibo.Adapter.StatusAdapter;
import com.zzy.minibo.Members.Status;
import com.zzy.minibo.Members.StatusTimeLine;
import com.zzy.minibo.R;
import com.zzy.minibo.Utils.AllParams.ParamsOfStatusTL;
import com.zzy.minibo.Utils.HttpCallBack;
import com.zzy.minibo.Utils.WBApiConnector;

import java.util.ArrayList;
import java.util.List;

public class StatusFragment extends Fragment {

    private static String TAG = StatusFragment.class.getSimpleName();

    private static final int MESSAGE_FROM_REFRESH = 0;
    private static final int MESSAGE_FROM_INITIAL = 1;
    private static final int MESSAGE_FROM_GET_MORE = 2;
    private static final int MESSAGE_FROM_ERROR = 3;
    public static final int RECYCLERVIEW_CACHE_SIZE = 20;

    private List<Status> statusList = new ArrayList<>();
    private List<Status> statusListCache = new ArrayList<>();
    private StatusAdapter statusAdapter;
    private StatusTimeLine statusTimeLine = new StatusTimeLine();
    private Oauth2AccessToken accessToken;

    private RecyclerView mStatus_rv;
    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout progress_layout;

    private boolean isBottom = false;
    private int page = 1;

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case MESSAGE_FROM_INITIAL:
                    statusAdapter = new StatusAdapter(statusList,getContext());
                    LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
                    linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
                    mStatus_rv.setLayoutManager(linearLayoutManager);
                    mStatus_rv.setItemViewCacheSize(RECYCLERVIEW_CACHE_SIZE);
                    mStatus_rv.setAdapter(statusAdapter);
                    break;
                case MESSAGE_FROM_REFRESH:
                    swipeRefreshLayout.setRefreshing(false);
                    if (statusListCache.size() == 0){
                        Toast.makeText(getContext(),"已经最新了！！",Toast.LENGTH_SHORT).show();
                    }else {
                        statusList.clear();
                        statusList.addAll(statusListCache);
                        statusAdapter.notifyDataSetChanged();
                        Toast.makeText(getContext(),"更新了"+statusListCache.size()+"条微博",Toast.LENGTH_SHORT).show();
                    }
                    break;
                case MESSAGE_FROM_GET_MORE:
                    statusList.addAll(statusListCache);
                    progress_layout.setVisibility(View.GONE);
                    statusAdapter.notifyDataSetChanged();
                    Toast.makeText(getContext(),"增加了"+statusListCache.size()+"条微博",Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_FROM_ERROR:
                    swipeRefreshLayout.setRefreshing(false);
                    progress_layout.setVisibility(View.GONE);
                    Toast.makeText(getContext(),"失败了",Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    public StatusFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_status, container, false);
        mStatus_rv = view.findViewById(R.id.fg_status_recyclerview);
        swipeRefreshLayout = view.findViewById(R.id.fg_status_refreshlayout);
        progress_layout = view.findViewById(R.id.fg_status_progressbar);
        accessToken = AccessTokenKeeper.readAccessToken(getContext());
        getInitialStatus();
        mStatus_rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE && statusAdapter.isNearBottom()){
                    isBottom = true;
                    //刷新
                    getMoreStatus();
                    statusAdapter.notifyDataSetChanged();
                }else {
                    isBottom = false;
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
        });
        initRefreshLayout();
        return view;
    }

    /**
     * 加载更多微博
     */
    private void getMoreStatus() {
        progress_layout.setVisibility(View.VISIBLE);
        if (accessToken != null){
            ParamsOfStatusTL params = new ParamsOfStatusTL.Builder()
                    .access_token(accessToken.getToken())
                    .page(++page)
                    .build();
            WBApiConnector.getStatusesHomeTimeline(params, new HttpCallBack() {
                @Override
                public void onSuccess(String response) {
                    StatusTimeLine timeLine = StatusTimeLine.getStatusesLine(getContext(),response);
                    statusListCache.clear();
                    statusListCache.addAll(timeLine.getStatuses());
                    Message message = new Message();
                    message.what = MESSAGE_FROM_GET_MORE;
                    handler.sendMessage(message);
                }

                @Override
                public void onError(Exception e) {
                    Message message = new Message();
                    message.what = MESSAGE_FROM_ERROR;
                    handler.sendMessage(message);
                }
            });
        }
    }

    /**
     * 初始化微博列表，第一次请求
     */
    private void getInitialStatus() {
        if (accessToken != null){
            ParamsOfStatusTL paramsOfStatusTL = new ParamsOfStatusTL.Builder()
                    .access_token(accessToken.getToken())
                    .build();
            WBApiConnector.getStatusesHomeTimeline(paramsOfStatusTL, new HttpCallBack() {
                @Override
                public void onSuccess(String response) {
                    statusTimeLine = StatusTimeLine.getStatusesLine(getContext(),response);
                    statusList = statusTimeLine.getStatuses();
                    Message message = new Message();
                    message.what = MESSAGE_FROM_INITIAL;
                    handler.sendMessage(message);
                }

                @Override
                public void onError(Exception e) {
                    Message message = new Message();
                    message.what = MESSAGE_FROM_ERROR;
                    handler.sendMessage(message);
                }
            });
        }
    }


    /**
     * 刷新微博
     */
    private void initRefreshLayout() {
        swipeRefreshLayout.setProgressViewOffset(true,0,120);
        swipeRefreshLayout.setSize(SwipeRefreshLayout.DEFAULT);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorMain);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                //刷新过程
                final ParamsOfStatusTL paramsOfStatusTL = new ParamsOfStatusTL.Builder()
                        .access_token(accessToken.getToken())
                        .since_id(statusTimeLine.getSince_id())
                        .build();
                WBApiConnector.getStatusesHomeTimeline(paramsOfStatusTL, new HttpCallBack() {
                    @Override
                    public void onSuccess(String response) {
                        statusTimeLine = StatusTimeLine.getStatusesLine(getContext(),response);
                        statusListCache.clear();
                        statusListCache.addAll(statusTimeLine.getStatuses());
                        page = 1;
                        Message message = new Message();
                        message.what = MESSAGE_FROM_REFRESH;
                        handler.sendMessage(message);
                    }

                    @Override
                    public void onError(Exception e) {
                        Message message = new Message();
                        message.what = MESSAGE_FROM_ERROR;
                        handler.sendMessage(message);
                    }
                });
            }
        });
    }

}
