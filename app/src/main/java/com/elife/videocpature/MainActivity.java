package com.elife.videocpature;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.eversince.screenrecord.R;
import com.qq.e.ads.AdListener;
import com.qq.e.ads.AdRequest;
import com.qq.e.ads.AdSize;
import com.qq.e.ads.AdView;

import java.io.File;
import java.util.ArrayList;


public class MainActivity extends Activity {
    public final static String RESULT = "result";
    public final static String DATA = "data";
    public final static String ACTION = "com.dchen.videocapture.complete";

    private MediaProjectionManager mProjectionManager;
    private static final int REQUEST_CODE = 0x10001;
    private boolean mIsRecording = false;
    private DisplayMetrics mMetrics  = new DisplayMetrics();
    private RecordThread mRecordThread;

    private ListView mList;
    private VideoListAdapter mAdapter;
    private RelativeLayout mBannerContainer;
    IntentFilter mFilter;
    final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equalsIgnoreCase(ACTION))
            MainActivity.this.initVideoList();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mProjectionManager = (MediaProjectionManager)(getSystemService(Context.MEDIA_PROJECTION_SERVICE));
        getWindowManager().getDefaultDisplay().getMetrics(mMetrics);

        mList = (ListView) findViewById(R.id.video_list);
        mList.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        mList.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                int selectedCount = mList.getCheckedItemCount();
                mode.setTitle(selectedCount+"");
                if (checked) {
                    mAdapter.addSelectedPos(position);
                } else  {
                    mAdapter.removeSelectedPos(position);
                }
                mAdapter.notifyDataSetChanged();
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.menu_context, menu);
                getWindow().setStatusBarColor(getResources().getColor(R.color.context_color_dark));
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.share:
                        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                        ArrayList<String> selectedFile = mAdapter.getSelectedFileName();
                        if (null == selectedFile)
                            break;
                        Uri screenshotUri = Uri.fromFile(new File(selectedFile.get(0)));
                        sharingIntent.setType("video/*");
                        sharingIntent.putExtra(Intent.EXTRA_STREAM, screenshotUri);
                        MainActivity.this.startActivity(Intent.createChooser(sharingIntent, "分享到："));
                        mode.finish();

                        break;
                    case R.id.delete:

                        new AlertDialog.Builder(MainActivity.this)
                                .setMessage("确认删除所选视频？")
                                .setPositiveButton("删除", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        ArrayList<String> fileNames = mAdapter.getSelectedFileName();
                                        for (String fileName : fileNames) {
                                            if (!TextUtils.isEmpty(fileName)) {
                                                File file = new File(fileName);
                                                file.delete();
                                            }
                                        }
                                        mode.finish();
                                    }
                                }).setNegativeButton("取消", null).show();
                        break;
                    default:
                        break;
                }
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                mAdapter.deSelectAll();
                initVideoList();
                getWindow().setStatusBarColor(getResources().getColor(R.color.color_primary_dark));
            }
        });
        initVideoList();
        mBannerContainer = (RelativeLayout) findViewById(R.id.banner_container);
        initAdvertise();
        mFilter = new IntentFilter(ACTION);
        registerReceiver(mReceiver, mFilter);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startSettingActivity();
            return true;
        } else if(id == R.id.action_donate) {
            //进入推荐按钮
            Utils.getInstance(this).shareApp();

        } else if (id == R.id.action_about) {
            //进入项目介绍页面
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);

        } else if (id == R.id.action_record) {
            btnClicked();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            Intent intent = new Intent(this, RecordService.class);
            intent.putExtra(RESULT, resultCode);
            intent.putExtra(DATA, data);
            startService(intent);
            moveTaskToBack(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        initVideoList();
    }

    private void initAdvertise() {
        AdView adv = new AdView(this, AdSize.BANNER, "1103948760","4040605069270554");
        mBannerContainer.addView(adv);
		/* 广告请求数据，可以设置广告轮播时间，默认为30s  */
        AdRequest adr = new AdRequest();
		/* 这个接口的作用是设置广告的测试模式，该模式下点击不扣费
		 * 未发布前请设置testad为true，
		 * 上线的版本请确保设置为false或者去掉这行调用
		 */
//        adr.setTestAd(true);
		/* 设置广告刷新时间，为30~120之间的数字，单位为s*/
        adr.setRefresh(31);
		/* 设置空广告和首次收到广告数据回调
		 * 调用fetchAd方法后会发起广告请求，广告轮播时不会产生回调
		 */
        adv.setAdListener(new AdListener() {
            @Override
            public void onBannerClosed() {

            }

            @Override
            public void onAdClicked() {

            }

            @Override
            public void onNoAd() {
                Log.i("no ad cb:","no");
            }
            @Override
            public void onAdReceiv() {
                Log.i("ad recv cb:","revc");
            }

            @Override
            public void onAdExposure() {

            }
        });
		/* 发起广告请求，收到广告数据后会展示数据	 */
        adv.fetchAd(adr);
    }
    private void initVideoList() {
        String dir = Environment.getExternalStorageDirectory().getPath() + "/" +
                getResources().getString(R.string.save_dir);
        File saveDir = new File(dir);
        if (!saveDir.exists()) {
            return;
        }
        File[] allFiles  = saveDir.listFiles();
        ArrayList<String> fileNames = new ArrayList<>();
        for (File file : allFiles) {
            if (file.getName().endsWith(".mp4")) {
                fileNames.add(file.getName());
            }
        }
        if (mAdapter == null) {
            mAdapter = new VideoListAdapter(this, fileNames, dir + "/");
            mList.setAdapter(mAdapter);
        } else {
            mAdapter.setData(fileNames);
            mAdapter.notifyDataSetChanged();
        }


    }

    private void btnClicked() {
        //stop record
        if (mIsRecording) {
            stopRecording();
        } else {
            //start recording
            startRecordingIntent();
        }
    }

    /**
     * 停止录像
     */
    public void stopRecording() {
        if (null != mRecordThread) {
            mRecordThread.quit();
            mRecordThread = null;
            mIsRecording = false;
        }
        initVideoList();
    }

    public void startRecordingIntent() {
        if (null != mProjectionManager) {
            Intent projectInt = mProjectionManager.createScreenCaptureIntent();
            startActivityForResult(projectInt, REQUEST_CODE);
        }
    }

    /**
     * 启动设置界面
     */
    private void startSettingActivity() {
        Intent it = new Intent(this, PreferenceActivity.class);
        startActivity(it);
    }

    @Override
    protected void onDestroy() {
        Log.i("duanjin", "activity get destroyed");
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

}