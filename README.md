# ÖZKEY Android — 0.6.0

O'zbek klaviaturasi (Kotlin, `InputMethodService`). Dasturchi: **Donyor**.

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
- `app/` — ilova kodi (`OZKeyInputMethod.kt` — klaviatura, `MainActivity.kt` — sozlash ilovasi, `Themes.kt`, `Clips.kt`, `Ornament.kt`)
- `store/` — ikonka, banner, do'kon matnlari, maxfiylik siyosati
- `docs/privacy.html` — GitHub Pages uchun maxfiylik sahifasi
- `backend/` — tarjima serveri (Cloudflare Worker)
