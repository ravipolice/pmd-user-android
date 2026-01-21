package com.example.nudiconverter;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000Z\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0006\u001a^\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u00032\u0006\u0010\u0005\u001a\u00020\u00062\u0012\u0010\u0007\u001a\u000e\u0012\u0004\u0012\u00020\u0003\u0012\u0004\u0012\u00020\u00010\b2\u001e\b\u0002\u0010\t\u001a\u0018\u0012\u0004\u0012\u00020\n\u0012\u0004\u0012\u00020\u00010\b\u00a2\u0006\u0002\b\u000b\u00a2\u0006\u0002\b\fH\u0007\u00f8\u0001\u0000\u00a2\u0006\u0004\b\r\u0010\u000e\u001a2\u0010\u000f\u001a\u00020\u00012\u0006\u0010\u0010\u001a\u00020\u00032\f\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\u00010\u00122\u0012\u0010\u0013\u001a\u000e\u0012\u0004\u0012\u00020\u0014\u0012\u0004\u0012\u00020\u00010\bH\u0003\u001a\u0010\u0010\u0015\u001a\u00020\u00012\u0006\u0010\u0016\u001a\u00020\u0017H\u0007\u001a\f\u0010\u0018\u001a\u00020\u0019*\u00020\u001aH\u0002\u001a\u0014\u0010\u001b\u001a\u0004\u0018\u00010\u0019*\u00020\u001a2\u0006\u0010\u0002\u001a\u00020\u0003\u001a\u0014\u0010\u001c\u001a\u0004\u0018\u00010\u0019*\u00020\u001a2\u0006\u0010\u0002\u001a\u00020\u0003\u001a\u0014\u0010\u001d\u001a\u0004\u0018\u00010\u0019*\u00020\u001a2\u0006\u0010\u0002\u001a\u00020\u0003\u001a\u001c\u0010\u001e\u001a\u0004\u0018\u00010\u0019*\u00020\u001a2\u0006\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u001f\u001a\u00020\u0014\u001a\"\u0010 \u001a\u00020!*\u00020\u001a2\u0006\u0010\"\u001a\u00020#2\u0006\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u001f\u001a\u00020\u0014\u001a\u001a\u0010$\u001a\u00020\u0001*\u00020\u001a2\u0006\u0010%\u001a\u00020\u00192\u0006\u0010&\u001a\u00020\u0003\u001a\u0014\u0010\'\u001a\u00020\u0003*\u00020\u001a2\u0006\u0010(\u001a\u00020\u0003H\u0002\u0082\u0002\u0007\n\u0005\b\u00a1\u001e0\u0001\u00a8\u0006)"}, d2 = {"ConverterCard", "", "text", "", "label", "height", "Landroidx/compose/ui/unit/Dp;", "onValueChange", "Lkotlin/Function1;", "content", "Landroidx/compose/foundation/layout/ColumnScope;", "Landroidx/compose/runtime/Composable;", "Lkotlin/ExtensionFunctionType;", "ConverterCard-TDGSqEk", "(Ljava/lang/String;Ljava/lang/String;FLkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function1;)V", "FormatSelectionDialog", "title", "onDismiss", "Lkotlin/Function0;", "onFormatSelected", "Lcom/example/nudiconverter/NudiExportFormat;", "NudiConverterScreen", "viewModel", "Lcom/example/nudiconverter/viewmodel/NudiConverterViewModel;", "conversionsDir", "Ljava/io/File;", "Landroid/content/Context;", "exportToDocx", "exportToPdf", "exportToTxt", "generateFile", "format", "saveToFolder", "", "treeUri", "Landroid/net/Uri;", "shareFile", "file", "mime", "timestampName", "ext", "nudi_debug"})
public final class MainActivityKt {
    
    @kotlin.OptIn(markerClass = {androidx.compose.material3.ExperimentalMaterial3Api.class})
    @androidx.compose.runtime.Composable()
    public static final void NudiConverterScreen(@org.jetbrains.annotations.NotNull()
    com.example.nudiconverter.viewmodel.NudiConverterViewModel viewModel) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void FormatSelectionDialog(java.lang.String title, kotlin.jvm.functions.Function0<kotlin.Unit> onDismiss, kotlin.jvm.functions.Function1<? super com.example.nudiconverter.NudiExportFormat, kotlin.Unit> onFormatSelected) {
    }
    
    private static final java.io.File conversionsDir(android.content.Context $this$conversionsDir) {
        return null;
    }
    
    private static final java.lang.String timestampName(android.content.Context $this$timestampName, java.lang.String ext) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public static final java.io.File generateFile(@org.jetbrains.annotations.NotNull()
    android.content.Context $this$generateFile, @org.jetbrains.annotations.NotNull()
    java.lang.String text, @org.jetbrains.annotations.NotNull()
    com.example.nudiconverter.NudiExportFormat format) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public static final java.io.File exportToTxt(@org.jetbrains.annotations.NotNull()
    android.content.Context $this$exportToTxt, @org.jetbrains.annotations.NotNull()
    java.lang.String text) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public static final java.io.File exportToPdf(@org.jetbrains.annotations.NotNull()
    android.content.Context $this$exportToPdf, @org.jetbrains.annotations.NotNull()
    java.lang.String text) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public static final java.io.File exportToDocx(@org.jetbrains.annotations.NotNull()
    android.content.Context $this$exportToDocx, @org.jetbrains.annotations.NotNull()
    java.lang.String text) {
        return null;
    }
    
    public static final boolean saveToFolder(@org.jetbrains.annotations.NotNull()
    android.content.Context $this$saveToFolder, @org.jetbrains.annotations.NotNull()
    android.net.Uri treeUri, @org.jetbrains.annotations.NotNull()
    java.lang.String text, @org.jetbrains.annotations.NotNull()
    com.example.nudiconverter.NudiExportFormat format) {
        return false;
    }
    
    public static final void shareFile(@org.jetbrains.annotations.NotNull()
    android.content.Context $this$shareFile, @org.jetbrains.annotations.NotNull()
    java.io.File file, @org.jetbrains.annotations.NotNull()
    java.lang.String mime) {
    }
}