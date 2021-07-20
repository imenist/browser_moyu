package com.moyu.browser_moyu.newspage.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;

import android.content.Context;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.moyu.browser_moyu.R;
import com.moyu.browser_moyu.newspage.entity.Link;
import com.moyu.browser_moyu.newspage.util.APIutil;
import com.moyu.browser_moyu.newspage.util.NewsListViewAdapter;
import com.moyu.browser_moyu.newspage.viewmodel.NewsPageViewModel;


public class NewsPageActivity extends AppCompatActivity {

    private NewsPageViewModel mNewsPageViewModel;
    private ListView news_list;
    private EditText news_keyWord;
    private Button news_search;

    private void initBindView(){
        news_list = findViewById(R.id.news_list);
        news_keyWord = findViewById(R.id.news_keyWord);
        news_search = findViewById(R.id.news_search);
    }

    private static final String TAG = "NewsPageActivity";
    private void setListener() {
        // 设置焦点监听器
        news_keyWord.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                EditText editText = (EditText) v;

                if (hasFocus) {
                    // 获得焦点时，光标定位在关键词末尾
                    editText.setSelection(editText.getText().length());
                }else {

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
        initBindView();

        // view绑定监听器
        setListener();

        mNewsPageViewModel = new NewsPageViewModel();

        APIutil.initTitleUrlList();
        NewsListViewAdapter newsListViewAdapter = new NewsListViewAdapter(NewsPageActivity.this, R.layout.news_list_item, APIutil.getTitleUrlList());

        mNewsPageViewModel.getKeyWord().observe(this, new Observer<String>() {
            @Override
            // 关键词发生改变时
            public void onChanged(String newKeyWord) {
                APIutil.updateTitleUrlList(newKeyWord, 1);

                // 重新渲染ListView显示内容
                news_list.setAdapter(newsListViewAdapter);
            }
        });

        // ListView 每个item点击事件
        // TODO 点击访问URL链接
        news_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Link currentLink = APIutil.getTitleUrlList().get(position);
                String url = currentLink.getUrl();
                String title = currentLink.getTitle();

                // TODO
                StringBuilder builder = new StringBuilder();
                builder.append("title : ").append(title).append("\n");
                builder.append("url : ").append(url);
                Toast.makeText(NewsPageActivity.this, builder.toString(), Toast.LENGTH_SHORT).show();
            }
        });

    }
}