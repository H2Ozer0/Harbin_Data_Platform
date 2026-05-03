# Frontend Router Upgrade - Learnings

## Conventions
- Dark theme: `#0a0e1a` bg, `#0cf` accent, `#fff` text
- Vue 3.4.x + vue-router@4 + Pinia + Vite 5
- All new pages use `<style scoped>` with placeholder pattern
- `@` alias resolves to `frontend/src/`
- Original map styles moved to `OriginalMapPage.vue` as `<style scoped>` (global reset styles kept in `App.vue`)

## Decisions
- App.vue global styles (`*`, `html/body/#app`) preserved - only page-specific template/logic removed
- OriginalMapPage uses `@/` import prefix (not relative `./`) for consistency with router lazy imports
- DashboardLayout uses Pinia `useDashboardStore` for shared timeline state
