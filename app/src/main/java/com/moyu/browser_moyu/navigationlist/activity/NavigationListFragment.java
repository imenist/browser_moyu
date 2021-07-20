package com.moyu.browser_moyu.navigationlist.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import com.moyu.browser_moyu.R;
import com.moyu.browser_moyu.bookmark.activity.BookMarkActivity;
import com.moyu.browser_moyu.databinding.FragmentNavigationListBinding;
import com.moyu.browser_moyu.db.viewmodel.BookmarkRecordViewModel;
import com.moyu.browser_moyu.history.activity.HistoryRecordActivity;
import com.moyu.browser_moyu.navigationlist.viewmodel.NavSearViewModel;
import com.moyu.browser_moyu.newspage.activity.NewsPageActivity;
import com.moyu.browser_moyu.searchpage.activity.SearchPageFragment;

import java.util.ArrayList;
import java.util.HashMap;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class NavigationListFragment extends Fragment implements View.OnClickListener {

    private BookmarkRecordViewModel bookmarkRecordViewModel;
    private CompositeDisposable mDisposable;
    //WebView
    private WebView webView;
    //后退，前进，菜单，窗口，主页面
    private ImageView iv_goBack, iv_goForward, iv_menu, iv_windows, iv_mainWindow;
    //menu菜单Dialog，窗口Dialog
    private AlertDialog menuDialog, windowDialog;
    //menu布局控件
    private GridView menuGrid;
    //menu布局
    private View menuView;
    //fragment视图
    private View view;
    //碎片管理
    FragmentManager fragmentManager;
    //导航栏上方碎片
    SearchPageFragment searchPageFragment;

//    //绑定
//    private FragmentNavigationListBinding fragmentNavigationListBinding;
//    //ViewModel
//    private NavigationListViewModel navigationListViewModel;

    //Dialog
    private String[] menu_name_array = {"历史记录", "书签", "加入书签", "资讯"};
    private int[] menu_image_array = {R.drawable.history1, R.drawable.bookmark, R.drawable.add_bookmark,
            R.drawable.news};

    //DialogItem
    private final int ITEM_HISTORY = 0;
    private final int ITEM_BOOKMARK = 1;
    private final int ITEM_ADD_BOOKMARK = 2;
    //    private final int ITEM_CREATE_WINDOW = 3;
    private final int ITEM_NEWS = 3;

    //与SearchPageFragment通信
    private NavSearViewModel viewModel;
    private FragmentNavigationListBinding fragmentNavigationListBinding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        //绑定
        fragmentNavigationListBinding = DataBindingUtil.inflate
                (inflater, R.layout.fragment_navigation_list, container, false);
        viewModel = ViewModelProviders.of(getActivity()).get(NavSearViewModel.class);
        fragmentNavigationListBinding.setViewModel(viewModel);
        fragmentNavigationListBinding.setLifecycleOwner(getActivity());

        view = fragmentNavigationListBinding.getRoot();

        //初始化
        initView();

        menuDialog.setView(menuView);
        setDialogPosition();
        menuDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            public boolean onKey(DialogInterface dialog, int keyCode,
                                 KeyEvent event) {
                // 监听按键
                if (keyCode == KeyEvent.KEYCODE_MENU) {
                    dialog.dismiss();
                }
                return false;
            }
        });

        menuGrid.setAdapter(getMenuAdapter(menu_name_array, menu_image_array));
        menuDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            public boolean onKey(DialogInterface dialog, int keyCode,
                                 KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_MENU)// 监听按键
                    dialog.dismiss();
                return false;
            }
        });

        menuGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                    long arg3) {
                switch (arg2) {
                    //查看历史记录
                    case ITEM_HISTORY:
                        menuDialog.dismiss();
                        Intent toHistory = new Intent(getActivity(), HistoryRecordActivity.class);
                        startActivity(toHistory);
                        break;
                    //查看书签
                    case ITEM_BOOKMARK:
                        menuDialog.dismiss();
                        Intent toBookmark = new Intent(getActivity(), BookMarkActivity.class);
                        startActivity(toBookmark);
                        break;
                    //加入书签
                    case ITEM_ADD_BOOKMARK:
                        menuDialog.dismiss();
                        //获得WebView的title和url
                        String title = webView.getTitle();
                        String url = webView.getUrl();
                        mDisposable.add(bookmarkRecordViewModel.insertBookmarkRecord(title, url)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe()
                        );
                        break;
                    //创建窗口
//                    case ITEM_CREATE_WINDOW:
//                        menuDialog.dismiss();
//                        //保存当前Fragment数据
//
//                        //创建一个新的Fragment
//
//                        //新Fragment加载主页
//
//                        break;
                    case ITEM_NEWS:
                        menuDialog.dismiss();
                        Intent toNews = new Intent(getActivity(), NewsPageActivity.class);
                        startActivity(toNews);
                        break;
                }
            }
        });

        //菜单点击事件
        iv_menu.setOnClickListener(this);
        //窗口点击事件
        iv_windows.setOnClickListener(this);
        //后退
        iv_goBack.setOnClickListener(this);
        //前进
        iv_goForward.setOnClickListener(this);
        //回主页面
        iv_mainWindow.setOnClickListener(this);

        return view;
    }

    //初始化
    public void initView() {
        iv_goBack = (ImageView) view.findViewById(R.id.goBack);
        iv_goForward = (ImageView) view.findViewById(R.id.goForward);
        iv_menu = (ImageView) view.findViewById(R.id.menu);
        iv_windows = (ImageView) view.findViewById(R.id.windows);
        iv_mainWindow = (ImageView) view.findViewById(R.id.goHome);
        menuView = view.inflate(getActivity(), R.layout.gridview_menu, null);
        menuDialog = new AlertDialog.Builder(getActivity()).create();
        menuGrid = (GridView) menuView.findViewById(R.id.gridview);
        fragmentManager = getActivity().getSupportFragmentManager();
        searchPageFragment = (SearchPageFragment) fragmentManager.findFragmentById(R.id.fragment_search_page);
        webView = searchPageFragment.webView;
        bookmarkRecordViewModel = new ViewModelProvider(getActivity()).get(BookmarkRecordViewModel.class);
        mDisposable = new CompositeDisposable();
    }

    //菜单
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add("menu");
    }

    //菜单
    private SimpleAdapter getMenuAdapter(String[] menuNameArray,
                                         int[] imageResourceArray) {
        ArrayList<HashMap<String, Object>> data = new ArrayList<HashMap<String, Object>>();
        for (int i = 0; i < menuNameArray.length; i++) {
            HashMap<String, Object> map = new HashMap<String, Object>();
            map.put("itemImage", imageResourceArray[i]);
            map.put("itemText", menuNameArray[i]);
            data.add(map);
        }
        SimpleAdapter simperAdapter = new SimpleAdapter(getActivity(), data,
                R.layout.item_menu, new String[]{"itemImage", "itemText"},
                new int[]{R.id.item_image, R.id.item_text});
        return simperAdapter;
    }

    //改变弹窗的位置
    public void setDialogPosition() {
        WindowManager wm = getActivity().getWindowManager();
        Window window = menuDialog.getWindow();
        //弹窗位于底部
        window.setGravity(Gravity.BOTTOM);
        Display display = wm.getDefaultDisplay();
        WindowManager.LayoutParams params = window.getAttributes();
        //相对底部偏移量
        params.y = display.getHeight() / 16;
        params.width = display.getWidth();
        window.setAttributes(params);
    }

    //点击事件
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            //菜单
            case R.id.menu:
                //弹出对话框
                menuDialog.show();
                break;
            //多窗口
            case R.id.windows:

                break;
            //后退
            case R.id.goBack:
                viewModel.getData().setGoBack(1);
                break;
            //前进
            case R.id.goForward:
                viewModel.getData().setGoForward(2);
                break;
            //回主页
            case R.id.goHome:
                viewModel.getData().setGoHome(3);
                break;
            default:
                Toast.makeText(getActivity(), "出错", Toast.LENGTH_SHORT).show();
        }
    }
}