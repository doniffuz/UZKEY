# ÖZKEY Android — V8 (V7 auditi)

> DIQQAT: Gradle build, Kotlin kompilyatsiyasi, JVM unit testlar, androidTest va qurilma sinovi BAJARILMAGAN (NOT RUN) — muhitda Gradle/Kotlin/Android SDK/tarmoq/qurilma yo'q. To'liq hisobot: `docs/AUDIT_V8.md`.

- **Wrapper jar:** kelib chiqishi ISBOTLANMAGAN. `tools/verify-wrapper.sh` (rasmiy checksum bilan solishtirish) va `tools/regenerate-wrapper.sh` (rasmiy Gradle 8.11.1 dan qayta yaratish) qo'shildi; `docs/WRAPPER_PROVENANCE.md`. Jar/gradlew o'zgartirilmadi.
- **ClipBackup:** iteratsiya chegarasi 100 000..2 000 000 (V7 da 5 000 000 edi); v2 uchun parol NFC (V7 da normallashtirmasdan yozilgan fayllar ham ochiladi); parol/kalit/ochilgan matn baytlari ishlatilgach nolga to'ldiriladi; bo'sh parol rad etiladi.
- **Testlar:** Unicode/NFC, AAD, chegaralar qo'shildi (NOT RUN); `ClipBackupDeviceTest` (androidTest) — 600 000 iteratsiya vaqti qurilmada o'lchanishi uchun (`docs/BACKUP_DEVICE_TEST.md`, natija jadvali bo'sh).
- `tools/reference-port/ClipBackupPort.java` — algoritmning Java'dagi ma'lumot nusxasi (Kotlin testi EMAS).

# ÖZKEY Android — V7 (wrapper checksum + zaxira shifrlash v2)

> DIQQAT: Gradle build, JVM unit testlar, APK/AAB va qurilma sinovi BAJARILMAGAN (NOT RUN): muhitda Gradle, Kotlin kompilyatori, Android SDK va tarmoq yo'q. Yangi Kotlin kodi va testlar kompilyatsiya qilib ko'rilmagan. Birinchi `./gradlew testDebugUnitTest` va telefonda sinov SHART.

- **Gradle wrapper:** `distributionUrl` = `gradle-8.11.1-all.zip`, `distributionSha256Sum` = `89d4e70e4e84e2d2dfbb63e4daa53e21b25017cc70c37e4eea31ee51fb15098a` (v6 da allaqachon shunday edi, matn bo'yicha tekshirildi; internetsiz rasmiy saytdan qayta hisoblanmadi). `gradle-wrapper.jar` kelib chiqishi isbotlanmagan (SHA-256 `7d3a4ac4de1c32b59bc6a4eb8ecb8e612ccd0cf1ae1e99f66902da64df296172`): CI dagi `gradle/actions/wrapper-validation` uni rasmiy ro'yxat bilan tekshiradi; xohlasangiz o'zingiz `./gradlew wrapper --gradle-version 8.11.1 --distribution-type all` bilan qayta yarating.
- **Zaxira (.ozkb) v2:** `ClipBackup` endi `ClipBackup.kt` da. Yangi zaxiralar: `OZKB2` + iteratsiya(4 bayt) + tuz(16) + iv(12) + AES-256-GCM; kalit PBKDF2-HMAC-SHA256, 600 000 iteratsiya (OWASP); sarlavha GCM AAD bilan himoyalangan; iteratsiya chegarasi 100 000..5 000 000 (V8 da 2 000 000 ga tushirildi). Eski `OZKB1` (PBKDF2-HMAC-SHA1, 120 000) avtomatik aniqlanadi va ochiladi. API 23–25 da `PBKDF2WithHmacSHA256` yo'q — qo'lda HMAC-SHA256 varianti ishlatiladi (ASCII bo'lmagan parollar uchun ham har doim shu, parol UTF-8). Noto'g'ri parol/buzilgan fayl — `null`, crash yo'q. Parol va kalit loglanmaydi.
- **Testlar:** `ClipBackupTest`, `WrapperConfigTest` (NOT RUN).

# ÖZKEY Android — V6 (xavfsizlik + tez yozish + barqarorlik)

> DIQQAT: Gradle build, APK/AAB, androidTest (SQLite) va qurilma sinovi BAJARILMAGAN (Android SDK va tarmoq yo'q). JVM'da sof-mantiq testlari Android stub bilan ishga tushirildi: 53/53 o'tdi. `PerfSmokeTest` raqamlari — JVM (ish stoli) o'lchovi, telefon emas.

**Xavfsizlik:** `network_security_config.xml` (faqat HTTPS, faqat tizim sertifikatlari); tarmoq javoblari hajmi cheklandi va redirect'ga ergashilmaydi (`Net.readCapped`); til paketi ro'yxati faqat https va kod formati tekshiriladi; `onDowngrade` (Bufer bazasi eski versiyaga qaytganda crash qilmasin); import qilingan kategoriya nomi 24 belgigacha; release'da `isDebuggable=false` va R8 orqali `Log.*` olib tashlanadi; wrapper `distributionSha256Sum` (-all) + CI'da `wrapper-validation`.
**Tez yozish:** probel (ACTION_UP da ishlaydi) endi keyingi harf bosilganda tartibni buzmaydi (`PendingSpace`); harfda ortiqcha IPC (`getTextBeforeCursor`) yo'q, probel/Enter/tinishda bitta so'rov; ⌫ uzoq bosilganda 10 ta o'chirishdan keyin tezlashadi, klaviatura qayta qurilsa takrorlash to'xtaydi (avval abadiy qolib ketishi mumkin edi); ekran aylanganda klaviatura qayta quriladi.

# ÖZKEY Android — V5 (GitHub Actions build tayyorligi auditi)

> DIQQAT: haqiqiy build BAJARILMAGAN. Muhitda Android SDK yo'q va tarmoq yopiq (Gradle/AGP yuklanmaydi). `./gradlew testDebugUnitTest`, `assembleDebug`, `assembleRelease`, `bundleRelease`, `connectedDebugAndroidTest` — NOT RUN. APK/AAB yo'q. Quyidagilar faqat statik tekshiruv: Gradle konfiguratsiyasi, workflow YAML sintaksisi, resurs havolalari, Kotlin sintaksisi.

- **Gradle Wrapper qo'shildi** (avval yo'q edi): `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.{jar,properties}`, Gradle 8.11.1. Wrapper jar Gradle 8.14.3 distributividan olindi; `distributionSha256Sum` v7 da qo'yilgan (yuqoriga qarang).
- **AGP 8.7.3 → 8.9.1:** compileSdk/targetSdk 36 AGP 8.9.1 dan boshlab rasman qo'llanadi (8.7.3 faqat "suppress" bilan o'tardi). 8.9.1 uchun Gradle >= 8.11.1 shart; avval workflowda 8.9 turgan edi — bu mos kelmasdi.
- **Workflow (`.github/workflows/build.yml`):** `gradle` (8.9) o'rniga `./gradlew`; `testDebugUnitTest` qadami qo'shildi; `pull_request`, `permissions: contents: read`, timeout, concurrency; release faqat 4 ta sir ham bo'lsa; keystore sirlari `env` orqali beriladi (shell'ga to'g'ridan-to'g'ri qo'yilmaydi) va oxirida o'chiriladi; xatoda test hisobotlari yuklanadi.
- **.gitignore:** `*.p12`, `*.b64`, `keystore.properties`, `signing.properties` qo'shildi.
- **Qiymatlar:** compileSdk 36, targetSdk 36, minSdk 23, versionCode 18 (o'zgartirilmadi), versionName 1.0.0. Test bog'liqliklari: junit 4.13.2, androidx.test core 1.6.1 / ext:junit 1.2.1 / runner 1.6.2.
- `docs/TERMUX_GITHUB.md`: release buyrug'i `./gradlew` ga o'zgardi.

# ÖZKEY Android — V4 (yakuniy xatolar va avto-tuzatish auditi)

> DIQQAT: Android SDK/Gradle wrapper yo'q muhitda bajarildi. `gradle test`, `assembleDebug`, `assembleRelease`, `bundleRelease` — NOT RUN. `ClipStoreTest` (androidTest, SQLite) — NOT RUN. Sof-mantiq testlari (AlphabetTest, AutoCorrectTest, KeyStatsTest, LogicTest*) Gradle'siz, Android stub bilan JUnit orqali ishga tushirildi: 42/42 o'tdi (*LogicTest'dagi 3 ta `ClipStore.looksSensitive` testi SQLite/org.json sababli bu usulda ishlamadi — NOT RUN).

- **Bufer tahriri (`ClipStore.edit/editId`):** tahrirlangan matn boshqa yozuv bilan bir xil bo'lsa, ikki yozuv bitta tranzaksiyada birlashtiriladi (takror yo'q). Ishlab qolgan id — tahrirlangan yozuv; Mahkamlash/Sevimli ikkalasidan biri bo'lsa saqlanadi; kategoriya tahrirlangan yozuvniki (bo'sh bo'lsa ikkinchisiniki); vaqt takror bo'lmasa o'zgarmaydi, birlashganda yangisi. Bo'sh/juda uzun matn va mavjud bo'lmagan id rad etiladi, xatoda hech narsa o'zgarmaydi.
- **Avto-tuzatish xavfsizligi (`WordModel.autoFix`, yangi `AutoCorrect.kt`):** lug'at ~390 so'z, shuning uchun faqat tor holat tuzatiladi: so'z >= 5 harf, lug'atda yo'q, natija O'RNATILGAN lug'atdan (foydalanuvchi o'rgangan so'zlar maqsad emas), qo'shni ikki harf almashgan va nomzod AYNAN bitta. Noaniq bo'lsa tegilmaydi. Havola/email/hashtag/raqam yonidagi, tutuqli, KATTA/aralash harfli va so'z o'rtasidagi so'zlar tegilmaydi. Enter bilan avto-tuzatish ENDI YO'Q (xabar yuborilib, bekor qilib bo'lmasligi uchun). ⌫ bekor qilish va tinish belgisini saqlash o'zgarmadi. Takliflar (suggestions) to'liq saqlandi.
- **C+H → ç:** mantiq o'zgarmadi; IME endi `Alphabet.toNew` ni ishlatadi (testlangan).
- **Statistika (yangi `KeyStats.kt`):** harf, probel, Enter, ⌫ alohida sanaladi (`stat_letters/spaces/enters/backs`), `stat_keys` — hammasi. So'z chegara tugmasida (probel, Enter, . , ! ? ; :) BIR marta sanaladi; chegara ⌫ bilan o'chirilib qayta bosilsa qayta sanalmaydi. Hisoblagichlar har 20 hodisada va klaviatura yopilganda yoziladi. Tarjima rejimida ham probel/Enter sanaladi.
- **Ruxsatlar:** manifestda faqat `INTERNET` (ixtiyoriy onlayn tarjima); `VIBRATE` yo'q, maxfiylik hujjatlari mos. Tebranish `View.performHapticFeedback` orqali.
- **Testlar:** `AutoCorrectTest`, `KeyStatsTest` (yangi), `AlphabetTest` (C+H), `LogicTest` (moslandi), `ClipStoreTest` (edit/pin/fav/tartib).

# ÖZKEY Android — 3-qism (chuqur audit)

> DIQQAT: Gradle/Android SDK/kotlinc yo'q muhitda bajarildi. `gradle test`, `assembleDebug`, `assembleRelease`, `bundleRelease` — NOT RUN. Yozilgan testlar ishga tushirilmagan. Qurilmada sinov o'tkazilmagan.

- **Landscape:** klaviatura balandligi endi ekran balandligining 60% idan oshmaydigan koeffitsient bilan hisoblanadi (`OZKeyInputMethod.computeScreenFactor`, harflar sahifasining haqiqiy balandligidan), pastki chegara 0.5. Emoji, Bufer, Iboralar, Asboblar va Tarjima paneli ham xuddi shu koeffitsientdan foydalanadi (avval ular landscape'da kichraymasdi).
- **Avto-tuzatish:** ⌫ bilan bekor qilganda tinish belgisi (`.`, `,`...) endi o'chib ketmaydi; faqat bo'shliq olib tashlanadi.
- **Statistika:** «Bosilgan tugmalar» endi Space, Enter va ⌫ ni ham hisoblaydi; Enter bilan tugagan so'z ham «Yozilgan so'zlar»ga qo'shiladi.
- **Ruxsatlar:** ishlatilmagan `VIBRATE` ruxsati olib tashlandi (tebranish `View.performHapticFeedback` orqali, ruxsat kerak emas). Maxfiylik hujjatlari (`store/PRIVACY_POLICY.md`, `docs/privacy.html`) moslandi.
- **Testlar (yozildi, ishga tushirilmadi):** `app/src/test/.../AlphabetTest.kt`, `LogicTest.kt` (JVM), `app/src/androidTest/.../ClipStoreTest.kt` (qurilma/emulyator). `app/build.gradle.kts` ga test bog'liqliklari qo'shildi.

# ÖZKEY Android — 2-qism davomi (tekshiruv va tuzatishlar)

> DIQQAT: Gradle wrapper va Android SDK yo'q muhitda bajarildi. `assembleDebug` / `assembleRelease` / `bundleRelease` — NOT RUN. Qurilmada sinov o'tkazilmagan.

- **Maxfiylik kartasi:** «Anthropic Claude» matni endi faqat `Translator.available` (tarjima serveri ulangan) bo'lsagina ko'rsatiladi. Server ulanmagan bo'lsa karta ilova internetga chiqmasligini aytadi. Karta ostiga «Privacy Policy» tugmasi qo'shildi (`https://doniffuz.github.io/UZKEY/privacy.html`); u Haqida va Sozlamalar ekranlarida ko'rinadi.
- **Splash:** `drawable/splash_window.xml` dan logo olib tashlandi (u ilova ichidagi splashdagi logo bilan turli o'lchamda ikki marta chaqnardi); gradient yo'nalishi ilova ichidagi splash bilan moslandi.
- **Hujjatlar:** `store/STORE_TEXTS.md` va README dagi eskirgan «toza Kun/Tun» / «standart Avto» ibora premium mavzu tizimiga moslandi.

# ÖZKEY Android — Premium UI redesign (1-qism)

> DIQQAT: bu o'zgarishlar kompilyatorsiz muhitda yozilgan (Gradle/Android SDK/internet yo'q). `assembleDebug` ishga tushirilmagan va qurilmada sinalmagan. Birinchi `gradle assembleDebug` va telefonda sinov SHART.

- **Dizayn tizimi:** deep navy fon, electric blue/cyan aksent, yumaloq kartalar, yagona ikonka uslubi (`res/drawable/ic_*.xml`), `MainActivity.kt` ichidagi `c*` rang tokenlari + `res/values/colors.xml`, `themes.xml`.
- **Standart mavzu:** `ozdark` (Premium qora, deep navy). `ozlight` — Oq tema. «Rang uslubi»: Blue/Cyan/Purple/Green/Orange/Pink (`accent` sozlamasi) Enter, aktiv Shift va belgilarga qo'llanadi. Eski 32 mavzu «Mavzu» ekranida qoldi.
- **Ikonka:** `tools/make-icons.py` yaratadi (adaptive: foreground + gradient fon + monoxrom; legacy mipmap; store/icon-512.png). Manifest endi `@mipmap/ic_launcher(_round)` ishlatadi.
- **Splash:** Android 12+ tizim splash (`values-v31/themes.xml`), Android 11- uchun `drawable/splash_window.xml`, ilova ichida qisqa (~1 s) animatsiyali splash.
- **Ekranlar:** Asosiy (hero karta + qatorlar), Statistika, Haqida (pastki navigatsiya); Sozlamalar (sarlavhadagi gear), Mavzu, Til/Layout, Clipboard — ichki ekranlar (orqaga tugmasi bilan).
- **Klaviatura:** premium tugmalar, Enter — aksent rangida, aktiv Shift — aksent. Yangi sozlama: `sound` (Tovush), statistika hisoblagichlari (`stat_keys`, `stat_words`).
- **Ç/ç tuzatish:** `Languages.UZ.extra` endi kichik harflarda (`ğ ö ş ç '`); oddiy holatda `ç`, ⇧ bilan `Ç`. Ç/ç tugmasi kengroq (vazn 1.7), qator balandligi 46 dp.

# ÖZKEY Android — 1.0.0 (versionCode 18) — final candidate

> DIQQAT: bu o'zgarishlar kompilyator va qurilmasiz kiritilgan (Android SDK yo'q muhitda). Birinchi `gradle assembleDebug`, qurilmada sinov va Play Console'da «Internal testing» SHART.

## Final candidate: nima tuzatildi
- **Avto-tuzatish xatosi (haqiqiy):** kichik lug'atda (390 so'z) qo'shni-tugma almashtirishi to'g'ri so'zlarni buzardi (`bordik→bordim`, `keldik→keldim`, `ko'rdik→kördim`, `oldim→oldin`, `burun→butun`, `karta→katta`). Endi qo'shni-tugma tuzatishi faqat lug'at ≥ 3000 so'z bo'lganda ishlaydi; harflar o'rni almashgan xatolar (`bugnu→bugun`) avvalgidek tuzatiladi. 223 ta to'g'ri so'zda sinab ko'rildi: 0 ta noto'g'ri tuzatish.
- **Bufer:** Bufer ochilganda oxirgi nusxa qayta qo'shilib, tozalangan/o'chirilgan matn «qaytib kelardi» — tuzatildi.
- **Tugmalar:** Ğ Ö Ş Ç ' qatori 34→42 dp va teng enli; ↵ / 🌐 / 😊 / ?123 yozuvlari 11sp edi (⌫ ⇧ 22sp) — bir xil o'lchamga keltirildi; tepa panel 48→44 dp (umumiy balandlik deyarli o'zgarmadi).
- **Dizayn:** standart mavzu — premium «Qora tema» (`ozdark`), unga mos «Oq tema» (`ozlight`) va 6 ta rang uslubi. «Avto» mavzusi (telefon Kun/Tun rejimiga qarab Qora/Oq premium) ro'yxatda qoldi; Neon/RGB mavzular ham saqlangan.
- **Play:** `targetSdk/compileSdk 36` (Play 2026-08-31 dan API 36 talab qiladi — 35 bilan yuklab bo'lmasdi); kalitsiz `bundleRelease/assembleRelease` endi to'xtaydi (avval jimgina debug kalit bilan imzolanardi); `versionCode 18`.
- **Klaviatura sozlamalari:** tizimdagi klaviatura sozlamasi tugmasi endi ÖZKEY ilovasini ochadi (`settingsActivity`).
- **AAB'da APK ulashish:** Play o'rnatishida APK bo'laklarga bo'linadi, shuning uchun «APKni ulashish» o'rniga do'kon havolasini ulashish ko'rsatiladi (havola `ozkeyStoreUrl` da).
- **Maxfiylik:** siyosat va ilova kartasi tarjima xizmati (Anthropic) va serverdagi IP bo'yicha kunlik limit haqida aniq yozadigan bo'ldi; `STORE_TEXTS.md`dagi eskirgan versiya/«tarjima yo'q» yozuvi tuzatildi.

## Tegilmagan, lekin bilish kerak
- So'z lug'ati juda kichik (390 so'z): takliflar cheklangan. Qo'shni-tugma avto-tuzatish lug'at kattalashganda (≥3000) o'zi yoqiladi.
- targetSdk 36 uchun AGP 8.7.3 + `android.suppressUnsupportedCompileSdk=36`. CI muammo bersa AGP 8.9.1+ va Gradle 8.11.1+ ga ko'taring.



> DIQQAT: bu o'zgarishlar kompilyator va qurilmasiz yozilgan. Birinchi `gradle assembleDebug` va telefonda sinov SHART. Sinovdan o'tmagan narsa «ishlaydi» deb hisoblanmasin.

## 1-qism: majburiy Telegram obunasi olib tashlandi (versiya raqami o'zgarmadi: 1.0.0 / 17)
- Obuna oynasi, obunani tekshirish, kunlik qayta tekshiruv, «Telegram bog'lanishini o'chirish» tugmasi va obuna serveriga barcha chaqiruvlar olib tashlandi. `Subscription.kt` va `backend/subscribe/` o'chirildi; `ozkeySubscribeUrl` / `SUBSCRIBE_URL` olib tashlandi.
- **INTERNET ruxsati qaytarildi** (onlayn tarjima/backend uchun kerak). `ozkeyTranslateUrl` va `ozkeyPacksUrl` bo'sh bo'lsa ilova internetga chiqmaydi. Obuna/Telegram tekshiruvi uchun hech qanday so'rov yo'q.
- Telegram faqat ixtiyoriy havola sifatida qoldi: Sozlama → Ilova haqida → @Doniku_Admin, @OZKEY_Official. (@OZKEY_YordamchiBot tugmasi olib tashlandi: bot faqat obuna tasdiqlash uchun edi.)
- `Tr.kt` (3 tilli matnlar infratuzilmasi) saqlandi, faqat obuna matnlari olib tashlandi.
- Maxfiylik siyosati (`store/`, `docs/privacy.html`), do'kon matni va ilova ichidagi maxfiylik kartasi INTERNET ruxsatining ixtiyoriy onlayn tarjima uchun ekanini aytadigan qilib yangilandi.
- Tarjima (`backend/worker.js`) saqlandi; `ozkeyTranslateUrl` berilmaguncha o'chiq. `ozkeyPacksUrl` va `Languages.fetchRemoteIndex/installRemote` hozircha hech qaysi ekranga ulanmagan (faqat APK ichidagi til paketlari ishlaydi).

> DIQQAT: bu o'zgarishlar Android SDK bo'lmagan muhitda kiritilgan. `gradle assembleDebug` va qurilmada sinov hali bajarilmagan.

# ÖZKEY Android — 0.10.0 (versionCode 16)

> DIQQAT: bu o'zgarishlar kompilyator va qurilmasiz yozilgan (Gradle/Android SDK bo'lmagan muhitda). Birinchi `gradle assembleDebug` va qurilmada sinov SHART.

- **0.10.0 qo'shimchalar:** Text Fix `!!`, `??`, `..` ni normallashtiradi; Smart Reply'ga «Ha / Yo'q / Keyinroq / Salom / Qalaysiz» qoidalari; Sozlamada «Shaxsiy lug'atni ko'rish / so'z o'chirish»; `words_uz.txt` va `bigrams_uz.txt` dublikatlardan tozalandi (405 → 390 so'z, 91 bigram).
- **✍️ Matnni tuzatish (Text Fix)** — 🔧 panelida. Internetsiz, qoidalar asosida: ortiqcha bo'shliq, tinish belgilari, salomdan keyin vergul, savol belgisi, bosh harf, aniq (noaniq emas) xatolar. Natija avval ko'rsatiladi, «✓ Qo'llash» bosilgandagina maydon o'zgaradi.
- **⚡ Iboralar kategoriyalari:** Umumiy, Ariza, Maktab, Ish, Telegram, Savdo, Tabassum + **Mening** (o'zingiz maydondan saqlaysiz, ✕ bilan o'chirasiz). Shablondagi [joy] qo'yilgach belgilanadi.
- **🔢 Raqam/Email tez tugmalari** (?123 sahifasida va asboblarda): +998, @, .com, .uz, https://, gmail.com, www., 📅 sana, 🕒 vaqt.
- **💬 Smart Reply:** maydon bo'sh bo'lganda va yaqinda xabar NUSXALANGAN bo'lsa, takliflar panelida qisqa javoblar. Sozlamada o'chiriladi.
- **Shaxsiy so'zlar** endi lug'at so'zlaridan yuqoriga chiqadi (ko'p yozilsa).
- **Maxfiylik:** ilova «inkognito/shaxsiylashtirmang» (IME_FLAG_NO_PERSONALIZED_LEARNING) yoki «takliflarsiz» deb so'ragan maydonlarda takliflar, avto-tuzatish va o'rganish o'chadi.

# ÖZKEY Android — 0.9.0

- **Do'konlarga mos build:** tarjima funksiyasi o'chirildi (rasmiy bo'lmagan Google manzili olib tashlandi). Tarjima tugmalari `ozkeyTranslateUrl` berilmaguncha ko'rinmaydi.
- **INTERNET ruxsati olib tashlandi** — ilova internetga chiqmaydi; maxfiylik siyosati va do'kon matnlari shunga moslandi.
- Release build o'z kalitingiz bo'lmasa **to'xtaydi** (debug kalit bilan imzolangan APK do'konga yuklanmasin). `tools/make-keystore.sh` + 4 ta GitHub siri.
- Tarjimani qaytarish: `backend/worker.js` ni joylang, `gradle.properties` ga `ozkeyTranslateUrl=https://...` yozing, AndroidManifest.xml ga INTERNET ruxsatini qaytaring, maxfiylik siyosatini yangilang.

# ÖZKEY Android — 0.8.0

## 0.8.0 — Aqlli yozish
- **So'z takliflari:** klaviatura tepasida 3 tagacha taklif (yozayotgan so'zni tugatish, probeldan keyin keyingi so'z). Lug'at: `assets/words_uz.txt`, `assets/bigrams_uz.txt` (kengaytirish mumkin). O'zingiz yozgan so'zlarni o'rganadi
- **Avto-tuzatish (xavfsiz):** faqat qo'shni harflar almashgan yoki qo'shni tugma bosilgan aniq xatolar tuzatiladi; probeldan keyin ⌫ bossangiz asl so'z qaytadi va u qayta tuzatilmaydi
- **Smart Touch:** tugma chetiga bosilganda, yozilgan harf hech bir so'zga olib bormasa-yu qo'shni harf olib borsagina, qo'shni tanlanadi
- **Bekor qilish / Qayta qilish** tugmalari (⚙ asboblar paneli)
- Sozlamada uchalasini o'chirish va o'rganilgan so'zlarni tozalash. Hammasi faqat telefonda; parol/email/havola maydonlarida o'rganilmaydi
- Yangi o'zbek alifbosi va mavjud funksiyalar o'zgarmagan

# ÖZKEY Android — 0.7.0

O'zbek klaviaturasi (Kotlin, `InputMethodService`). Dasturchi: **Donyor**.

## 0.7.0 — Bufer (professional clipboard)
- **Tarix | ⭐ Sevimlilar | 📌 Mahkamlangan** bo'limlari (klaviaturada va ilovadagi yangi «Bufer» sahifasida)
- Tarix 30 → **300 ta**, bitta matn 3000 → **20 000 belgi**; baza SQLite (tez, ro'yxat faqat qisqa ko'rinish oladi — RAM tejaladi). Eski qadalganlar avtomatik ⭐ + 📌 bo'ladi
- **Sevimli / Mahkamlangan / kategoriyadagi** matnlar avtomatik va «Tarixni tozalash» da o'chmaydi; o'chirishda «Bu matnni o'chirmoqchimisiz?» so'raladi
- **Qidiruv**, **tahrirlash**, **to'liq matn oynasi** (Nusxa / Tahrirlash / Sevimli / Ulashish / O'chirish), **kategoriyalar** (+ o'zingiznikini qo'shish)
- **Eksport / Import:** faqat saqlangan matnlar, parol bilan AES-256-GCM shifrlangan `.ozkb` fayl (v2: PBKDF2-HMAC-SHA256; eski v1 ham ochiladi; parol saqlanmaydi)
- **Maxfiylik:** OTP kod va karta raqamiga o'xshash nusxalar saqlanmaydi (Sozlamadan o'chiriladi)

## 0.6.2
- Sozlamada «Ilova haqida» kartasi: dasturchi **Donyor**, Telegram **@Doniku_Admin** tugmasi (taklif/xato yozish uchun)
- Maxfiylik siyosatidagi aloqa: Telegram @Doniku_Admin

## 0.6.1 (qulaylik va tezlik)
- **Tugmalar kattaroq:** harflar 24sp, qatorlar balandroq; tepa panel va chetlardagi «o'lik zona» kamaytirildi
- **Bo'shliq tasodifan bosilmaydi:** barmoq tugma ICHIDA ko'tarilgandagina yoziladi, atrofida kichik bo'sh joy bor
- **Tezlik:** shift/avto-katta harf klaviaturani qayta qurmaydi (faqat yozuvlar almashadi); sh/ch/g'/o' tekshiruvi faqat kerak joyda
- **Bufer:** yangi nusxa tepa panelda taklif chipi bo'lib chiqadi (bir bosishda qo'yiladi), sarlavhada soni; ★ qadalganlar tepada
- **Dark + Light:** hamma mavzular joyida + yangi «Avto» (telefon Kun/Tun rejimiga mos)
- **Soddalashtirish:** tepa panel 6 ta tugma, asboblar 8 ta katta tugma, ilovada 3 ta bo'lim (Bosh / Mavzular / Sozlama), kirish 2 sahifa
- Tarjima va Lotin ⇄ Kirill o'zgarishsiz saqlangan

## 0.6.0 yangiliklari
- **Yangi dizayn:** o'zbekona naqsh (girih yulduz), feruza–lojuvard–oltin ranglar, yangi ikonka
- **Tarjima paneli:** yozayotganda jonli tarjima, ✓ — tarjimani yozadi, «Asl + tarjima» — ikkalasini; panel belgilar/emoji sahifalarida ham saqlanadi
- **📋 Bufer tarixi:** tepa panelning eng chap chetida; bosib qo'yish, ★ qadash, ✕ o'chirish; Sozlamadan o'chirish mumkin
- **Harflar kattalashtirildi** + «Harflar o'lchami» va «Balandlik» sozlamalari
- **32 ta mavzu** (hammasi bepul) + **galereyadan rasm fon** (xiralik sozlanadi)
- Premium / API kalit / server tillari kabi keraksiz qismlar olib tashlandi
- Uptodown uchun tayyor: release APK (CI), `store/UPTODOWN.md`, maxfiylik siyosati `docs/privacy.html`

## Ishga tushirish
1. GitHub'ga push qiling → Actions → `OZKEY-build` artifact (debug APK, release APK, AAB).
2. Release'ni o'z kalitingiz bilan imzolash: `bash tools/make-keystore.sh` (bir marta) va 4 ta GitHub sirini kiriting.
3. Ilovani oching → ÖZKEY ni yoqing va tanlang.
4. Tarjima serveri (ixtiyoriy, tavsiya): `backend/worker.js`, URL ni `gradle.properties` ga `ozkeyTranslateUrl=` deb yozing.

## Papkalar
- `app/` — ilova kodi (`OZKeyInputMethod.kt` — klaviatura, `MainActivity.kt` — sozlash ilovasi, `Themes.kt`, `Clips.kt` — Bufer bazasi, `BuferTab.kt` — ilovadagi Bufer sahifasi, `ClipBackup.kt` — zaxira shifrlash, `Ornament.kt`)
- `store/` — ikonka, banner, do'kon matnlari, maxfiylik siyosati
- `docs/privacy.html` — GitHub Pages uchun maxfiylik sahifasi
- `backend/` — tarjima serveri (Cloudflare Worker)
