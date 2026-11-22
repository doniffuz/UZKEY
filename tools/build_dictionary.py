#!/usr/bin/env python3
"""ÖZKEY lug'at yasagich.

Oddiy matn korpusidan (har qatorda gap/abzats) ikkita fayl yasaydi:
  words_uz.txt    -> "so'z<TAB>chastota"          (chastota kamayish tartibida)
  bigrams_uz.txt  -> "so'z1 so'z2<TAB>soni"       (soni kamayish tartibida)

Ishlatish:
  python3 tools/build_dictionary.py corpus.txt [corpus2.txt ...] -o app/src/main/assets
  python3 tools/build_dictionary.py kirill_korpus.txt --cyrillic -o out/

Hech qanday so'z o'ylab topilmaydi: natija faqat berilgan korpusdagi haqiqiy chastotalardan iborat.
Korpus kichik bo'lsa lug'at ham kichik chiqadi (statistika oxirida bosiladi).
"""
import argparse
import collections
import os
import re
import sys
import unicodedata

# Barcha tutuq/apostrof variantlari -> bitta standart (oddiy ' ; klaviatura Alphabet.toNew shuni tushunadi)
APOS_RE = re.compile("[ʻʼ’‘`´＇′]")
WORD_RE = re.compile(r"[a-zа-яёўқғҳ']+", re.IGNORECASE)

CYR = {
    "а": "a", "б": "b", "в": "v", "г": "g", "д": "d", "е": "e", "ё": "yo", "ж": "j", "з": "z",
    "и": "i", "й": "y", "к": "k", "л": "l", "м": "m", "н": "n", "о": "o", "п": "p", "р": "r",
    "с": "s", "т": "t", "у": "u", "ф": "f", "х": "x", "ц": "s", "ч": "ch", "ш": "sh", "щ": "sh",
    "ъ": "'", "ы": "i", "ь": "", "э": "e", "ю": "yu", "я": "ya",
    "ў": "o'", "қ": "q", "ғ": "g'", "ҳ": "h",
}


def cyr_to_latin(text):
    out = []
    for i, ch in enumerate(text):
        low = ch.lower()
        if low not in CYR:
            out.append(ch)
            continue
        rep = CYR[low]
        # "е" so'z boshida yoki unlidan keyin -> "ye"
        if low == "е" and (i == 0 or text[i - 1].lower() in "аеёиоуўыэюя ъь-"):
            rep = "ye"
        if ch.isupper():
            rep = rep.capitalize()
        out.append(rep)
    return "".join(out)


def normalize(text):
    text = unicodedata.normalize("NFC", text)
    text = APOS_RE.sub("'", text)
    return text.lower()


def tokenize_sentences(line):
    """Gaplarni tinish belgilarida bo'lib, har gapdagi so'zlar ro'yxatini qaytaradi."""
    for sent in re.split(r"[.!?;:\n]+", line):
        words = []
        for w in WORD_RE.findall(sent):
            w = w.strip("'")
            # faqat harf va ichki tutuq; raqamli so'zlar WORD_RE da yo'q
            if len(w) >= 1 and re.fullmatch(r"[a-z]+(?:'[a-z]+)*", w):
                words.append(w)
        if words:
            yield words


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("corpus", nargs="+", help="oddiy matn fayl(lar)i (UTF-8)")
    ap.add_argument("-o", "--out", default=".", help="chiqish papkasi")
    ap.add_argument("--cyrillic", action="store_true", help="korpus kirillda: avval lotinga o'giriladi")
    ap.add_argument("--min-count", type=int, default=3, help="so'z/juftlik eng kam uchrashi (standart 3)")
    ap.add_argument("--max-words", type=int, default=60000)
    ap.add_argument("--max-bigrams", type=int, default=40000)
    a = ap.parse_args()

    words = collections.Counter()
    bigrams = collections.Counter()
    lines = 0
    for path in a.corpus:
        with open(path, encoding="utf-8", errors="replace") as f:
            for line in f:
                lines += 1
                if a.cyrillic:
                    line = cyr_to_latin(line)
                for ws in tokenize_sentences(normalize(line)):
                    words.update(ws)
                    bigrams.update(f"{x} {y}" for x, y in zip(ws, ws[1:]))

    w_out = [(w, c) for w, c in words.most_common() if c >= a.min_count][: a.max_words]
    keep = {w for w, _ in w_out}
    b_out = [(b, c) for b, c in bigrams.most_common()
             if c >= a.min_count and all(x in keep for x in b.split(" "))][: a.max_bigrams]

    os.makedirs(a.out, exist_ok=True)
    with open(os.path.join(a.out, "words_uz.txt"), "w", encoding="utf-8", newline="\n") as f:
        for w, c in w_out:
            f.write(f"{w}\t{c}\n")
    with open(os.path.join(a.out, "bigrams_uz.txt"), "w", encoding="utf-8", newline="\n") as f:
        for b, c in b_out:
            f.write(f"{b}\t{c}\n")

    print(f"korpus: {lines} qator, {sum(words.values())} so'z, {len(words)} noyob so'z")
    print(f"words_uz.txt:   {len(w_out)} so'z   (maqsad 30000+)")
    print(f"bigrams_uz.txt: {len(b_out)} juftlik (maqsad 20000+)")
    if len(w_out) < 30000 or len(b_out) < 20000:
        print("DIQQAT: maqsadga yetilmadi - korpus kichik. Ko'proq haqiqiy matn kerak; lug'at to'qib to'ldirilmaydi.", file=sys.stderr)


if __name__ == "__main__":
    main()
