# --- PDFium native bridge (com.shockwave.pdfium) ---
# The prebuilt .so libraries resolve JNI symbols by exact class/method name
# (e.g. Java_com_shockwave_pdfium_PdfiumCore_nativeOpenDocument), and native
# code constructs Size via its (II)V constructor. Keep them so a minifying
# consumer app cannot rename or strip the native interface.
-keep class com.shockwave.pdfium.** { *; }
-keepclasseswithmembernames class com.shockwave.pdfium.** {
    native <methods>;
}
