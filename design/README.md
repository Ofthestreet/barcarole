# Design canvas

`local-music-screens.html` is the published canvas — eight phone artboards for the app's screens.
The artboards themselves are the `.dc.html` files; `canvas.json` places them; `_gen.py` writes them
all from one shared stylesheet, so a palette or spacing change is a single edit plus `python3 _gen.py`.

The screens deliberately mirror the Compose code rather than reimagining it: 64dp rows, 48dp and
40dp artwork, the same top-bar actions and tab order, the same control order in the player. Palette
sampled from the app icon — navy `#0E1526`, turquoise `#00DCDF` accent, coral `#FF8F6C` held in
reserve. Roboto, because that is what Material 3 already renders in the app.
