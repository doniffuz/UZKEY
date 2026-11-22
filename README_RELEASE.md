# Release build (Play Market uchun .aab)

1. Kalit yaratish (BIR MARTA; faylni va parolni xavfsiz joyga ZAXIRALANG, yo'qolsa ilovani yangilab bo'lmaydi):
   ```
   keytool -genkeypair -v -keystore release-keystore.jks -alias ozkey -keyalg RSA -keysize 2048 -validity 10000
   ```
2. `cp keystore.properties.example keystore.properties` va ichini to'ldiring (`storeFile` — jks fayl yo'li, loyiha ildiziga nisbatan).
3. Yig'ish:
   ```
   ./gradlew clean bundleRelease
   ```
   Natija: `app/build/outputs/bundle/release/app-release.aab`
4. Imzoni tekshirish: `jarsigner -verify -verbose -certs app/build/outputs/bundle/release/app-release.aab`
   Sertifikat "Android Debug" bo'lsa — yuklamang.
5. `keystore.properties` va `*.jks` hech qachon commit qilinmaydi (`.gitignore` da bor).

Kalitsiz `bundleRelease`/`assembleRelease` ataylab to'xtatiladi (debug kalit bilan chiqib ketmasligi uchun).
