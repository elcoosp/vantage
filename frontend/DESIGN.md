---
name: Vantage — Operations Platform
description: Calm, precise operations surface for independent merchants. Light-first with a serious dark mode.
colors:
  primary: "#5257CE"
  primary-hover: "#4347AA"
  primary-subtle: "#ECEFFD"
  neutral-bg: "#F7F8FA"
  neutral-surface: "#FFFFFF"
  neutral-surface-elevated: "#F3F4F8"
  neutral-text: "#17181C"
  neutral-text-secondary: "#5A5E66"
  neutral-text-tertiary: "#878C96"
  neutral-border: "#E7E9EF"
  neutral-border-strong: "#D8DBE2"
  status-success: "#1EAD72"
  status-warning: "#D3932B"
  status-danger: "#E5484D"
  status-info: "#5B7CFA"
typography:
  display:
    fontFamily: '"Inter Variable", -apple-system, BlinkMacSystemFont, "Segoe UI", ui-sans-serif, system-ui, sans-serif'
    fontSize: "24px"
    fontWeight: 600
    lineHeight: 1.3
    letterSpacing: "-0.02em"
  title:
    fontFamily: '"Inter Variable", -apple-system, BlinkMacSystemFont, "Segoe UI", ui-sans-serif, system-ui, sans-serif'
    fontSize: "16px"
    fontWeight: 600
    lineHeight: 1.4
    letterSpacing: "-0.01em"
  body:
    fontFamily: '"Inter Variable", -apple-system, BlinkMacSystemFont, "Segoe UI", ui-sans-serif, system-ui, sans-serif'
    fontSize: "14px"
    fontWeight: 400
    lineHeight: 1.6
  label:
    fontFamily: '"Inter Variable", -apple-system, BlinkMacSystemFont, "Segoe UI", ui-sans-serif, system-ui, sans-serif'
    fontSize: "13px"
    fontWeight: 500
    lineHeight: 1.4
rounded:
  sm: "6px"
  md: "8px"
  lg: "12px"
  xl: "16px"
  pill: "9999px"
spacing:
  xs: "4px"
  sm: "8px"
  md: "16px"
  lg: "24px"
  xl: "32px"
components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "#FFFFFF"
    rounded: "{rounded.md}"
    padding: "8px 16px"
  button-primary-hover:
    backgroundColor: "{colors.primary-hover}"
    rounded: "{rounded.md}"
  button-secondary:
    backgroundColor: "{colors.neutral-surface}"
    textColor: "{colors.neutral-text}"
    rounded: "{rounded.md}"
    padding: "8px 16px"
  button-ghost:
    backgroundColor: "transparent"
    textColor: "{colors.neutral-text-secondary}"
    rounded: "{rounded.md}"
    padding: "6px 12px"
  input:
    backgroundColor: "{colors.neutral-surface}"
    textColor: "{colors.neutral-text}"
    rounded: "{rounded.md}"
    padding: "8px 12px"
    height: "38px"
  card:
    backgroundColor: "{colors.neutral-surface}"
    textColor: "{colors.neutral-text}"
    rounded: "{rounded.lg}"
    padding: "20px"
  status-badge:
    rounded: "{rounded.pill}"
---

# Design System — Vantage

## Overview

Vantage is a calm, precise operations surface for independent merchants who run products, inventory,
orders, forecasting, and developer tooling from a single day-lit desktop during working hours. It is an
operating surface, not a marketing page: every screen asks "what needs attention" and lets the operator
act immediately.

The visual world is **restrained precision**. Soft neutral canvases, hairline borders, quiet violet-indigo
accents, and one well-behaved typeface (Inter Variable) keep scan-heavy surfaces legible and confident.
Interaction is gentle and quick: short transitions, subtle lifts, and focus rings that never shout. Dark
mode is a genuine secondary session (nights, darker shops) and must hold real quality, not a flip of the
light theme.

## Colors

- **Brand** is a calm violet-indigo. `brand-600 #5257CE` is the only action hue; `brand-50`–`brand-300`
  give tinted hover/selected states. It is used for primary actions, active nav, and focus rings only —
  never for decorative flourish.
- **Neutrals are paired light/dark tokens**, everywhere. `canvas`, `card`, `card2`, `ink`, `ink2`, `ink3`,
  `line`, `line2` each carry a `-light` and `-dark` value and are consumed as paired classes
  (`bg-canvas-light dark:bg-canvas-dark`). Content, chrome, and borders always move together with theme.
- **Hairline borders** define structure: `border-line-light dark:border-line-dark` for surfaces,
  `line2` for stronger/active edges.
- **Status colors** come as three-part sets — `soft` fill, `solid` dot/indicator, `ink` text — so a badge
  always stays legible on any surface. Dark mode overlays the solid/soft fills at reduced opacity
  (`dark:bg-[#HEX]/15`) against dark cards instead of re-mixing the tints.
- **Semantics:** success = fulfilled/bottom-line ($, orders shipped, stock healthy), warning = attention
  (low stock, pending forecast), danger = action needed (stockouts, payment failures), info = neutral
  helper/neutral emphasis. One color per signal — never yellow for "good".

## Typography

- **Inter Variable** is the only UI face (system fallback stack). **JetBrains Mono Variable** is reserved
  for code: API keys, endpoints, key/value payloads, command hints, Kbds.
- Hierarchy: page heading (24/600/-0.02em) → section title (16/600) → body (14/400) → label (13/500) →
  caption/meta (12, ink3). Density is tight; comfort is reserved for forms and reading text.
- Numerals in metrics/data heavy surfaces may use `font-variant-numeric: tabular-nums` for aligned
  columns. Weights live on the 400/500/600 axis — no extralight body, no black headlines.

## Layout

- **App shell:** fixed left sidebar (icon rail + labels) on a `canvas` background, page content in a
  max-width container with generous padding. The top bar holds the page title, global search trigger
  (⌘K), theme toggle, notifications, and user menu.
- **Pages** open with a `PageHeader`: h1 + descriptive paragraph, then the page's action buttons on the
  right. Below: a page-level summary band (metrics or filter chips), then the working surface.
- **Grid rhythm:** 16px base spacing; dashboards use 24px gutters between cards. Cards collapse cleanly
  to a single column under ~1024px.
- **Density:** scan surfaces (tables, logs, inventories) stay compact — 40px rows, roomy but not loose.
  Forms and modals get more air (20–24px padding).
- **Focus, not chrome:** white/1px borders do the structuring, shadows stay subtle, and the primary
  signal is hierarchy of type + a single accent color.

## Elevation & Depth

- Depth comes from a four-step shadow vocabulary, all tuned from the ink color (very low alpha):
  `card` (resting surfaces) → `lift` (hover, dropdowns off-trigger) → `pop` (menus, toasts, popovers)
  → `float` (modals, command palette).
- Surfaces rest flat on the canvas; elevation is reserved for what the operator is interacting with, on
  purpose. Hovered cards translate up 2px with `lift`; panels that open above content use `pop`/`float`.
- In dark mode shadows are used more sparingly — separation comes from the lighter `card` vs `canvas`
  fill and hairline borders rather than glow.

## Shapes

- Radius scale: `sm 6px` (chips, small pills), `md 8px` (buttons, inputs, badges), `lg 12px` (cards,
  tables, panels), `xl 16px` (modals, command palette), `pill` (status badges, avatars, filter chips).
- Consistent 8px radius on interactions, 12px on containers. Nothing is aggressively rounded; the
  product reads as precise, not playful.
- Status badges and filter chips are pills; buttons and inputs are 8px; cards are 12px.

## Components

- **Primary button:** brand-600 fill, white label, 8px radius, 8×16px padding, `shadow-sm`, hover
  `brand-700`; focus ring `brand-500/50`. Secondary: card fill + line border. Ghost: transparent,
  ink2 label, 6×12px padding. Destructive: danger-solid fill / danger tinted ghost.
- **Card:** card fill, 1px line border, `shadow-card` resting, `lg` radius, 20px padding, optional hover
  lift — hover only where the card is an action (rows, selectable tiles).
- **Input/Select/Textarea:** card fill, line border, inset 1px shadow, 3px above-rest border radius,
  focus = brand-500 border + 4px brand-500/15 ring (soft, not harsh), sized `h-10` (2.5rem).
- **Status badge:** pill, soft fill + ink text + solid dot; conveys state at a glance in tables and cards.
- **Table:** header row 13px/600/ink2, body rows 14px, `hover:bg-card2`, 40px rows, hairline dividers,
  sticky header with card fill + bottom border; actions appear as quiet icon buttons on hover.
- **Modal:** background scrim 40% ink at 60% alpha, `float` shadow, `xl` radius, scale-in on open, one
  primary action in the footer; focus trapped, Esc closes.
- **Nav:** active item = brand-50 fill + brand-700 label + 4px brand left rail; inactive = ink2, hover =
  ink. Icons 16–18px, stroke-width 2.
- **Empty states** earn their space: icon tile, clear title, one-action guidance, never a bare "no data".
- **Skeletons** use `card2` pulsing blocks, matched to the shape of the content they replace.

## Do's and Don'ts

- **Do** keep one accent hue; neutrals built from paired light/dark tokens carry every non-action state.
- **Do** keep borders hairline and shadows soft — the interface earns trust through alignment, not effects.
- **Do** keep action verbs crisp and imperative ("Add product", "Update stock", "Pause stream").
- **Do** show real, seeded data with believable operating states in demos; never invent customer claims.
- **Don't** mix legacy grays/slate/blue utilities into the token system — stick to the paired tokens.
- **Don't** put a solid status fill directly on a bright card — use soft + ink + solid dot instead.
- **Don't** animate for flourish; motion (fade-up, scale-in, 150ms color transitions) marks hierarchy.
- **Don't** let dark mode be an afterthought: every pair flips together, and contrast holds in both.