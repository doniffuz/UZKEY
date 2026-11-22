# gradle-wrapper.jar kelib chiqishi (V8 auditi)

**Holat: ISHONCH ISBOTLANMAGAN.** Bu yerda jar rasmiy manba bilan solishtirilmagan (audit muhitida internet yo'q).
Quyidagi kuzatuvlar faqat «shubhali alomat topilmadi» degan ma'noda — «ishonchli» degani EMAS.

## Hozirgi fayllar
| Fayl | SHA-256 |
|---|---|
| `gradle/wrapper/gradle-wrapper.jar` (43 764 bayt) | `7d3a4ac4de1c32b59bc6a4eb8ecb8e612ccd0cf1ae1e99f66902da64df296172` |
| `gradlew` | `b187b4c52e749f5760afdd6fadc31b2a98ad35fb249bf0dff03b72650f320409` |
| `gradlew.bat` | `1d297e00bd21de3ace22b4d7f2de1f9dfa858883d66bbf7c1ccbecccec8f4f3b` |

## Nima ma'lum
- v5 README'ida: jar «Gradle 8.14.3 distributividan olindi», lekin loyiha 8.11.1 ga sozlangan — ya'ni jar versiyasi
  `distributionUrl` bilan mos EMAS (wrapper jar odatda orqaga mos, lekin bu qasddan emas, qo'lda ko'chirish natijasi).
- Jar'ning o'zi: 33 ta fayl, manifestda `Main-Class: org.gradle.wrapper.GradleWrapperMain`, sinflar faqat `org.gradle.cli`,
  `org.gradle.wrapper`, `org.gradle.internal.file`, `org.gradle.util.internal` paketlarida — haqiqiy wrapper tuzilmasiga o'xshaydi.
- `jdeps` bo'yicha bog'liqlik faqat `java.base` (java.net URLConnection/URI, java.security MessageDigest, reflection, nio). `Runtime.exec`/`ProcessBuilder`
  yo'q. Jar ichidagi URL'lar faqat `https://gradle.org/release-checksums/` va Apache litsenziyasi.
- Bu — evristika. Zararli o'zgartirish ham shu belgilarga ega bo'lishi mumkin. Isbot faqat rasmiy checksum bilan solishtirish.

## Tekshirish (internet bor joyda)
```
sh tools/verify-wrapper.sh
```
0 = jar va `distributionSha256Sum` rasmiy qiymatlarga mos; 1 = MOS EMAS; 2 = tekshirib bo'lmadi (internet/manba). Skript audit muhitida
ishga tushirilganda internet yo'qligi sababli **2** qaytardi — ya'ni haqiqiy tekshiruv HALI BAJARILMAGAN.
CI da `gradle/actions/wrapper-validation@v4` ham jar'ni Gradle'ning ma'lum checksumlari ro'yxatiga qarshi tekshiradi.
Qo'lda: https://gradle.org/release-checksums/ sahifasida 8.11.1 uchun «Wrapper JAR checksum» ni jar SHA-256 bilan solishtiring.

## Qayta yaratish (jar mos kelmasa yoki shubha qolsa)
Maqsad: yangi jar mavjud (ishonchsiz) jar orqali emas, **rasmiy Gradle 8.11.1 distributividan** chiqsin.
1. Rasmiy Gradle 8.11.1 ni yuklab, checksum'ini tekshirib o'rnating:
   ```
   curl -fLO https://services.gradle.org/distributions/gradle-8.11.1-bin.zip
   curl -fLO https://services.gradle.org/distributions/gradle-8.11.1-bin.zip.sha256
   echo "$(cat gradle-8.11.1-bin.zip.sha256)  gradle-8.11.1-bin.zip" | sha256sum -c -
   unzip gradle-8.11.1-bin.zip && export PATH="$PWD/gradle-8.11.1/bin:$PATH"
   ```
   (`.sha256` fayl Gradle saytining o'zidan; muqobil: SDKMAN `sdk install gradle 8.11.1`, lekin baribir yuqoridagi checksum bilan solishtiring.)
2. Loyiha ildizida: `sh tools/regenerate-wrapper.sh` (faqat tizim Gradle'i aynan 8.11.1 bo'lsa ishlaydi; `distributionSha256Sum` ni ham yozadi).
3. `sh tools/verify-wrapper.sh` va `git diff gradle/ gradlew gradlew.bat` — o'zgarishlarni ko'zdan kechiring.
4. `./gradlew --version` va `./gradlew testDebugUnitTest`.
Natijada jar, `gradlew`, `gradlew.bat` bir xil Gradle 8.11.1 dan bo'ladi. Bu fayllar V8 ZIP'da YANGILANMAGAN (internet va Gradle yo'q).
