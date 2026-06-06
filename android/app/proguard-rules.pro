# SENSE-LINK is a WebView shell; no special shrinking rules required for the demo.
# Keep the JavaScript bridge surface if one is added later.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
