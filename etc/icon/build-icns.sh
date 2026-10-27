#!/bin/zsh
# Rebuilds src/main/resources/icons/tvrenamer.icns from draw.swift.
#
# macOS 26 shrinks any icon it does not recognise as its own squircle onto a
# grey tile. Two things keep this one out of that tile:
#
#   mask.png      Apple's exact icon outline, taken from how macOS itself
#                 renders an Icon Composer icon. An outline that is merely
#                 close, such as a superellipse, still gets the tile.
#   128pt and up  macOS gives the grey tile to any 16pt or 32pt image in an
#                 icns, even one cut to Apple's outline, but scales a 128pt
#                 image down cleanly. So the file carries nothing smaller.
#
# Needs Xcode's swift, ImageMagick (magick) and sips/iconutil from macOS.
set -euo pipefail

here=${0:a:h}
out=${here}/../../src/main/resources/icons/tvrenamer.icns
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

swift "$here/draw.swift" "$work/art.png"
magick "$work/art.png" \( "$here/mask.png" -colorspace gray \) \
       -alpha off -compose CopyOpacity -composite "$work/icon.png"

mkdir "$work/icon.iconset"
for size in 128 256 512; do
    sips -z $size $size "$work/icon.png" \
         --out "$work/icon.iconset/icon_${size}x${size}.png" >/dev/null
    sips -z $((size * 2)) $((size * 2)) "$work/icon.png" \
         --out "$work/icon.iconset/icon_${size}x${size}@2x.png" >/dev/null
done
iconutil -c icns -o "$out" "$work/icon.iconset"
