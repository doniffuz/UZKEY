# ÖZKEY — reflection ishlatilmaydi, maxsus qoida kerak emas.

# Release'da Log chaqiruvlari butunlay olib tashlanadi (foydalanuvchi matni logga tushmasligi uchun qo'shimcha kafolat).
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}
