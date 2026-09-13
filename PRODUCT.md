# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

Primary user is the merchant owner/operator of an independent multi-channel business — someone who runs products, inventory, orders, and forecasting themselves, using Vantage from a day-lit desktop (office or shop) during working hours. They are not a platform operator; the tenant dashboard is their daily working surface.

## Product Purpose

Vantage is a full-stack SaaS platform for independent merchants to operate the whole merchandising loop — products, inventory, orders, and demand forecasting — in one place, with auditability and control.

## Positioning

A full-stack vendor platform purpose-built for independents: products, inventory, orders, demand forecasting, and developer access together, with a unified view a competitor (a spreadsheet workflow or a single-channel admin) could not truthfully copy.

## Operating Context

The merchant runs a single-tenant dashboard on desktop at work. It is an operating surface, not a marketing page: they come in to scan state, act on what is low or pending, and check trends. Dark mode is a genuine secondary session (nights, darker shops), so the theme must hold real quality in both.

## Capabilities and Constraints

- Multi-tenant SaaS: each merchant authenticates into their own tenant via JWT.
- Products and inventory management with optimistic concurrency (If-Match version headers).
- Distributed order orchestration with status lifecycle and payment circuit breaker.
- AI demand forecasting (Holt-Winters) with confidence intervals.
- Developer portal: API keys, webhooks, and API log stream.
- Live operations map streamed over STOMP/WebSocket.
- Command palette (Cmd+K), chat support widget, audit timeline.
- Existing stacks: React 19, Vite, Tailwind CSS (no design tokens defined yet), React Query, Zustand, recharts, react-table + react-virtual.
- There is no formal design system or token set today.

## Brand Commitments

Name: Vantage. Asset: logo.png. No tagline, no voice guide, no other brand assets are binding.

## Evidence on Hand

- Logo: frontend/src/assets/logo.png.
- Demo data is synthetic backend seed data; no invented customer claims are allowed.
- No real testimonial, case-study, or pricing material exists.

## Product Principles

- The merchant is the operator: every screen exists to answer "what needs my attention" and let them act immediately.
- Precision beats decoration; scan-heavy surfaces win on hierarchy, alignment, and legibility.
- Consistency is trust: one tokenized visual system across all surfaces, in light and dark.
- The product is the craft: calm, confident, premium—the interface should feel as deliberate as the concurrency guarantees under it.

## Accessibility & Inclusion

No product-specific accessibility requirement was established beyond interface legibility; the design must keep WCAG-grade contrast and keyboard operability as a baseline by craft, not by exception.