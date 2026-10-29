#!/usr/bin/env python3
"""Convert INHERITANCE_WALKTHROUGH.md day structure -> assets/walkthrough.json
Schema per day: {day, title, goal, dos[], donts[]}"""
import json, re, sys

import os
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
src = os.path.join(ROOT, "docs", "INHERITANCE_WALKTHROUGH.md")
out = os.path.join(ROOT, "app", "src", "main", "assets", "walkthrough.json")
text = open(src, encoding="utf-8").read()

def parse_day(body):
    """Extract (dos, donts) from one day's markdown body.

    Handles: explicit DO:/DON'T: blocks, ❌ bullets (multi-line, bold
    stripped), and column-0 numbered plan steps as DOs. Nested/indented
    lines are continuations of the current item, never new items."""
    dos, donts = [], []
    cur = None          # list currently being filled (dos/donts), or None
    item = None         # lines of the item being accumulated
    item_is_dont = False

    def flush():
        nonlocal item
        if item:
            text_ = re.sub(r"\s+", " ", " ".join(item)).strip()
            text_ = re.sub(r"\*\*(.+?)\*\*", r"\1", text_).strip()
            target = donts if item_is_dont else dos
            if text_:
                target.append(text_)
            item = None

    for line in body.split("## The scam-defense card")[0].splitlines():
        s = line.strip()
        if re.match(r"^(DO|Do|Do today)[:.]?$", s.replace("**", "")):
            flush(); cur = dos; continue
        if re.match(r"^(DON'T|Don't|DONT)[:.]?$", s.replace("**", "")):
            flush(); cur = donts; continue
        bullet = re.match(r"^-\s+(.*)$", s)
        numbered = re.match(r"^(\d+)\.\s+(.*)$", s)
        # only COLUMN-0 bullets/numbered items start a new item; indented
        # ones are nested sub-steps and stay inside the current item's text
        col0 = bool(line) and not line[0].isspace()
        if (bullet or numbered) and col0:
            flush()
            raw = bullet.group(1) if bullet else (numbered.group(2) if numbered else "")
            item_is_dont = "❌" in s
            raw = raw.replace("❌", "").strip()
            item = [raw] if raw else None
            # numbered plan steps outside a DO/DON'T block are DOs
            if numbered and cur is None:
                cur = dos
        elif item is not None and (bool(s) and not col0):
            # continuation or nested sub-step: keep inside the item's wording
            item.append(s)
        elif s and not s.startswith("#") and item is not None:
            flush()
    flush()
    return dos, donts


days = []
# split on "## Day N — Title"
parts = re.split(r"\n## Day (\d) — ", text)
for i in range(1, len(parts), 2):
    day = int(parts[i]); title = parts[i+1].split("\n", 1)[0]
    body = parts[i+1].split("\n", 1)[1]
    m = re.search(r"\*\*Goal today: (.+?)\.\s*\*\*", body, re.DOTALL)
    goal = ""
    if m:
        goal = " ".join(m.group(1).replace("*", "").split())
    dos, donts = parse_day(body)
    days.append({"day": day, "title": title, "goal": goal, "dos": dos, "donts": donts})

doc = {
    "title": "The First 7 Days — An Inheritance Walkthrough",
    "app": "Bitcoin Custody Guardian",
    "version": "0.1",
    "rule_one": "Nothing is urgent. Bitcoin cannot be stolen by waiting. It can be lost by rushing. Every irreversible mistake in Bitcoin happens in minutes; every safe step can be taken calmly. You have time. Use it.",
    "days": days,
}
with open(out, "w", encoding="utf-8") as f:
    json.dump(doc, f, indent=1, ensure_ascii=False)
for d in days:
    print(d["day"], d["title"], "| goal:", d["goal"][:40], "| dos:", len(d["dos"]), "| donts:", len(d["donts"]))
