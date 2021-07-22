package com.moyu.browser_moyu.newspage.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.moyu.browser_moyu.MainActivity;
import com.moyu.browser_moyu.R;
import com.moyu.browser_moyu.newspage.entity.Link;
import com.moyu.browser_moyu.newspage.util.APIutil;
import com.moyu.browser_moyu.newspage.util.NewsListViewAdapter;
import com.moyu.browser_moyu.newspage.viewmodel.NewsPageViewModel;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;


public class NewsPageActivity extends AppCompatActivity {

    private NewsPageViewModel mNewsPageViewModel;
    private ListView news_list;                         // 列表view
    private EditText news_keyWord;                      // 关键词编辑框view
    private Button news_search;                         // 搜索按钮view
    private NewsListViewAdapter newsListViewAdapter;
    private List<Link> latestUrlAndTitle;               // 最新返回的url和title
    View footview;                                      // list底部view
    Button news_nextpage;                               // 下一页按钮

    private void init() {
        news_list = findViewById(R.id.news_list);
        news_keyWord = findViewById(R.id.news_keyWord);
        news_search = findViewById(R.id.news_search);
        mNewsPageViewModel = new NewsPageViewModel();

        footview = LayoutInflater.from(NewsPageActivity.this).inflate(R.layout.news_list_footer, null);
        news_nextpage = footview.findViewById(R.id.news_nextpage);
    }

    private static final String TAG = "NewsPageActivity";

    private void setListenerForEnterKeyWord() {
        // 设置焦点监听器
        news_keyWord.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                EditText editText = (EditText) v;

                if (hasFocus) {
                    // 获得焦点时，光标定位在关键词末尾
                    editText.setSelection(editText.getText().length());
                } else {

                    // 失去焦点时，同步viewmodel中的 keyWord
                    mNewsPageViewModel.getKeyWord().setValue(editText.getText().toString());
                }
            }
        });

        // 设置按键监听器，输入回车完成输入
        news_keyWord.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {

                if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {

                    // 执行搜索
                    news_search.callOnClick();
                    // 清除输入框焦点
                    news_keyWord.clearFocus();

                }
                return false;

            }
        });

        InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        news_search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (news_keyWord.hasFocus()) {
                    // 隐藏软键盘
                    if (manager.isActive()) {
                        manager.hideSoftInputFromWindow(news_keyWord.getApplicationWindowToken(), 0);
                    }
                    news_keyWord.clearFocus();
                } else {
                    mNewsPageViewModel.getKeyWord().setValue(news_keyWord.getText().toString());
                }
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news_page);

        //add followed if test this activity
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        // 绑定view对象
        init();

        // view绑定监听器
        setListenerForEnterKeyWord();

        news_nextpage.setVisibility(View.INVISIBLE);
        news_list.addFooterView(footview);


        news_nextpage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mNewsPageViewModel.getKeyWord().setValue(news_keyWord.getText().toString());
            }
        });


        mNewsPageViewModel.getKeyWord().observe(this, new Observer<String>() {
            @Override
            // 关键词发生改变时
            public void onChanged(String newKeyWord) {
                news_nextpage.setVisibility(View.VISIBLE);
                news_nextpage.setText("加载中……");

                cleanList();

                startRefreshEventFlow(newKeyWord);

            }
        });


        mNewsPageViewModel.getUrlAndTitle().observe(this, new Observer<List<Link>>() {
            @Override
            public void onChanged(List<Link> links) {

                cleanList();

                newsListViewAdapter = new NewsListViewAdapter(NewsPageActivity.this, R.layout.news_list_item, links);
                news_list.setAdapter(newsListViewAdapter);

                if (latestUrlAndTitle.size() == 0) {
                    news_nextpage.setText("无相关资讯");
                } else {
                    news_nextpage.setText("下一页");
                }

            }
        });

        // ListView 每个item点击事件
        news_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Link currentLink = mNewsPageViewModel.getUrlAndTitle().getValue().get(position);
                String url = currentLink.getUrl();

                Intent intent = new Intent(NewsPageActivity.this, MainActivity.class);
                intent.putExtra("url", url);
                intent.putExtra("useOther", 4);
                startActivity(intent);
            }
        });

        // 预留关键词
        news_keyWord.setText("热搜");
        mNewsPageViewModel.getKeyWord().setValue(news_keyWord.getText().toString());

    }

    /**
     * 清除显示的链接
     */
    private void cleanList() {
        if (newsListViewAdapter!=null) {
            newsListViewAdapter.clearList();
            newsListViewAdapter.notifyDataSetChanged();
        }
    }

    /**
     * 使用RxJava异步调用API
     * @param keyWord
     */
    private void startRefreshEventFlow(String keyWord) {
        Observable.just(keyWord)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new io.reactivex.Observer<String>() {
            @Override
            public void onSubscribe(@NotNull Disposable d) {

            }

            @Override
            public void onNext(@NotNull String s) {
                latestUrlAndTitle = APIutil.getNewsPage(s);
            }

            @Override
            public void onError(@NotNull Throwable e) {

            }

            @Override
            public void onComplete() {
                Toast.makeText(NewsPageActivity.this, "完成加载,当前 "+APIutil.getCurrentPageToken()+" 页", Toast.LENGTH_SHORT).show();
                mNewsPageViewModel.getUrlAndTitle().setValue(latestUrlAndTitle);
            }
        });
    }
}