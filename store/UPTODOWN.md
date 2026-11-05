# ÖZKEY — Uptodown / do'konlarga joylash to'plami

Hammasi shu yerda: matnlar (o'zbekcha, inglizcha, ruscha), nima yuklash kerak va tekshiruv ro'yxati.
Bu matnlar Play Market, RuStore, APKPure va boshqa do'konlar uchun ham mos.

---

## 1. Yuklash uchun fayllar
| Nima | Qayerda |
|---|---|
| **Imzolangan APK** (Uptodown shuni so'raydi) | GitHub Actions → Artifacts → `OZKEY-build` → `app/build/outputs/apk/release/app-release.apk` |
| AAB (faqat Play Market uchun) | xuddi shu artifact → `bundle/release/app-release.aab` |
| Ikonka 512×512 | `store/icon-512.png` |
| Banner 1024×500 | `store/feature-graphic-1024x500.png` |
| Skrinshotlar | O'zingiz olasiz (pastda ro'yxat) |
| Maxfiylik siyosati | `docs/privacy.html` ni GitHub Pages ga qo'ying → havola shu bo'ladi |

> ⚠️ **Imzo.** Avval `bash tools/make-keystore.sh` ni BIR marta ishga tushiring va 4 ta sirni GitHub'ga qo'shing.
> Aks holda release APK *debug* kalit bilan imzolanadi — Uptodown'da qabul qilinishi mumkin, lekin keyin kalitni almashtirsangiz
> foydalanuvchilar yangilay olmaydi. `ozkey.jks` va parolni yo'qotmang.

## 2. Ilova ma'lumotlari
- **Nomi:** ÖZKEY — O'zbek klaviaturasi
- **Paket (package):** `uz.ozkey.keyboard`
- **Versiya:** 0.6.0 (versionCode 7)
- **Dasturchi:** Donyor
- **Kategoriya:** Vositalar (Tools) / Shaxsiylashtirish → Klaviaturalar
- **Yosh chegarasi:** Hammaga (3+)
- **Tillar:** O'zbekcha (asosiy), English, Русский
- **Kalit so'zlar:** o'zbek klaviatura, uzbek keyboard, lotin kirill, tarjima, bufer, klaviatura mavzulari, ўзбек клавиатура

## 3. Qisqa tavsif (80 belgigacha)
- **UZ:** `Lotin va kirill o'zbek klaviaturasi: jonli tarjima, bufer, 30+ mavzu` 
- **EN:** `Uzbek keyboard: Latin & Cyrillic, live translate, clipboard, 30+ themes`
- **RU:** `Узбекская клавиатура: латиница и кириллица, перевод, буфер, 30+ тем`

## 4. To'liq tavsif — O'zbekcha
**ÖZKEY — o'zbek tilida tez, qulay va chiroyli yozing.**

ÖZKEY — o'zbek tili uchun maxsus yaratilgan klaviatura. Lotin va kirill alifbosi bir joyda, tarjima va bufer esa yozayotganda qo'l ostida.

**Nimalar bor:**
• 🔤 **Lotin ⇄ Kirill** — bitta tugma bilan almashtiring (O'z | Ўз). Ğ, Ö, Ş, Ç va Ў, Қ, Ғ, Ҳ harflari tayyor.
• 🌍 **Jonli tarjima** — yozayotganingizda tarjimasi pastda ko'rinadi. ✓ bosing — tarjima matnga yoziladi, yoki «Asl + tarjima» bilan ikkalasini birga yuboring. O'zbekcha, Ўзбекча, ruscha va inglizcha.
• 📋 **Bufer tarixi** — nusxalagan matnlaringiz saqlanadi. Keyin bosib qo'yasiz. Kerakligini ★ bilan qadab qo'ying.
• 🎨 **30 dan ortiq mavzu** — Samarqand, Registon, Buxoro, Atlas, neon RGB, oq, qora va boshqalar.
• 🖼 **O'z rasmingiz** — galereyadan istalgan rasmni klaviatura foni qiling.
• 😊 Emoji, tez iboralar («Assalomu alaykum», «Rahmat»…), bir qo'lda yozish rejimi.
• 🔠 Harflar o'lchami va klaviatura balandligini o'zingizga moslang.
• 🔒 Parol, email va havola maydonlarida avto-almashtirish va tarjima o'chirilgan.

**Maxfiylik:** yozganingiz serverga yuborilmaydi. Bufer tarixi va rasm faqat telefoningizda turadi. Internet faqat tarjima uchun ishlatiladi.

**Qanday yoqiladi:** ilovani oching → «ÖZKEY ni yoqing» → «Klaviaturani tanlang». Hammasi shu.

Dasturchi: **Donyor** 🇺🇿

## 5. To'liq tavsif — English
**ÖZKEY — type Uzbek fast, comfortably and in style.**

ÖZKEY is a keyboard built for the Uzbek language. Latin and Cyrillic live side by side, with translation and clipboard always at hand.

**Features:**
• 🔤 **Latin ⇄ Cyrillic** — switch with one tap. Ğ, Ö, Ş, Ç and Ў, Қ, Ғ, Ҳ are built in.
• 🌍 **Live translation** — see the translation while you type. Tap ✓ to insert it, or "Original + translation" to send both. Uzbek (Latin/Cyrillic), Russian and English.
• 📋 **Clipboard history** — copied texts are kept so you can paste them later. Pin favorites with ★.
• 🎨 **30+ themes** — Samarkand, Registan, Bukhara, Atlas, neon RGB, white, black and more.
• 🖼 **Your own photo** — use any picture from your gallery as the keyboard background.
• 😊 Emoji, quick phrases, one-handed mode, adjustable key height and letter size.
• 🔒 Auto-replace and translation are disabled in password, email and URL fields.

**Privacy:** what you type is not sent to any server. Clipboard history and your background photo stay on your phone. The internet is used only for translation.

Developer: **Donyor** 🇺🇿

## 6. To'liq tavsif — Русский
**ÖZKEY — печатайте по-узбекски быстро, удобно и красиво.**

ÖZKEY — клавиатура, созданная для узбекского языка. Латиница и кириллица в одном месте, перевод и буфер обмена всегда под рукой.

**Возможности:**
• 🔤 **Латиница ⇄ Кириллица** — переключение одним нажатием. Ğ, Ö, Ş, Ç и Ў, Қ, Ғ, Ҳ уже есть.
• 🌍 **Перевод на лету** — перевод появляется, пока вы печатаете. Нажмите ✓ — он вставится в текст, либо «Оригинал + перевод».
• 📋 **История буфера обмена** — скопированное сохраняется. Закрепляйте нужное ★.
• 🎨 **30+ тем** — Самарканд, Регистан, Бухара, Атлас, неон RGB, белая, чёрная и другие.
• 🖼 **Своё фото** — любой снимок из галереи как фон клавиатуры.
• 😊 Эмодзи, быстрые фразы, режим одной руки, настройка высоты и размера букв.
• 🔒 В полях пароля, email и ссылок автозамена и перевод отключены.

**Конфиденциальность:** набранный текст не отправляется на сервер. История буфера и фото хранятся только на телефоне. Интернет нужен лишь для перевода.

Разработчик: **Donyor** 🇺🇿

## 7. Nima yangi (0.6.0)
**UZ**
• Yangi dizayn: o'zbekona naqshlar va ranglar
• Jonli tarjima paneli, «Asl + tarjima»
• 📋 Bufer tarixi (tepa panelning chap chetida)
• 30 dan ortiq mavzu va galereyadan rasm fon
• Harflar kattalashtirildi, harf o'lchami sozlanadi
• Kichik xatolar tuzatildi

**EN**
• New Uzbek-style design
• Live translation panel with "Original + translation"
• 📋 Clipboard history (far-left of the top bar)
• 30+ themes and a gallery photo as background
• Larger letters, adjustable letter size
• Bug fixes

## 8. Skrinshotlar (kamida 4 ta, telefon ekrani 1080×1920 atrofida)
1. Ilovaning bosh sahifasi (feruza sarlavha, «Ulash — 2 qadam»)
2. Chatda klaviatura ochiq — Samarqand mavzusi
3. Tarjima paneli: tepada yozilgan matn, ostida jonli tarjima
4. Bufer oynasi (📋) — bir nechta nusxa, ★ bilan
5. Mavzular sahifasi
6. Galereya rasmi fon bo'lgan klaviatura

## 9. Yuborishdan oldin tekshiruv
- [ ] `tools/make-keystore.sh` → GitHub sirlari kiritildi
- [ ] Release APK telefonga o'rnatilib sinab ko'rildi (yoqish, yozish, 📋, tarjima, rasm fon)
- [ ] Maxfiylik siyosatidagi `[email manzilingiz]` o'rniga o'z emailingiz yozildi va `docs/privacy.html` internetga qo'yildi (GitHub Pages)
- [ ] Skrinshotlar tayyor
- [ ] Tarjima serveri: `gradle.properties` ga `ozkeyTranslateUrl=` yozilsa, tarjima o'z serveringiz orqali ishlaydi (tavsiya etiladi). Yozilmasa — zaxira bepul xizmat ishlatiladi, u rasmiy emas va to'xtab qolishi mumkin.
