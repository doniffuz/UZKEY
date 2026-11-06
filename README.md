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
