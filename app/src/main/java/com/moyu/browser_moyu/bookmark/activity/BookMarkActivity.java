package com.moyu.browser_moyu.bookmark.activity;


import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.appcompat.widget.Toolbar;


import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupMenu;

import com.moyu.browser_moyu.MainActivity;
import com.moyu.browser_moyu.R;
import com.moyu.browser_moyu.bookmark.adapter.BookMarkAdapter;
import com.moyu.browser_moyu.db.MyDataBase;
import com.moyu.browser_moyu.db.entity.BookmarkRecord;
import com.moyu.browser_moyu.db.viewmodel.BookmarkRecordViewModel;

import java.util.ArrayList;
import java.util.List;


import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class BookMarkActivity extends AppCompatActivity {


    MyDataBase myDataBase;
    BookmarkRecordViewModel mViewModel;
    ListView listView;
    List<BookmarkRecord> bookmarkRecordList = new ArrayList<>();
    private CompositeDisposable mDisposable;
    private BookMarkAdapter bookMarkAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_mark);
        Toolbar toolbar = findViewById(R.id.bookmark_toolbar);
        toolbar.setTitle("书签");
        setSupportActionBar(toolbar);


        myDataBase = MyDataBase.getInstance(getApplicationContext());
        mViewModel = new ViewModelProvider(this).get(BookmarkRecordViewModel.class);
        listView = findViewById(R.id.bookmarkListView);
        mDisposable = new CompositeDisposable();

        bookMarkAdapter = new BookMarkAdapter(BookMarkActivity.this, bookmarkRecordList);
        listView.setAdapter(bookMarkAdapter);


        //获得数据库数据更新列表
        mViewModel.getLiveDataBookmarkRecord().observe(this, new Observer<List<BookmarkRecord>>() {
            @Override
            public void onChanged(List<BookmarkRecord> bookmarkRecords) {
                bookmarkRecordList.clear();
                bookmarkRecordList.addAll(bookmarkRecords);
                bookMarkAdapter.notifyDataSetChanged();
            }
        });



        //短按item逻辑
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BookmarkRecord bookmarkRecord = bookmarkRecordList.get(position);
                String url = bookmarkRecord.getUrl();

                Intent intent = new Intent(BookMarkActivity.this, MainActivity.class);
                intent.putExtra("url",url);
                intent.putExtra("useOther",4);
                startActivity(intent);
            }
        });

        //长按item逻辑
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                PopupMenu popupMenu = new PopupMenu(BookMarkActivity.this,view);
                getMenuInflater().inflate(R.menu.bookmark_popup_menu,popupMenu.getMenu());
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()){
                            case R.id.delect:
                                delectBookmark(position);
                                break;
                            case R.id.rename:
                                renameBookmark(position);
                                break;
                            default:

                        }
                        return true;
                    }
                });
                popupMenu.show();
                return true;
            }
        });

    }



    //顶部菜单
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.bookmark_toolbar,menu);
        return true;
    }

    //顶部菜单栏选中操作
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case R.id.delect_all_bookmark:
                AlertDialog.Builder dialog = new AlertDialog.Builder(BookMarkActivity.this);
                dialog.setMessage("是否删除所有书签");
                dialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDisposable.add(mViewModel.deleteBookmarkRecords()
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

    public void delectBookmark(int position){
        BookmarkRecord bookmarkRecord = bookmarkRecordList.get(position);
        mDisposable.add(mViewModel.deleteBookmarkRecord(bookmarkRecord)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe());
    }
    public void renameBookmark(int position){
        BookmarkRecord bookmarkRecord = bookmarkRecordList.get(position);
        final EditText input = new EditText(BookMarkActivity.this);
        AlertDialog.Builder builder = new AlertDialog.Builder(BookMarkActivity.this);
        builder.setTitle("新名称").setView(input).setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        //更新操作
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String text;
                text = input.getText().toString();
                bookmarkRecord.setTitle(text);
                mDisposable.add(mViewModel.updateBookmarkRecord(bookmarkRecord)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe());
            }
        });
        builder.show();

    }

    @Override
    protected void onStop() {
        super.onStop();
        mDisposable.clear();
    }
}
