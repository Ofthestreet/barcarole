# -*- coding: utf-8 -*-
import pathlib
OUT = pathlib.Path('/Users/cdelarue/Public/local-music-player/design')

# Palette relevee sur l'icone de l'app ; surfaces Material 3 sombres accordees au bleu marine.
STYLE = """
    :root {
      --bg: #0E1526;
      --surface: #141C31;
      --surface-hi: #1B2440;
      --surface-sel: #123A4A;
      --on: #E6EBF5;
      --on-var: #97A3BF;
      --primary: #00DCDF;
      --on-primary: #00363A;
      --coral: #FF8F6C;
      --outline: #29334F;
    }
    * { box-sizing: border-box; }
    body { margin: 0; font-family: Roboto, system-ui, -apple-system, sans-serif; }
    .screen {
      width: 390px; height: 844px; background: var(--bg); color: var(--on);
      display: flex; flex-direction: column; overflow: hidden;
    }
    .appbar {
      height: 64px; display: flex; align-items: center; gap: 4px;
      padding: 0 4px 0 16px; flex: 0 0 auto;
    }
    .appbar h1 { font-size: 22px; font-weight: 400; margin: 0; flex: 1; }
    .iconbtn {
      width: 48px; height: 48px; display: flex; align-items: center; justify-content: center;
      border-radius: 24px; color: var(--on-var); flex: 0 0 auto;
    }
    .tabs { display: flex; border-bottom: 1px solid var(--outline); flex: 0 0 auto; }
    .tab {
      flex: 1; height: 48px; display: flex; align-items: center; justify-content: center;
      font-size: 14px; font-weight: 500; color: var(--on-var);
    }
    .tab.on { color: var(--primary); box-shadow: inset 0 -3px 0 var(--primary); }
    .list { flex: 1; overflow: hidden; }
    .row { display: flex; align-items: center; gap: 12px; padding: 8px 16px; height: 64px; }
    .art { border-radius: 8px; flex: 0 0 auto; display: flex; align-items: center; justify-content: center; }
    .meta { flex: 1; min-width: 0; }
    .t1 { font-size: 16px; line-height: 22px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .t2 { font-size: 12px; line-height: 16px; color: var(--on-var); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .dur { font-size: 12px; color: var(--on-var); font-variant-numeric: tabular-nums; }
    .rail { width: 24px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 1px; padding: 8px 0; }
    .rail span { font-size: 10px; line-height: 12px; color: rgba(151,163,191,.35); }
    .rail span.on { color: var(--primary); }
    .mini { background: var(--surface-hi); flex: 0 0 auto; }
    .mini-row { display: flex; align-items: center; gap: 12px; padding: 8px 12px; }
    .bar { height: 4px; background: var(--outline); }
    .bar i { display: block; height: 4px; background: var(--primary); }
    .navspace { height: 24px; background: var(--surface-hi); }
    .section { font-size: 14px; font-weight: 500; color: var(--primary); padding: 16px 16px 4px; }
    .btn {
      height: 48px; border-radius: 24px; display: flex; align-items: center; justify-content: center;
      gap: 8px; font-size: 14px; font-weight: 500; flex: 1;
    }
    .btn.fill { background: var(--primary); color: var(--on-primary); }
    .btn.out { border: 1px solid var(--outline); color: var(--on); }
    a { color: var(--primary); } a:hover { color: var(--coral); }
"""

def page(body, extra=""):
    return f"""<!doctype html>
<html>
<head>
  <meta charset="utf-8">
  <script src="./support.js"></script>
</head>
<body>
<x-dc>
<helmet>
  <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Roboto:wght@400;500&display=swap">
  <style>{STYLE}{extra}
  </style>
</helmet>
{body}
</x-dc>
</body>
</html>
"""

def icon(d, size=24, color="currentColor", fill="none", width="2"):
    return (f'<svg width="{size}" height="{size}" viewBox="0 0 24 24" fill="{fill}" '
            f'stroke="{color}" stroke-width="{width}" stroke-linecap="round" stroke-linejoin="round">{d}</svg>')

I = {
  "search": '<circle cx="11" cy="11" r="7"></circle><path d="M20 20l-3.5-3.5"></path>',
  "sort": '<path d="M4 7h10M4 12h7M4 17h4"></path><path d="M17 5v14M17 19l-3-3M17 19l3-3"></path>',
  "refresh": '<path d="M20 11a8 8 0 1 0-2.3 6.3"></path><path d="M20 5v6h-6"></path>',
  "gear": '<circle cx="12" cy="12" r="3"></circle><path d="M12 3v2M12 19v2M3 12h2M19 12h2M5.6 5.6l1.4 1.4M17 17l1.4 1.4M18.4 5.6L17 7M7 17l-1.4 1.4"></path>',
  "note": '<circle cx="8" cy="17" r="3"></circle><path d="M11 17V5l9-2v12"></path><circle cx="17" cy="15" r="3"></circle>',
  "play": '<path d="M7 4l13 8-13 8z"></path>',
  "pause": '<path d="M8 4v16M16 4v16"></path>',
  "next": '<path d="M6 4l10 8-10 8z"></path><path d="M19 4v16"></path>',
  "prev": '<path d="M18 4L8 12l10 8z"></path><path d="M5 4v16"></path>',
  "shuffle": '<path d="M3 6h4l10 12h4M17 4l3 2-3 2"></path><path d="M3 18h4l3-3.5M17 20l3-2-3-2"></path>',
  "repeat": '<path d="M4 9a3 3 0 0 1 3-3h13"></path><path d="M17 3l3 3-3 3"></path><path d="M20 15a3 3 0 0 1-3 3H4"></path><path d="M7 21l-3-3 3-3"></path>',
  "down": '<path d="M6 9l6 6 6-6"></path>',
  "back": '<path d="M19 12H5"></path><path d="M12 19l-7-7 7-7"></path>',
  "queue": '<path d="M4 6h11M4 11h11M4 16h7"></path><path d="M17 13v7"></path><circle cx="15" cy="20" r="2"></circle>',
  "drag": '<path d="M5 9h14M5 15h14"></path>',
  "volume": '<path d="M5 10v4h3l4 3V7l-4 3z"></path><path d="M16 9a4 4 0 0 1 0 6"></path>',
  "folder": '<path d="M3 7a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path>',
  "person": '<circle cx="12" cy="8" r="3.5"></circle><path d="M5 20a7 7 0 0 1 14 0"></path>',
  "disc": '<circle cx="12" cy="12" r="8.5"></circle><circle cx="12" cy="12" r="2"></circle>',
  "trash": '<path d="M4 7h16M9 7V5h6v2M6 7l1 13h10l1-13"></path>',
  "plus": '<path d="M12 5v14M5 12h14"></path>',
}

# Pochettes : aplats tires de l'icone, avec la note en surimpression.
COVERS = ["#0286AE", "#00A6A8", "#3A5BB8", "#C4633F", "#1E7F8C", "#7A4B8F"]

def art(size, i=0, radius=8, icon_size=None):
    c = COVERS[i % len(COVERS)]
    s = icon_size or max(14, size // 3)
    return (f'<div class="art" style="width:{size}px;height:{size}px;background:{c};'
            f'border-radius:{radius}px">{icon(I["note"], s, "rgba(255,255,255,.85)", "none", "1.6")}</div>')

def appbar(title, icons, back=False):
    lead = f'<div class="iconbtn">{icon(I["back"])}</div>' if back else ''
    acts = "".join(f'<div class="iconbtn">{icon(I[n])}</div>' for n in icons)
    pad = 'padding: 0 4px;' if back else ''
    return (f'<div class="appbar" style="{pad}">{lead}<h1>{title}</h1>{acts}</div>')

def tabs(active):
    names = ["Songs", "Albums", "Artists", "Folders"]
    return '<div class="tabs">' + "".join(
        f'<div class="tab{" on" if n == active else ""}">{n}</div>' for n in names) + '</div>'

def song_row(title, artist, album, dur, i):
    return (f'<div class="row">{art(48, i)}'
            f'<div class="meta"><div class="t1">{title}</div>'
            f'<div class="t2">{artist} · {album}</div></div>'
            f'<div class="dur">{dur}</div></div>')

def mini(playing=True, progress=42):
    ic = I["pause"] if playing else I["play"]
    return f"""<div class="mini">
  <div class="mini-row">{art(44, 1, 6)}
    <div class="meta"><div class="t1" style="font-size:14px">Digital love</div>
      <div class="t2">Daft Punk</div></div>
    <div class="iconbtn" style="width:44px;height:44px;color:var(--on)">{icon(ic, 24, "currentColor", "currentColor" if ic == I["play"] else "none")}</div>
    <div class="iconbtn" style="width:44px;height:44px;color:var(--on)">{icon(I["next"], 24, "currentColor", "currentColor")}</div>
  </div>
  <div class="bar"><i style="width:{progress}%"></i></div>
  <div class="navspace"></div>
</div>"""

SONGS = [
    ("9 Lives", "Wovenhand", "Consider the Birds", "4:12"),
    ("Aerodynamic", "Daft Punk", "Discovery", "3:27"),
    ("All Blues", "Miles Davis", "Kind of Blue", "11:33"),
    ("Blue in Green", "Miles Davis", "Kind of Blue", "5:37"),
    ("Digital Love", "Daft Punk", "Discovery", "5:01"),
    ("Dreams", "Fleetwood Mac", "Rumours", "4:14"),
    ("Go Your Own Way", "Fleetwood Mac", "Rumours", "3:38"),
]

# 1. Bibliotheque, onglet Morceaux
rail = "".join(
    f'<span class="{"on" if l in "#ABDG" else ""}">{l}</span>'
    for l in ["#"] + [chr(c) for c in range(65, 91)])
rows = "".join(song_row(t, a, al, d, i) for i, (t, a, al, d) in enumerate(SONGS))
body = f"""<div class="screen">
{appbar("Library", ["search", "sort", "refresh", "gear"])}
{tabs("Songs")}
<div style="display:flex;flex:1;min-height:0">
  <div class="list" style="flex:1">{rows}</div>
  <div class="rail">{rail}</div>
</div>
{mini()}
</div>"""
(OUT / "Main.dc.html").write_text(page(body), encoding="utf-8")

# 2. Bibliotheque, onglet Albums
ALBUMS = [("Discovery", "Daft Punk"), ("Kind of Blue", "Miles Davis"),
          ("Rumours", "Fleetwood Mac"), ("Blue Train", "John Coltrane"),
          ("Consider the Birds", "Wovenhand"), ("Homework", "Daft Punk")]
cards = "".join(
    f"""<div>{art(163, i, 12, 48)}
    <div class="t1" style="font-size:14px;margin-top:8px">{t}</div>
    <div class="t2">{a}</div></div>""" for i, (t, a) in enumerate(ALBUMS))
body = f"""<div class="screen">
{appbar("Library", ["search", "sort", "refresh", "gear"])}
{tabs("Albums")}
<div class="list" style="padding:8px 8px 0">
  <div style="display:grid;grid-template-columns:repeat(2, minmax(0, 1fr));gap:16px 8px;padding:8px">{cards}</div>
</div>
{mini()}
</div>"""
(OUT / "LibraryAlbums.dc.html").write_text(page(body), encoding="utf-8")
print("1-2 ok")

# 3. Detail album
TRACKS = [("One More Time", "5:20"), ("Aerodynamic", "3:27"), ("Digital Love", "5:01"),
          ("Harder, Better, Faster, Stronger", "3:45"), ("Crescendolls", "3:31"),
          ("Nightvision", "1:44"), ("Superheroes", "3:57")]
tr = "".join(
    f'<div class="row" style="height:56px">{art(40, (i + 1) % 6, 6)}'
    f'<div class="meta"><div class="t1" style="font-size:15px">{t}</div></div>'
    f'<div class="dur">{d}</div></div>' for i, (t, d) in enumerate(TRACKS))
body = f"""<div class="screen">
{appbar("Discovery", [], back=True)}
<div style="display:flex;gap:16px;padding:0 16px 16px;align-items:center">
  {art(96, 0, 12, 34)}
  <div>
    <div style="font-size:16px">Daft Punk</div>
    <div class="t2" style="margin-top:4px">14 tracks · 1:00:52</div>
  </div>
</div>
<div style="display:flex;gap:12px;padding:0 16px 16px">
  <div class="btn fill">{icon(I["play"], 20, "currentColor", "currentColor")}<span>Play all</span></div>
  <div class="btn out">{icon(I["shuffle"], 20)}<span>Shuffle</span></div>
</div>
<div class="list">{tr}</div>
{mini()}
</div>"""
(OUT / "AlbumDetail.dc.html").write_text(page(body), encoding="utf-8")

# 4. Lecteur complet
body = f"""<div class="screen">
<div class="appbar" style="padding:0 4px">
  <div class="iconbtn">{icon(I["down"])}</div>
  <h1 style="font-size:14px;font-weight:500;text-align:center;color:var(--on-var)">Discovery</h1>
  <div class="iconbtn">{icon(I["queue"])}</div>
</div>
<div style="padding:8px 24px 0">{art(342, 0, 16, 96)}</div>
<div style="padding:24px 24px 0">
  <div style="font-size:24px;line-height:30px">Digital Love</div>
  <div style="font-size:16px;color:var(--on-var);margin-top:4px">Daft Punk</div>
</div>
<div style="padding:24px 24px 0">
  <div style="height:4px;background:var(--outline);border-radius:2px;position:relative">
    <div style="width:42%;height:4px;background:var(--on);border-radius:2px"></div>
    <div style="position:absolute;left:42%;top:-6px;width:16px;height:16px;border-radius:8px;background:var(--on);margin-left:-8px"></div>
  </div>
  <div style="display:flex;justify-content:space-between;margin-top:10px">
    <span class="dur">2:07</span><span class="dur">5:01</span>
  </div>
</div>
<div style="display:flex;align-items:center;justify-content:space-between;padding:20px 24px 0">
  <div class="iconbtn" style="color:var(--primary)">{icon(I["shuffle"])}</div>
  <div class="iconbtn" style="color:var(--on)">{icon(I["prev"], 32, "currentColor", "currentColor")}</div>
  <div style="width:64px;height:64px;border-radius:32px;background:var(--primary);color:var(--on-primary);display:flex;align-items:center;justify-content:center">{icon(I["pause"], 30, "currentColor", "none", "2.5")}</div>
  <div class="iconbtn" style="color:var(--on)">{icon(I["next"], 32, "currentColor", "currentColor")}</div>
  <div class="iconbtn">{icon(I["repeat"])}</div>
</div>
<div style="flex:1"></div>
<div style="text-align:center;font-size:12px;color:var(--on-var);padding-bottom:32px">Track 3 of 14</div>
</div>"""
(OUT / "NowPlaying.dc.html").write_text(page(body), encoding="utf-8")

# 5. File d'attente
QUEUE = [("Digital Love", "Daft Punk", True), ("Harder, Better, Faster, Stronger", "Daft Punk", False),
         ("Crescendolls", "Daft Punk", False), ("Nightvision", "Daft Punk", False),
         ("Superheroes", "Daft Punk", False), ("High Life", "Daft Punk", False),
         ("Something About Us", "Daft Punk", False), ("Voyager", "Daft Punk", False)]
qr = ""
for i, (t, a, cur) in enumerate(QUEUE):
    bg = "background:var(--surface-sel);" if cur else ""
    tail = f'<div style="color:var(--primary)">{icon(I["volume"], 20)}</div>' if cur else ""
    qr += (f'<div class="row" style="height:64px;{bg}">'
           f'<div style="color:var(--on-var)">{icon(I["drag"], 20)}</div>{art(40, i % 6, 6)}'
           f'<div class="meta"><div class="t1" style="font-size:15px">{t}</div>'
           f'<div class="t2">{a}</div></div>{tail}</div>')
body = f"""<div class="screen">
<div class="appbar" style="padding:0 4px">
  <div class="iconbtn">{icon(I["back"])}</div>
  <h1>Queue</h1>
  <div class="iconbtn" style="color:var(--primary)">{icon(I["shuffle"])}</div>
  <div style="height:48px;display:flex;align-items:center;padding:0 12px;font-size:14px;font-weight:500;color:var(--primary)">Clear</div>
</div>
<div class="list">{qr}</div>
{mini()}
</div>"""
(OUT / "Queue.dc.html").write_text(page(body), encoding="utf-8")
print("3-5 ok")

# 6. Recherche
def plain_row(ic, t, s, i=None):
    left = art(48, i, 8) if i is not None else (
        f'<div class="art" style="width:48px;height:48px;background:var(--surface-hi);border-radius:24px;color:var(--on-var)">{icon(ic, 22)}</div>')
    return (f'<div class="row">{left}<div class="meta"><div class="t1">{t}</div>'
            f'<div class="t2">{s}</div></div></div>')

body = f"""<div class="screen">
<div class="appbar" style="padding:0 4px;height:72px">
  <div class="iconbtn">{icon(I["back"])}</div>
  <div style="flex:1;height:48px;background:var(--surface-hi);border-radius:24px;display:flex;align-items:center;gap:12px;padding:0 16px">
    <span style="color:var(--on-var)">{icon(I["search"], 20)}</span>
    <span style="font-size:16px">blue</span>
    <span style="width:2px;height:20px;background:var(--primary)"></span>
  </div>
  <div style="width:8px"></div>
</div>
<div class="list">
  <div class="section">Songs</div>
  {song_row("Blue in Green", "Miles Davis", "Kind of Blue", "5:37", 1)}
  {song_row("All Blues", "Miles Davis", "Kind of Blue", "11:33", 1)}
  <div class="section">Albums</div>
  {plain_row(I["disc"], "Kind of Blue", "Miles Davis · 5 tracks", 1)}
  {plain_row(I["disc"], "Blue Train", "John Coltrane · 5 tracks", 3)}
  <div class="section">Artists</div>
  {plain_row(I["person"], "Blue Öyster Cult", "3 albums · 26 songs")}
  <div class="section">Folders</div>
  {plain_row(I["folder"], "Blue Note", "/Music/Jazz/Blue Note · 42 songs")}
</div>
{mini()}
</div>"""
(OUT / "Search.dc.html").write_text(page(body), encoding="utf-8")

# 7. Reglages
def radio(label, on):
    dot = ('<div style="width:20px;height:20px;border-radius:10px;border:2px solid var(--primary);display:flex;align-items:center;justify-content:center">'
           '<div style="width:10px;height:10px;border-radius:5px;background:var(--primary)"></div></div>') if on else \
          '<div style="width:20px;height:20px;border-radius:10px;border:2px solid var(--on-var)"></div>'
    return (f'<div style="display:flex;align-items:center;gap:16px;height:48px;padding:0 16px">'
            f'{dot}<span style="font-size:16px">{label}</span></div>')

def switch(on=True):
    return ('<div style="width:52px;height:32px;border-radius:16px;background:var(--primary);display:flex;align-items:center;justify-content:flex-end;padding:0 4px">'
            '<div style="width:24px;height:24px;border-radius:12px;background:var(--on-primary)"></div></div>')

body = f"""<div class="screen">
{appbar("Settings", [], back=True)}
<div class="list">
  <div class="section">Appearance</div>
  {radio("Follow the system", True)}
  {radio("Light", False)}
  {radio("Dark", False)}
  <div style="display:flex;align-items:center;gap:16px;padding:8px 16px;height:64px">
    <div class="meta"><div class="t1">Dynamic color</div>
      <div class="t2">Take the palette from the wallpaper</div></div>{switch()}
  </div>
  <div style="height:1px;background:var(--outline);margin:8px 16px"></div>
  <div class="section">Library</div>
  <div style="padding:4px 16px 0">
    <div class="t1">Ignore tracks shorter than 30 s</div>
    <div class="t2" style="margin-top:2px">Keeps ringtones and voice memos out of the library.</div>
    <div style="height:4px;background:var(--outline);border-radius:2px;margin:20px 0 8px;position:relative">
      <div style="width:25%;height:4px;background:var(--primary);border-radius:2px"></div>
      <div style="position:absolute;left:25%;top:-8px;width:20px;height:20px;border-radius:10px;background:var(--primary);margin-left:-10px"></div>
    </div>
  </div>
  <div style="display:flex;align-items:center;gap:16px;padding:8px 16px;height:64px">
    <div class="meta"><div class="t1">Rescan library</div><div class="t2">412 tracks indexed</div></div>
    <div style="height:48px;display:flex;align-items:center;padding:0 12px;font-size:14px;font-weight:500;color:var(--primary)">Rescan</div>
  </div>
  <div style="height:1px;background:var(--outline);margin:8px 16px"></div>
  <div class="section">About</div>
  <div style="padding:0 16px 8px">
    <div class="t1" style="font-size:14px">Local music 0.1.0</div>
    <div class="t2" style="margin-top:4px">Offline player. No network permission, no accounts, no telemetry.</div>
  </div>
</div>
</div>"""
(OUT / "Settings.dc.html").write_text(page(body), encoding="utf-8")

# 8. Demande de permission
body = f"""<div class="screen" style="justify-content:center;align-items:center;padding:0 32px;text-align:center">
  <div style="width:96px;height:96px;border-radius:28px;background:#182464;display:flex;align-items:center;justify-content:center;margin-bottom:32px">
    {icon(I["note"], 48, "var(--primary)", "none", "1.6")}
  </div>
  <div style="font-size:24px;line-height:32px">Access to your audio files</div>
  <div style="font-size:16px;line-height:24px;color:var(--on-var);margin-top:16px;text-wrap:pretty">
    This app only reads the music already stored on this phone. It has no network access and sends nothing anywhere.
  </div>
  <div class="btn fill" style="flex:0 0 auto;margin-top:32px;padding:0 32px;min-width:200px">Grant access</div>
</div>"""
(OUT / "Permission.dc.html").write_text(page(body), encoding="utf-8")
print("6-8 ok")
