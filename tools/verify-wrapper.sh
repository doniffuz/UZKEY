#!/bin/sh
# gradle-wrapper.jar va distributionSha256Sum ni Gradle'ning RASMIY manbasi bilan solishtiradi.
# INTERNET kerak. Internet yo'q / manba o'qilmasa — chiqish kodi 2 ("tekshirib BO'LMADI", "OK" emas).
# Chiqish kodlari: 0 = ikkalasi ham mos; 1 = MOS EMAS (jar ni ishlatmang); 2 = tekshirib bo'lmadi.
# Ishlatish:  sh tools/verify-wrapper.sh
set -u
cd "$(dirname "$0")/.." || exit 2
PROPS=gradle/wrapper/gradle-wrapper.properties
JAR=gradle/wrapper/gradle-wrapper.jar
VER=8.11.1
command -v curl >/dev/null 2>&1 || { echo "curl yo'q — tekshirib bo'lmadi"; exit 2; }
command -v python3 >/dev/null 2>&1 || { echo "python3 yo'q — tekshirib bo'lmadi"; exit 2; }
command -v sha256sum >/dev/null 2>&1 || { echo "sha256sum yo'q (macOS: shasum -a 256) — tekshirib bo'lmadi"; exit 2; }

LOCAL_JAR=$(sha256sum "$JAR" | cut -d' ' -f1)
LOCAL_DIST=$(sed -n 's/^distributionSha256Sum=//p' "$PROPS" | tr -d '\r ')
URL=$(sed -n 's/^distributionUrl=//p' "$PROPS" | tr -d '\r' | sed 's#\\##g')
echo "distributionUrl : $URL"
echo "local jar sha256: $LOCAL_JAR"
echo "local dist sha  : $LOCAL_DIST"
case "$URL" in */gradle-$VER-all.zip) ;; *) echo "distributionUrl gradle-$VER-all.zip emas — bu skript $VER uchun"; exit 1;; esac

# 1) Rasmiy versiyalar ro'yxatidan wrapper jar checksum URL ini o'qish
WURL=$(curl -fsSL --max-time 30 https://services.gradle.org/versions/all 2>/dev/null | python3 -c '
import json,sys
v=sys.argv[1]
for e in json.load(sys.stdin):
    if e.get("version")==v: print(e.get("wrapperChecksumUrl","")); break
' "$VER" 2>/dev/null)
[ -n "$WURL" ] || { echo "wrapperChecksumUrl ni olib bo'lmadi (internet/format) — tekshirib bo'lmadi"; exit 2; }
OFFICIAL_JAR=$(curl -fsSL --max-time 30 "$WURL" 2>/dev/null | tr -d '\r\n ')
[ -n "$OFFICIAL_JAR" ] || { echo "$WURL o'qilmadi — tekshirib bo'lmadi"; exit 2; }
echo "official jar sha: $OFFICIAL_JAR  ($WURL)"

# 2) -all distributiv checksum
OFFICIAL_DIST=$(curl -fsSL --max-time 30 "https://services.gradle.org/distributions/gradle-$VER-all.zip.sha256" 2>/dev/null | tr -d '\r\n ')
[ -n "$OFFICIAL_DIST" ] || { echo "-all.zip.sha256 o'qilmadi — tekshirib bo'lmadi"; exit 2; }
echo "official dist   : $OFFICIAL_DIST"

RC=0
[ "$LOCAL_JAR"  = "$OFFICIAL_JAR"  ] && echo "JAR : MOS" || { echo "JAR : MOS EMAS — gradle-wrapper.jar ga ISHONMANG, tools/regenerate-wrapper.sh bilan qayta yarating"; RC=1; }
[ "$LOCAL_DIST" = "$OFFICIAL_DIST" ] && echo "DIST: MOS" || { echo "DIST: MOS EMAS — distributionSha256Sum ni tuzating"; RC=1; }
exit $RC
