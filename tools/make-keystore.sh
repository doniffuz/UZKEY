#!/data/data/com.termux/files/usr/bin/bash
# ÖZKEY imzo kalitini BIR MARTA yaratadi. Faylni (ozkey.jks) va parolni yo'qotmang —
# Uptodown / Play'da keyingi yangilanishlar SHU kalit bilan imzolanishi shart.
# Ishlatish:  bash tools/make-keystore.sh
set -e
command -v keytool >/dev/null || { echo "keytool topilmadi. Termux: pkg install openjdk-17"; exit 1; }
if [ -f ozkey.jks ]; then echo "ozkey.jks allaqachon bor — qayta yaratilmadi."; exit 0; fi
read -r -s -p "Kalit paroli (kamida 6 belgi): " PASS; echo
keytool -genkeypair -v -keystore ozkey.jks -alias ozkey -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass "$PASS" -keypass "$PASS" -dname "CN=Donyor, O=OZKEY, C=UZ"
echo
echo "Tayyor: ozkey.jks"
echo "GitHub → Settings → Secrets and variables → Actions ga shularni qo'shing:"
echo "  OZKEY_KEYSTORE_B64     = $(base64 -w0 ozkey.jks | cut -c1-24)...   (to'liq qiymat: base64 -w0 ozkey.jks)"
echo "  OZKEY_STORE_PASSWORD   = (kiritgan parolingiz)"
echo "  OZKEY_KEY_ALIAS        = ozkey"
echo "  OZKEY_KEY_PASSWORD     = (xuddi shu parol)"
echo "ozkey.jks ni GitHub'ga YUKLAMANG (.gitignore'da bor). Zaxira nusxasini boshqa joyda saqlang!"
