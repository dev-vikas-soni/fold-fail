# ViewLint Rule Index (FF001 – FF010)

ViewLint categorizes rules into three core domains:
1. **Manifest Rules** (FF001 – FF002): Analyzes activity configuration restrictions in `AndroidManifest.xml`.
2. **XML Layout Rules** (FF003 – FF006): Analyzes layout structures, root bounds, and positioning attributes in `res/layout*`.
3. **Jetpack Compose Rules** (FF007 – FF010): Analyzes modifier chains, composable roles, and adaptive layout cues in `.kt` files.

---

| Rule ID | Rule Name | Category | Default Severity | Confidence | Target Constructs |
| :--- | :--- | :--- | :--- | :--- | :--- |
| [**FF001**](FF001.md) | Activity orientation is restricted | Manifest | `WARNING` | `MEDIUM` | `<activity android:screenOrientation>` |
| [**FF002**](FF002.md) | Activity aspect ratio is restricted | Manifest | `WARNING` | `MEDIUM` | `<activity android:maxAspectRatio / minAspectRatio>` |
| [**FF003**](FF003.md) | Root layout has fixed width | XML Layout | `WARNING` | `MEDIUM` | Root `<View android:layout_width="...dp">` |
| [**FF004**](FF004.md) | Fixed large inner dimensions | XML Layout | `INFO` | `LOW` | Inner `<View android:layout_width/height=">=300dp">` |
| [**FF005**](FF005.md) | Complex layout lacks adaptive variants | XML Layout | `INFO` | `LOW` | Complex XML layout (>15 nodes) without `-w600dp` / `-sw600dp` |
| [**FF006**](FF006.md) | AbsoluteLayout or absolute coordinates | XML Layout | `WARNING` | `HIGH` / `MEDIUM` | `<AbsoluteLayout>` or `layout_x`/`layout_y` |
| [**FF007**](FF007.md) | Composable uses hardcoded dimensions | Compose | `WARNING` | `HIGH` / `MEDIUM` / `LOW` | `Modifier.width(...)`, `.height(...)`, `.size(...)` |
| [**FF008**](FF008.md) | Composable uses restrictive required size | Compose | `WARNING` / `INFO` | `HIGH` / `LOW` | `Modifier.requiredWidth(...)`, `.requiredHeight(...)`, `.requiredSize(...)` |
| [**FF009**](FF009.md) | Missing adaptive layout handling | Compose | `INFO` | `LOW` | Screen composable without `WindowSizeClass` or `BoxWithConstraints` |
| [**FF010**](FF010.md) | Restrictive modifier chain combination | Compose | `WARNING` | `HIGH` / `MEDIUM` / `LOW` | Modifier chain combining fixed width & height |

---

## Severity & Priority Matrix

ViewLint computes finding **Priority** based on the combination of Severity and Confidence:

- **HIGH Priority**: `ERROR` severity with any confidence, OR `WARNING` severity with `HIGH` confidence.
- **MEDIUM Priority**: `WARNING` severity with `MEDIUM`/`LOW` confidence, OR `INFO` severity with `HIGH` confidence.
- **LOW Priority**: `INFO` severity with `MEDIUM`/`LOW` confidence.
