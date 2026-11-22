# ÖZKEY — reflection ishlatilmaydi, maxsus qoida kerak emas.

# Release'da Log chaqiruvlari butunlay olib tashlanadi (foydalanuvchi matni logga tushmasligi uchun qo'shimcha kafolat).
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}

# InputMethodService va Activity/Provider AndroidManifest orqali avtomatik saqlanadi (AGP). Qo'shimcha reflection ishlatilmaydi.
# Foydalanuvchi matni logga tushmasin: println/printStackTrace ham release'da kerak emas.
-assumenosideeffects class java.io.PrintStream {
    public void println(...);
    public void print(...);
}
