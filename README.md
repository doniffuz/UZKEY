# ÖZKEY Android — 1.0.0 (versionCode 18) — final candidate

> DIQQAT: bu o'zgarishlar kompilyator va qurilmasiz kiritilgan (Android SDK yo'q muhitda). Birinchi `gradle assembleDebug`, qurilmada sinov va Play Console'da «Internal testing» SHART.

## Final candidate: nima tuzatildi
- **Avto-tuzatish xatosi (haqiqiy):** kichik lug'atda (390 so'z) qo'shni-tugma almashtirishi to'g'ri so'zlarni buzardi (`bordik→bordim`, `keldik→keldim`, `ko'rdik→kördim`, `oldim→oldin`, `burun→butun`, `karta→katta`). Endi qo'shni-tugma tuzatishi faqat lug'at ≥ 3000 so'z bo'lganda ishlaydi; harflar o'rni almashgan xatolar (`bugnu→bugun`) avvalgidek tuzatiladi. 223 ta to'g'ri so'zda sinab ko'rildi: 0 ta noto'g'ri tuzatish.
- **Bufer:** Bufer ochilganda oxirgi nusxa qayta qo'shilib, tozalangan/o'chirilgan matn «qaytib kelardi» — tuzatildi.
- **Tugmalar:** Ğ Ö Ş Ç ' qatori 34→42 dp va teng enli; ↵ / 🌐 / 😊 / ?123 yozuvlari 11sp edi (⌫ ⇧ 22sp) — bir xil o'lchamga keltirildi; tepa panel 48→44 dp (umumiy balandlik deyarli o'zgarmadi).
- **Dizayn:** standart mavzu endi «Avto» (telefon Kun/Tun rejimiga qarab Yorug'/Qora). Neon/RGB mavzular ro'yxatda qoldi. (Mavzuni avval tanlagan foydalanuvchilarga ta'sir qilmaydi.)
- **Play:** `targetSdk/compileSdk 36` (Play 2026-08-31 dan API 36 talab qiladi — 35 bilan yuklab bo'lmasdi); kalitsiz `bundleRelease/assembleRelease` endi to'xtaydi (avval jimgina debug kalit bilan imzolanardi); `versionCode 18`.
- **Klaviatura sozlamalari:** tizimdagi klaviatura sozlamasi tugmasi endi ÖZKEY ilovasini ochadi (`settingsActivity`).
- **AAB'da APK ulashish:** Play o'rnatishida APK bo'laklarga bo'linadi, shuning uchun «APKni ulashish» o'rniga do'kon havolasini ulashish ko'rsatiladi (havola `ozkeyStoreUrl` da).
- **Maxfiylik:** siyosat va ilova kartasi tarjima xizmati (Anthropic) va serverdagi IP bo'yicha kunlik limit haqida aniq yozadigan bo'ldi; `STORE_TEXTS.md`dagi eskirgan versiya/«tarjima yo'q» yozuvi tuzatildi.

## Tegilmagan, lekin bilish kerak
- So'z lug'ati juda kichik (390 so'z): takliflar cheklangan. Qo'shni-tugma avto-tuzatish lug'at kattalashganda (≥3000) o'zi yoqiladi.
- Sozlamalar ekrani (MainActivity) faqat yorug' mavzuda; klaviaturaning o'zi Kun/Tun'ga moslashadi.
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
- **Eksport / Import:** faqat saqlangan matnlar, parol bilan AES-256 shifrlangan `.ozkb` fayl (parol saqlanmaydi)
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
- `app/` — ilova kodi (`OZKeyInputMethod.kt` — klaviatura, `MainActivity.kt` — sozlash ilovasi, `Themes.kt`, `Clips.kt` — Bufer bazasi, `BuferTab.kt` — ilovadagi Bufer sahifasi + zaxira, `Ornament.kt`)
- `store/` — ikonka, banner, do'kon matnlari, maxfiylik siyosati
- `docs/privacy.html` — GitHub Pages uchun maxfiylik sahifasi
- `backend/` — tarjima serveri (Cloudflare Worker)
