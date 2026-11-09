"""Downloads the full Quran text (Uthmani script) and a Quran font into the app's assets.

Runs automatically during the GitHub build, so the finished app contains the whole Mushaf
and works without internet. Run it yourself before building in Android Studio:
    python3 tools/fetch_quran.py
"""
import json
import os
import sys
import time
import urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "app", "src", "main", "assets")
TOTAL_AYAT = 6236


def get(url, tries=4):
    for i in range(tries):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": "nujoom-calendar-build"})
            with urllib.request.urlopen(req, timeout=90) as r:
                return r.read()
        except Exception as e:  # noqa: BLE001
            print(f"  attempt {i + 1} failed for {url}: {e}")
            time.sleep(5)
    return None


def strip_basmala(text):
    words = text.split()
    if len(words) > 4 and words[0].startswith("بِسْمِ"):
        return " ".join(words[4:])
    return text


def from_alquran_cloud():
    print("Downloading from api.alquran.cloud ...")
    raw = get("https://api.alquran.cloud/v1/quran/quran-uthmani")
    if not raw:
        return None
    surahs = json.loads(raw)["data"]["surahs"]
    out = []
    for s in surahs:
        ayat = [a["text"].replace("﻿", "").strip() for a in s["ayahs"]]
        if s["number"] not in (1, 9):
            ayat[0] = strip_basmala(ayat[0])
        out.append({"t": "m" if s.get("revelationType") == "Meccan" else "d", "a": ayat})
    return out


def from_tanzil():
    print("Downloading from tanzil.net ...")
    raw = get("https://tanzil.net/pub/download/index.php?marks=true&sajdah=true&rub=false"
              "&tatweel=true&quranType=uthmani&outType=txt-2&agree=true")
    if not raw:
        return None
    out = [{"t": "", "a": []} for _ in range(114)]
    for line in raw.decode("utf-8").splitlines():
        line = line.replace("﻿", "").strip()
        if not line or line.startswith("#"):
            continue
        parts = line.split("|", 2)
        if len(parts) != 3:
            continue
        s = int(parts[0])
        text = parts[2].strip()
        if s not in (1, 9) and parts[1] == "1":
            text = strip_basmala(text)
        out[s - 1]["a"].append(text)
    return out


def valid(q):
    return q is not None and len(q) == 114 and sum(len(s["a"]) for s in q) == TOTAL_AYAT


def main():
    os.makedirs(ASSETS, exist_ok=True)
    quran = from_alquran_cloud()
    if not valid(quran):
        print("First source failed or incomplete, trying the second source.")
        quran = from_tanzil()
    if not valid(quran):
        print("ERROR: could not download a complete Quran text.")
        sys.exit(1)

    with open(os.path.join(ASSETS, "quran.js"), "w", encoding="utf-8") as f:
        f.write("window.QURAN=")
        json.dump(quran, f, ensure_ascii=False, separators=(",", ":"))
        f.write(";")
    print(f"Quran saved: 114 surahs, {TOTAL_AYAT} ayat.")

    font = get("https://github.com/google/fonts/raw/main/ofl/amiriquran/AmiriQuran-Regular.ttf", tries=2)
    if font and len(font) > 50000:
        with open(os.path.join(ASSETS, "AmiriQuran.ttf"), "wb") as f:
            f.write(font)
        print("Quran font saved.")
    else:
        print("Quran font not downloaded; the phone's Arabic font will be used.")


if __name__ == "__main__":
    main()
