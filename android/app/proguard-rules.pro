# Add project specific ProGuard rules here.
# 保留行号信息，方便调试崩溃日志
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# 保留注解
-keepattributes *Annotation*

# 保留泛型
-keepattributes Signature

# 保留异常
-keepattributes Exceptions

# ========== Android 核心组件 ==========
# 保留 Activity
-keep public class * extends android.app.Activity
-keep public class * extends androidx.appcompat.app.AppCompatActivity

# 保留 Fragment
-keep public class * extends androidx.fragment.app.Fragment

# 保留 Service、BroadcastReceiver、ContentProvider
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# ========== Room 数据库（必须保留）==========
-keep class * extends androidx.room.RoomDatabase
-keep class **_Impl { *; }
-keep class **_*Dao_Impl { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep interface * {
    @androidx.room.* <methods>;
}
-keepclassmembers class * {
    @androidx.room.* <fields>;
    @androidx.room.* <methods>;
}
-dontwarn androidx.room.paging.**
-dontwarn androidx.room.**

# 保留数据库实体类的所有字段（Room 需要反射访问）
-keepclassmembers @androidx.room.Entity class * {
    <fields>;
}

# ========== Gson（JSON 序列化）==========
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# 保留被 Gson 使用的类
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# ========== OkHttp ==========
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ========== 自定义 View ==========
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(***);
}

# ========== WebView ==========
-keepclassmembers class * extends android.webkit.WebViewClient {
    public void *(android.webkit.WebView, java.lang.String, android.graphics.Bitmap);
    public boolean *(android.webkit.WebView, java.lang.String);
}

# ========== RecyclerView Adapter ==========
-keepclassmembers class * extends androidx.recyclerview.widget.RecyclerView$ViewHolder {
    public <init>(...);
}

# ========== Parcelable ==========
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

# ========== 移除日志（Release 版本不需要）==========
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
}

# ========== 优化设置 ==========
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# ========== Jsoup ==========
-dontwarn org.jspecify.annotations.**
-dontwarn org.jsoup.**

# ========== 不警告 ==========
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ========== 移除未使用的资源和代码 ==========
# ProGuard 会自动删除未使用的类、方法、字段
# 配合 isShrinkResources = true 可以删除未使用的资源
