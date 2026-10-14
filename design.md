---
version: "alpha"
name: "Estilo de Tecnologia de Precisão"
description: "Precise and industrial landing page for 2nm chips. Ideal for landing pages, modern websites. AI-ready template."
colors:
  primary: "#003366"
  secondary: "#C0C0C0"
  tertiary: "#FFFFFF"
  neutral: "#E0E0E0"
  surface: "#00BFFF"
  accent: "#36454F"
typography:
  h1:
    fontFamily: Roboto
    fontSize: 2.5rem
    fontWeight: 700
  body-md:
    fontFamily: Roboto
    fontSize: 1rem
    fontWeight: 400
components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.neutral}"
    padding: 12px
---

## Overview

Precise and industrial landing page for 2nm chips. Ideal for landing pages, modern websites. AI-ready template. There's something almost paradoxical about it. Companies operating at the nanometer scale — where a single misaligned atom ruins everything — need to communicate that obsession visually. At human scale. On screens. ASML doesn't show you an actual EUV lithography chamber in their branding. They show you the *feeling* of it: hairline grids, impossible cleanliness, colors that suggest sterile environments and laser wavelengths.

TSMC, Applied Materials, Zeiss SMT — they all arrived at similar conclusions independently. Dark substrates. Precise geometric constructions. Typography that breathes but never wastes. The aesthetic isn't decorative; it mirrors the engineering philosophy. Every pixel justified, every element load-bearing. No ornamentation for its own sake.

What's fascinating is how this visual language crossed from semiconductor into biotech and scientific instrumentation. Turns out, when your product requires explaining invisible precision to investors and partners, you reach for the same toolkit: clinical color palettes, mathematical spacing, and layouts that feel engineered rather than designed. The grid isn't a suggestion — it's structural.

- Density: 3/10 — Airy
- Variance: 2/10 — Structured
- Motion: 4/10 — Subtle

- **Style:** Industrial, Precise, Minimalist
- **Keywords:** semiconductors, nanotechnology, precision, engineering, innovation, cleanroom, advanced, reliable, structured, efficient
- **Era:** 2026+ Nano-Tecnologia
- **Light/Dark:** ✓ Full / ✗ No

## Colors

- **Azul Industrial** (#003366) — Accent highlight, links and focus states
- **Prata** (#C0C0C0) — Secondary surface or text color
- **Branco** (#FFFFFF) — Light surface, card backgrounds
- **Cinza Claro** (#E0E0E0) — Secondary text, borders, muted elements
- **Azul Elétrico** (#00BFFF) — Secondary accent
- **Cinza Escuro** (#36454F) — Deep contrast surface
- **Verde** (#008000) — Success states, positive indicators
- **Laranja** (#FFA500) — Warm accent, call-to-action secondary


## Typography

- **Display / Hero:** Roboto — Weight 700, tight tracking, used for headline impact
- **Body:** Roboto — Weight 400, 16px/1.6 line-height, max 72ch per line
- **UI Labels / Captions:** Roboto — 0.875rem, weight 500, slight letter-spacing
- **Monospace:** JetBrains Mono — Used for code, metadata, and technical values

Scale:
- Hero: clamp(2.5rem, 5vw, 4rem)
- H1: 2.25rem
- H2: 1.5rem
- Body: 1rem / 1.6
- Small: 0.875rem


## Layout

- **Grid:** CSS Grid primary. Max-width containment: 1280px centered with 1.5rem side padding.
- **Spacing rhythm:** Balanced. Base unit: 0.5rem (8px).
- **Section vertical gaps:** clamp(4rem, 8vw, 8rem).
- **Hero layout:** Split-screen (text left, visual right).
- **Feature sections:** Zig-zag alternating text+image rows. No 3-equal-columns.
- **Mobile collapse:** All multi-column layouts collapse below 768px. No horizontal overflow.
- **z-index contract:** base (0) / sticky-nav (100) / overlay (200) / modal (300) / toast (500).


## Elevation & Depth

Grids de engenharia, diagramas de circuitos, animações de montagem de chips, brilhos sutis em elementos tecnológicos, tipografia técnica e limpa, micro-interações de dados complexos.

- **Physics:** Ease-out curves, 200-300ms duration. Smooth and predictable.
- **Entry animations:** Fade + translate-Y (16px → 0) over 420ms ease-out. Staggered cascades for lists: 80ms between items.
- **Hover states:** Subtle color shift + shadow adjustment over 200ms.
- **Page transitions:** Fade only (200ms).
- **Performance:** Only transform and opacity animated. No layout-triggering properties.


## Shapes

Base corner radius: 8px. See rounded tokens in front matter for the full scale.


## Components

- **Primary Button:** Subtly rounded (0.5rem) shape. Accent color fill. Hover: 8% darken + subtle lift shadow. Active: -1px translate tactile press. Font weight 600. No outer glows.
- **Secondary / Ghost Button:** Outline variant. 1.5px border in muted color. Text in primary color. Hover: subtle background fill.
- **Cards:** Subtly rounded (0.5rem) corners. Surface background. Subtle shadow (0 2px 12px rgba(0,0,0,0.06)). 1px border stroke.
- **Inputs:** Label above input. 1px border stroke. Focus ring: 2px accent color offset 2px. Error text below in semantic red. No floating labels.
- **Navigation:** Primary surface background. Active item: accent color indicator. Font weight 500 when active.
- **Skeletons:** Shimmer animation matching component dimensions. No circular spinners.
- **Empty States:** Icon-based composition with descriptive text and action button.


## Do's and Don'ts

- No emojis in UI — use icon system only (Lucide, Heroicons)
- No pure black (#000000) — use off-black or charcoal variants
- No oversaturated accent colors (saturation cap: 80%)
- No 3-column equal-width feature layouts — use zig-zag or asymmetric grid
- No `h-screen` — use `min-h-[100dvh]`
- No AI copywriting clichés: "Elevate", "Seamless", "Unleash", "Next-Gen"
- No broken external image links — use picsum.photos or inline SVG
- No generic lorem ipsum in demos

- Do Grids de engenharia
- Do Diagramas de circuitos
- Do Animações de montagem
- Do Tipografia técnica
- Do Micro-interações precisas
- Do Visualizações de dados.


## Use Case

Landing pages, Modern websites
