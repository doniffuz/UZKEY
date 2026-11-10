# Termux build va GitHub push

## Tekshirish va build (siz Termux'da)
```
cd ~/OZKEY_Android
./gradlew assembleDebug        # yoki: gradle assembleDebug
# release (o'z kalitingiz bilan):
export OZKEY_KEYSTORE=/yo'l/ozkey.jks OZKEY_STORE_PASSWORD=... OZKEY_KEY_ALIAS=... OZKEY_KEY_PASSWORD=...
gradle assembleRelease bundleRelease
```
APK: `app/build/outputs/apk/{debug,release}/`, AAB: `app/build/outputs/bundle/release/`.
Kalit va parollarni faylga/GitHub'ga yozmang (`tools/make-keystore.sh` ga qarang).

## GitHub'ga push
```
git add -A
git status          # token, .jks, local.properties yo'qligini tekshiring
git commit -m "ÖZKEY: majburiy Telegram obunasi olib tashlandi"
git push
```
`.github/workflows/build.yml` o'zgarmagan — push'dan keyin Actions o'zi yig'adi.
