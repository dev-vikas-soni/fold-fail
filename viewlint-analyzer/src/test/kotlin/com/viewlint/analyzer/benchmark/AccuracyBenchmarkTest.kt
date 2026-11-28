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
import com.viewlint.core.LayoutResource
import com.viewlint.core.ViewlintContext
import com.viewlint.core.ViewlintEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

data class BenchmarkFixture(
    val id: String,
    val targetRule: String,
    val isPositiveFixture: Boolean, // true = valid code (should NOT report finding), false = defect code (SHOULD report finding)
    val fileType: String, // "kt", "xml", "manifest"
    val fileContent: String,
    val notes: String = ""
)

class AccuracyBenchmarkTest {

    val fixtures = listOf(
        // FX-01: OtpTextField character cell (Positive)
        BenchmarkFixture(
            id = "FX-01",
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
        // FX-02: Touch target avatar 40dp (Positive)
        BenchmarkFixture(
            id = "FX-02",
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
        // FX-03: Action Vector Icon 24dp (Positive)
        BenchmarkFixture(
            id = "FX-03",
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
        // FX-04: Product Thumbnail Image 60dp (Positive)
        BenchmarkFixture(
            id = "FX-04",
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
        // FX-05: Flexible Card widthIn (Positive)
        BenchmarkFixture(
            id = "FX-05",
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
        // FX-06: Flexible Panel heightIn (Positive)
        BenchmarkFixture(
            id = "FX-06",
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
        // FX-07: SizeIn Box (Positive)
        BenchmarkFixture(
            id = "FX-07",
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
        // FX-08: GridCells Adaptive (Positive)
        BenchmarkFixture(
            id = "FX-08",
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
        // FX-09: Parameter Adaptive State (Positive)
        BenchmarkFixture(
            id = "FX-09",
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
        // FX-10: NavigationSuiteScaffold (Positive)
        BenchmarkFixture(
            id = "FX-10",
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
        // FX-11: ListDetailPaneScaffold (Positive)
        BenchmarkFixture(
            id = "FX-11",
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
        // FX-12: XML with layout-sw600dp (Positive)
        BenchmarkFixture(
            id = "FX-12",
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
        // FX-13: Content Card with Fixed Height & Variable Text on non-scroll (Negative)
        BenchmarkFixture(
            id = "FX-13",
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
            notes = "Fixed height 150dp card with variable text and no scrolling"
        ),
        // FX-14: Product Edit Sheet Fixed Height without Scroll (Negative)
        BenchmarkFixture(
            id = "FX-14",
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
            notes = "Fixed height 200dp sheet without internal scroll"
        ),
        // FX-15: Required size override on container (Negative)
        BenchmarkFixture(
            id = "FX-15",
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
            notes = "requiredWidth / requiredHeight overrides parent constraints on container"
        ),
        // FX-16: AdminNotificationsScreen missing adaptive cues (Negative)
        BenchmarkFixture(
            id = "FX-16",
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
            notes = "Screen container without adaptive cues"
        ),
        // FX-17: Locked Card both axes (Negative)
        BenchmarkFixture(
            id = "FX-17",
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
            notes = "Fixed width 320dp and height 480dp locks both axes without scroll"
        ),
        // FX-18: Activity Orientation Lock (Negative)
        BenchmarkFixture(
            id = "FX-18",
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
            notes = "Explicit portrait orientation lock in manifest"
        ),
        // FX-19: Aspect Ratio Lock (Negative)
        BenchmarkFixture(
            id = "FX-19",
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
            notes = "maxAspectRatio 2.1 with resizeableActivity false on SDK 23"
        ),
        // FX-20: AbsoluteLayout Usage (Negative)
        BenchmarkFixture(
            id = "FX-20",
            targetRule = "FF006",
            isPositiveFixture = false,
            fileType = "xml",
            fileContent = """
                <AbsoluteLayout xmlns:android="http://schemas.android.com/apk/res/android"
                    android:layout_width="match_parent"
                    android:layout_height="match_parent" />
            """.trimIndent(),
            notes = "Deprecated AbsoluteLayout usage"
        ),
        // FX-21: Manifest without orientation lock (Positive)
        BenchmarkFixture(
            id = "FX-21",
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
        // FX-22: Manifest with resizeableActivity true (Positive)
        BenchmarkFixture(
            id = "FX-22",
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
        // FX-23: XML Root Layout match_parent (Positive)
        BenchmarkFixture(
            id = "FX-23",
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
        // FX-24: XML Fixed Root Width Without Variant (Negative)
        BenchmarkFixture(
            id = "FX-24",
            targetRule = "FF003",
            isPositiveFixture = false,
            fileType = "xml",
            fileContent = """
                <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
                    android:layout_width="360dp"
                    android:layout_height="match_parent" />
            """.trimIndent(),
            notes = "Fixed 360dp root width without adaptive resource qualifier variant"
        ),
        // FX-25: XML Small Icon Dimensions 48dp (Positive)
        BenchmarkFixture(
            id = "FX-25",
            targetRule = "FF004",
            isPositiveFixture = true,
            fileType = "xml",
            fileContent = """
                <ImageView xmlns:android="http://schemas.android.com/apk/res/android"
                    android:layout_width="48dp"
                    android:layout_height="48dp" />
            """.trimIndent(),
            notes = "Small 48dp fixed dimensions on leaf control allowed"
        ),
        // FX-26: XML Fixed Large Inner Container Dimensions (Negative)
        BenchmarkFixture(
            id = "FX-26",
            targetRule = "FF004",
            isPositiveFixture = false,
            fileType = "xml",
            fileContent = """
                <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
                    android:layout_width="350dp"
                    android:layout_height="400dp" />
            """.trimIndent(),
            notes = "Fixed 350x400dp on non-scroll inner container"
        ),
        // FX-27: XML Complex Layout with sw600dp Variant (Positive)
        BenchmarkFixture(
            id = "FX-27",
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
        // FX-28: XML Complex Layout Lacking Adaptive Variant (Negative)
        BenchmarkFixture(
            id = "FX-28",
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
            notes = "Complex layout (>15 nodes) lacking sw600dp adaptive variant"
        ),
        // FX-29: XML ConstraintLayout Relative Positioning (Positive)
        BenchmarkFixture(
            id = "FX-29",
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
        // FX-30: XML Hardcoded Absolute Coordinates layout_x (Negative)
        BenchmarkFixture(
            id = "FX-30",
            targetRule = "FF006",
            isPositiveFixture = false,
            fileType = "xml",
            fileContent = """
                <FrameLayout xmlns:android="http://schemas.android.com/apk/res/android">
                    <View android:layout_width="100dp" android:layout_height="100dp" android:layout_x="50dp" android:layout_y="100dp" />
                </FrameLayout>
            """.trimIndent(),
            notes = "Hardcoded layout_x / layout_y positioning"
        ),
        // FX-31: Compose Internal Vertical Scroll Container (Positive for FF007)
        BenchmarkFixture(
            id = "FX-31",
            targetRule = "FF007",
            isPositiveFixture = true,
            fileType = "kt",
            fileContent = """
                package com.example
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.unit.dp

                @Composable
                fun ScrollableCard() {
                    Column(modifier = Modifier.fillMaxWidth().height(250.dp).verticalScroll(rememberScrollState())) {
                        Text("Dynamic Title")
                        Text("Long description content that can scroll freely inside the fixed 250dp window.")
                    }
                }
            """.trimIndent(),
            notes = "Internal verticalScroll prevents content clipping in fixed height container"
        ),
        // FX-32: Compose Standard Size Modifier on Leaf (Positive for FF008)
        BenchmarkFixture(
            id = "FX-32",
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
        // FX-33: Compose WindowSizeClass Adaptive Screen (Positive for FF009)
        BenchmarkFixture(
            id = "FX-33",
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
        // FX-34: Compose Fixed Width Only (Single Axis) (Positive for FF010)
        BenchmarkFixture(
            id = "FX-34",
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
            notes = "Width fixed at 280dp but height flexible (fillMaxHeight) is not locked on both axes"
        ),
        // FX-35: Compose BoxWithConstraints Adaptive Container (Positive for FF009)
        BenchmarkFixture(
            id = "FX-35",
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
        // FX-36: Compose RequiredWidth on Leaf Button (Negative for FF008)
        BenchmarkFixture(
            id = "FX-36",
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
            notes = "requiredWidth on leaf button generates INFO finding for overriding parent constraints"
        ),
        // FX-37: Compose Internal LazyColumn Container (Positive for FF010)
        BenchmarkFixture(
            id = "FX-37",
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
        // FX-38: Compose RequiredSize on Content Container (Negative for FF008)
        BenchmarkFixture(
            id = "FX-38",
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
            notes = "requiredSize(500.dp) on screen container overrides parent constraints"
        ),
        // FX-39: Compose Both Axes Fixed Size Modifier (Negative for FF010)
        BenchmarkFixture(
            id = "FX-39",
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
            notes = "Modifier.size(280.dp) locks both dimensions on a non-leaf container"
        ),
        // FX-40: Compose Large Hardcoded Width > 120dp on Container (Negative for FF007)
        BenchmarkFixture(
            id = "FX-40",
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
            notes = "Hardcoded width(350.dp) on non-adaptive form container"
        )
    )

    @Test
    fun `executes accuracy benchmark suite and verifies precision and recall`() {
        val testDir = Files.createTempDirectory("accuracy-benchmark").toFile()

        var tp = 0
        var fp = 0
        var fn = 0
        var tn = 0

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

                val findings = engine.analyze(context).filter { it.ruleId == fixture.targetRule }

                if (fixture.isPositiveFixture) {
                    if (findings.isEmpty()) {
                        tn++
                    } else {
                        fp++
                        println("BENCHMARK FP in ${fixture.id} for rule ${fixture.targetRule}: ${findings.map { it.message }}")
                    }
                } else {
                    if (findings.isNotEmpty()) {
                        tp++
                    } else {
                        fn++
                        println("BENCHMARK FN in ${fixture.id} for rule ${fixture.targetRule}: expected finding but got none")
                    }
                }
            }

            val total = fixtures.size
            val precision = if (tp + fp > 0) (tp.toDouble() / (tp + fp)) * 100 else 100.0
            val recall = if (tp + fn > 0) (tp.toDouble() / (tp + fn)) * 100 else 100.0

            println("==========================================")
            println("VIEWLINT v0.9.2 ACCURACY BENCHMARK SUMMARY")
            println("Total Fixtures Evaluated: $total")
            println("True Positives (TP): $tp")
            println("True Negatives (TN): $tn")
            println("False Positives (FP): $fp")
            println("False Negatives (FN): $fn")
            println("Measured Precision: %.2f%%".format(precision))
            println("Measured Recall: %.2f%%".format(recall))
            println("==========================================")

            assertEquals("False Positives must be zero on benchmark suite", 0, fp)
            assertEquals("False Negatives must be zero on benchmark suite", 0, fn)
            assertTrue("Precision must be 100%", precision >= 99.0)
            assertTrue("Recall must be 100%", recall >= 99.0)
        } finally {
            testDir.deleteRecursively()
        }
    }
}
