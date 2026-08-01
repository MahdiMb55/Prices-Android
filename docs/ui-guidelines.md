# Prices UI guidelines

## Interaction language

The product may be visually and behaviorally inspired by Telegram's efficient information design, but must not copy Telegram branding, assets, icons, or proprietary screens. Prices uses Persian Green `#00A693` as its primary brand source.

- Keep list rows compact, readable, and information-dense.
- Use stable, simple top app bars and make search fast and prominent.
- Prefer clear hierarchy, restrained separators, and immediate touch feedback.
- Avoid excessive cards, oversized headers, decorative gradients, and visual clutter.
- Keep scrolling efficient and preserve scroll position.
- Use pull-to-refresh only when it adds clear value.
- Give immediate loading, success, empty, and error feedback.

Product rows should establish this hierarchy where applicable: product image, title, SKU, current price, and status. Prices and identifiers should be easy to scan without turning every row into a card.

## Color

Derive accessible Material 3 light and dark semantic roles from `#00A693`; do not apply the raw value everywhere or assume white text is always sufficient. Keep primary, success, warning, error, information, background, surface, outline, and divider roles distinct. Use low-saturation neutral surfaces and avoid pure black as the entire dark theme.

Semantic names belong in the design system. Do not name colors after screens or temporary visual treatments.

## Typography and direction

Use system fonts for now. Do not download or embed an unapproved proprietary font. Keep weights readable and compact for long product lists. Use stable or tabular number presentation where practical for prices.

Support Persian RTL and English LTR. Keep codes, SKUs, URLs, prices, and identifiers directionally correct even when surrounding copy is RTL. Validate both directions in previews and UI tests.

## Motion

- Prefer short 150–250 ms transitions.
- Use fade, size, or subtle position changes to explain state or navigation.
- Do not use continuous or decorative animation.
- Do not animate large list layouts unnecessarily or delay user actions.
- Respect reduced-animation preferences where practical.

## Accessibility and feedback

Use Material semantics, meaningful content descriptions, touch targets, readable contrast, and visible focus/pressed states. Loading and error states must be understandable without relying only on color. Error messages should explain the next useful action.

The design-system showcase is development validation only. It is not the final product home screen and must not become a fake feature flow.
