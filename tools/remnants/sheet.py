"""Compose Blockbench preview renders into one labelled contact sheet for review."""
import glob
import os
import sys

from PIL import Image, ImageDraw


def contact_sheet(folder, out, cols=5, cell=(300, 343), crop=(100, 60, 800, 860)):
    files = sorted(glob.glob(os.path.join(folder, 'pose_*.png'))) + sorted(glob.glob(os.path.join(folder, 'preview_*.png')))
    w, h = cell
    rows = (len(files) + cols - 1) // cols
    sheet = Image.new('RGBA', (cols * w, rows * (h + 20)), (255, 255, 255, 255))
    draw = ImageDraw.Draw(sheet)
    for i, f in enumerate(files):
        im = Image.open(f).convert('RGBA').crop(crop).resize(cell)
        x, y = (i % cols) * w, (i // cols) * (h + 20)
        sheet.paste(im, (x, y + 20), im)
        draw.text((x + 5, y + 3), os.path.basename(f)[:-4], fill=(0, 0, 0, 255))
    sheet.save(out)
    return out


if __name__ == '__main__':
    print(contact_sheet(sys.argv[1], sys.argv[2]))
