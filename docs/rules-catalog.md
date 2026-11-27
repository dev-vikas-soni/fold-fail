# Viewlint Rules Catalog (FF001 – FF010)

Viewlint rules are organized into three primary categories: Manifest Rules, XML Layout Rules, and Compose Rules.

---

## Manifest Rules

### FF001 — Activity orientation is restricted
- **Problem Addressed**: Explicit activity orientation locking (`screenOrientation`) prevents apps from adapting when users rotate devices, fold/unfold foldables, or resize windows in multi-window mode.
- **Analyzed Constructs**: `<activity>` elements in `AndroidManifest.xml`.
- **Detection Conditions**: Activity specifies `android:screenOrientation` set to a restricted value (`portrait`, `landscape`, `reversePortrait`, `reverseLandscape`, `sensorPortrait`, `sensorLandscape`, `locked`).
- **Severity**: `WARNING`
- **Confidence**: `MEDIUM`
- **Remediation**: Review whether orientation restrictions are necessary. Allow orientation changes and resizing. Note that for apps targeting Android 16 (API 36), orientation restrictions are ignored on form factors with smallest width >= 600dp.

### FF002 — Activity aspect ratio is restricted
- **Problem Addressed**: Fixed aspect ratio constraints (`maxAspectRatio` or `minAspectRatio`) cause letterboxing or pillarboxing on wider tablets and foldables.
- **Analyzed Constructs**: `<activity>` elements in `AndroidManifest.xml`.
- **Detection Conditions**: Activity specifies `maxAspectRatio` or `minAspectRatio` while effective `resizeableActivity` is `false` (resolved via explicit activity attribute > explicit application attribute > target SDK platform default: `true` for `targetSdkVersion` $\ge 24$, `false` for `targetSdkVersion` $< 24$ or omitted).
- **Severity**: `WARNING`
- **Confidence**: `MEDIUM`
- **Remediation**: Remove fixed aspect ratio constraints or ensure `resizeableActivity="true"` is explicitly configured. Note that on Android 16 (API 36), aspect-ratio restrictions are ignored on displays with smallest width >= 600dp unless opted out, but restrictions remain active on compact screens.

---

## XML Layout Rules

### FF003 — Root layout has fixed width
- **Problem Addressed**: Root layout containers with hardcoded dp widths do not scale across larger screens.
- **Analyzed Constructs**: Root view elements in layout XML files (`res/layout*`).
- **Detection Conditions**: Root view specifies `layout_width` as a hardcoded dp value (e.g. `"400dp"`) and no alternative qualified layout variant (`-sw600dp`, `-w600dp`, `-land`, etc.) exists for that layout.
- **Severity**: `WARNING`
- **Confidence**: `MEDIUM`
- **Remediation**: Use `match_parent`, `wrap_content`, `0dp` (match constraints), or provide adaptive layout qualifiers.

### FF004 — Fixed large inner dimensions
- **Problem Addressed**: Non-scroll container children with rigid large dimensions cause clipping or overflow on compact or multi-window screens.
- **Analyzed Constructs**: Inner child nodes in layout XML hierarchies.
- **Detection Conditions**: Non-scroll container child specifies both `layout_width` and `layout_height` as hardcoded dp values $\ge 300\text{dp}$.
- **Severity**: `INFO`
- **Confidence**: `LOW`
- **Remediation**: Use flexible constraints (`match_parent`, weights, or constraints) instead of fixed large bounds.

### FF005 — Complex layout lacks adaptive variants
- **Problem Addressed**: Complex layouts may require specialized multi-pane or adapted layouts for tablets.
- **Analyzed Constructs**: Layout XML structure and qualified variants.
- **Detection Conditions**: Layout has high structural complexity (>15 DOM nodes) but only a single default layout variant.
- **Severity**: `INFO`
- **Confidence**: `LOW`
- **Remediation**: Provide qualified layout variants (e.g., `layout-sw600dp` or `layout-land`).

### FF006 — AbsoluteLayout or absolute coordinates
- **Problem Addressed**: Deprecated `AbsoluteLayout` and hardcoded absolute coordinates (`layout_x`, `layout_y`) do not adapt to screen dimension changes.
- **Analyzed Constructs**: XML layout view tags and attributes.
- **Detection Conditions**: Usage of `<AbsoluteLayout>` or attributes `layout_x`/`layout_y`.
- **Severity**: `WARNING`
- **Confidence**: `HIGH` (`AbsoluteLayout`) / `MEDIUM` (`layout_x`/`y`)
- **Remediation**: Replace with `ConstraintLayout`, `RelativeLayout`, or `FrameLayout`.

---

## Compose Rules

### FF007 — Composable uses hardcoded dimensions
- **Problem Addressed**: Fixed width, height, or size modifier calls restrict component flexibility across window sizes.
- **Analyzed Constructs**: Kotlin source files (`.kt`) containing `@Composable` functions.
- **Detection Conditions**: Composable uses `.width(...)`, `.height(...)`, or `.size(...)` with explicit `dp` values without detectable adaptive cues (`WindowSizeClass`, `BoxWithConstraints`, or conditional sizing branches).
- **Severity**: `WARNING`
- **Confidence**: `MEDIUM`
- **Remediation**: Use flexible modifiers (`fillMaxWidth`, weights, `wrapContentWidth`) or adapt to screen size classes.

### FF008 — Composable uses restrictive required size
- **Problem Addressed**: `requiredWidth`, `requiredHeight`, or `requiredSize` modifiers force dimensions regardless of incoming parent constraints, risking clipping.
- **Analyzed Constructs**: Kotlin source files (`.kt`) containing `@Composable` functions.
- **Detection Conditions**: Composable uses `.requiredWidth(...)`, `.requiredHeight(...)`, or `.requiredSize(...)`.
- **Severity**: `WARNING`
- **Confidence**: `MEDIUM`
- **Remediation**: Use standard size modifiers unless overriding incoming constraints is strictly necessary.

### FF009 — Missing adaptive layout handling
- **Problem Addressed**: Complex screen-level composables may benefit from explicit adaptive layout branching.
- **Analyzed Constructs**: Kotlin source files (`.kt`) containing `@Composable` functions.
- **Detection Conditions**: Composable has high complexity (multiple fixed modifiers or long body text) without detected adaptive cues.
- **Severity**: `INFO`
- **Confidence**: `LOW`
- **Remediation**: Incorporate `WindowSizeClass`, `BoxWithConstraints`, or responsive layout branching.

### FF010 — Restrictive modifier chain combination
- **Problem Addressed**: Combining fixed width and height or fixed size modifiers locks component boundaries.
- **Analyzed Constructs**: Kotlin source files (`.kt`) containing `@Composable` functions.
- **Detection Conditions**: Composable combines both fixed width and height modifiers or a fixed `.size(...)` modifier without adaptive cues.
- **Severity**: `WARNING`
- **Confidence**: `MEDIUM`
- **Remediation**: Avoid locking both dimensions to fixed dp values; use flexible sizing constraints.
