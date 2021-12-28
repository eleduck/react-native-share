package cn.reactnative.modules.share;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.UiThreadUtil;
import com.sina.weibo.sdk.api.TextObject;
import com.sina.weibo.sdk.api.WebpageObject;
import com.sina.weibo.sdk.api.WeiboMultiMessage;
import com.sina.weibo.sdk.auth.AuthInfo;

import com.sina.weibo.sdk.WbSdk;
import com.sina.weibo.sdk.share.WbShareHandler;
import com.tencent.connect.share.QQShare;
import com.tencent.tauth.Tencent;
import java.util.Date;


/**
 * Created by lvbingru on 12/22/15.
 */
public class AppShareModule extends ReactContextBaseJavaModule{
  public final String TAG = "ShareModule";
  private Context mContext;

  private String weiboAppId;
  private Tencent api;
  private String qqAppId;

  private WbShareHandler shareHandler;

  private static final String RCTSharePlatform = "platform";
  private static final String RCTShareTitle = "title";
  private static final String RCTShareDescription = "description";
  private static final String RCTShareWebpageUrl = "webpageUrl";
  private static final String RCTShareImageUrl = "imageUrl";

  private static AppShareModule gModule = null;

  public AppShareModule(ReactApplicationContext reactContext) {
    super(reactContext);
    mContext = reactContext;
    ApplicationInfo appInfo = null;
    try {
      appInfo = reactContext.getPackageManager().getApplicationInfo(reactContext.getPackageName(), PackageManager.GET_META_DATA);
    } catch (PackageManager.NameNotFoundException e) {
      throw new Error(e);
    }
    if (!appInfo.metaData.containsKey("WB_APPID")) {
      throw new Error("meta-data WB_APPID not found in AndroidManifest.xml");
    }
    this.weiboAppId = appInfo.metaData.get("WB_APPID").toString();

    if (!appInfo.metaData.containsKey("QQ_APPID")) {
      throw new Error("meta-data QQ_APPID not found in AndroidManifest.xml");
    }
    this.qqAppId = appInfo.metaData.get("QQ_APPID").toString();
  }

  @Override
  public void initialize() {
    super.initialize();
    gModule = this;
  }

  @Override
  public void onCatalystInstanceDestroy() {
    super.onCatalystInstanceDestroy();
    gModule = null;
    shareHandler = null;
  }

  @Override
  public String getName() {
    return "RCTShareAPI";
  }

  private void registerWeibo() {
    if (shareHandler == null) {
      String redirectURI = "https://api.weibo.com/oauth2/default.html";
      String scope = "all";
      final AuthInfo sinaAuthInfo = new AuthInfo(getReactApplicationContext(), this.weiboAppId, redirectURI, scope);
      WbSdk.install(getCurrentActivity(), sinaAuthInfo);
      shareHandler = new WbShareHandler(getCurrentActivity());
      shareHandler.registerApp();
    }
  }

  private void registerQQ() {
    if (api == null) {
      String packageNames = null;
      try {
        PackageInfo info = mContext
          .getPackageManager()
          .getPackageInfo(mContext.getPackageName(), 0);
        packageNames = info.packageName;
      } catch (PackageManager.NameNotFoundException e) {
        e.printStackTrace();
      }

      if (packageNames != null) {
        Log.d(TAG, "packageNames: " + packageNames);
        api =
          Tencent.createInstance(
            qqAppId,
            getReactApplicationContext().getApplicationContext(),
            packageNames + ".fileprovider"
          );
      } else {
        api =
          Tencent.createInstance(
            qqAppId,
            getReactApplicationContext().getApplicationContext()
          );
      }
    }
  }

  @ReactMethod
  public void isQQInstalled(Promise promise) {
    registerQQ();
    if (api.isSupportSSOLogin(getCurrentActivity())) {
      promise.resolve(true);
    } else {
      promise.reject("not installed");
    }
  }

  @ReactMethod
  public void isWeiboInstalled(Promise promise) {
    registerWeibo();
    if (WbSdk.isWbInstall(mContext)) {
      promise.resolve(true);
    } else {
      promise.reject("not installed");
    }
  }

  @ReactMethod
  public void share(final ReadableMap data, final Promise promise) {
    if (data.getString(RCTSharePlatform).equals("qq")) {
      registerQQ();
      Bundle bundle = new Bundle();
      if (data.hasKey(RCTShareTitle)) {
        bundle.putString(
          QQShare.SHARE_TO_QQ_TITLE,
          data.getString(RCTShareTitle)
        );
      }
      if (data.hasKey(RCTShareDescription)) {
        bundle.putString(
          QQShare.SHARE_TO_QQ_SUMMARY,
          data.getString(RCTShareDescription)
        );
      }
      if (data.hasKey(RCTShareWebpageUrl)) {
        bundle.putString(
          QQShare.SHARE_TO_QQ_TARGET_URL,
          data.getString(RCTShareWebpageUrl)
        );
      }
      if (data.hasKey(RCTShareImageUrl)) {
        bundle.putString(
          QQShare.SHARE_TO_QQ_IMAGE_URL,
          data.getString(RCTShareImageUrl)
        );
      }
      if (data.hasKey("appName")) {
        bundle.putString(QQShare.SHARE_TO_QQ_APP_NAME, data.getString("appName"));
      }
      bundle.putInt(
        QQShare.SHARE_TO_QQ_KEY_TYPE,
        QQShare.SHARE_TO_QQ_TYPE_DEFAULT
      );
      Log.e("QQShare", bundle.toString());

      bundle.putInt(
        QQShare.SHARE_TO_QQ_EXT_INT,
        QQShare.SHARE_TO_QQ_FLAG_QZONE_ITEM_HIDE
      );
      api.shareToQQ(getCurrentActivity(), bundle, null);
    } else {
      registerWeibo();
      WeiboMultiMessage weiboMessage = new WeiboMultiMessage();//初始化微博的分享消息
      weiboMessage.textObject = new TextObject();
      WebpageObject webpageObject = new WebpageObject();
      if (data.hasKey(RCTShareWebpageUrl)) {
        webpageObject.actionUrl = data.getString(RCTShareWebpageUrl);
      }
      weiboMessage.mediaObject = webpageObject;
      if (data.hasKey(RCTShareDescription)) {
        weiboMessage.mediaObject.description = data.getString(RCTShareDescription);
      }
      if (data.hasKey(RCTShareTitle)) {
        weiboMessage.mediaObject.title = data.getString(RCTShareTitle);
        weiboMessage.textObject.text = weiboMessage.mediaObject.title;
      }
      weiboMessage.mediaObject.setThumbImage(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_logo));
      weiboMessage.mediaObject.identify = new Date().toString();
      shareHandler.shareMessage(weiboMessage, true);
    }
    UiThreadUtil.runOnUiThread(
      new Runnable() {
        @Override
        public void run() {
          promise.resolve(null);
        }
      }
    );
  }

}
