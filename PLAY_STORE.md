# Play Market'ga chiqarish — ro'yxat

## 1. Texnik
- [ ] Imzo kaliti yarating: `keytool -genkey -v -keystore ozkey.jks -alias ozkey -keyalg RSA -keysize 2048 -validity 10000` (faylni yo'qotmang!)
- [ ] GitHub → Settings → Secrets: `OZKEY_KEYSTORE_B64` (`base64 ozkey.jks`), `OZKEY_STORE_PASSWORD`, `OZKEY_KEY_ALIAS`, `OZKEY_KEY_PASSWORD`
- [ ] CI `.aab` beradi → Play Console'ga yuklang (APK emas, AAB talab qilinadi)
- [ ] `targetSdk` — Play joriy talabini tekshiring (hozir 35); talab oshsa `app/build.gradle.kts` da yangilang
- [ ] Sinov premium (logo 5 marta bosish) kodini olib tashlang
- [ ] Yangi shaxsiy akkauntlarda Play ko'pincha 12+ sinovchi bilan 14 kunlik yopiq sinov talab qiladi — Console'da tekshiring

## 2. Do'kon sahifasi
- Ikonka: `store/icon-512.png`; Feature graphic: `store/feature-graphic-1024x500.png`
- Skrinshotlar (kamida 2): telefonda klaviatura ochilgan holda (turli mavzular, tarjima, kirill) oling
- Qisqa tavsif (80 belgigacha): *Lotin va kirill o'zbek klaviaturasi: tarjima, mavzular, emoji*
- **Maxfiylik siyosati (majburiy, havola kerak)** — pastdagi shablonni saytga/GitHub Pages ga joylang
- Data safety formasi: klaviatura matn "yig'maydi"; tarjima tugmasi bosilganda matn tarjima uchun yuboriladi (saqlanmaydi) — shuni to'g'ri belgilang
- Klaviaturalar uchun Play "kirish qurilmasi" ogohlantirishini ko'rsatadi — bu normal

## 3. Pul topish (klaviatura uchun xavfsiz yo'llar)
- ❌ **Klaviatura ichiga reklama qo'ymang.** Yozilayotgan matn bor joyda reklama — Play siyosati va maxfiylik nuqtai nazaridan rad etilish/bloklanish xavfi yuqori.
- ✅ **Obuna / bir martalik premium** (Google Play Billing): premium mavzular, stikerlar — `Premium.kt` shunga tayyor.
- ✅ **Ilova ichidagi (klaviaturadan tashqari) reklama:** Mavzular/Sozlama ekranida banner yoki "reklama ko'rib mavzuni 24 soatga ochish" (Rewarded, AdMob). AdMob ID ni o'zingiz olasiz; foydalanuvchi roziligi (UMP/GDPR) va Data safety'ga reklama SDK qo'shilganini yozing.
- ✅ Stiker paketlari, maxsus mavzular sotish.

## 4. Emoji / stikerlar haqida (iPhone uslubi)
- Apple emoji rasmlari Apple mulki — ilovaga qo'shib bo'lmaydi (Play'dan olib tashlanishi mumkin).
- Hozir tizim emojilari ishlaydi. iPhone'ga yaqin ko'rinish uchun **ochiq litsenziyali to'plamlar** bor: Microsoft Fluent Emoji (MIT), Twemoji (CC-BY 4.0, havola kerak), OpenMoji (CC-BY-SA). Ularni rasm sifatida qo'shish alohida bosqich (internet bilan yuklab, `assets` ga joylanadi).

## 5. Tarjima va API kalit
- Kalitni **APK ichiga yozmang** — dekompilyatsiya qilib o'g'irlanadi va hisobingizdan pul yechiladi.
- `backend/worker.js` — kalit serverda; kunlik limit bor. Xarajatni Anthropic Console'da limit bilan cheklang.

## 6. Maxfiylik siyosati shabloni (o'zbekcha)
**ÖZKEY maxfiylik siyosati.** ÖZKEY yozgan matningizni to'plamaydi, saqlamaydi va sotmaydi. Klaviatura barcha matnni qurilmada qayta ishlaydi. Faqat siz «→» tarjima tugmasini bosganda tanlangan matn tarjima uchun serverimizga, u yerdan tarjima xizmatiga yuboriladi; matn saqlanmaydi. Parol, email va havola maydonlarida tarjima o'chirilgan. Ilova sozlamalari (mavzu, til) faqat qurilmada saqlanadi. Aloqa: [sizning email].
