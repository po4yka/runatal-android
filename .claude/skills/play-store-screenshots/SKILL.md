---
name: play-store-screenshots
description: "Generate Google Play Store screenshots and feature graphic for the Runatal app. Use when: creating Play Store listing assets, marketing screenshots, feature graphic, or store listing images. Triggers on: play store, google play, screenshots, marketing, store listing, feature graphic."
---

# Google Play Screenshots Generator (Runatal)

## Overview

Build a Next.js page that renders Google Play Store screenshots as **advertisements** (not UI showcases) and exports them via `html-to-image` at Google Play's required resolutions. Screenshots are the single most important conversion asset on the Play Store.

**Google Play constraints:**
- Max 8 screenshots per device type (phone, 7" tablet, 10" tablet)
- Text overlay must not exceed 20% of the screenshot area
- No promotional text (pricing, rankings, awards)
- 24-bit PNG or JPEG only (no alpha transparency)
- Max 8 MB per image
- Minimum 2 screenshots to publish, 4+ recommended for visibility

## Core Principle

**Screenshots are advertisements, not documentation.** Every screenshot sells one idea. If you're showing UI, you're doing it wrong -- you're selling a *feeling*, an *outcome*, or killing a *pain point*.

## Step 1: Confirm Runatal Defaults with the User

The following brand details are pre-filled for Runatal. Confirm them and collect the remaining items.

### Pre-Filled (confirm, don't ask from scratch)

| Item | Default |
|------|---------|
| App name | Runatal |
| Brand colors | Background: `#0E1113` (dark) / `#F4F6F8` (light) |
| | Text: `#E0E2E5` (dark mode) / `#191C1E` (light mode) |
| | Accent: `#68707A` / `#C1C7D0` |
| | Surface: `#14171A` |
| Font | Roboto Flex (Google Fonts) |
| Style direction | Dark/moody, monochromatic, stone-carving aesthetic |
| Feature list | 1. Daily runic quotes 2. Three runic scripts (Elder Futhark, Younger Futhark, Cirth) 3. Live transliteration with animated reveal 4. Home screen widget 5. User-created quotes 6. Favorites system 7. Quote sharing 8. Material 3 with dynamic theming 9. Five app icon themes 10. Offline-first |

### Ask the User

1. **App screenshots** -- "Where are your app screenshots? (PNG files from Pixel emulator, use 6.1" Pixel 8 simulator)"
2. **App icon** -- "Where is your app icon PNG?"
3. **Number of slides** -- "How many screenshots do you want? (Google Play allows up to 8)"
4. **Tablet screenshots** -- "Do you have tablet screenshots (7" or 10")? We can generate tablet-size listings too."
5. **Feature Graphic** -- "Shall I generate a Feature Graphic (1024x500)? It's required for Play Store listings."
6. **Device frame** -- "Google Play discourages device frames. Use frameless (recommended) or CSS Pixel frame?"
7. **Localized screenshots** -- "Do you want screenshots in multiple languages? If yes, which languages?"
8. **Component assets** -- "Do you have any UI element PNGs (cards, widgets) for floating decorations?"
9. **Additional instructions** -- "Any specific requirements or preferences?"

### Derived from answers (do NOT ask -- decide yourself)

Based on the user's style direction, brand colors, and app aesthetic, decide:
- **Background style**: gradient direction, colors, whether light or dark base
- **Decorative elements**: rune-inspired glows, stone textures, subtle geometric shapes, or none
- **Dark vs light slides**: how many of each, which features suit dark treatment
- **Typography treatment**: weight, tracking, line height
- **Color palette**: derive secondary colors, shadow tints from the brand colors

**IMPORTANT:** If the user gives additional instructions at any point during the process, follow them. User instructions always override skill defaults.

## Step 2: Set Up the Project

### Detect Package Manager

Check what's available, use this priority: **bun > pnpm > yarn > npm**

```bash
# Check in order
which bun && echo "use bun" || which pnpm && echo "use pnpm" || which yarn && echo "use yarn" || echo "use npm"
```

### Scaffold (if no existing Next.js project)

```bash
# With bun:
bunx create-next-app@latest . --typescript --tailwind --app --src-dir --no-eslint --import-alias "@/*"
bun add html-to-image

# With pnpm:
pnpx create-next-app@latest . --typescript --tailwind --app --src-dir --no-eslint --import-alias "@/*"
pnpm add html-to-image

# With yarn:
yarn create next-app . --typescript --tailwind --app --src-dir --no-eslint --import-alias "@/*"
yarn add html-to-image

# With npm:
npx create-next-app@latest . --typescript --tailwind --app --src-dir --no-eslint --import-alias "@/*"
npm install html-to-image
```

### File Structure

```
project/
├── public/
│   ├── app-icon.png            # Runatal app icon
│   ├── screenshots/            # Phone screenshots (from Pixel emulator)
│   │   ├── home.png
│   │   ├── feature-1.png
│   │   └── ...
│   └── screenshots-tablet/     # Tablet screenshots (optional)
│       ├── home.png
│       └── ...
├── src/app/
│   ├── layout.tsx              # Roboto Flex font setup
│   └── page.tsx                # The screenshot generator (single file)
└── package.json
```

No mockup PNG is needed. Phone frames are CSS-only (optional) and frameless is the default.

**Multi-language:** nest screenshots under a locale folder per language. The generator switches the `base` path; all slide image srcs stay identical.

```
└── screenshots/
    ├── en/
    │   ├── home.png
    │   └── ...
    ├── de/
    │   └── ...
    └── {locale}/
```

**The entire generator is a single `page.tsx` file.** No routing, no extra layouts, no API routes.

### Multi-language: Locale Tabs

Add a `LOCALES` array and locale tabs to the toolbar. Every slide src uses `base` -- no hardcoded paths:

```tsx
const LOCALES = ["en", "de", "es"] as const; // use whatever langs were defined
type Locale = typeof LOCALES[number];

// In ScreenshotsPage:
const [locale, setLocale] = useState<Locale>("en");
const base = `/screenshots/${locale}`;

// Toolbar tabs:
{LOCALES.map(l => (
  <button key={l} onClick={() => setLocale(l)}
    style={{ fontWeight: locale === l ? 700 : 400 }}>
    {l.toUpperCase()}
  </button>
))}

// In every slide -- unchanged between single and multi-language:
<Screenshot src={`${base}/home.png`} alt="Home" />
```

### Font Setup

```tsx
// src/app/layout.tsx
import { Roboto_Flex } from "next/font/google";
const font = Roboto_Flex({ subsets: ["latin"] });

export default function Layout({ children }: { children: React.ReactNode }) {
  return <html><body className={font.className}>{children}</body></html>;
}
```

## Step 3: Plan the Slides

### Screenshot Framework (Narrative Arc)

Adapt this framework to the user's requested slide count (max 8). Not all slots are required -- pick what fits:

| Slot | Purpose | Runatal Suggestion |
|------|---------|-------------------|
| #1 | **Hero / Main Benefit** | Daily quote with runes on dark bg. "Ancient wisdom, one rune at a time" |
| #2 | **Differentiator** | Three runic scripts showcase. "Three scripts. Thousands of years." |
| #3 | **Core Feature** | Live transliteration animation. "Watch letters become runes" |
| #4 | **Ecosystem** | Home screen widget. "Runes on your home screen" |
| #5 | **Core Feature** | User-created quotes. "Carve your own words" |
| #6 | **Core Feature** | Favorites + sharing. "Keep. Share. Inspire." |
| #7 | **Trust Signal** | Theming / icon variants. "Five looks. One soul." |
| #8 | **More Features** | Feature pills + coming soon. "And so much more." |

**Rules:**
- Each slide sells ONE idea. Never two features on one slide.
- Vary layouts across slides -- never repeat the same template structure.
- Include 1-2 contrast slides (inverted bg) for visual rhythm.
- **Text overlay must not exceed 20% of the screenshot area.**
- **No promotional pricing, rankings, or award claims.**

## Step 4: Write Copy FIRST

Get all headlines approved before building layouts. Bad copy ruins good design.

### The Iron Rules

1. **One idea per headline.** Never join two things with "and."
2. **Short, common words.** 1-2 syllables. No jargon unless it's domain-specific.
3. **3-5 words per line.** Must be readable at thumbnail size in the Play Store.
4. **Line breaks are intentional.** Control where lines break with `<br />`.
5. **Max 20% text overlay.** Keep text compact and high on the slide.

### Three Approaches (pick one per slide)

| Type | What it does | Example |
|------|-------------|---------|
| **Paint a moment** | You picture yourself doing it | "Glance at runes over morning coffee." |
| **State an outcome** | What your life looks like after | "Your words, carved in ancient script." |
| **Kill a pain** | Name a problem and destroy it | "Never Google a rune chart again." |

### What NEVER Works

- **Feature lists as headlines**: "Transliterate text into Elder Futhark, Younger Futhark, and Cirth"
- **Two ideas joined by "and"**: "Create quotes and share with friends"
- **Compound clauses**: "Save and customize quotes for every script you like"
- **Vague aspirational**: "Every rune, transliterated"
- **Marketing buzzwords**: "AI-powered rune engine" (unless it's actually AI)
- **Promotional text**: "Best rune app 2026" or "Free for limited time"

### Bad-to-Better Headline Examples (Runatal)

| Weak | Better | Why it wins |
|------|--------|-------------|
| Transliterate text into three runic scripts | Three scripts. Thousands of years. | emotional scale, not a feature list |
| Add and manage your own quotes with runic transliteration | Carve your own words | action verb, concrete metaphor |
| Beautiful Material 3 design with five icon themes | Five looks. One soul. | poetic, memorable, sells personality |
| View daily inspirational quotes in runes | Ancient wisdom, daily | shorter, warmer, more evocative |
| Share quotes as images with friends | Send runes to the world | bigger feeling, not a UI description |

### Copy Process

1. Write 3 options per slide using the three approaches
2. Read each at arm's length -- if you can't parse it in 1 second, it's too complex
3. Check: does each line have 3-5 words? If not, adjust line breaks
4. Verify total text fits within 20% of the screenshot area
5. Present options to the user with reasoning for each

### Example Prompt Shape

If the user gives a vague request like "make Play Store screenshots," reshape it using the pre-filled defaults:

```text
Generate Google Play screenshots for Runatal, an Android app
that displays inspiring quotes transliterated into ancient runic scripts.
The app's main strengths are three runic scripts, live animated transliteration,
and a home screen widget. I want 8 slides, dark/moody style with
monochromatic tones, and a timeless stone-carving aesthetic.
```

The pattern is: app name + core outcome, top features in priority order, slide count, style direction.

### Reference Apps for Copy Style

- **Raycast** -- specific, descriptive, one concrete value per slide
- **Turf** -- ultra-simple action verbs, conversational
- **Mela / Notion** -- warm, minimal, elegant

## Step 5: Build the Page

### Architecture

```
page.tsx
├── Constants (PHONE_W/H, TABLET sizes, FEATURE_GRAPHIC, BRAND tokens)
├── Screenshot component (frameless default -- rounded-rect + shadow)
├── PixelPhone component (CSS-only frame, optional)
├── Tablet component (CSS-only frame)
├── Caption component (label + headline, accepts canvasW for scaling)
├── Decorative components (blobs, glows, rune-inspired shapes)
├── phoneSlide1..N components (one per slide)
├── tabletSlide1..N components (optional, reuse phone designs)
├── FeatureGraphic component (1024x500 landscape)
├── PHONE_SCREENSHOTS / TABLET_SCREENSHOTS arrays (registries)
├── ScreenshotPreview (ResizeObserver scaling + hover export)
└── ScreenshotsPage (grid + device toggle + size dropdown + export logic)
```

### Export Sizes (Google Play, portrait)

#### Phone

```typescript
const PHONE_W = 1080;
const PHONE_H = 1920;

const PHONE_SIZES = [
  { label: "Phone", w: 1080, h: 1920 },
] as const;
```

Design at 1080x1920 (single target resolution).

#### Tablet (Optional)

If the user provides tablet screenshots, also generate tablet Play Store screenshots:

```typescript
const TABLET_SIZES = [
  { label: '7" Tablet', w: 1200, h: 1920 },
  { label: '10" Tablet', w: 1800, h: 2560 },
] as const;
```

#### Device Toggle

When supporting both devices, add a toggle (Phone / Tablet) in the toolbar next to the size dropdown. The size dropdown should switch between phone and tablet sizes based on the selected device. Support a `?device=tablet` URL parameter for headless/automated capture workflows.

### Rendering Strategy

Each screenshot is designed at full resolution (1080x1920px for phone). Two copies exist:

1. **Preview**: CSS `transform: scale()` via ResizeObserver to fit a grid card
2. **Export**: Offscreen at `position: absolute; left: -9999px` at true resolution

### Runatal Brand Tokens

```typescript
const BRAND = {
  bg: "#0E1113",
  bgLight: "#F4F6F8",
  surface: "#14171A",
  text: "#E0E2E5",
  textDark: "#191C1E",
  accent: "#68707A",
  accentLight: "#C1C7D0",
} as const;
```

### Screenshot Component (Frameless -- Default)

Google Play discourages device frames. The default renders the app screenshot in a styled container with no device chrome.

```tsx
function Screenshot({ src, alt, style, className = "" }: {
  src: string; alt: string; style?: React.CSSProperties; className?: string;
}) {
  return (
    <div className={`relative ${className}`}
      style={{ aspectRatio: `${PHONE_W}/${PHONE_H}`, ...style }}>
      <div style={{
        width: "100%", height: "100%",
        borderRadius: "3.5%", overflow: "hidden",
        boxShadow: "0 8px 40px rgba(0,0,0,0.3)",
      }}>
        <img src={src} alt={alt}
          style={{ display: "block", width: "100%", height: "100%",
            objectFit: "cover", objectPosition: "top" }}
          draggable={false} />
      </div>
    </div>
  );
}
```

### PixelPhone Component (CSS-Only Frame -- Optional)

If the user requests device frames despite Google's guidance, use a CSS-only Pixel-style frame. No PNG mockup needed.

```tsx
function PixelPhone({ src, alt, style, className = "" }: {
  src: string; alt: string; style?: React.CSSProperties; className?: string;
}) {
  return (
    <div className={`relative ${className}`}
      style={{ aspectRatio: "440/900", ...style }}>
      <div style={{
        width: "100%", height: "100%", borderRadius: "8% / 4%",
        background: "linear-gradient(180deg, #2C2C2E 0%, #1C1C1E 100%)",
        position: "relative", overflow: "hidden",
        boxShadow: "inset 0 0 0 1px rgba(255,255,255,0.1), 0 8px 40px rgba(0,0,0,0.6)",
      }}>
        {/* Camera cutout */}
        <div style={{
          position: "absolute", top: "1.8%", left: "50%",
          transform: "translateX(-50%)", width: "8%", height: "1.2%",
          borderRadius: "50%", background: "#111113",
          border: "1px solid rgba(255,255,255,0.08)", zIndex: 20,
        }} />
        {/* Bezel edge highlight */}
        <div style={{
          position: "absolute", inset: 0, borderRadius: "8% / 4%",
          border: "1px solid rgba(255,255,255,0.06)",
          pointerEvents: "none", zIndex: 15,
        }} />
        {/* Screen area */}
        <div style={{
          position: "absolute", left: "3.5%", top: "3%",
          width: "93%", height: "94%",
          borderRadius: "4% / 2%", overflow: "hidden", background: "#000",
        }}>
          <img src={src} alt={alt}
            style={{ display: "block", width: "100%", height: "100%",
              objectFit: "cover", objectPosition: "top" }}
            draggable={false} />
        </div>
      </div>
    </div>
  );
}
```

### Tablet Component (CSS-Only)

```tsx
function Tablet({ src, alt, style, className = "" }: {
  src: string; alt: string; style?: React.CSSProperties; className?: string;
}) {
  return (
    <div className={`relative ${className}`}
      style={{ aspectRatio: "770/1000", ...style }}>
      <div style={{
        width: "100%", height: "100%", borderRadius: "5% / 3.6%",
        background: "linear-gradient(180deg, #2C2C2E 0%, #1C1C1E 100%)",
        position: "relative", overflow: "hidden",
        boxShadow: "inset 0 0 0 1px rgba(255,255,255,0.1), 0 8px 40px rgba(0,0,0,0.6)",
      }}>
        {/* Front camera dot */}
        <div style={{
          position: "absolute", top: "1.2%", left: "50%",
          transform: "translateX(-50%)", width: "0.9%", height: "0.65%",
          borderRadius: "50%", background: "#111113",
          border: "1px solid rgba(255,255,255,0.08)", zIndex: 20,
        }} />
        {/* Bezel edge highlight */}
        <div style={{
          position: "absolute", inset: 0, borderRadius: "5% / 3.6%",
          border: "1px solid rgba(255,255,255,0.06)",
          pointerEvents: "none", zIndex: 15,
        }} />
        {/* Screen area */}
        <div style={{
          position: "absolute", left: "4%", top: "2.8%",
          width: "92%", height: "94.4%",
          borderRadius: "2.2% / 1.6%", overflow: "hidden", background: "#000",
        }}>
          <img src={src} alt={alt}
            style={{ display: "block", width: "100%", height: "100%",
              objectFit: "cover", objectPosition: "top" }}
            draggable={false} />
        </div>
      </div>
    </div>
  );
}
```

**Tablet layout adjustments vs Phone:**
- Use `width: "65-70%"` for tablet mockups (vs 82-86% for phone)
- Two-tablet layouts work the same as two-phone layouts but with adjusted widths
- Caption font sizes should scale from `canvasW` (1200 or 1800 for tablet vs 1080 for phone)
- Same slide designs/copy can be reused -- just swap Screenshot/PixelPhone for Tablet and adjust positioning

### Typography (Resolution-Independent)

All sizing relative to canvas width W (1080 for phone):

| Element | Size | Weight | Line Height |
|---------|------|--------|-------------|
| Category label | `W * 0.032` (~35px) | 600 (semibold) | default |
| Headline | `W * 0.095` to `W * 0.11` (~103-119px) | 700 (bold) | 1.0 |
| Hero headline | `W * 0.11` (~119px) | 700 (bold) | 0.92 |

### Phone Placement Patterns

Vary across slides -- NEVER use the same layout twice in a row:

**Centered phone** (hero, single-feature):
```
bottom: 0, width: "82-86%", translateX(-50%) translateY(12-14%)
```

**Two phones layered** (comparison):
```
Back: left: "-8%", width: "65%", rotate(-4deg), opacity: 0.55
Front: right: "-4%", width: "82%", translateY(10%)
```

**Phone + floating elements** (only if user provided component PNGs):
```
Cards should NOT block the phone's main content.
Position at edges, slight rotation (2-5deg), drop shadows.
If distracting, push partially off-screen or make smaller.
```

### "More Features" Slide (Optional)

Dark/contrast background with app icon, headline ("And so much more."), and feature pills. Can include a "Coming Soon" section with dimmer pills.

## Step 5.5: Feature Graphic

The Feature Graphic (1024x500) is required for all Play Store listings. Generate it as part of the screenshot page.

### Dimensions

```typescript
const FEATURE_GRAPHIC = { w: 1024, h: 500 };
```

### Layout

- **Landscape format** (~2:1 aspect ratio)
- **Content**: App icon (centered or left-aligned), "Runatal" app name, tagline
- **Background**: Brand gradient (`#0E1113` to `#14171A`) with subtle rune-inspired decorative elements
- **No device frames** in the feature graphic
- **Minimal text, large and readable** at thumbnail size (the graphic appears small in search results)
- **24-bit PNG** (no alpha) -- set `backgroundColor` in export options

### Example Component

```tsx
function FeatureGraphic() {
  return (
    <div style={{
      width: FEATURE_GRAPHIC.w, height: FEATURE_GRAPHIC.h,
      background: `linear-gradient(135deg, ${BRAND.bg} 0%, ${BRAND.surface} 100%)`,
      display: "flex", alignItems: "center", justifyContent: "center",
      flexDirection: "column", gap: 16, position: "relative", overflow: "hidden",
    }}>
      {/* Decorative rune-inspired elements */}
      <img src="/app-icon.png" alt=""
        style={{ width: 120, height: 120, borderRadius: 24 }} />
      <div style={{
        color: BRAND.text, fontSize: 48, fontWeight: 700,
        letterSpacing: "-0.02em",
      }}>
        Runatal
      </div>
      <div style={{
        color: BRAND.accentLight, fontSize: 20, fontWeight: 400,
      }}>
        Ancient wisdom, one rune at a time
      </div>
    </div>
  );
}
```

## Step 6: Export

### Why html-to-image, NOT html2canvas

`html2canvas` breaks on CSS filters, gradients, drop-shadow, backdrop-filter, and complex clipping. `html-to-image` uses native browser SVG serialization -- handles all CSS faithfully.

### Export Implementation

```typescript
import { toPng } from "html-to-image";

// Before capture: move element on-screen
el.style.left = "0px";
el.style.opacity = "1";
el.style.zIndex = "-1";

const opts = {
  width: W, height: H, pixelRatio: 1, cacheBust: true,
  backgroundColor: '#0E1113', // Strip alpha -- Google Play requires 24-bit PNG
};

// CRITICAL: Double-call trick -- first warms up fonts/images, second produces clean output
await toPng(el, opts);
const dataUrl = await toPng(el, opts);

// After capture: move back off-screen
el.style.left = "-9999px";
el.style.opacity = "";
el.style.zIndex = "";
```

### Key Rules

- **Double-call trick**: First `toPng()` loads fonts/images lazily. Second produces clean output. Without this, exports are blank.
- **On-screen for capture**: Temporarily move to `left: 0` before calling `toPng`.
- **Offscreen container**: Use `position: absolute; left: -9999px` (not `fixed`).
- **Resizing**: Load data URL into Image, draw onto canvas at target size.
- 300ms delay between sequential exports.
- Set `fontFamily` on the offscreen container.
- **backgroundColor**: Always set to strip alpha transparency. Google Play rejects PNGs with alpha channels.
- **Numbered filenames**: Prefix exports with zero-padded index so they sort correctly: `01-hero-1080x1920.png`, `02-differentiator-1080x1920.png`, etc. Use `String(index + 1).padStart(2, "0")`.
- **Feature Graphic filename**: `feature-graphic-1024x500.png` (no numeric prefix).

## Step 7: Final QA Gate

Before handing the page back to the user, review every slide against this checklist:

### Message Quality

- **One idea per slide**: if a headline sells two ideas, split it or simplify it
- **First slide is strongest**: the hero slide should communicate the main benefit immediately
- **Readable in one second**: if you cannot parse it instantly at arm's length, rewrite it

### Visual Quality

- **No repeated layouts in sequence**: adjacent slides should not feel templated
- **Decorative elements support the story**: they should add energy without covering the app UI
- **Visual rhythm exists**: include at least one contrast slide when the set is long enough

### Export Quality

- **No clipped text or assets** after scaling to the selected export size
- **Screenshots are correctly aligned** inside the frame (if framed) or container (if frameless)
- **Filenames sort correctly** with zero-padded numeric prefixes

### Google Play Compliance

- [ ] No alpha transparency in exported PNGs (`backgroundColor` set in export options)
- [ ] Text overlay does not exceed 20% of the screenshot area
- [ ] No promotional pricing, rankings, or award text in copy
- [ ] Aspect ratio valid: max dimension <= 2x min dimension (1920/1080 = 1.78, passes)
- [ ] Minimum 2 screenshots exported, 4+ recommended for visibility
- [ ] Feature Graphic is exactly 1024x500
- [ ] Each exported file is under 8 MB
- [ ] All exports are JPEG or 24-bit PNG format

### Hand-off Behavior

When you present the finished work:

1. briefly explain the narrative arc across the slides
2. mention any slides that intentionally use contrast or different layout treatment
3. call out any assumptions you made about brand tone, copy, or missing assets

## Common Mistakes

| Mistake | Fix |
|---------|-----|
| All slides look the same | Vary phone position (center, left, right, two-phone, no-phone) |
| Decorative elements invisible | Increase size and opacity -- better too visible than invisible |
| Copy is too complex | "One second at arm's length" test |
| Floating elements block the phone | Move off-screen edges or above the phone |
| Plain white/black background | Use gradients -- even subtle ones add depth |
| Too cluttered | Remove floating elements, simplify to phone + caption |
| Too simple/empty | Add larger decorative elements, floating items at edges |
| Headlines use "and" | Split into two slides or pick one idea |
| No visual contrast across slides | Mix light and dark backgrounds |
| Export is blank | Use double-call trick; move element on-screen before capture |
| Alpha in exported PNG | Set `backgroundColor` in html-to-image options |
| Text overlay too large | Google enforces max 20% text area -- reduce font size or shorten copy |
| Promotional text in copy | Remove pricing, awards, rankings -- Google Play rejects these |
| Feature Graphic wrong size | Must be exactly 1024x500 pixels |
