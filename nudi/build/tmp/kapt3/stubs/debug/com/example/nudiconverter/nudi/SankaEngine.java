package com.example.nudiconverter.nudi;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000D\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u000b\b\u0007\u0018\u00002\u00020\u0001:\u0003\u001c\u001d\u001eB\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0016\u0010\u0011\u001a\u00020\u00122\u0006\u0010\u0013\u001a\u00020\u00142\u0006\u0010\u0015\u001a\u00020\u0006J\u000e\u0010\u0016\u001a\u00020\u00122\u0006\u0010\u0002\u001a\u00020\u0003J \u0010\u0017\u001a\u00020\u00122\u0006\u0010\u0018\u001a\u00020\u00142\u0006\u0010\u0013\u001a\u00020\u00142\u0006\u0010\u0015\u001a\u00020\u0006H\u0002J \u0010\u0019\u001a\u00020\u00122\u0006\u0010\u0018\u001a\u00020\u00142\u0006\u0010\u0013\u001a\u00020\u00142\u0006\u0010\u0015\u001a\u00020\u0006H\u0002J\b\u0010\u001a\u001a\u00020\u0012H\u0002J\u0016\u0010\u001b\u001a\u00020\u00122\u0006\u0010\u0013\u001a\u00020\u00142\u0006\u0010\u0015\u001a\u00020\u0006R\u0010\u0010\u0005\u001a\u0004\u0018\u00010\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u000b\u001a\u0004\u0018\u00010\fX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\r\u001a\u00020\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u000f\u001a\u00020\u00108\u0002X\u0083\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001f"}, d2 = {"Lcom/example/nudiconverter/nudi/SankaEngine;", "", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "callback", "Lcom/example/nudiconverter/nudi/SankaEngine$Callback;", "isPageLoaded", "", "mainHandler", "Landroid/os/Handler;", "pendingCall", "Lcom/example/nudiconverter/nudi/SankaEngine$PendingCall;", "webContext", "Landroid/content/MutableContextWrapper;", "webView", "Landroid/webkit/WebView;", "asciiToUnicode", "", "text", "", "cb", "attachContext", "executeJS", "func", "executeJsInternal", "flushPending", "unicodeToAscii", "AndroidBridge", "Callback", "PendingCall", "nudi_debug"})
public final class SankaEngine {
    @org.jetbrains.annotations.Nullable()
    private com.example.nudiconverter.nudi.SankaEngine.Callback callback;
    @org.jetbrains.annotations.NotNull()
    private final android.os.Handler mainHandler = null;
    @org.jetbrains.annotations.NotNull()
    private final android.content.MutableContextWrapper webContext = null;
    @org.jetbrains.annotations.Nullable()
    private com.example.nudiconverter.nudi.SankaEngine.PendingCall pendingCall;
    @kotlin.jvm.Volatile()
    private volatile boolean isPageLoaded = false;
    @android.annotation.SuppressLint(value = {"SetJavaScriptEnabled"})
    @org.jetbrains.annotations.NotNull()
    private final android.webkit.WebView webView = null;
    
    public SankaEngine(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        super();
    }
    
    public final void attachContext(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
    }
    
    @kotlin.jvm.Synchronized()
    private final synchronized void flushPending() {
    }
    
    private final void executeJS(java.lang.String func, java.lang.String text, com.example.nudiconverter.nudi.SankaEngine.Callback cb) {
    }
    
    private final void executeJsInternal(java.lang.String func, java.lang.String text, com.example.nudiconverter.nudi.SankaEngine.Callback cb) {
    }
    
    public final void asciiToUnicode(@org.jetbrains.annotations.NotNull()
    java.lang.String text, @org.jetbrains.annotations.NotNull()
    com.example.nudiconverter.nudi.SankaEngine.Callback cb) {
    }
    
    public final void unicodeToAscii(@org.jetbrains.annotations.NotNull()
    java.lang.String text, @org.jetbrains.annotations.NotNull()
    com.example.nudiconverter.nudi.SankaEngine.Callback cb) {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\b\u0082\u0004\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006H\u0007J\u0010\u0010\u0007\u001a\u00020\u00042\u0006\u0010\b\u001a\u00020\u0006H\u0007\u00a8\u0006\t"}, d2 = {"Lcom/example/nudiconverter/nudi/SankaEngine$AndroidBridge;", "", "(Lcom/example/nudiconverter/nudi/SankaEngine;)V", "onConverted", "", "text", "", "onError", "msg", "nudi_debug"})
    final class AndroidBridge {
        
        public AndroidBridge() {
            super();
        }
        
        @android.webkit.JavascriptInterface()
        public final void onConverted(@org.jetbrains.annotations.NotNull()
        java.lang.String text) {
        }
        
        @android.webkit.JavascriptInterface()
        public final void onError(@org.jetbrains.annotations.NotNull()
        java.lang.String msg) {
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\bf\u0018\u00002\u00020\u0001J\u0010\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H&J\u0010\u0010\u0006\u001a\u00020\u00032\u0006\u0010\u0007\u001a\u00020\u0005H&\u00a8\u0006\b"}, d2 = {"Lcom/example/nudiconverter/nudi/SankaEngine$Callback;", "", "onError", "", "error", "", "onResult", "result", "nudi_debug"})
    public static abstract interface Callback {
        
        public abstract void onResult(@org.jetbrains.annotations.NotNull()
        java.lang.String result);
        
        public abstract void onError(@org.jetbrains.annotations.NotNull()
        java.lang.String error);
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u000b\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\b\u0082\b\u0018\u00002\u00020\u0001B\u001d\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u0012\u0006\u0010\u0005\u001a\u00020\u0006\u00a2\u0006\u0002\u0010\u0007J\t\u0010\r\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u000e\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u000f\u001a\u00020\u0006H\u00c6\u0003J\'\u0010\u0010\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00032\b\b\u0002\u0010\u0005\u001a\u00020\u0006H\u00c6\u0001J\u0013\u0010\u0011\u001a\u00020\u00122\b\u0010\u0013\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\u0014\u001a\u00020\u0015H\u00d6\u0001J\t\u0010\u0016\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u0005\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\b\u0010\tR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\u000bR\u0011\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\u000b\u00a8\u0006\u0017"}, d2 = {"Lcom/example/nudiconverter/nudi/SankaEngine$PendingCall;", "", "func", "", "text", "callback", "Lcom/example/nudiconverter/nudi/SankaEngine$Callback;", "(Ljava/lang/String;Ljava/lang/String;Lcom/example/nudiconverter/nudi/SankaEngine$Callback;)V", "getCallback", "()Lcom/example/nudiconverter/nudi/SankaEngine$Callback;", "getFunc", "()Ljava/lang/String;", "getText", "component1", "component2", "component3", "copy", "equals", "", "other", "hashCode", "", "toString", "nudi_debug"})
    static final class PendingCall {
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String func = null;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String text = null;
        @org.jetbrains.annotations.NotNull()
        private final com.example.nudiconverter.nudi.SankaEngine.Callback callback = null;
        
        public PendingCall(@org.jetbrains.annotations.NotNull()
        java.lang.String func, @org.jetbrains.annotations.NotNull()
        java.lang.String text, @org.jetbrains.annotations.NotNull()
        com.example.nudiconverter.nudi.SankaEngine.Callback callback) {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getFunc() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getText() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.example.nudiconverter.nudi.SankaEngine.Callback getCallback() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component2() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.example.nudiconverter.nudi.SankaEngine.Callback component3() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.example.nudiconverter.nudi.SankaEngine.PendingCall copy(@org.jetbrains.annotations.NotNull()
        java.lang.String func, @org.jetbrains.annotations.NotNull()
        java.lang.String text, @org.jetbrains.annotations.NotNull()
        com.example.nudiconverter.nudi.SankaEngine.Callback callback) {
            return null;
        }
        
        @java.lang.Override()
        public boolean equals(@org.jetbrains.annotations.Nullable()
        java.lang.Object other) {
            return false;
        }
        
        @java.lang.Override()
        public int hashCode() {
            return 0;
        }
        
        @java.lang.Override()
        @org.jetbrains.annotations.NotNull()
        public java.lang.String toString() {
            return null;
        }
    }
}