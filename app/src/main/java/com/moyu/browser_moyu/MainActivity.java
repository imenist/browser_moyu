package com.moyu.browser_moyu;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.moyu.browser_moyu.navigationlist.viewmodel.NavSearViewModel;

import static android.view.KeyEvent.KEYCODE_BACK;

public class MainActivity extends AppCompatActivity {

    private NavSearViewModel navSearViewModel;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        if(savedInstanceState == null){
//            getSupportFragmentManager().beginTransaction()
//                    .setReorderingAllowed(true)
//                    .add(R.id.fragment_search_page, SearchPageFragment.class, null)
//                    .commit();
//        }
        navSearViewModel = new ViewModelProvider(this).get(NavSearViewModel.class);

    }

    @Override
    protected void onResume() {
        super.onResume();

        //唤醒阶段通知viewModel设置url传入标志
        Intent intent = getIntent();
        int uesOther = 0;
        if(intent != null){
            uesOther = intent.getIntExtra("useOther", 0);
        }
        navSearViewModel.getData().setUseOther(uesOther);
    }

    //获得新的intent
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    //将返回键消费掉，防止直接推出
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KEYCODE_BACK ){
            navSearViewModel.getData().setGoBack(1);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}