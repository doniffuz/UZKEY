# ÖZKEY Android — 0.5.0 (1-bosqich)

Premium o'zbek klaviaturasi (Kotlin, `InputMethodService`).

## 0.5.0 (1-bosqich) yangiliklari
- **Tepa panel:** O'z | Ўз tez almashtirish, emoji, asboblar (⚙), klaviaturani yopish (⌄)
- **Asboblar paneli:** Tarjima, Maydonni tarjima, Bufer, Iboralar, Lotin⇄Kirill, Oddiy nusxa, Mavzu, Tungi rejim, Tebranish, Bir qo'l, Tarjima tili, Sozlamalar
- **Tarjima paneli:** «Avto → til» chiplari, yozish maydoni, ✓ bosilsa tarjima ilovaga yoziladi
- **Iboralar:** tez yuboriladigan 15 ta ibora (tanlangan yozuvda)
- Keyingi bosqichlar: 2) mavzu galereyasi (kategoriya, rasm fon), 3) stikerlar

## 0.4.0 yangiliklari
- **Logo** va ikonkalar (adaptiv + eski Android), Play Market grafikalari: `store/`
- **Kirish ekrani** (3 sahifa) va yangi dizayn: Bosh / Mavzular / Tillar / Sozlama
- **Tillar:** O'zbek lotin (ÖZ), **O'zbek kirill (ў қ ғ ҳ)**, Русский, English
- **Qo'shimcha tillar:** Türkçe, Deutsch, Қазақша, Кыргызча, Тоҷикӣ — "Tillar" bo'limidan yuklanadi (server orqali ham: `ozkeyPacksUrl`)
- **Tarjima:** maqsad tili tanlanadi (UZ lotin / ЎЗ kirill / RU / EN), 🌍 tugmasi; server orqali (`backend/worker.js`) — foydalanuvchi API kalit kiritmaydi
- **Konvertor:** Lotin ⇄ Kirill ⇄ ÖZ (Bosh sahifada va klaviaturadagi ÖZ⇄std tugmasida)
- **Mavzular:** 9 ta (3 bepul, 6 premium), neon/aurora animatsiyali
- **Emoji:** 6 bo'lim, aylantiriladigan ro'yxat
- Release build + AAB (CI), `Premium` tayyor (Billing ulanishi kerak)

## Ishga tushirish
1. Android Studio da oching yoki GitHub Actions dan APK oling (`.github/workflows/build.yml`).
2. Ilovani oching → kirish ekrani → ÖZKEY ni yoqing va tanlang.
3. Tarjima serveri: `backend/` papkasi (worker.js) — qadamlar fayl boshida. URL ni `gradle.properties` ga yozing.

## Sinov uchun premium
Ilova sarlavhasidagi logoni 5 marta bosing — premium yoqiladi/o'chadi. **Nashrdan oldin `MainActivity.kt` dagi bu qismni olib tashlang.**

## Eslatma
Bu kod bu yerda kompilyatsiya qilinmagan (Android SDK yo'q edi). Birinchi build da kichik xatolar chiqsa, xabarini yuboring — tuzataman.
