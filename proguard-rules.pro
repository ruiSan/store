# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in D:\Android\sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
-optimizationpasses 5
-dontoptimize

# keep annotated by NotProguard
-keep @com.sevenonechat.sdk.util.NotProguard class * {*;}
-keepclassmembers class * {
@com.sevenonechat.sdk.util.NotProguard <methods>;
}

-keep public class com.sevenonechat.sdk.views.** { *; }
-keep class com.sevenonechat.sdk.compts.** { *; }
-keep public class com.sevenonechat.sdk.chatview.layout.KeyboardDetectorRelativeLayout
-keep public class com.sevenonechat.sdk.chatview.widgets.OnlyView
-keep public class com.sevenonechat.sdk.chatview.widgets.ChatToolBox
-keep public class com.sevenonechat.sdk.chatview.widgets.emotion.EmotionView
-keep public class com.sevenonechat.sdk.pictureselector.widget.SquareRelativeLayout
-keep public class com.sevenonechat.sdk.pictureselector.widget.PreviewViewPager
-keep public class android.support.v4.view.ViewPager
-keep public class android.support.v7.widget.AppCompatSpinner
-keep public class android.support.v7.widget.RecyclerView
-keep public class android.support.v7.widget.Toolbar

-keep class com.sevenonechat.sdk.bean.**{*;}
-keep class com.sevenonechat.sdk.EventBusBean.**{*;}
-keep class com.sevenonechat.sdk.model.**{*;}
-keep class com.sevenonechat.sdk.sdkCallBack.**{*;}
-keep class com.sevenonechat.sdk.util.**{*;}
-keep class com.sevenonechat.sdk.http.RequestApi**{
    <fields>;
    <methods>;
 }
 -keep class com.sevenonechat.sdk.chatview.widgets.emotion.data.Emoticon{
 public <methods>;
 }
-keep class com.sevenonechat.sdk.service.ChatService{
public <methods>;
}
-keep class com.sevenonechat.sdk.thirdParty.**{*;}

-keep class com.sevenonechat.sdk.sdkinfo.SdkRunningClient
#-keep class com.sevenonechat.sdk.sdkinfo.SdkRunningClient{
#    public <methods>;
#}
-keep class com.sevenonechat.sdk.sdkCustomUi.UiCustomOptions**{
    <fields>;
}

-keepattributes Exceptions
-keepattributes Signature
-keepattributes *Annotation*

-keepclassmembers class ** {
    @com.sevenonechat.sdk.thirdParty.eventbus.Subscribe <methods>;
}
-keep enum com.sevenonechat.sdk.thirdParty.eventbus.ThreadMode { *; }
#-keep @interface com.sevenonechat.sdk.thirdParty.eventbus.Subscribe { *; }

# Only required if you use AsyncExecutor
-keepclassmembers class * extends com.sevenonechat.sdk.thirdParty.eventbus.util.ThrowableFailureEvent {
    <init>(java.lang.Throwable);
}

#混淆忽略第三方jar包，并关闭警告
-keep class com.qysn.** { *; }
-dontwarn com.qysn.cj.cj.manager.**

