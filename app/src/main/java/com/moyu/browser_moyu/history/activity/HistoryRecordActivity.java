package com.moyu.browser_moyu.history.activity;


import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.moyu.browser_moyu.MainActivity;
import com.moyu.browser_moyu.R;
import com.moyu.browser_moyu.db.MyDataBase;
import com.moyu.browser_moyu.db.entity.HistoryRecord;
import com.moyu.browser_moyu.db.viewmodel.BookmarkRecordViewModel;
import com.moyu.browser_moyu.db.viewmodel.HistoryViewModel;
import com.moyu.browser_moyu.history.adapter.ListViewAdapter;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;


public class HistoryRecordActivity extends AppCompatActivity {


    private MyDataBase myDataBase ;
    private HistoryViewModel mViewModel;
    private BookmarkRecordViewModel bookmarkRecordViewModel;

    private ListView listView;
    private List<HistoryRecord> historyList= new ArrayList<>();

    private CompositeDisposable mDisposable ;
    private ListViewAdapter listViewAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_history_record);

        myDataBase = MyDataBase.getInstance(getApplicationContext());
        mViewModel = new ViewModelProvider(this).get(HistoryViewModel.class);
        bookmarkRecordViewModel = new ViewModelProvider(this).get(BookmarkRecordViewModel.class);
        mDisposable = new CompositeDisposable();

        listView = findViewById(R.id.historyListView);

        listViewAdapter = new ListViewAdapter(HistoryRecordActivity.this,historyList);
        listView.setAdapter(listViewAdapter);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("历史记录");
        setSupportActionBar(toolbar);



        //观察数据库历史记录的变化
        mViewModel.getLiveDataHistoryRecord().observe(this, new Observer<List<HistoryRecord>>() {
            @Override
            public void onChanged(List<HistoryRecord> historyRecords) {
                historyList.clear();
                historyList.addAll(historyRecords);
                listViewAdapter.notifyDataSetChanged();
            }
        });

        //Item短按跳转至网页
        listView.setOnItemClickListener((parent, view, position, id) -> {
            HistoryRecord historyRecord = historyList.get(position);
            String url = historyRecord.url;

            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("url", url);
            intent.putExtra("useOther", 4);
            startActivity(intent);


        });

        //Item长按删除选项
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            myPopupMenu(view, position);
            return true;

        });

    }

    @Override
    protected void onStop() {
        super.onStop();
        mDisposable.clear();
    }


    //菜单项选中动作
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.delete_all_historyRecord:
                AlertDialog.Builder dialog = new AlertDialog.Builder(HistoryRecordActivity.this);
                dialog.setMessage("是否删除所有记录");
                dialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDisposable.add(mViewModel.deleteAllHistoryRecords()
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe()
                        );
                    }
                });
                dialog.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                dialog.show();
                break;
            default:

        }
        return true;
    }


    //创建标题栏菜单
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.history_record_menu, menu);
        return true;

    }


    //
    private void myPopupMenu(View v, int position) {
        //定义PopupMenu对象
        PopupMenu popupMenu = new PopupMenu(HistoryRecordActivity.this, v);
        popupMenu.setGravity(Gravity.CENTER);
        //设置PopupMenu对象的布局
        popupMenu.getMenuInflater().inflate(R.menu.history_record_more_action, popupMenu.getMenu());
        //设置PopupMenu的点击事件
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()){
                    case R.id.delete:
                        deleteHistoryRecord(position);
                        break;
                    case R.id.star:
                        starHistoryRecord(position);
                        break;
                    default:
                }
                return true;
            }

        });
        //显示菜单
        popupMenu.show();
    }

    private void deleteHistoryRecord( int position){
        AlertDialog.Builder dialog = new AlertDialog.Builder(HistoryRecordActivity.this);
        dialog.setMessage("是否删除记录");
        dialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                HistoryRecord historyRecord = historyList.get(position);
                mDisposable.add(mViewModel.deleteHistoryRecord(historyRecord)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe()
                );
            }
        });
        dialog.setNegativeButton("取消", (dialog1, which) -> { });
        dialog.show();
    }

    //收藏记录到书签
    private void starHistoryRecord(int position){
        HistoryRecord historyRecord = historyList.get(position);
        Boolean flag = mDisposable.add(bookmarkRecordViewModel.insertBookmarkRecord(historyRecord.title, historyRecord.url)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe()
        );

        if (flag){
            Toast.makeText(HistoryRecordActivity.this, "收藏成功", Toast.LENGTH_SHORT ).show();
        }else{
            Toast.makeText(HistoryRecordActivity.this, "收藏失败", Toast.LENGTH_SHORT ).show();
        }

    }


}