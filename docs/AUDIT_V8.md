# OZKEY V7 → V8 yakuniy audit hisoboti

## 0. Bajarilgan va BAJARILMAGAN narsalar (halol holat)
| Narsa | Holat |
|---|---|
| `./gradlew testDebugUnitTest` (Kotlin testlar) | **NOT RUN** — Gradle, Kotlin kompilyatori, Android SDK, tarmoq yo'q |
| `assembleDebug` / `assembleRelease` / `bundleRelease` | **NOT RUN** |
| androidTest (`ClipBackupDeviceTest` va boshqalar) | **NOT RUN** — qurilma/emulyator yo'q |
| Real qurilmada 600 000 iteratsiya vaqti | **O'LCHANMAGAN** |
| `gradle-wrapper.jar` ni rasmiy manba bilan solishtirish | **BAJARILMAGAN** (internet yo'q); `tools/verify-wrapper.sh` offline `exit 2` qaytardi |
| Kotlin kodi kompilyatsiyasi | **BAJARILMAGAN**. Faqat qavslar/importlar balansi qo'lda tekshirildi (bu kompilyatsiya emas) |
| Java reference port (`tools/reference-port/ClipBackupPort.java`) | **ISHLATILDI**: `pass=62 fail=0` — lekin bu Kotlin kodi emas, uning qo'lda yozilgan Java nusxasi |
| Python bilan mustaqil fixture'lar (v1, v2, v2-unicode) | yaratildi va ochilishi Python'da tekshirildi |

Reference port natijasi Kotlin testlarining o'rnini BOSMAYDI: port va Kotlin orasida farq (xato) bo'lishi mumkin.

## 1. gradle-wrapper.jar
- Kelib chiqishi **isbotlanmagan**. v5 README'si jarni «Gradle 8.14.3 distributividan olindi» deydi, `distributionUrl` esa 8.11.1.
- Jar tahlili (shubhali alomat topilmadi, lekin bu isbot emas): 33 fayl, faqat wrapper paketlari, `jdeps` bo'yicha faqat `java.base`,
  `Runtime.exec/ProcessBuilder` yo'q, ichki URL'lar faqat `gradle.org/release-checksums` va Apache litsenziyasi.
- `distributionSha256Sum` matn bo'yicha foydalanuvchi bergan qiymatga teng; rasmiy manbadan qayta olinmadi.
- Qilingan ish: `tools/verify-wrapper.sh` (rasmiy checksum bilan solishtirish; tekshirib bo'lmasa `2`, hech qachon yolg'on «OK» emas),
  `tools/regenerate-wrapper.sh` (faqat rasmiy tekshirilgan Gradle 8.11.1 dan; ishonchsiz jar bilan bootstrap qilmaydi),
  `docs/WRAPPER_PROVENANCE.md` (hash'lar, qadamlar). Jar, `gradlew`, `gradlew.bat` V8'da **o'zgartirilmadi**.
- **Qaror sizda:** jarni «ishonchli» deb hisoblash uchun `sh tools/verify-wrapper.sh` ni internet bor joyda `0` bilan tugating yoki qayta yarating.

## 2. ClipBackup.kt — topilmalar
Belgilar: ✅ tuzatildi (V8), ⚠️ ochiq/cheklov, ℹ️ ma'lumot.

| # | Mavzu | Topilma | Holat |
|---|---|---|---|
| 1 | Qo'lda PBKDF2-HMAC-SHA256 | RFC 8018 §5.2 ga mos: `U1=HMAC(P, S‖INT(i))`, `Ui=HMAC(P, Ui-1)`, XOR. RFC 7914 vektori va tizim variantiga solishtirish testi bor (Kotlin: NOT RUN; Java port: o'tdi, 62/62). Chaqiruvchi massivini o'zgartirmaydi; oraliq `u`/`t` massivlari nolga to'ldiriladi. Bo'sh parol rad etiladi | ✅ |
| 2 | API 23–25 | `PBKDF2WithHmacSHA256` API 26 dan; yo'q bo'lsa `GeneralSecurityException` ushlanib qo'lda yo'lga o'tiladi. `Mac(HmacSHA256)`, `Cipher(AES/GCM/NoPadding)`, `GCMParameterSpec`, `updateAAD` API 23 da mavjud. Qurilmada sinalmagan. Qo'lda sikl 600 000 × `Mac.doFinal` (JNI + 32-bayt ajratish) — sekin bo'lishi mumkin | ⚠️ o'lchash kerak |
| 3 | Unicode parollar | ASCII bo'lmagan parol har doim UTF-8 + qo'lda yo'l (provayderlar orasidagi baytlash farqidan qochish). **Yangi:** v2 uchun NFC normalizatsiya (turli klaviaturalar `é` ni `e+◌́` deb yuborishi mumkin); ochishda avval NFC, bo'lmasa xom ko'rinish (V7 da yozilgan fayllar uchun). Python fixture + NFC/NFD testlar. Narxi: Unicode parolning noto'g'ri urinishida KDF ikki marta ishlaydi; Unicode parol API 26+ da ham sekinroq yo'lda | ✅ / ⚠️ tezlik |
| 4 | AES-GCM AAD | Sarlavha (magic+iteratsiya+tuz, 25 bayt) AAD sifatida autentifikatsiya qilinadi; sarlavha baytini o'zgartirish ochishni buzadi (test: bayt 8, 12; AAD'siz ochish muvaffaqiyatsiz). IV AAD'da emas, lekin GCM'ning o'zi ishlatadi. Har fayl uchun yangi tuz → yangi kalit, tasodifiy 96-bit IV: nonce takrorlanishi xavfi amalda yo'q | ✅ |
| 5 | OZKB1 | v6 algoritmi bilan AYNAN bir xil (SHA1, 120 000, parol `toCharArray`, AAD yo'q) — o'zgartirilmagan. Python bilan mustaqil yaratilgan fixture ochiladi (Java port: o'tdi). SHA1/120k hozirgi talablarga nisbatan kuchsiz; faqat o'qiladi, yangi v1 yozilmaydi. Foydalanuvchiga eski zaxirani qayta eksport qilish tavsiya etiladi (avtomatik migratsiya yo'q) | ✅ / ℹ️ |
| 6 | OZKB2 | Format: `OZKB2`+iter(4,BE)+tuz(16)+iv(12)+shifrmatn+tag(16). Mustaqil Python fixture ochiladi, yaratib-ochish testi bor | ✅ |
| 7 | Noto'g'ri parol / buzilgan fayl | `decrypt` hech qachon exception tashlamaydi: bo'sh parol, noma'lum magic, qisqa fayl, tag xatosi — `null`. Test: kesish (12 xil uzunlik), bayt almashtirish, axlat. Noto'g'ri parol va buzilgan faylni ajratib bo'lmaydi (GCM bunday) — toast bitta matn beradi. `OutOfMemoryError` ushlanmaydi (fayl 8 MB bilan cheklangan) | ✅ |
| 8 | Iteratsiya DoS | **Topilma:** V7 da MAX=5 000 000 — sekin telefonda zararli fayl bir necha o'n soniya (qo'lda yo'lda) hisoblatishi mumkin edi. **V8:** MAX=2 000 000 (joriyning ~3.3×), MIN=100 000; tekshiruv KDF'dan OLDIN; `encryptV2` ham chegaralaydi. Orqa oqimda ishlaydi (UI qotmaydi), lekin 2M ham sekin qurilmada sezilarli bo'lishi mumkin | ✅ / ⚠️ |
| 9 | Xotirani tozalash | Biz yaratgan `CharArray` (parol nusxasi), `PBEKeySpec` (`clearPassword`), UTF-8 parol baytlari, olingan kalit baytlari (`SecretKeySpec` o'z nusxasini oladi, asl nusxa tozalanadi), ochilgan/shifrlanadigan matn baytlari, PBKDF2 oraliq massivlari — ishlatilgach nolga to'ldiriladi (`finally`). **Tozalab BO'LMAYDIGAN:** UI'dagi `String` parol (`EditText.text.toString()`, `BuferTab.exportPw`), `Mac`/`Cipher`/`SecretKeySpec` ichki nusxalari, GC ko'chirgan nusxalar, `json: String` va ochilgan `String`. Bu «imkon qadar», kafolat emas | ⚠️ cheklov |
| 10 | Loglar | `ClipBackup.kt` da `Log`/`print` yo'q (test shuni tekshiradi). Qurilma benchmarki faqat vaqt/API loglaydi. Release'da R8 `Log.*` ni olib tashlaydi (README) | ✅ |
| 11 | Bo'sh parol | `encrypt` bo'sh parolni rad etadi (UI ≥ 6 belgi talab qiladi, lekin kod ham himoyalandi) | ✅ |

## 3. Real qurilma (600 000)
`docs/BACKUP_DEVICE_TEST.md`: nega kerak, qanday o'lchash (`connectedDebugAndroidTest --tests ...ClipBackupDeviceTest`, `adb logcat -s OZKEY-BENCH`),
to'ldirilmagan natija jadvali, qaror mezoni. Hech qanday qurilma raqami o'ylab topilmadi. Audit muhitidagi server JVM'ida ~0.2 s —
telefon uchun ma'noga ega emas.

## 4. O'zgargan fayllar (V7 → V8)
- `app/src/main/java/uz/ozkey/keyboard/ClipBackup.kt` — MAX_ITER 2M, NFC + fallback, xotira tozalash, bo'sh parol, tuzilma tozalandi.
- `app/src/test/.../ClipBackupTest.kt` — +Unicode fixture, NFC/NFD, V7-uslubidagi fayl, AAD, iteratsiya chegaralari, bo'sh parol, massiv o'zgarmasligi.
- `app/src/test/.../WrapperConfigTest.kt` — +gradlew/CI `wrapper-validation` tekshiruvi.
- `app/src/androidTest/.../ClipBackupDeviceTest.kt` — YANGI, qurilma benchmarki + moslik.
- `tools/verify-wrapper.sh`, `tools/regenerate-wrapper.sh`, `tools/reference-port/ClipBackupPort.java` — YANGI.
- `docs/WRAPPER_PROVENANCE.md`, `docs/BACKUP_DEVICE_TEST.md`, `docs/AUDIT_V8.md` — YANGI. `README.md` — V8 bo'limi.
- Gradle wrapper fayllari, `build.gradle.kts`, CI, ilova mantig'i (ClipBackup'dan tashqari) — O'ZGARTIRILMAGAN.

## 5. Qolgan xavflar (ustuvorlik tartibida)
1. Kotlin kodi hech qachon kompilyatsiya qilinmagan (smart-cast, overload, `internal` ko'rinishi va h.k. xatolari mumkin). Birinchi navbatda `./gradlew testDebugUnitTest`.
2. Wrapper jar rasmiy tasdiqlanmagan.
3. 600 000 iteratsiya (ayniqsa API 23–25 va Unicode parol yo'li) telefonda o'lchanmagan.
4. v1 (SHA1) zaxiralar kuchsiz KDF bilan qoladi, foydalanuvchi qayta eksport qilmaguncha.
5. Parol `String` sifatida xotirada qoladi (cheklov, §2.9).
