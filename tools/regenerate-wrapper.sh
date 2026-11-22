#!/bin/sh
# gradle-wrapper.jar, gradlew, gradlew.bat ni Gradle 8.11.1 RASMIY distributivi yordamida qayta yaratadi.
# MUHIM: mavjud (kelib chiqishi noma'lum) gradle-wrapper.jar bilan BOOTSTRAP QILMANG. Buning uchun
# PATH da rasmiy Gradle 8.11.1 (o'zi tekshirilgan) turishi kerak — docs/WRAPPER_PROVENANCE.md ga qarang.
# Ishlatish:  sh tools/regenerate-wrapper.sh
set -eu
cd "$(dirname "$0")/.."
command -v gradle >/dev/null 2>&1 || { echo "PATH da 'gradle' yo'q. docs/WRAPPER_PROVENANCE.md dagi 1-qadamni bajaring."; exit 1; }
GV=$(gradle --version 2>/dev/null | sed -n 's/^Gradle //p' | head -n1 | tr -d '\r ')
[ "$GV" = "8.11.1" ] || { echo "Tizim Gradle'i '$GV' — aynan 8.11.1 kerak."; exit 1; }
SUM=89d4e70e4e84e2d2dfbb63e4daa53e21b25017cc70c37e4eea31ee51fb15098a
gradle wrapper --gradle-version 8.11.1 --distribution-type all --gradle-distribution-sha256-sum "$SUM"
echo
echo "Tayyor. Endi:"
echo "  sha256sum gradle/wrapper/gradle-wrapper.jar"
echo "  sh tools/verify-wrapper.sh      # rasmiy checksum bilan solishtirish (internet kerak)"
echo "  git diff gradle/ gradlew gradlew.bat   # nima o'zgarganini ko'ring"
