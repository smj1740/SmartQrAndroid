# Keep WebView JS interfaces if added later
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
