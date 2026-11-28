package com.viewlint.analyzer.benchmark

import com.viewlint.analyzer.AspectRatioAnalyzer
import com.viewlint.analyzer.FixedDimensionAnalyzer
import com.viewlint.analyzer.FixedRootWidthAnalyzer
import com.viewlint.analyzer.LayoutPositioningAnalyzer
import com.viewlint.analyzer.MissingAdaptiveVariantsAnalyzer
import com.viewlint.analyzer.OrientationRestrictionAnalyzer
import com.viewlint.analyzer.compose.rules.HardcodedDimensionAnalyzer
import com.viewlint.analyzer.compose.rules.MissingAdaptiveHandlingAnalyzer
import com.viewlint.analyzer.compose.rules.RequiredSizeAnalyzer
import com.viewlint.analyzer.compose.rules.RestrictiveModifierChainsAnalyzer
import com.viewlint.core.Confidence
import com.viewlint.core.LayoutResource
import com.viewlint.core.Severity
import com.viewlint.core.ViewlintContext
import com.viewlint.core.ViewlintEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

data class BenchmarkFixture(
    val id: String,
    val description: String,
    val isPositiveFixture: Boolean, // true = valid code (should NOT report expected defect), false = defect code (SHOULD report finding)
    val targetRule: String,
    val fileType: String, // "kt", "xml", "manifest"
    val fileContent: String,
    val expectedSeverity: Severity? = null,
    val expectedConfidence: Confidence? = null,
    val expectedEvidence: String? = null,
    val expectedLine: Int? = null,
    val permittedAdditionalRules: List<String> = emptyList(),
    val notes: String = ""
)

data class RuleBenchmarkStats(
    var tp: Int = 0,
    var fp: Int = 0,
    var tn: Int = 0,
    var fn: Int = 0
) {
    val precision: Double get() = if (tp + fp > 0) (tp.toDouble() / (tp + fp)) * 100.0 else 100.0
    val recall: Double get() = if (tp + fn > 0) (tp.toDouble() / (tp + fn)) * 100.0 else 100.0
    val f1: Double get() = if (precision + recall > 0) (2 * precision * recall) / (precision + recall) else 0.0
}

class AccuracyBenchmarkTest {

    val fixtures = listOf(
        BenchmarkFixture(
            id = "FX-01",
            description = "OtpTextField character cell input surface",
            targetRule = "FF010",
            isPositiveFixture = true,
            fileType = "kt",
            fileContent = """
                package com.example
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.unit.dp

                @Composable
                fun OtpTextFieldCell() {
                    Box(modifier = Modifier.width(16.dp).height(52.dp))
                }
            """.trimIndent(),
            notes = "Small input cell fixed dimensions exempt from FF010"
        ),
        BenchmarkFixture(
            id = "FX-02",
            description = "Touch target avatar 40dp",
            targetRule = "FF007",
            isPositiveFixture = true,
            fileType = "kt",
            fileContent = """
                package com.example
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.unit.dp

                @Composable
                fun UserAvatar() {
                    Box(modifier = Modifier.size(40.dp))
                }
            """.trimIndent(),
            notes = "Standard 40dp avatar touch target exempt"
        ),
        BenchmarkFixture(
            id = "FX-03",
            description = "Action Vector Icon 24dp",
            targetRule = "FF007",
            isPositiveFixture = true,
            fileType = "kt",
            fileContent = """
                package com.example
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.unit.dp

                @Composable
                fun ActionIcon() {
                    Icon(modifier = Modifier.size(24.dp), contentDescription = null)
                }
            """.trimIndent(),
            notes = "24dp standard icon size exempt"
        ),
        BenchmarkFixture(
            id = "FX-04",
            description = "Product Thumbnail Image 60dp",
            targetRule = "FF007",
            isPositiveFixture = true,
            fileType = "kt",
            fileContent = """
                package com.example
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.unit.dp

                @Composable
                fun ProductThumbnail() {
                    Image(modifier = Modifier.size(60.dp), contentDescription = null)
                }
            """.trimIndent(),
            notes = "60dp pure image thumbnail without text content exempt"
        ),
        BenchmarkFixture(
            id = "FX-05",
            description = "Flexible Card widthIn max constraint",
            targetRule = "FF007",
            isPositiveFixture = true,
            fileType = "kt",
            fileContent = """
                package com.example
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.unit.dp

                @Composable
                fun FlexibleCard() {
                    Box(modifier = Modifier.widthIn(max = 400.dp))
                }
            """.trimIndent(),
            notes = "widthIn max constraint is responsive"
        ),
        BenchmarkFixture(
            id = "FX-06",
            description = "Flexible Panel heightIn max constraint",
            targetRule = "FF007",
            isPositiveFixture = true,
            fileType = "kt",
            fileContent = """
                package com.example
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.unit.dp

                @Composable
                fun FlexiblePanel() {
                    Box(modifier = Modifier.heightIn(max = 300.dp))
                }
            """.trimIndent(),
            notes = "heightIn max constraint is responsive"
        ),
        BenchmarkFixture(
            id = "FX-07",
            description = "SizeIn Box range constraint",
            targetRule = "FF007",
            isPositiveFixture = true,
            fileType = "kt",
            fileContent = """
                package com.example
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.unit.dp

                @Composable
                fun SizeInBox() {
                    Box(modifier = Modifier.sizeIn(min = 100.dp, max = 200.dp))
                }
            """.trimIndent(),
            notes = "sizeIn range constraint is responsive"
        ),
        BenchmarkFixture(
            id = "FX-08",
            description = "GridCells Adaptive grid layout",
            targetRule = "FF009",
            isPositiveFixture = true,
            fileType = "kt",
            fileContent = """
                package com.example
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.unit.dp

                @Composable
                fun ProductGridScreen() {
                    LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 160.dp)) {
                        items(10) { Text("Item") }
                    }
                }
            """.trimIndent(),
            notes = "GridCells.Adaptive is responsive"
        ),
        BenchmarkFixture(
            id = "FX-09",
            description = "Parameter Adaptive State isExpandedScreen",
            targetRule = "FF009",
            isPositiveFixture = true,
            fileType = "kt",
            fileContent = """
                package com.example
                import androidx.compose.runtime.Composable

                @Composable
                fun MainScreen(isExpandedScreen: Boolean) {
                    if (isExpandedScreen) {
                        Text("Two pane")
                    } else {
                        Text("Single pane")
                    }
                }
            """.trimIndent(),
            notes = "isExpandedScreen adaptive branching parameter"
        ),
        BenchmarkFixture(
            id = "FX-10",
            description = "NavigationSuiteScaffold component",
            targetRule = "FF009",
            isPositiveFixture = true,
            fileType = "kt",
            fileContent = """
                package com.example
                import androidx.compose.runtime.Composable

                @Composable
                fun AppScaffoldScreen() {
                    NavigationSuiteScaffold {
                        Text("Content")
                    }
                }
            """.trimIndent(),
            notes = "NavigationSuiteScaffold is adaptive component"
        ),
        BenchmarkFixture(
            id = "FX-11",
            description = "ListDetailPaneScaffold component",
            targetRule = "FF009",
            isPositiveFixture = true,
            fileType = "kt",
            fileContent = """
                package com.example
                import androidx.compose.runtime.Composable

                @Composable
                fun DetailScreen() {
                    ListDetailPaneScaffold {
                        Text("Detail")
                    }
                }
            """.trimIndent(),
            notes = "ListDetailPaneScaffold is adaptive pane layout"
        ),
        BenchmarkFixture(
            id = "FX-12",
            description = "XML layout with layout-sw600dp qualifier variant",
            targetRule = "FF003",
            isPositiveFixture = true,
            fileType = "xml",
            fileContent = """
                <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
                    android:layout_width="400dp"
                    android:layout_height="match_parent" />
            """.trimIndent(),
            notes = "Fixed root width has sw600dp adaptive qualifier variant"
        ),
        BenchmarkFixture(
            id = "FX-13",
            description = "Content Card with Fixed Height & Variable Text on non-scroll layout",
            targetRule = "FF007",
            isPositiveFixture = false,
            fileType = "kt",
            fileContent = """
                package com.example
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.unit.dp

                @Composable
                fun AdminCard() {
                    Column(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                        Text("Title")
                        Text("Dynamic Description text that can overflow...")
                    }
                }
            """.trimIndent(),
            expectedSeverity = Severity.WARNING,
            expectedConfidence = Confidence.HIGH,
            expectedEvidence = "height(150.dp)",
            expectedLine = 6,
            notes = "Fixed height 150dp card with variable text and no scrolling"
        ),
        BenchmarkFixture(
            id = "FX-14",
            description = "Product Edit Sheet Fixed Height without Scroll",
            targetRule = "FF007",
            isPositiveFixture = false,
            fileType = "kt",
            fileContent = """
                package com.example
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.unit.dp

                @Composable
                fun ProductEditSheet() {
                    Column(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                        Text("Form Header")
                        Text("Dynamic content")
                    }
                }
            """.trimIndent(),
            expectedSeverity = Severity.WARNING,
            expectedConfidence = Confidence.HIGH,
            expectedEvidence = "height(200.dp)",
            expectedLine = 6,
            notes = "Fixed height 200dp sheet without internal scroll"
        ),
        BenchmarkFixture(
            id = "FX-15",
            description = "Required size override on container",
            targetRule = "FF008",
            isPositiveFixture = false,
            fileType = "kt",
            fileContent = """
                package com.example
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.unit.dp

                @Composable
                fun DashboardContainer() {
                    Column(modifier = Modifier.requiredWidth(400.dp).requiredHeight(600.dp)) {
                        Text("Dashboard")
                    }
                }
            """.trimIndent(),
            expectedSeverity = Severity.WARNING,
            expectedConfidence = Confidence.HIGH,
            expectedEvidence = "requiredWidth(400.dp)",
            expectedLine = 6,
            permittedAdditionalRules = listOf("FF008", "FF009"),
            notes = "requiredWidth / requiredHeight overrides parent constraints on container"
        ),
        BenchmarkFixture(
            id = "FX-16",
            description = "AdminNotificationsScreen missing adaptive cues",
            targetRule = "FF009",
            isPositiveFixture = false,
            fileType = "kt",
            fileContent = """
                package com.example
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.unit.dp

                @Composable
                fun AdminNotificationsScreen() {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        Text("Notifications Header")
                        Row(modifier = Modifier.width(400.dp)) {
                            Text("Notification Item 1")
                            Text("Notification Item 2")
                        }
                    }
                }
            """.trimIndent(),
            expectedSeverity = Severity.INFO,
            expectedConfidence = Confidence.LOW,
            expectedLine = 6,
            permittedAdditionalRules = listOf("FF007"),
            notes = "Screen container without adaptive cues"
        ),
        BenchmarkFixture(
            id = "FX-17",
            description = "Locked Card both axes fixed size",
            targetRule = "FF010",
            isPositiveFixture = false,
            fileType = "kt",
            fileContent = """
                package com.example
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.unit.dp

                @Composable
                fun LockedCard() {
                    Column(modifier = Modifier.width(320.dp).height(480.dp)) {
                        Text("Locked Card Content")
                    }
                }
            """.trimIndent(),
            expectedSeverity = Severity.WARNING,
            expectedConfidence = Confidence.HIGH,
            expectedEvidence = "width(320.dp), height(480.dp)",
            expectedLine = 6,
            notes = "Fixed width 320dp and height 480dp locks both axes without scroll"
        ),
        BenchmarkFixture(
            id = "FX-18",
            description = "Activity Orientation Lock in manifest",
            targetRule = "FF001",
            isPositiveFixture = false,
            fileType = "manifest",
            fileContent = """
                <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                    <application>
                        <activity android:name=".MainActivity" android:screenOrientation="portrait" />
                    </application>
                </manifest>
            """.trimIndent(),
            expectedSeverity = Severity.WARNING,
            expectedConfidence = Confidence.MEDIUM,
            expectedEvidence = "android:screenOrientation=\"portrait\"",
            notes = "Explicit portrait orientation lock in manifest"
        ),
        BenchmarkFixture(
            id = "FX-19",
            description = "Aspect Ratio Lock in manifest",
            targetRule = "FF002",
            isPositiveFixture = false,
            fileType = "manifest",
            fileContent = """
                <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                    <uses-sdk android:targetSdkVersion="23" />
                    <application>
                        <activity android:name=".MainActivity" android:maxAspectRatio="2.1" android:resizeableActivity="false" />
                    </application>
                </manifest>
            """.trimIndent(),
            expectedSeverity = Severity.WARNING,
            expectedConfidence = Confidence.MEDIUM,
            expectedEvidence = "android:maxAspectRatio=\"2.1\"",
            notes = "maxAspectRatio 2.1 with resizeableActivity false on SDK 23"
        ),
        BenchmarkFixture(
            id = "FX-20",
            description = "AbsoluteLayout Usage in XML",
            targetRule = "FF006",
            isPositiveFixture = false,
            fileType = "xml",
            fileContent = """
                <AbsoluteLayout xmlns:android="http://schemas.android.com/apk/res/android"
                    android:layout_width="match_parent"
                    android:layout_height="match_parent" />
            """.trimIndent(),
            expectedSeverity = Severity.WARNING,
            expectedConfidence = Confidence.HIGH,
            expectedEvidence = "<AbsoluteLayout>",
            notes = "Deprecated AbsoluteLayout usage"
        ),
        BenchmarkFixture(
            id = "FX-21",
            description = "Manifest without orientation lock",
            targetRule = "FF001",
            isPositiveFixture = true,
            fileType = "manifest",
            fileContent = """
                <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                    <application>
                        <activity android:name=".MainActivity" android:screenOrientation="unspecified" />
                    </application>
                </manifest>
            """.trimIndent(),
            notes = "unspecified orientation lock is allowed"
        ),
        BenchmarkFixture(
            id = "FX-22",
            description = "Manifest with resizeableActivity true",
            targetRule = "FF002",
            isPositiveFixture = true,
            fileType = "manifest",
            fileContent = """
                <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                    <uses-sdk android:targetSdkVersion="34" />
                    <application>
                        <activity android:name=".MainActivity" android:resizeableActivity="true" android:maxAspectRatio="2.1" />
                    </application>
                </manifest>
            """.trimIndent(),
            notes = "maxAspectRatio ignored by platform when resizeableActivity is true on SDK >= 24"
        ),
        BenchmarkFixture(
            id = "FX-23",
            description = "XML Root Layout match_parent",
            targetRule = "FF003",
            isPositiveFixture = true,
            fileType = "xml",
            fileContent = """
                <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
                    android:layout_width="match_parent"
                    android:layout_height="match_parent" />
            """.trimIndent(),
            notes = "Root layout match_parent is responsive"
        ),
        BenchmarkFixture(
            id = "FX-24",
            description = "XML Fixed Root Width Without Variant",
            targetRule = "FF003",
            isPositiveFixture = false,
            fileType = "xml",
            fileContent = """
                <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
                    android:layout_width="360dp"
                    android:layout_height="match_parent" />
            """.trimIndent(),
            expectedSeverity = Severity.WARNING,
            expectedConfidence = Confidence.MEDIUM,
            expectedEvidence = "layout_width=\"360dp\"",
            notes = "Fixed 360dp root width without adaptive resource qualifier variant"
        ),
        BenchmarkFixture(
            id = "FX-25",
            description = "XML Small Icon Dimensions 48dp",
            targetRule = "FF004",
            isPositiveFixture = true,
            fileType = "xml",
            fileContent = """
                <ImageView xmlns:android="http://schemas.android.com/apk/res/android"
                    android:layout_width="48dp"
                    android:layout_height="48dp" />
            """.trimIndent(),
            permittedAdditionalRules = listOf("FF003"),
            notes = "Small 48dp fixed dimensions on leaf control allowed"
        ),
        BenchmarkFixture(
            id = "FX-26",
            description = "XML Fixed Large Inner Container Dimensions",
            targetRule = "FF004",
            isPositiveFixture = false,
            fileType = "xml",
            fileContent = """
                <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
                    android:layout_width="350dp"
                    android:layout_height="400dp" />
            """.trimIndent(),
            expectedSeverity = Severity.INFO,
            expectedConfidence = Confidence.LOW,
            expectedEvidence = "layout_width=\"350dp\", layout_height=\"400dp\"",
            permittedAdditionalRules = listOf("FF003"),
            notes = "Fixed 350x400dp on non-scroll inner container"
        ),
        BenchmarkFixture(
            id = "FX-27",
            description = "XML Simple Layout Node Count <= 15",
            targetRule = "FF005",
            isPositiveFixture = true,
            fileType = "xml",
            fileContent = """
                <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
                    android:layout_width="match_parent"
                    android:layout_height="match_parent">
                    <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" />
                </LinearLayout>
            """.trimIndent(),
            notes = "Simple layout (<=15 nodes) does not require adaptive variant"
        ),
        BenchmarkFixture(
            id = "FX-28",
            description = "XML Complex Layout Lacking Adaptive Variant",
            targetRule = "FF005",
            isPositiveFixture = false,
            fileType = "xml",
            fileContent = """
                <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
                    android:layout_width="match_parent"
                    android:layout_height="match_parent">
                    <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" />
                    <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" />
                    <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" />
                    <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" />
                    <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" />
                    <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" />
                    <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" />
                    <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" />
                    <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" />
                    <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" />
                    <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" />
                    <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" />
                    <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" />
                    <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" />
                    <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" />
                    <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" />
                    <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" />
                </LinearLayout>
            """.trimIndent(),
            expectedSeverity = Severity.INFO,
            expectedConfidence = Confidence.LOW,
            notes = "Complex layout (>15 nodes) lacking sw600dp adaptive variant"
        ),
        BenchmarkFixture(
            id = "FX-29",
            description = "XML ConstraintLayout Relative Positioning",
            targetRule = "FF006",
            isPositiveFixture = true,
            fileType = "xml",
            fileContent = """
                <androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
                    android:layout_width="match_parent"
                    android:layout_height="match_parent" />
            """.trimIndent(),
            notes = "ConstraintLayout relative positioning allowed"
        ),
        BenchmarkFixture(
            id = "FX-30",
            description = "XML Hardcoded Absolute Coordinates layout_x / layout_y",
            targetRule = "FF006",
            isPositiveFixture = false,
            fileType = "xml",
            fileContent = """
                <FrameLayout xmlns:android="http://schemas.android.com/apk/res/android">
                    <View android:layout_width="100dp" android:layout_height="100dp" android:layout_x="50dp" android:layout_y="100dp" />
                </FrameLayout>
            """.trimIndent(),
            expectedSeverity = Severity.WARNING,
            expectedConfidence = Confidence.MEDIUM,
            notes = "Hardcoded layout_x / layout_y positioning"
        ),
        BenchmarkFixture(
            id = "FX-31",
            description = "Scrollable ancestor LazyColumn with unscrollable fixed-height Card child (Phase 2 Rule 1)",
            targetRule = "FF007",
            isPositiveFixture = false,
            fileType = "kt",
            fileContent = """
                package com.example
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.unit.dp

                @Composable
                fun ScrollableAncestorCardScreen() {
                    LazyColumn {
                        items(10) {
                            Card(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                                Text("Dynamic Title")
                                Text("Long description content that can overflow 150dp window.")
                            }
                        }
                    }
                }
            """.trimIndent(),
            expectedSeverity = Severity.WARNING,
            expectedConfidence = Confidence.HIGH,
            expectedEvidence = "height(150.dp)",
            expectedLine = 6,
            permittedAdditionalRules = listOf("FF009"),
            notes = "Phase 2 Rule 1: Scrollable ancestor LazyColumn does NOT make fixed-height child Card safe"
        ),
        BenchmarkFixture(
            id = "FX-32",
            description = "Compose Standard Size Modifier on Leaf",
            targetRule = "FF008",
            isPositiveFixture = true,
            fileType = "kt",
            fileContent = """
                package com.example
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.unit.dp

                @Composable
                fun ProfileBadge() {
                    Box(modifier = Modifier.size(24.dp))
                }
            """.trimIndent(),
            notes = "Standard size(24.dp) modifier is not requiredSize"
        ),
        BenchmarkFixture(
            id = "FX-33",
            description = "Compose WindowSizeClass Adaptive Screen",
            targetRule = "FF009",
            isPositiveFixture = true,
            fileType = "kt",
            fileContent = """
                package com.example
                import androidx.compose.runtime.Composable

                @Composable
                fun DashboardScreen(windowSizeClass: WindowSizeClass) {
                    if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded) {
                        TwoPaneLayout()
                    } else {
                        SinglePaneLayout()
                    }
                }
            """.trimIndent(),
            notes = "Explicit WindowSizeClass responsive layout handling"
        ),
        BenchmarkFixture(
            id = "FX-34",
            description = "Compose Fixed Width Only (Single Axis)",
            targetRule = "FF010",
            isPositiveFixture = true,
            fileType = "kt",
            fileContent = """
                package com.example
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.unit.dp

                @Composable
                fun SidebarMenu() {
                    Column(modifier = Modifier.width(280.dp).fillMaxHeight()) {
                        Text("Navigation Items")
                    }
                }
            """.trimIndent(),
            permittedAdditionalRules = listOf("FF007"),
            notes = "Width fixed at 280dp but height flexible (fillMaxHeight) is not locked on both axes for FF010"
        ),
        BenchmarkFixture(
            id = "FX-35",
            description = "Compose BoxWithConstraints Adaptive Container",
            targetRule = "FF009",
            isPositiveFixture = true,
            fileType = "kt",
            fileContent = """
                package com.example
                import androidx.compose.runtime.Composable

                @Composable
                fun AdaptiveFormScreen() {
                    BoxWithConstraints {
                        if (maxWidth > 600.dp) {
                            WideForm()
                        } else {
                            NarrowForm()
                        }
                    }
                }
            """.trimIndent(),
            notes = "BoxWithConstraints responsive cue present"
        ),
        BenchmarkFixture(
            id = "FX-36",
            description = "Compose RequiredWidth on Leaf Button",
            targetRule = "FF008",
            isPositiveFixture = false,
            fileType = "kt",
            fileContent = """
                package com.example
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.unit.dp

                @Composable
                fun ActionButton() {
                    Button(onClick = {}, modifier = Modifier.requiredWidth(120.dp)) {
                        Text("Submit")
                    }
                }
            """.trimIndent(),
            expectedSeverity = Severity.INFO,
            expectedConfidence = Confidence.LOW,
            expectedEvidence = "requiredWidth(120.dp)",
            expectedLine = 6,
            notes = "requiredWidth on leaf button generates INFO finding for overriding parent constraints"
        ),
        BenchmarkFixture(
            id = "FX-37",
            description = "Compose Internal LazyColumn Container (Phase 2 Rule 2)",
            targetRule = "FF010",
            isPositiveFixture = true,
            fileType = "kt",
            fileContent = """
                package com.example
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.unit.dp

                @Composable
                fun ScrollableDialog() {
                    LazyColumn(modifier = Modifier.width(300.dp).height(400.dp)) {
                        items(20) { Text("Item") }
                    }
                }
            """.trimIndent(),
            notes = "LazyColumn internal scroll state exempt from FF010 locking both axes"
        ),
        BenchmarkFixture(
            id = "FX-38",
            description = "Compose RequiredSize on Content Container",
            targetRule = "FF008",
            isPositiveFixture = false,
            fileType = "kt",
            fileContent = """
                package com.example
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.unit.dp

                @Composable
                fun RigidPanelScreen() {
                    Column(modifier = Modifier.requiredSize(500.dp)) {
                        Text("Rigid Screen Panel Header")
                    }
                }
            """.trimIndent(),
            expectedSeverity = Severity.WARNING,
            expectedConfidence = Confidence.HIGH,
            expectedEvidence = "requiredSize(500.dp)",
            expectedLine = 6,
            notes = "requiredSize(500.dp) on screen container overrides parent constraints"
        ),
        BenchmarkFixture(
            id = "FX-39",
            description = "Compose Both Axes Fixed Size Modifier on Container",
            targetRule = "FF010",
            isPositiveFixture = false,
            fileType = "kt",
            fileContent = """
                package com.example
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.unit.dp

                @Composable
                fun RigidCard() {
                    Box(modifier = Modifier.size(280.dp)) {
                        Text("Fixed size card")
                    }
                }
            """.trimIndent(),
            expectedSeverity = Severity.WARNING,
            expectedConfidence = Confidence.HIGH,
            expectedEvidence = "size(280.dp)",
            expectedLine = 6,
            notes = "Modifier.size(280.dp) locks both dimensions on a non-leaf container"
        ),
        BenchmarkFixture(
            id = "FX-40",
            description = "Compose Large Hardcoded Width > 120dp on Container",
            targetRule = "FF007",
            isPositiveFixture = false,
            fileType = "kt",
            fileContent = """
                package com.example
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.unit.dp

                @Composable
                fun FixedWidthFormScreen() {
                    Column(modifier = Modifier.width(350.dp).fillMaxHeight()) {
                        Text("Form Field 1")
                        Text("Form Field 2")
                    }
                }
            """.trimIndent(),
            expectedSeverity = Severity.WARNING,
            expectedConfidence = Confidence.HIGH,
            expectedEvidence = "width(350.dp)",
            expectedLine = 6,
            permittedAdditionalRules = listOf("FF009"),
            notes = "Hardcoded width(350.dp) on non-adaptive form container"
        ),
        BenchmarkFixture(
            id = "FX-41",
            description = "Internally scrollable content container (Phase 2 Rule 2)",
            targetRule = "FF007",
            isPositiveFixture = true,
            fileType = "kt",
            fileContent = """
                package com.example
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.unit.dp

                @Composable
                fun InternalScrollContainer() {
                    Column(modifier = Modifier.fillMaxWidth().height(250.dp).verticalScroll(rememberScrollState())) {
                        Text("Header")
                        Text("Dynamic content that can scroll freely inside 250dp window...")
                    }
                }
            """.trimIndent(),
            notes = "Phase 2 Rule 2: verticalScroll in modifier chain mitigates fixed height 250dp constraint"
        ),
        BenchmarkFixture(
            id = "FX-42",
            description = "Fixed-size content-bearing card eligible for analysis (Phase 2 Rule 3)",
            targetRule = "FF010",
            isPositiveFixture = false,
            fileType = "kt",
            fileContent = """
                package com.example
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.unit.dp

                @Composable
                fun FixedContentCard() {
                    Card(modifier = Modifier.width(350.dp).height(500.dp)) {
                        Text("Title")
                        Text("Description text...")
                    }
                }
            """.trimIndent(),
            expectedSeverity = Severity.WARNING,
            expectedConfidence = Confidence.HIGH,
            expectedEvidence = "width(350.dp), height(500.dp)",
            expectedLine = 6,
            notes = "Phase 2 Rule 3: Fixed-size content-bearing card must remain eligible for FF010 / FF007 analysis"
        ),
        BenchmarkFixture(
            id = "FX-43",
            description = "Unknown component role explicitly UNKNOWN (Phase 2 Rule 5)",
            targetRule = "FF007",
            isPositiveFixture = false,
            fileType = "kt",
            fileContent = """
                package com.example
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.unit.dp

                @Composable
                fun CustomUnclassifiedWidget() {
                    Column(modifier = Modifier.width(300.dp)) {
                        Text("Custom Text")
                    }
                }
            """.trimIndent(),
            expectedSeverity = Severity.WARNING,
            expectedConfidence = Confidence.MEDIUM,
            expectedEvidence = "width(300.dp)",
            expectedLine = 6,
            notes = "Phase 2 Rule 5: Role is UNKNOWN (not guessed as Container), evaluated with Confidence.MEDIUM"
        ),
        BenchmarkFixture(
            id = "FX-44",
            description = "Intentionally fixed-size input control (Phase 2 Rule 4)",
            targetRule = "FF007",
            isPositiveFixture = true,
            fileType = "kt",
            fileContent = """
                package com.example
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.unit.dp

                @Composable
                fun PinInputCell() {
                    Box(modifier = Modifier.width(20.dp).height(50.dp))
                }
            """.trimIndent(),
            notes = "Phase 2 Rule 4: Fixed-size input control is exempt"
        ),
        BenchmarkFixture(
            id = "FX-45",
            description = "Leaf graphics small avatar without text (Phase 2 Rule 4)",
            targetRule = "FF007",
            isPositiveFixture = true,
            fileType = "kt",
            fileContent = """
                package com.example
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.unit.dp

                @Composable
                fun UserAvatarIcon() {
                    Icon(modifier = Modifier.size(40.dp), contentDescription = null)
                }
            """.trimIndent(),
            notes = "Phase 2 Rule 4: Pure leaf graphics without text exempt"
        )
    )

    @Test
    fun `executes evidence-driven accuracy benchmark suite and verifies precision and recall`() {
        val testDir = Files.createTempDirectory("viewlint-evidence-benchmark").toFile()

        val allRuleIds = listOf("FF001", "FF002", "FF003", "FF004", "FF005", "FF006", "FF007", "FF008", "FF009", "FF010")
        val ruleStats = allRuleIds.associateWith { RuleBenchmarkStats() }.toMutableMap()

        try {
            for (fixture in fixtures) {
                val subDir = File(testDir, fixture.id)
                subDir.mkdirs()

                val manifests = mutableListOf<java.nio.file.Path>()
                val layouts = mutableListOf<LayoutResource>()
                val kotlinSources = mutableListOf<java.nio.file.Path>()

                when (fixture.fileType) {
                    "kt" -> {
                        val ktFile = File(subDir, "src/main/java/com/example/${fixture.id}.kt")
                        ktFile.parentFile.mkdirs()
                        ktFile.writeText(fixture.fileContent)
                        kotlinSources.add(ktFile.toPath())
                    }
                    "xml" -> {
                        val xmlFile = File(subDir, "res/layout/activity_${fixture.id.lowercase()}.xml")
                        xmlFile.parentFile.mkdirs()
                        xmlFile.writeText(fixture.fileContent)
                        layouts.add(LayoutResource("activity_${fixture.id.lowercase()}", "layout", xmlFile.toPath()))

                        if (fixture.id == "FX-12" || fixture.id == "FX-27") {
                            val swFile = File(subDir, "res/layout-sw600dp/activity_${fixture.id.lowercase()}.xml")
                            swFile.parentFile.mkdirs()
                            swFile.writeText(fixture.fileContent)
                            layouts.add(LayoutResource("activity_${fixture.id.lowercase()}", "layout-sw600dp", swFile.toPath()))
                        }
                    }
                    "manifest" -> {
                        val manFile = File(subDir, "src/main/AndroidManifest.xml")
                        manFile.parentFile.mkdirs()
                        manFile.writeText(fixture.fileContent)
                        manifests.add(manFile.toPath())
                    }
                }

                val engine = ViewlintEngine(
                    rules = listOf(
                        OrientationRestrictionAnalyzer(),
                        AspectRatioAnalyzer(),
                        FixedRootWidthAnalyzer(),
                        FixedDimensionAnalyzer(),
                        MissingAdaptiveVariantsAnalyzer(),
                        LayoutPositioningAnalyzer(),
                        HardcodedDimensionAnalyzer(),
                        RequiredSizeAnalyzer(),
                        MissingAdaptiveHandlingAnalyzer(),
                        RestrictiveModifierChainsAnalyzer()
                    )
                )

                val context = ViewlintContext(
                    projectRoot = subDir.toPath(),
                    manifests = manifests,
                    layouts = layouts,
                    kotlinSources = kotlinSources
                )

                val allFindings = engine.analyze(context)
                val targetRuleFindings = allFindings.filter { it.ruleId == fixture.targetRule }

                val matchingTargetFindings = targetRuleFindings.filter { finding ->
                    val matchesSeverity = fixture.expectedSeverity == null || finding.severity == fixture.expectedSeverity
                    val matchesConfidence = fixture.expectedConfidence == null || finding.confidence == fixture.expectedConfidence
                    val matchesEvidence = fixture.expectedEvidence == null || finding.evidence.contains(fixture.expectedEvidence)
                    val matchesLine = fixture.expectedLine == null || finding.line == fixture.expectedLine
                    matchesSeverity && matchesConfidence && matchesEvidence && matchesLine
                }

                val targetStats = ruleStats.getValue(fixture.targetRule)

                if (fixture.isPositiveFixture) {
                    if (matchingTargetFindings.isEmpty()) {
                        targetStats.tn++
                    } else {
                        targetStats.fp++
                        println("BENCHMARK FP in ${fixture.id} (${fixture.description}) for target rule ${fixture.targetRule}: $matchingTargetFindings")
                    }
                } else {
                    if (matchingTargetFindings.isNotEmpty()) {
                        targetStats.tp++
                    } else {
                        targetStats.fn++
                        println("BENCHMARK FN in ${fixture.id} (${fixture.description}) for target rule ${fixture.targetRule}: expected finding matching evidence '${fixture.expectedEvidence}' on line ${fixture.expectedLine} but got $allFindings")
                    }
                }

                val unpermittedFindings = allFindings.filter { finding ->
                    finding.ruleId != fixture.targetRule && finding.ruleId !in fixture.permittedAdditionalRules
                }

                if (unpermittedFindings.isNotEmpty()) {
                    for (unpermitted in unpermittedFindings) {
                        ruleStats.getValue(unpermitted.ruleId).fp++
                        println("BENCHMARK UNEXPECTED FP in ${fixture.id} (${fixture.description}) for unpermitted rule ${unpermitted.ruleId}: ${unpermitted.message}")
                    }
                }
            }

            var overallTp = 0
            var overallFp = 0
            var overallTn = 0
            var overallFn = 0

            println("=========================================================================================")
            println("VIEWLINT v1.0.0 EVIDENCE-DRIVEN ACCURACY BENCHMARK BREAKDOWN PER RULE")
            println("=========================================================================================")
            println("%-8s | %-4s | %-4s | %-4s | %-4s | %-10s | %-8s | %-8s".format("Rule", "TP", "TN", "FP", "FN", "Precision", "Recall", "F1 Score"))
            println("-----------------------------------------------------------------------------------------")

            for (ruleId in allRuleIds) {
                val stats = ruleStats.getValue(ruleId)
                overallTp += stats.tp
                overallFp += stats.fp
                overallTn += stats.tn
                overallFn += stats.fn

                println("%-8s | %-4d | %-4d | %-4d | %-4d | %-9.2f%% | %-7.2f%% | %-8.2f".format(
                    ruleId, stats.tp, stats.tn, stats.fp, stats.fn, stats.precision, stats.recall, stats.f1
                ))
            }

            val totalEvaluatedFixtures = overallTp + overallTn + overallFp + overallFn
            val overallPrecision = if (overallTp + overallFp > 0) (overallTp.toDouble() / (overallTp + overallFp)) * 100.0 else 100.0
            val overallRecall = if (overallTp + overallFn > 0) (overallTp.toDouble() / (overallTp + overallFn)) * 100.0 else 100.0
            val overallF1 = if (overallPrecision + overallRecall > 0) (2 * overallPrecision * overallRecall) / (overallPrecision + overallRecall) else 0.0

            println("-----------------------------------------------------------------------------------------")
            println("%-8s | %-4d | %-4d | %-4d | %-4d | %-9.2f%% | %-7.2f%% | %-8.2f".format(
                "OVERALL", overallTp, overallTn, overallFp, overallFn, overallPrecision, overallRecall, overallF1
            ))
            println("=========================================================================================")
            println("Total Fixtures Evaluated: $totalEvaluatedFixtures")
            println("DISCLAIMER: Benchmark results reflect performance on the synthetic fixture corpus")
            println("and do not constitute proof of real-world production accuracy on arbitrary external codebases.")
            println("=========================================================================================")

            assertEquals("Total evaluated fixtures must equal fixture list size (45)", fixtures.size, totalEvaluatedFixtures)
            assertEquals("False Positives must be zero on evidence-driven benchmark suite", 0, overallFp)
            assertEquals("False Negatives must be zero on evidence-driven benchmark suite", 0, overallFn)
            assertTrue("Overall Precision must be 100%", overallPrecision >= 99.0)
            assertTrue("Overall Recall must be 100%", overallRecall >= 99.0)
        } finally {
            testDir.deleteRecursively()
        }
    }

    @Test
    fun `failing benchmark when finding is reported on wrong line`() {
        val testDir = Files.createTempDirectory("viewlint-line-test").toFile()
        try {
            val ktFile = File(testDir, "src/main/java/com/example/WrongLine.kt")
            ktFile.parentFile.mkdirs()
            ktFile.writeText(
                """
                package com.example
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.unit.dp

                @Composable
                fun FixedHeightCard() {
                    Column(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                        Text("Title")
                    }
                }
                """.trimIndent()
            )

            val context = ViewlintContext(
                projectRoot = testDir.toPath(),
                manifests = emptyList(),
                kotlinSources = listOf(ktFile.toPath())
            )

            val findings = HardcodedDimensionAnalyzer().analyze(context)
            assertEquals(1, findings.size)
            val finding = findings.first()

            // The actual finding is on line 6
            assertEquals(6, finding.line)

            // A fixture expecting line 999 MUST fail to match
            val fixtureWithWrongLine = BenchmarkFixture(
                id = "FX-TEST-LINE",
                description = "Test wrong line number",
                isPositiveFixture = false,
                targetRule = "FF007",
                fileType = "kt",
                fileContent = "",
                expectedLine = 999
            )

            val matches = findings.filter { it.line == fixtureWithWrongLine.expectedLine }
            assertTrue("Finding on line 6 must not match expectedLine 999", matches.isEmpty())
        } finally {
            testDir.deleteRecursively()
        }
    }
}
