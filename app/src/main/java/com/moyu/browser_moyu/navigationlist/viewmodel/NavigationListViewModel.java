package com.moyu.browser_moyu.navigationlist.viewmodel;

import android.util.Log;
import android.view.MenuItem;

public class NavigationListViewModel {

//        private Context mContext;
//    private ImageView iv_menu;
    //书签
    private MenuItem bookmark;
    //历史记录
    private MenuItem history;
    //加入书签
    private MenuItem addBookmark;

//    public NavigationListViewModel(Context context, ImageView imageView) {
//        mContext = context;
//        iv_menu = imageView;
//    }


    //后退
    public void goBack() {
        Log.i("asdas", "123");
    }

    //前进
    public void goForward() {

    }

    //菜单展开

    public void displayMenu() {

    }

    //窗口展示
    public void displayWindows() {
        //点击的时候，将当前的view加入ViewPager中

    }

    //返回主界面
    public void goBackToMainInterface() {

    }
}
