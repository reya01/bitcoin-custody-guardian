#!/usr/bin/env python3
"""Convert INHERITANCE_WALKTHROUGH.md day structure -> assets/walkthrough.json
Schema per day: {day, title, goal, dos[], donts[]}"""
import json, re, sys

src = "/opt/data/projects/custody-guardian/INHERITANCE_WALKTHROUGH.md"
out = "/opt/data/work/guardian-android/app/src/main/assets/walkthrough.json"
text = open(src, encoding="utf-8").read()

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
    dos, donts = [], []
    cur = None
    bullets = False
    for line in body.splitlines():
        s = line.strip()
        if re.match(r"^(DO|Do):?$", s.replace("**","")):
            cur = dos; continue
        if re.match(r"^DON'?T:?$", s.replace("**","")):
            cur = donts; continue
        if cur is not None and (s.startswith("- ") or s.startswith("- ❌ ")):
            item = s.lstrip("- ").replace("❌ ", "").strip()
            item = re.sub(r"\*\*(.+?)\*\*", r"\1", item)
            if item and not item.startswith(("|","For")):
                cur.append(item)
        elif cur is not None and (s.startswith("- ") or (bullets and s and not s.startswith("#"))):
            pass
    # fallback for days whose DO/DON'T sections use different headers
    if not dos and not donts:
        body = body.split("## The scam-defense card")[0]
        for line in body.splitlines():
            s = line.strip()
            if s.startswith("- ") and "❌" not in s:
                item = re.sub(r"\*\*(.+?)\*\*", r"\1", s[2:].strip())
                if item and not item.startswith(("|","For","An app","A laptop")):
                    dos.append(item)
            elif "❌" in s and s.startswith("- "):
                donts.append(re.sub(r"\*\*(.+?)\*\*", r"\1", s.lstrip("- ❌ ").strip()))
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
