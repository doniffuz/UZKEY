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
- **Versiya:** 0.8.0 (versionCode 11)
- **Dasturchi:** Donyor
- **Kategoriya:** Vositalar (Tools) / Shaxsiylashtirish → Klaviaturalar
- **Yosh chegarasi:** Hammaga (3+)
- **Tillar:** O'zbekcha (asosiy), English, Русский
- **Kalit so'zlar:** o'zbek klaviatura, uzbek keyboard, lotin kirill, tarjima, bufer, klaviatura mavzulari, ўзбек клавиатура

## 3. Qisqa tavsif (80 belgigacha)
- **UZ:** `O'zbek klaviaturasi: so'z takliflari, tarjima, bufer, Lotin va Kirill`
- **EN:** `Uzbek keyboard: word suggestions, live translate, clipboard, Latin & Cyrillic`
- **RU:** `Узбекская клавиатура: подсказки слов, перевод, буфер, латиница и кириллица`

## 4. To'liq tavsif — O'zbekcha
**ÖZKEY Keyboard 🇺🇿**
**O‘zbekcha yozuv — yangi daraja.**

ÖZKEY — o‘zbek tilida qulay, zamonaviy va tez yozish uchun yaratilgan Android klaviatura.
Ilova yangi o‘zbek alifbosini qo‘llab-quvvatlaydi va yozishni yanada qulay qilishga qaratilgan.

**Nimalar bor:**
• ✨ **So'z takliflari** — yozayotganda 3 tagacha taklif, probeldan keyin keyingi so'z. Ko'p yozgan so'zlaringizni o'rganadi.
• ✅ **Xavfsiz avto-tuzatish** — faqat aniq xatolarni tuzatadi; ⌫ bosing — asl so'z qaytadi. Sozlamadan o'chiriladi.
• 🎯 **Smart Touch** — tugma chetiga tegib ketsangiz, to'g'ri harfni aniqlaydi.
• ↶ ↷ **Bekor qilish / Qayta qilish** tugmalari.
• 🔤 **Lotin ⇄ Kirill** — bitta tugma bilan almashtiring (O'z | Ўз). Ğ, Ö, Ş, Ç va Ў, Қ, Ғ, Ҳ harflari tayyor.
• 🌍 **Jonli tarjima** — yozayotganingizda tarjimasi pastda ko'rinadi. ✓ bosing — tarjima matnga yoziladi, yoki «Asl + tarjima» bilan ikkalasini birga yuboring. O'zbekcha, Ўзбекча, ruscha va inglizcha.
• 📋 **Bufer** — 300 tagacha nusxa tarixi, ⭐ Sevimlilar (o'chmaydi), 📌 mahkamlash, kategoriyalar, qidiruv, tahrirlash va parol bilan shifrlangan zaxira (Eksport/Import).
• 🎨 **30 dan ortiq mavzu** — Samarqand, Registon, Buxoro, Atlas, neon RGB, oq, qora, Avto (Kun/Tun) va boshqalar.
• 🖼 **O'z rasmingiz** — galereyadan istalgan rasmni klaviatura foni qiling.
• 😊 Emoji, tez iboralar («Assalomu alaykum», «Rahmat»…), bir qo'lda yozish rejimi.
• 🔠 Harflar o'lchami va klaviatura balandligini o'zingizga moslang.
• 🔒 Parol, email va havola maydonlarida avto-almashtirish va tarjima o'chirilgan.

**Maxfiylik:** yozganingiz serverga yuborilmaydi. O'rganilgan so'zlar, bufer tarixi va rasm faqat telefoningizda turadi. Internet faqat tarjima uchun ishlatiladi.

**Qanday yoqiladi:** ilovani oching → «ÖZKEY ni yoqing» → «Klaviaturani tanlang». Hammasi shu.

👨‍💻 **Dasturchi**
Donyor

ÖZKEY — foydalanuvchilar uchun foydali va qulay bo‘ladigan mahsulotlar yaratish maqsadida ishlab chiqilmoqda.
Sizda taklif, xato yoki yangi g‘oya bo‘lsa, bizga yozishingiz mumkin.

📩 Telegram: @Doniku_Admin

Sizning fikringiz ÖZKEY'ni yanada yaxshilashga yordam beradi.

🚀 **Kelajakdagi maqsad**
ÖZKEY'ni yanada qulay, tezkor va foydali qilish hamda o‘zbek tilidagi raqamli mahsulotlarni ko‘paytirish.

ÖZKEY — O‘zbekcha yozuv, yangi avlod!

## 5. To'liq tavsif — English
**ÖZKEY — type Uzbek fast, comfortably and in style.**

ÖZKEY is a keyboard built for the Uzbek language. Latin and Cyrillic live side by side, with translation and clipboard always at hand.

**Features:**
• ✨ **Word suggestions** — up to 3 suggestions while typing, plus next-word prediction. Learns the words you use most.
• ✅ **Safe auto-correct** — fixes only clear typos; press ⌫ to restore your word. Can be turned off.
• 🎯 **Smart Touch** — picks the right letter when you tap a key's edge.
• ↶ ↷ **Undo / Redo** buttons.
• 🔤 **Latin ⇄ Cyrillic** — switch with one tap. Ğ, Ö, Ş, Ç and Ў, Қ, Ғ, Ҳ are built in.
• 🌍 **Live translation** — see the translation while you type. Tap ✓ to insert it, or "Original + translation" to send both. Uzbek (Latin/Cyrillic), Russian and English.
• 📋 **Clipboard** — up to 300 copied texts, ⭐ favorites that never expire, 📌 pins, categories, search, editing and password-encrypted backup (export/import).
• 🎨 **30+ themes** — Samarkand, Registan, Bukhara, Atlas, neon RGB, white, black and more.
• 🖼 **Your own photo** — use any picture from your gallery as the keyboard background.
• 😊 Emoji, quick phrases, one-handed mode, adjustable key height and letter size.
• 🔒 Auto-replace and translation are disabled in password, email and URL fields.

**Privacy:** what you type is not sent to any server. Learned words, clipboard history and your background photo stay on your phone. The internet is used only for translation.

Developer: **Donyor** 🇺🇿 · Feedback & ideas: Telegram @Doniku_Admin

## 6. To'liq tavsif — Русский
**ÖZKEY — печатайте по-узбекски быстро, удобно и красиво.**

ÖZKEY — клавиатура, созданная для узбекского языка. Латиница и кириллица в одном месте, перевод и буфер обмена всегда под рукой.

**Возможности:**
• ✨ **Подсказки слов** — до 3 подсказок при наборе и предсказание следующего слова. Запоминает ваши частые слова.
• ✅ **Безопасная автокоррекция** — исправляет только явные опечатки; ⌫ возвращает ваше слово. Можно отключить.
• 🎯 **Smart Touch** — угадывает нужную букву при нажатии на край клавиши.
• ↶ ↷ Кнопки **отмены / повтора**.
• 🔤 **Латиница ⇄ Кириллица** — переключение одним нажатием. Ğ, Ö, Ş, Ç и Ў, Қ, Ғ, Ҳ уже есть.
• 🌍 **Перевод на лету** — перевод появляется, пока вы печатаете. Нажмите ✓ — он вставится в текст, либо «Оригинал + перевод».
• 📋 **История буфера обмена** — скопированное сохраняется. Закрепляйте нужное ★.
• 🎨 **30+ тем** — Самарканд, Регистан, Бухара, Атлас, неон RGB, белая, чёрная и другие.
• 🖼 **Своё фото** — любой снимок из галереи как фон клавиатуры.
• 😊 Эмодзи, быстрые фразы, режим одной руки, настройка высоты и размера букв.
• 🔒 В полях пароля, email и ссылок автозамена и перевод отключены.

**Конфиденциальность:** набранный текст не отправляется на сервер. Выученные слова, история буфера и фото хранятся только на телефоне. Интернет нужен лишь для перевода.

Разработчик: **Donyor** 🇺🇿 · Предложения и ошибки: Telegram @Doniku_Admin

## 7. Nima yangi (0.8.0)
**UZ**
• So'z takliflari va keyingi so'z bashorati
• Xavfsiz avto-tuzatish (⌫ bilan bekor qilinadi)
• Smart Touch: tugma chetiga bosilganda aniqlash
• Bekor qilish / Qayta qilish tugmalari
• Sozlamada yoqish-o'chirish va o'rganilgan so'zlarni tozalash

**EN**
• Word suggestions and next-word prediction
• Safe auto-correct (undo with ⌫)
• Smart Touch for edge taps
• Undo / Redo buttons
• Settings to toggle features and clear learned words

### Oldingi (0.6.2)
**UZ**
• Yangi dizayn: o'zbekona naqshlar va ranglar
• Jonli tarjima paneli, «Asl + tarjima»
• 📋 Bufer tarixi (tepa panelning chap chetida)
• 30 dan ortiq mavzu va galereyadan rasm fon
• Harflar kattalashtirildi, harf o'lchami sozlanadi
• Tugmalar kattaroq, bo'shliq tasodifan bosilmaydi
• Tezroq ishlaydi (shift va katta harf endi bir zumda)
• Yangi nusxa tepa panelda taklif bo'lib chiqadi
• Yangi «Avto» mavzu: telefon Kun/Tun rejimiga moslashadi
• Soddaroq interfeys, «Ilova haqida» bo'limi va dasturchi bilan aloqa (Telegram), kichik xatolar tuzatildi

**EN**
• New Uzbek-style design
• Live translation panel with "Original + translation"
• 📋 Clipboard history (far-left of the top bar)
• 30+ themes and a gallery photo as background
• Larger letters, adjustable letter size
• Larger keys, safer space bar, faster shift/caps
• Clipboard suggestion chip in the top bar
• New "Auto" theme follows system light/dark
• Simpler interface, bug fixes

## 8. Skrinshotlar (kamida 4 ta, telefon ekrani 1080×1920 atrofida)
1. Ilovaning bosh sahifasi (feruza sarlavha, «Ulash — 2 qadam»)
2. Chatda klaviatura ochiq — Samarqand mavzusi
3. Tarjima paneli: tepada yozilgan matn, ostida jonli tarjima
4. Bufer oynasi (📋) — bir nechta nusxa, ★ bilan
5. Mavzular sahifasi
6. Galereya rasmi fon bo'lgan klaviatura

## 9. Yuborishdan oldin tekshiruv
- [ ] `tools/make-keystore.sh` → GitHub sirlari kiritildi
- [ ] 0.8.0: takliflar, avto-tuzatish (⌫ bekor), Smart Touch, Bekor/Qayta tugmalari telefonda sinab ko'rildi
- [ ] Release APK telefonga o'rnatilib sinab ko'rildi (yoqish, yozish, 📋, tarjima, rasm fon)
- [ ] `docs/privacy.html` internetga qo'yildi (GitHub Pages) — havola do'kon formasiga kiritiladi (aloqa: Telegram @Doniku_Admin)
- [ ] Skrinshotlar tayyor
- [ ] Tarjima serveri: `gradle.properties` ga `ozkeyTranslateUrl=` yozilsa, tarjima o'z serveringiz orqali ishlaydi (tavsiya etiladi). Yozilmasa — zaxira bepul xizmat ishlatiladi, u rasmiy emas va to'xtab qolishi mumkin.
