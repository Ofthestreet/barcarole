# Design canvas

`local-music-screens.html` is the published canvas — eight phone artboards for the app's screens.
The artboards themselves are the `.dc.html` files; `canvas.json` places them; `_gen.py` writes them
all from one shared stylesheet, so a palette or spacing change is a single edit plus `python3 _gen.py`.

No artwork anywhere: the library has none, so rows are typographic, the album grid is a list (a grid
with nothing to show is just padding), and on the player the title takes the space a cover would
have had. Queue is the fourth tab in place of Folders, which moved into settings.

Otherwise the screens mirror the Compose code: 56dp text rows, the same top-bar actions, the same
control order in the player. Palette sampled from the app icon — navy `#0E1526`, turquoise
`#00DCDF` accent, coral `#FF8F6C` held in reserve. Roboto, because that is what Material 3 already
renders in the app.
