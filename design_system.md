## Design System: LoXia

### Pattern
- **Name:** App Store Style Landing
- **Conversion Focus:** Show real screenshots. Include ratings (4.5+ stars). QR code for mobile. Platform-specific CTAs.
- **CTA Placement:** Download buttons prominent (App Store + Play Store) throughout
- **Color Strategy:** Dark/light matching app store feel. Star ratings in gold. Screenshots with device frames.
- **Sections:** 1. Hero with device mockup, 2. Screenshots carousel, 3. Features with icons, 4. Reviews/ratings, 5. Download CTAs

### Style
- **Name:** Exaggerated Minimalism
- **Mode Support:** Light ✓ Full | Dark ✓ Full
- **Keywords:** Bold minimalism, oversized typography, high contrast, negative space, loud minimal, statement design
- **Best For:** Fashion, architecture, portfolios, agency landing pages, luxury brands, editorial
- **Performance:** ⚡ Excellent | **Accessibility:** ✓ WCAG AA

### Colors
| Role | Hex | CSS Variable |
|------|-----|--------------|
| Primary | `#BE185D` | `--color-primary` |
| On Primary | `#FFFFFF` | `--color-on-primary` |
| Secondary | `#EC4899` | `--color-secondary` |
| Accent/CTA | `#D97706` | `--color-accent` |
| Background | `#FDF2F8` | `--color-background` |
| Foreground | `#0F172A` | `--color-foreground` |
| Muted | `#FBF1F5` | `--color-muted` |
| Border | `#F7E3EB` | `--color-border` |
| Destructive | `#DC2626` | `--color-destructive` |
| Ring | `#BE185D` | `--color-ring` |

*Notes: Fashion rose + gold accent*

### Typography
- **Heading:** Roboto
- **Body:** Roboto
- **Mood:** material design 3, md3, android, google, tonal, friendly, rounded, accessible, adaptive
- **Best For:** Android apps, cross-platform tools, productivity software, data-heavy B2B dashboards, enterprise mobile
- **Google Fonts:** https://fonts.googleapis.com/css2?family=Roboto:ital,wght@0,300;0,400;0,500;0,700;1,400
- **CSS Import:**
```css
@import url('https://fonts.googleapis.com/css2?family=Roboto:ital,wght@0,300;0,400;0,500;0,700;1,400&display=swap');
```

### Key Effects
font-size: clamp(3rem 10vw 12rem), font-weight: 900, letter-spacing: -0.05em, massive whitespace

### Avoid (Anti-patterns)
- Excessive decoration

### Pre-Delivery Checklist
- [ ] No emojis as icons (use SVG: Heroicons/Lucide)
- [ ] cursor-pointer on all clickable elements
- [ ] Hover states with smooth transitions (150-300ms)
- [ ] Light mode: text contrast 4.5:1 minimum
- [ ] Focus states visible for keyboard nav
- [ ] prefers-reduced-motion respected
- [ ] Responsive: 375px, 768px, 1024px, 1440px

