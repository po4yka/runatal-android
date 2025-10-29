# Fonts Directory

This directory contains the font files required for the Runic Quotes app.

## Required Fonts

Download and place the following font files in this directory:

### 1. Roboto Flex (Variable Font) - UI Typography
**Purpose:** Main UI text following Material 3 Expressive guidelines

- **Download:** [Google Fonts - Roboto Flex](https://fonts.google.com/specimen/Roboto+Flex)
- **File name:** `roboto_flex.ttf`
- **License:** Apache License 2.0 (Open Source)
- **Instructions:**
  1. Visit the Google Fonts page
  2. Click "Download family"
  3. Extract the ZIP file
  4. Locate `RobotoFlex-VariableFont_*.ttf` in the extracted folder
  5. Rename to `roboto_flex.ttf`
  6. Place in this directory

### 2. Noto Sans Runic - Primary Runic Font
**Purpose:** Elder Futhark and Younger Futhark runes (Unicode U+16A0–U+16FF)

- **Download:** [Google Fonts - Noto Sans Runic](https://fonts.google.com/noto/specimen/Noto+Sans+Runic)
- **File name:** `noto_sans_runic.ttf`
- **License:** SIL Open Font License (OFL)
- **Instructions:**
  1. Visit the Google Fonts page
  2. Click "Download family"
  3. Extract the ZIP file
  4. Locate `NotoSansRunic-Regular.ttf`
  5. Rename to `noto_sans_runic.ttf`
  6. Place in this directory

### 3. BabelStone Runic - Alternative Runic Font
**Purpose:** Alternative rendering for Elder/Younger Futhark with different style

- **Download:** [BabelStone Fonts](https://www.babelstone.co.uk/Fonts/Download/BabelStoneRunic.ttf)
- **File name:** `babelstone_runic.ttf`
- **License:** Free for personal and commercial use
- **Instructions:**
  1. Download directly from the link above
  2. Rename to `babelstone_runic.ttf` (if needed)
  3. Place in this directory

### 4. Cirth/Angerthas Font (Optional - For Future v2.0.0)
**Purpose:** Tolkien's Cirth runes (Angerthas Moria, Erebor)

Choose ONE of the following options:

#### Option A: Cirth Erebor
- **Download:** [DaFont - Cirth Erebor](https://www.dafont.com/cirth-erebor.font)
- **File name:** `erebor.ttf`
- **License:** ⚠️ Verify license before commercial use

#### Option B: Angerthas Moria
- **Download:** [DaFont - Angerthas Moria](https://www.dafont.com/angerthas-moria.font)
- **File name:** `angerthas_moria.ttf`
- **License:** ⚠️ Verify license before commercial use

**Note:** Cirth fonts are NOT required for MVP (v1.0.0). Only add them if planning to implement v2.0.0 Extended Scripts support.

## Directory Structure After Download

```
app/src/main/res/font/
├── FONTS_README.md          # This file
├── roboto_flex.ttf          # Material 3 UI font (REQUIRED)
├── noto_sans_runic.ttf      # Primary runic font (REQUIRED)
├── babelstone_runic.ttf     # Alternative runic font (REQUIRED)
└── erebor.ttf               # Cirth font (OPTIONAL - v2.0.0)
```

## Font Usage in Code

After downloading fonts, they can be used in Compose like this:

```kotlin
// In Typography.kt or similar
val RobotoFlex = FontFamily(
    Font(R.font.roboto_flex, FontWeight.Normal)
)

val NotoSansRunic = FontFamily(
    Font(R.font.noto_sans_runic, FontWeight.Normal)
)

val BabelStoneRunic = FontFamily(
    Font(R.font.babelstone_runic, FontWeight.Normal)
)

// Usage
Text(
    text = "ᚠᚢᚦᚨᚱᚲ", // Elder Futhark
    fontFamily = NotoSansRunic,
    fontSize = 24.sp
)
```

## License Compliance

| Font | License | Commercial Use | Attribution Required |
|------|---------|----------------|---------------------|
| Roboto Flex | Apache 2.0 | ✅ Yes | No |
| Noto Sans Runic | SIL OFL | ✅ Yes | No |
| BabelStone Runic | Free Use | ✅ Yes | No |
| Cirth fonts | ⚠️ Varies | ⚠️ Verify | ⚠️ Check license |

## Troubleshooting

### Font not displaying
- Verify the font file is placed in `app/src/main/res/font/`
- Ensure file name uses lowercase and underscores (e.g., `roboto_flex.ttf`)
- Clean and rebuild the project
- Check that the font file is not corrupted

### Runic characters showing as boxes
- Verify you're using Noto Sans Runic or BabelStone Runic for runic text
- Ensure the Unicode range U+16A0–U+16FF is supported by the font
- Test with known working runes: ᚠᚢᚦᚨᚱᚲ (FUTHARK)

## References

- [Material 3 Expressive Typography](https://m3.material.io/foundations/typography)
- [Unicode Runic Block](https://unicode.org/charts/PDF/U16A0.pdf)
- [Android Custom Fonts Guide](https://developer.android.com/develop/ui/views/text-and-emoji/fonts-in-xml)
