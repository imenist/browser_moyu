package com.moyu.browser_moyu.searchpage.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import com.bumptech.glide.Glide;
import com.moyu.browser_moyu.R;
import com.moyu.browser_moyu.databinding.FragmentSearchPageBinding;
import com.moyu.browser_moyu.db.viewmodel.HistoryViewModel;
import com.moyu.browser_moyu.navigationlist.activity.Data;
import com.moyu.browser_moyu.navigationlist.viewmodel.NavSearViewModel;
import com.moyu.browser_moyu.searchpage.util.JavascriptInterface;
import com.moyu.browser_moyu.searchpage.util.WebViewUtil;
import com.moyu.browser_moyu.searchpage.viewmodel.SearchPageViewModel;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;



public class SearchPageFragment extends Fragment implements View.OnClickListener {

    private boolean goBackNum = true, goForwardNum = true, goHomeNum = true;
    private FragmentSearchPageBinding mBinding_;
    //NavFragment与SearFragment通信ViewModel
    private NavSearViewModel viewModel;

    private SearchPageViewModel m_search_view_model_;
    //将private改成public
    public WebView webView;
    private ProgressBar progressBar;
    private EditText textUrl;
    private ImageView webIcon, goBack, goForward, navSet, goHome, btnStart;

    private long exitTime = 0;
    private View mView_;
    private Context mContext;
    private InputMethodManager manager;

    private static final String HTTP = "http://";
    private static final String HTTPS = "https://";
    private static final int PRESS_BACK_EXIT_GAP = 2000;

    private CompositeDisposable mDisposable ;
    private HistoryViewModel historyViewModel;



    public SearchPageFragment() {
        super(R.layout.fragment_search_page);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this.getContext();
        manager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);

        //历史记录数据库
        historyViewModel = new ViewModelProvider(this).get(HistoryViewModel.class);
        mDisposable = new CompositeDisposable();




    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mBinding_ = DataBindingUtil.inflate(inflater, R.layout.fragment_search_page, container, false);
        viewModel = ViewModelProviders.of(getActivity()).get(NavSearViewModel.class);
        mBinding_.setViewModel(viewModel);
        mBinding_.setLifecycleOwner(getActivity());

        viewModel.getData().observe(getActivity(), new Observer<Data>() {
            @Override
            public void onChanged(Data data) {
                if (data.getGoBack() == 1) {
//                    Toast.makeText(getActivity(), "goback", Toast.LENGTH_SHORT).show();
                    if (webView.canGoBack()) {
                        webView.goBack();
                    }else{
                        getActivity().finish();
                    }
                    data.setGoBack(0);
                }
                if (data.getGoForward() == 2) {
//                    Toast.makeText(getActivity(), "goforward", Toast.LENGTH_SHORT).show();
                    if (webView.canGoForward()) {
                        webView.goForward();
                    }
                    data.setGoForward(0);
                }
                if (data.getGoHome() == 3) {
//                    Toast.makeText(getActivity(), "goHome", Toast.LENGTH_SHORT).show();
                    webView.loadUrl("https://www.baidu.com");
                    data.setGoHome(0);
                }
                if(data.getUseOther() == 4){
                    String url = useOtherUrl();
                    webView.loadUrl(url);
                    data.setUseOther(0);
                }
            }
        });

        mView_ = mBinding_.getRoot();
//        mView_ = inflater.inflate(R.layout.fragment_search_page, container, false);

        /*
        // 1、对布局需要绑定的内容进行加载
        mBinding_ = DataBindingUtil.inflate(inflater, R.layout.fragment_search_page, container, false);
        // 2、获取到视图
        View view = mBinding_.getRoot();

        mBinding_.setSearchViewModel(m_search_view_model_);

        mView_ = view;
        */

        // 绑定控件
        initView();

        // 初始化 WebView
        initWeb();

        return mView_;

    }

    /**
     * 绑定控件
     */
    private void initView() {
        webView = mView_.findViewById(R.id.webView);
        progressBar = mView_.findViewById(R.id.progressBar);
        textUrl = mView_.findViewById(R.id.textUrl);
        webIcon = mView_.findViewById(R.id.webIcon);
        btnStart = mView_.findViewById(R.id.btnStart);
        //goHome = mView_.findViewById(R.id.goHome);

        // 绑定按钮点击事件
        btnStart.setOnClickListener(this);
        //navSet.setOnClickListener(this);
        //goHome.setOnClickListener(this);

        // 地址输入栏获取与失去焦点处理
        textUrl.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    // 显示当前网址链接 TODO:搜索页面显示搜索词
                    textUrl.setText(webView.getUrl());
                    // 光标置于末尾
                    textUrl.setSelection(textUrl.getText().length());
                    // 显示因特网图标
                    webIcon.setImageResource(R.drawable.internet);
                    // 显示跳转按钮
                    btnStart.setImageResource(R.drawable.go);
                } else {
                    // 显示网站名
                    textUrl.setText(webView.getTitle());
                    // 显示网站图标
                    webIcon.setImageBitmap(webView.getFavicon());
                    // 显示刷新按钮
                    btnStart.setImageResource(R.drawable.refresh);
                }
            }
        });



        // 监听键盘回车搜索
        textUrl.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
                if (keyCode == KeyEvent.KEYCODE_ENTER && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                    // 执行搜索
                    btnStart.callOnClick();
                    textUrl.clearFocus();
                }
                return false;
            }
        });
    }


    /**
     * 初始化 web
     */
    @SuppressLint("SetJavaScriptEnabled")
    private void initWeb() {

        WebSettings settings = webView.getSettings();
        // 启用 js 功能
        settings.setJavaScriptEnabled(true);
        // 设置浏览器 UserAgent
        settings.setUserAgentString(settings.getUserAgentString() + " mkBrowser/" + getVerName(mContext));

        // 将图片调整到适合 WebView 的大小
        settings.setUseWideViewPort(true);
        // 缩放至屏幕的大小
        settings.setLoadWithOverviewMode(true);

        // 支持缩放，默认为true。是下面那个的前提。
        settings.setSupportZoom(true);
        // 设置内置的缩放控件。若为false，则该 WebView 不可缩放
        settings.setBuiltInZoomControls(true);
        // 隐藏原生的缩放控件
        settings.setDisplayZoomControls(false);

        // 缓存
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        // 设置可以访问文件
        settings.setAllowFileAccess(true);
        // 支持通过JS打开新窗口
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        // 支持自动加载图片
        settings.setLoadsImagesAutomatically(true);
        // 设置默认编码格式
        settings.setDefaultTextEncodingName("utf-8");
        // 本地存储
        settings.setDomStorageEnabled(true);
        settings.setPluginState(WebSettings.PluginState.ON);

        // 资源混合模式
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        // 加载首页
        //webView.loadUrl(getResources().getString(
        // R.string.home_url));
        String url = "https://zhuanlan.zhihu.com/p/191061926";

        webView.loadUrl(url);
        webView.addJavascriptInterface(new JavascriptInterface(getContext()), "imagelistener");
        // 重写 WebViewClient
        webView.setWebViewClient(new MkWebViewClient());
        // 重写 WebChromeClient
        webView.setWebChromeClient(new MkWebChromeClient());
    }

    /**
     * 拦截器初始化
     *
     * @param context
     */
    private void initWebViewUtil(Context context) {
        WebViewUtil.getInstance(context);
        WebViewUtil.addUrl("sohu", ".sohu.com");
        WebViewUtil.addUrl("163", ".163.com");
        WebViewUtil.setLocalDestPage("file:///android_asset/destpage.html");
    }

    private static final String TAG = "SearchPageFragment";

    /**
     * 重写 WebViewClient
     */
    private class MkWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            // 设置在webView点击打开的新网页在当前界面显示,而不跳转到新的浏览器中

            if (url == null) {
                // 返回true自己处理，返回false不处理
                return true;
            }

            // 正常的内容，打开
            if (url.startsWith(HTTP) || url.startsWith(HTTPS)) {
                // 初始化拦截器
                initWebViewUtil(view.getContext());

                // 当前URL在拦截列表内
                if (WebViewUtil.isNeedIntercept(url.toString())) {
                    // 更改为本地html文件资源
                    url = WebViewUtil.getLocalDestPage();
                }
                view.loadUrl(url);
                return true;
            }

            // 调用第三方应用，防止crash (如果手机上没有安装处理某个scheme开头的url的APP, 会导致crash)
            try {
                // TODO:弹窗提示用户，允许后再调用
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
                return true;
            } catch (Exception e) {
                return true;
            }
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);

            // 网页开始加载，显示进度条
            progressBar.setProgress(0);
            progressBar.setVisibility(View.VISIBLE);

            // 更新状态文字
            textUrl.setText("加载中...");

            // 切换默认网页图标
            webIcon.setImageResource(R.drawable.internet);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            view.getSettings().setJavaScriptEnabled(true);
            super.onPageFinished(view, url);

            //插入数据
            mDisposable.add(historyViewModel.insertHistoryRecord(view.getTitle(), view.getUrl())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe()
            );

            // 网页加载完毕，隐藏进度条
            progressBar.setVisibility(View.INVISIBLE);

            // 改变标题
            //mView_.setTitle(webView.getTitle());
            // 显示页面标题
            textUrl.setText(webView.getTitle());
            //getSource(view);
            addImageClickListener(view);//待网页加载完全后设置图片点击的监听方法

        }

        private void addImageClickListener(WebView view) {
            view.loadUrl("javascript:(function(){"
                    +"var objs = document.getElementsByTagName(\"img\"); "
                    +"window.imagelistener.setNewArray();"
                    +"var Expression=/https:\\/\\/([\\w-]+\\.)+[\\w-]+(\\/[\\w- .\\/?%&=]*)?/;"
                    +"var objExp=new RegExp(Expression);"
                    +"var array=new Array();"
                    +"var j = 0;"
                    + "for(var i=0;i<objs.length;i++)"
                    +"{"
                    +   "if(objExp.test(objs[i].src))"
                    +  "{"
                    +    "array[j++] = objs[i];"
                    +  "   window.imagelistener.showSource(objs[i].src);  "
                    +   "}"
                    +"}"
                   // "window.imagelistener.showSource(document.getElementsByTagName('html')[0].innerHTML);"
                    +"for(var i=0;i<array.length;i++)  "
                    +"{"
                    + "    array[i].onclick=function()  "
                    +"    {  "
                  //  +       "var Expression2=/https:\\/\\/([\\w-]+\\.)+[\\w-]+(\\/[\\w- .\\/?%&=]*)?/;"
                   // +       " var objExp2=new RegExp(Expression);"
                    +       "if(objExp.test(this.src))"
                    +       "{"
                    +           "window.imagelistener.openImage(this.src);  "
                    +       "}"
                    +"    }  " +
                    "}" +
                    "})()");
        }

        /**
         * 得到网页的源码
         */
        public void getSource(WebView view) {
            view.loadUrl("javascript:(function(){"
                    + "window.imagelistener.showSource(document.getElementsByTagName('html')[0].innerHTML);  " +//通过js代码找到标签为img的代码块，设置点击的监听方法与本地的openImage方法进行连接
                    "})()");
        }

    }


    /**
     * 重写 WebChromeClient
     */
    private class MkWebChromeClient extends WebChromeClient {
        private final static int WEB_PROGRESS_MAX = 100;

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);

            // 加载进度变动，刷新进度条
            progressBar.setProgress(newProgress);
            if (newProgress > 0) {
                if (newProgress == WEB_PROGRESS_MAX) {
                    progressBar.setVisibility(View.INVISIBLE);
                } else {
                    progressBar.setVisibility(View.VISIBLE);
                }
            }
        }

        @Override
        public void onReceivedIcon(WebView view, Bitmap icon) {
            super.onReceivedIcon(view, icon);

            // 改变图标
            webIcon.setImageBitmap(icon);
        }

        @Override
        public void onReceivedTitle(WebView view, String title) {
            super.onReceivedTitle(view, title);

            // 改变标题
            //setTitle(title);
            // 显示页面标题
            textUrl.setText(title);
        }
    }


    /**
     * 返回按钮处理
     */
//    @Override
//    public void onBackPressed() {
//        // 能够返回则返回上一页
//        if (webView.canGoBack()) {
//            webView.goBack();
//        } else {
//            if ((System.currentTimeMillis() - exitTime) > PRESS_BACK_EXIT_GAP) {
//                // 连点两次退出程序
//                Toast.makeText(mContext, "再按一次退出程序",
//                        Toast.LENGTH_SHORT).show();
//                exitTime = System.currentTimeMillis();
//            } else {
//                super.onBackPressed();
//            }
//
//        }
//    }
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            // 跳转 或 刷新
            case R.id.btnStart:
                if (textUrl.hasFocus()) {
                    // 隐藏软键盘
                    if (manager.isActive()) {
                        manager.hideSoftInputFromWindow(textUrl.getApplicationWindowToken(), 0);
                    }

                    // 地址栏有焦点，是跳转
                    String input = textUrl.getText().toString();
                    if (!isHttpUrl(input)) {
                        // 不是网址，加载搜索引擎处理
                        try {
                            // URL 编码
                            input = URLEncoder.encode(input, "utf-8");
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                        input = "https://www.baidu.com/s?wd=" + input + "&ie=UTF-8";
                    }

                    if(!(input.startsWith("http") || input.startsWith("https"))){
                        input = "https://" + input;
                    }
                    webView.loadUrl(input);

                    // 取消掉地址栏的焦点
                    textUrl.clearFocus();
                } else {
                    // 地址栏没焦点，是刷新
                    webView.reload();
                }
                break;
            /*
            // 后退
            case R.id.goBack:
                webView.goBack();
                break;

            // 前进
            case R.id.goForward:
                webView.goForward();
                break;

            // 设置
            case R.id.navSet:
                Toast.makeText(mContext, "功能开发中", Toast.LENGTH_SHORT).show();
                break;

            // 主页
            case R.id.goHome:
                webView.loadUrl(getResources().getString(R.string.home_url));
                break;
            */
            default:
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            webView.getClass().getMethod("onPause").invoke(webView);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            webView.getClass().getMethod("onResume").invoke(webView);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 判断字符串是否为URL（https://blog.csdn.net/bronna/article/details/77529145）
     *
     * @param urls 要勘定的字符串
     * @return true:是URL、false:不是URL
     */
    public static boolean isHttpUrl(String urls) {
        boolean isUrl;
        // 判断是否是网址的正则表达式
        String regex = "(((https|http)?://)?([a-z0-9]+[.])|(www.))"
                + "\\w+[.|\\/]([a-z0-9]{0,})?[[.]([a-z0-9]{0,})]+((/[\\S&&[^,;\u4E00-\u9FA5]]+)+)?([.][a-z0-9]{0,}+|/?)";

        Pattern pat = Pattern.compile(regex.trim());
        Matcher mat = pat.matcher(urls.trim());
        isUrl = mat.matches();
        return isUrl;
    }

    /**
     * 获取版本号名称
     *
     * @param context 上下文
     * @return 当前版本名称
     */
    private static String getVerName(Context context) {
        String verName = "unKnow";
        try {
            verName = context.getPackageManager().
                    getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return verName;
    }

    //获得历史记录，资讯或书签传递的url;
    public String useOtherUrl(){
        Intent intent = getActivity().getIntent();
        String url = null;
        if(intent != null){
            url = intent.getStringExtra("url");
        }
        return url;
    }

    @Override
    public void onDestroy() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Glide.get(getActivity()).clearDiskCache();//清理磁盘缓存需要在子线程中执行
            }
        }).start();
        Glide.get(getActivity()).clearMemory();//清理内存缓存可以在UI主线程中进行
        super.onDestroy();
    }



}