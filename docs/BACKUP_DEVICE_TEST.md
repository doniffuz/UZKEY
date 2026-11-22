# Zaxira (OZKB2, PBKDF2-HMAC-SHA256 × 600 000) — real qurilmada sinov

**Holat: O'LCHANMAGAN.** Hech bir real telefon/emulyatorda sinov bajarilmagan. Quyida ko'rsatilgan raqamlar yo'q —
jadvalni siz to'ldirasiz. (Audit muhitidagi JVM'da 600 000 iteratsiya ~0.2 s chiqdi, lekin bu server JVM'i; telefonga
ekstrapolyatsiya qilinmaydi.)

## Nega kerak
- `ITER_V2 = 600 000` OWASP tavsiyasi, lekin telefondagi vaqt noma'lum. Eksport/import orqa oqimda ishlaydi (UI qotmaydi),
  ammo juda uzoq kutish foydalanuvchini «ilova osilib qoldi» deb o'ylashga olib keladi.
- API 23–25 da tizim `PBKDF2WithHmacSHA256` yo'q → qo'lda `Mac` sikli (JNI chaqiruvi har iteratsiyada) — eng xavfli yo'l.
- ASCII bo'lmagan parol (`o‘`, kirill, `é`) har doim shu qo'lda yo'ldan o'tadi (API 26+ da ham).

## Qanday o'lchash
Emulyator/telefon ulang (USB-debugging), so'ng:
```
./gradlew connectedDebugAndroidTest --tests "uz.ozkey.keyboard.ClipBackupDeviceTest"
adb logcat -d -s OZKEY-BENCH
```
Test faqat vaqt va API darajasini loglaydi (parol/kalit/matn loglanmaydi). Kamida shu qurilmalarda takrorlang:
eng arzon/eski telefon, o'rtacha telefon, API 23–25 (emulyator yetarli, lekin real qurilma yaxshiroq), API 26+.

## Natija jadvali (to'ldirilmagan)
| Qurilma | API | ASCII encrypt | ASCII decrypt | Unicode encrypt | Unicode decrypt | Izoh |
|---|---|---|---|---|---|---|
| ? | ? | ? | ? | ? | ? | |

## Qaror mezoni (taklif, sizniki hal qiladi)
- ≤ ~2 s: qoldiring. ~2–5 s: toast «bir necha soniya» yetarli. > ~5 s eng sekin maqsadli qurilmada: `ITER_V2` ni tushirish
  (format iteratsiyani faylga yozadi, shuning uchun eski v2 fayllar ochilaveradi; pastki chegara `MIN_ITER_V2 = 100 000`) yoki
  progress ko'rsatish; unicode yo'li uchun tezroq usul (masalan native PBKDF2) ko'rib chiqiladi.
- Kamayishdan oldin xavfsizlik evaziga ekanini hujjatlang (iteratsiya kamaysa brute-force osonlashadi).

## Qo'shimcha qo'lda tekshiruv
1. Bir qurilmada zaxira yarating, boshqasida (boshqa API) oching — ASCII va unicode parol bilan.
2. v6 (OZKB1) bilan yaratilgan haqiqiy faylni v8 ilovada oching.
3. Noto'g'ri parol / kesilgan fayl: toast chiqadi, ilova yopilmaydi.
