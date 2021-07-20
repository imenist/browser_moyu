package com.moyu.browser_moyu.searchpage.util;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.moyu.browser_moyu.searchpage.activity.PhotoBrowserActivity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by Administrator on 2017/2/10.
 */
public class JavascriptInterface {
    private static final String TAG = "JavascriptInterface";
    private Context context;
    private String [] imageUrls;
    private ArrayList<String> imageSrc;

    public JavascriptInterface(Context context,String[] imageUrls) {
        this.context = context;
        this.imageUrls = imageUrls;
    }

    public JavascriptInterface(Context context) {
        this.context = context;
        this.imageSrc = new ArrayList<>();
    }

    @android.webkit.JavascriptInterface
    public void openImage(String img) {
        Intent intent = new Intent();
        intent.putStringArrayListExtra("imageUrls", imageSrc);
        intent.putExtra("curImageUrl", img);
        intent.setClass(context, PhotoBrowserActivity.class);
        context.startActivity(intent);
        /*
        for (int i = 0; i < imageSrc.toArray().length; i++) {
            Log.e("图片地址"+i,imageSrc.get(i).toString());
        }*/
    }

    @android.webkit.JavascriptInterface
    public void setNewArray() {
       this.imageSrc = new ArrayList<>();
    }
    @android.webkit.JavascriptInterface
    public void showSource(String src){
        Log.i(TAG,"html---"+src);
        //imageSrc = GetImageUtils.getImageSrc(GetImageUtils.getImageUrl(html));
        //imageSrc.add(html);
        //imageSrc = GetImageUtils.returnImageUrlsFromHtml(html);
        /*
        if(null!=listner){
            listner.getImageList(imageSrc);
        }*/
        imageSrc.add(src);
        for(String s :imageSrc){
            Log.i(TAG,s);
        }

    }
}
