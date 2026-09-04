# Icon assets

`icon-source-1600.png` is the master: the generated artwork with the Craiyon watermark removed
and everything outside the rounded contour made transparent, so the adaptive icon's navy
background shows through whatever shape a launcher masks it to.

The launcher densities under `app/src/main/res/drawable-*dpi/ic_launcher_illustration.png` are
resized from it:

    for pair in mdpi:108 hdpi:162 xhdpi:216 xxhdpi:324 xxxhdpi:432; do
      d=${pair%%:*}; s=${pair##*:}
      sips -s format png -z $s $s art/icon-source-1600.png \
        --out app/src/main/res/drawable-$d/ic_launcher_illustration.png
    done

`icon-512.png` is the square version for a store listing.

Colours sampled from the artwork: navy `#182464` (icon background and theme seed backdrop),
turquoise `#00DCDF` (theme seed), sail blue `#0286AE`, wave `#76DAF3`, orange `#FF8F6C`.
