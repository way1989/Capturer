# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/android/way/tools/sdk/tools/proguard/proguard-android.txt
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
#BUGTAG start
-keepattributes LineNumberTable,SourceFile

-keep class com.bugtags.library.** {*;}
-dontwarn org.apache.http.**
-dontwarn android.net.http.AndroidHttpClient
-dontwarn com.bugtags.library.**
#BUGTAG end
-dontwarn com.alexvasilkov.android.**
-dontwarn ly.img.android.**
-dontwarn android.support.**

#-dontwarn com.github.hiteshsondhi88.libffmpeg.**
#-keep com.github.hiteshsondhi88.libffmpeg.**{*;}
-keep class android.support.**{*;}
#-keep class com.way.captain.widget.**{*;}
#-keep class com.getbase.floatingactionbutton.**{*;}
#-keep class com.alexvasilkov.**{*;}


#spotlight
-keep class com.wooplr.spotlight.** { *; }
-keep interface com.wooplr.spotlight.**
-keep enum com.wooplr.spotlight.**

#bugly
-dontwarn com.tencent.bugly.**
-keep public class com.tencent.bugly.**{*;}