package com.example.nudiconverter.viewmodel;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000T\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0007\b\u0007\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u000e\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\u0011J \u0010\u0012\u001a\u00020\u000f2\u0006\u0010\u0013\u001a\u00020\u00142\u0006\u0010\u0015\u001a\u00020\u00162\u0006\u0010\u0017\u001a\u00020\u0018H\u0002J\u0010\u0010\u0019\u001a\u00020\u000f2\b\u0010\u001a\u001a\u0004\u0018\u00010\u001bJ\u0006\u0010\u001c\u001a\u00020\u000fJ\u000e\u0010\u001d\u001a\u00020\u000f2\u0006\u0010\u0015\u001a\u00020\u0016J\u0006\u0010\u001e\u001a\u00020\u000fJ\u000e\u0010\u001f\u001a\u00020\u000f2\u0006\u0010 \u001a\u00020\u0014J\u000e\u0010!\u001a\u00020\u000f2\u0006\u0010 \u001a\u00020\u0014R\u0014\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\n\u001a\b\u0012\u0004\u0012\u00020\u00070\u000b\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\r\u00a8\u0006\""}, d2 = {"Lcom/example/nudiconverter/viewmodel/NudiConverterViewModel;", "Landroidx/lifecycle/AndroidViewModel;", "app", "Landroid/app/Application;", "(Landroid/app/Application;)V", "_uiState", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/example/nudiconverter/viewmodel/NudiState;", "sanka", "Lcom/example/nudiconverter/nudi/SankaEngine;", "uiState", "Lkotlinx/coroutines/flow/StateFlow;", "getUiState", "()Lkotlinx/coroutines/flow/StateFlow;", "attachContext", "", "context", "Landroid/content/Context;", "convert", "raw", "", "mode", "Lcom/example/nudiconverter/viewmodel/ConverterMode;", "removeSpace", "", "handleFile", "uri", "Landroid/net/Uri;", "reset", "toggleMode", "toggleSpace", "updateInput", "text", "updateOutput", "nudi_debug"})
public final class NudiConverterViewModel extends androidx.lifecycle.AndroidViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.example.nudiconverter.nudi.SankaEngine sanka = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.example.nudiconverter.viewmodel.NudiState> _uiState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.example.nudiconverter.viewmodel.NudiState> uiState = null;
    
    public NudiConverterViewModel(@org.jetbrains.annotations.NotNull()
    android.app.Application app) {
        super(null);
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.example.nudiconverter.viewmodel.NudiState> getUiState() {
        return null;
    }
    
    public final void updateInput(@org.jetbrains.annotations.NotNull()
    java.lang.String text) {
    }
    
    public final void updateOutput(@org.jetbrains.annotations.NotNull()
    java.lang.String text) {
    }
    
    public final void toggleMode(@org.jetbrains.annotations.NotNull()
    com.example.nudiconverter.viewmodel.ConverterMode mode) {
    }
    
    public final void toggleSpace() {
    }
    
    public final void attachContext(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
    }
    
    public final void reset() {
    }
    
    private final void convert(java.lang.String raw, com.example.nudiconverter.viewmodel.ConverterMode mode, boolean removeSpace) {
    }
    
    public final void handleFile(@org.jetbrains.annotations.Nullable()
    android.net.Uri uri) {
    }
}