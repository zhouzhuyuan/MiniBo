package com.zzy.minibo.Activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.sina.weibo.sdk.api.ImageObject;
import com.sina.weibo.sdk.api.MultiImageObject;
import com.sina.weibo.sdk.auth.AccessTokenKeeper;
import com.sina.weibo.sdk.auth.Oauth2AccessToken;
import com.zzy.minibo.Adapter.EmotionsAdapter;
import com.zzy.minibo.Members.ImageBean;
import com.zzy.minibo.Members.LP_EMOTIONS;
import com.zzy.minibo.Members.LP_STATUS;
import com.zzy.minibo.Members.LP_USER;
import com.zzy.minibo.Members.Status;
import com.zzy.minibo.Members.User;
import com.zzy.minibo.MyViews.NineGlideView;
import com.zzy.minibo.R;
import com.zzy.minibo.Utils.AllParams.ParamsOfCreateStatus;
import com.zzy.minibo.Utils.KeyBoardManager;
import com.zzy.minibo.Utils.TextFilter;
import com.zzy.minibo.Utils.WBApiConnector;
import com.zzy.minibo.WBListener.HttpCallBack;
import com.zzy.minibo.WBListener.SimpleIntCallback;

import org.litepal.LitePal;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

public class StatusEditActivity extends BaseActivity {

    private static final String TAG = StatusEditActivity.class.getSimpleName();
    public static final int REPOST_FROM_MAIN = 0;
    public static final int REPOST_FROM_STATUS = 1;
    public static final int SELECT_PICTURE = 1001;

    //
    private Toolbar toolbar;
    private EditText mEdittextView;
    private NineGlideView mNineGlideView;
    private ConstraintLayout mRepostLayout;
    private ImageView repostImage;
    private TextView repostName;
    private TextView repostText;
    private ImageButton addPics;
    private ImageButton addEmotions;
    private RecyclerView emotionsRecycler;
    private ImageButton atFriends;
    private ImageButton statusSend;

    private Activity mActivity;

    private Oauth2AccessToken accessToken;
    private boolean isRepost = false;
    private Status repostStatus = null;

    private List<ImageBean> selectedList = new ArrayList<>();
    private List<String> selectedPath = new ArrayList<>();

    private List<String> emotionsPath = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status_edit);
        accessToken = AccessTokenKeeper.readAccessToken(this);
        mActivity = this;
        initView();
        initData();
    }

    private void initData() {
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        if (bundle != null){
            repostStatus = bundle.getParcelable("Status");
            if (repostStatus != null){
                mRepostLayout.setVisibility(View.VISIBLE);
                Log.d(TAG, "initData: "+repostStatus.getIdstr());
                isRepost = true;
                if (repostStatus.getRetweeted_status() != null){
                    String str1 = "//" + "@" + repostStatus.getUser().getScreen_name() + ":" +
                            repostStatus.getText();
                    mEdittextView.setText(TextFilter.statusTextFliter(this, str1,null));
                    Status status = repostStatus.getRetweeted_status();
                    if (status.getPic_urls() != null && status.getPic_urls().size() > 0){
                        Glide.with(this)
                                .load(status.getThumbnail_pic()+status.getPic_urls().get(0))
                                .into(repostImage);
                    }else {
                        Glide.with(this)
                                .load(status.getUser().getAvatar_hd())
                                .into(repostImage);
                    }
                    repostName.setText("@"+status.getUser().getScreen_name());
                    repostText.setText(status.getText());
                }else {
                    if (repostStatus.getPic_urls() != null && repostStatus.getPic_urls().size() > 0){
                        Glide.with(this)
                                .load(repostStatus.getThumbnail_pic()+repostStatus.getPic_urls().get(0))
                                .into(repostImage);
                    }else {
                        Glide.with(this)
                                .load(repostStatus.getUser().getAvatar_hd())
                                .into(repostImage);
                    }
                    repostName.setText("@"+repostStatus.getUser().getScreen_name());
                    repostText.setText(repostStatus.getText());
                }
            }
        }

    }

    private void initView() {
        toolbar = findViewById(R.id.edit_toolbar);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }
        mEdittextView = findViewById(R.id.edit_edit_text);
        mEdittextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                emotionsRecycler.setVisibility(View.GONE);
            }
        });
        KeyBoardManager.showKeyBoard(this,mEdittextView);
        mNineGlideView = findViewById(R.id.edit_nine_pic_layout);
        mNineGlideView.setSimpleIntCallback(new SimpleIntCallback() {
            @Override
            public void callback(int i) {
                selectedPath.remove(i);
                selectedList.remove(i);
                mNineGlideView.setUrlList(selectedPath);
            }
        });
        mRepostLayout = findViewById(R.id.edit_repost_layout);
        repostImage = findViewById(R.id.edit_repost_image);
        repostName = findViewById(R.id.edit_repost_user_name);
        repostText = findViewById(R.id.edit_repost_text);
        addPics = findViewById(R.id.edit_add_pics);
        addPics.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                emotionsRecycler.setVisibility(View.GONE);
                if (isRepost){
                    Toast.makeText(getBaseContext(),"该文本不支持添加图片",Toast.LENGTH_SHORT).show();
                }else {
                   Intent intent = new Intent(StatusEditActivity.this,GalleryActivity.class);
                   Bundle bundle = new Bundle();
                   bundle.putParcelableArrayList("imageList", (ArrayList<? extends Parcelable>) selectedList);
                   intent.putExtras(bundle);
                   startActivityForResult(intent,SELECT_PICTURE);
                }
            }
        });
        addEmotions = findViewById(R.id.edit_add_emotions);
        addEmotions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                KeyBoardManager.hideKeyBoard(getBaseContext(),mEdittextView);
                emotionsRecycler.setVisibility(View.VISIBLE);
            }
        });
        emotionsRecycler = findViewById(R.id.edit_emotion_recycler);
        final List<LP_EMOTIONS> lp_emotions = LitePal.findAll(LP_EMOTIONS.class);
        for (LP_EMOTIONS l : lp_emotions){
            emotionsPath.add(l.getValue());
        }
        EmotionsAdapter emotionsAdapter = new EmotionsAdapter(emotionsPath,this);
        emotionsAdapter.setSimpleIntCallback(new SimpleIntCallback() {
            @Override
            public void callback(int i) {
                int curPostion = mEdittextView.getSelectionStart();
                StringBuilder sb = new StringBuilder(mEdittextView.getText().toString());
                sb.insert(curPostion,emotionsPath.get(i));
                mEdittextView.setText(TextFilter.statusTextFliter(getBaseContext(),sb.toString(),null));
                mEdittextView.setSelection(curPostion+emotionsPath.get(i).length());
            }
        });
        StaggeredGridLayoutManager staggeredGridLayoutManager = new StaggeredGridLayoutManager(8,StaggeredGridLayoutManager.VERTICAL);
        emotionsRecycler.setLayoutManager(staggeredGridLayoutManager);
        emotionsRecycler.setAdapter(emotionsAdapter);
//        atFriends = findViewById(R.id.edit_add_at);
        statusSend = findViewById(R.id.edit_send_btn);
        statusSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Oauth2AccessToken accessToken = AccessTokenKeeper.readAccessToken(getBaseContext());
                ParamsOfCreateStatus paramsOfCreateStatus = new ParamsOfCreateStatus();
                paramsOfCreateStatus.setAccess_token(accessToken.getToken());
                paramsOfCreateStatus.setStatus(mEdittextView.getText().toString()+"https://www.baidu.com");
                if (selectedPath.size() != 0){
                    paramsOfCreateStatus.setPics(getMultiImageObject());
                }
                WBApiConnector.createStatus(paramsOfCreateStatus, new HttpCallBack() {
                    @Override
                    public void onSuccess(String response) {
                        Log.d(TAG, "onSuccess: "+response);
                    }

                    @Override
                    public void onError(Exception e) {

                    }
                });
//                Status status = StatusPacker();
                Intent intent = new Intent();
//                Bundle bundle = new Bundle();
//                bundle.putParcelable("Status",status);
//                intent.putExtras(bundle);
                setResult(RESULT_OK,intent);
                KeyBoardManager.hideKeyBoard(getBaseContext(),mEdittextView);
                finish();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (data != null){
            switch (requestCode){
                case SELECT_PICTURE:
                    if (resultCode == RESULT_OK){
                        Bundle bundle = data.getExtras();
                        if (bundle != null){
                            selectedList.clear();
                            selectedList = bundle.getParcelableArrayList("imageList");
                            for (ImageBean l : selectedList){
                                selectedPath.add(l.getPath());
                            }
                        }
                    }
                    mNineGlideView.setUrlList(selectedPath);
                    break;
            }
        }else {
            super.onActivityResult(requestCode,resultCode,data);
        }


    }

    private Status StatusPacker(){
        StringBuilder stringBuilder = new StringBuilder();
        String time = TextFilter.createTimeString();
        @SuppressLint("SimpleDateFormat") SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        Date date = new Date(System.currentTimeMillis());
        stringBuilder.append("{\"created_at\":").append("\"").append(time).append("\",");
        stringBuilder.append("\"id\":").append(simpleDateFormat.format(date)).append(",");
        stringBuilder.append("\"idstr\":").append("\"").append(simpleDateFormat.format(date)).append("\",");
        stringBuilder.append("\"user\":");
        Status status = new Status();
        User mUser = null;
        List<LP_USER> lp_users = LitePal.where("uidstr = ?",accessToken.getUid()).find(LP_USER.class);
        for (LP_USER l : lp_users){
            mUser = User.makeJsonToUser(l.getJson());
            stringBuilder.append(l.getJson()).append(",");
        }
        status.setLocal(true);
        stringBuilder.append("\"can_edit\":").append("true,");
        status.setUser(mUser);
        status.setPic_urls(selectedPath);
        stringBuilder.append("\"pic_urls\":[");
        for (int i = 0 ;i < selectedPath.size();i++){
            if (i == selectedPath.size()-1){
                stringBuilder.append("{\"thumbnail_pic\":\"").append(selectedPath.get(i)).append("\"}");
            }else {
                stringBuilder.append("{\"thumbnail_pic\":\"").append(selectedPath.get(i)).append("\"},");
            }
        }
        stringBuilder.append("],");
        status.setCreated_at(time);
        status.setText(mEdittextView.getText().toString());
        if (selectedPath.size() != 0 && status.getText().length() == 0){
            status.setText("分享图片");
        }
        stringBuilder.append("\"text\":\"").append(status.getText()).append("\",");
        if (isRepost){
            status.setRetweeted_status(repostStatus.getRetweeted_status());
            List<LP_STATUS> lp_statuses = LitePal.where("idstr = ?",repostStatus.getIdstr()).find(LP_STATUS.class);
            for (LP_STATUS l : lp_statuses){
                stringBuilder.append("\"retweeted_status\":").append(l.getJson()).append(",");
            }
        }
        status.setReposts_count("0");
        status.setComments_count("0");
        status.setAttitudes_count("0");
        stringBuilder.append("\"reposts_count\":0," +
                "\"comments_count\":0," +
                "\"attitudes_count\":0 }");
        LP_STATUS lp_status = new LP_STATUS();
        lp_status.setIdstr(simpleDateFormat.format(date));
        lp_status.setJson(stringBuilder.toString());
        Log.d(TAG, "StatusPacker: "+stringBuilder.toString());
        lp_status.save();
        return status;
    }

    private MultiImageObject getMultiImageObject(){
        MultiImageObject multiImageObject = new MultiImageObject();
        //pathList设置的是本地本件的路径,并且是当前应用可以访问的路径，现在不支持网络路径（多图分享依靠微博最新版本的支持，所以当分享到低版本的微博应用时，多图分享失效
        // 可以通过WbSdk.hasSupportMultiImage 方法判断是否支持多图分享,h5分享微博暂时不支持多图）多图分享接入程序必须有文件读写权限，否则会造成分享失败
        ArrayList<Uri> pathList = new ArrayList<Uri>();
        for (String path : selectedPath){
            pathList.add(Uri.fromFile(new File(path)));
        }
        multiImageObject.setImageList(pathList);
        return multiImageObject;
    }

}
