package com.almanac.sarawak2026

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.provider.CalendarContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

// --- PREMIUM PALETTE ---
val Slate950 = Color(0xFF020617)
val Slate800 = Color(0xFF1E293B)
val Slate500 = Color(0xFF64748B)
val Amber500 = Color(0xFFF59E0B)
val Red600 = Color(0xFFDC2626)

// --- DATA MODELS ---
data class UserPlan(val note: String, val time: String = "", val isLeave: Boolean = false)
data class UserProfile(val entitlement: Int = 14)
data class AlmanacDayInfo(val isPH: Boolean, val remark: String?)
data class BudgetEntry(val id: Long = System.currentTimeMillis(), val item: String, val amount: Double, val date: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AlmanacTheme {
                MainAppContainer()
            }
        }
    }
}

@Composable
fun AlmanacTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(primary = Slate950, secondary = Amber500, tertiary = Red600),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContainer() {
    val context = LocalContext.current
    val gson = Gson()
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    // Low-RAM efficient state management
    var currentScreen by remember { mutableStateOf("Calendar") }
    var userProfile by remember { mutableStateOf(loadProfile(context, gson)) }
    var plans by remember { mutableStateOf(loadPlans(context, gson)) }
    var budgetEntries by remember { mutableStateOf(loadBudget(context, gson)) }

    if (isTablet) {
        Row(Modifier.fillMaxSize()) {
            NavigationRail(containerColor = Slate950) {
                Spacer(Modifier.height(16.dp))
                val screens = listOf("Calendar", "Itinerary", "Tools", "Settings", "About")
                screens.forEach { screen ->
                    NavigationRailItem(
                        selected = currentScreen == screen,
                        onClick = { currentScreen = screen },
                        label = { Text(screen, fontSize = 10.sp, color = if(currentScreen == screen) Amber500 else Color.Gray) },
                        icon = {
                            Icon(
                                when(screen) {
                                    "Calendar" -> Icons.Default.CalendarMonth
                                    "Itinerary" -> Icons.Default.FormatListBulleted
                                    "Tools" -> Icons.Default.Build
                                    "Settings" -> Icons.Default.Person
                                    else -> Icons.Default.Info
                                },
                                contentDescription = screen,
                                tint = if(currentScreen == screen) Amber500 else Color.Gray
                            )
                        },
                        colors = NavigationRailItemDefaults.colors(indicatorColor = Color.White.copy(alpha = 0.1f))
                    )
                }
            }
            // Vertical Divider
            Box(Modifier.width(1.dp).fillMaxHeight().background(Color.Gray.copy(alpha = 0.2f)))
            
            // Content
            Column(Modifier.fillMaxSize()) {
                TopAppBar(
                    title = {
                        Column {
                            Text("ALMANAC 2026", fontWeight = FontWeight.Black, color = Amber500, fontSize = 18.sp)
                        }
                    },
                    actions = { MalaysianStandardClock() },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Slate950)
                )
                
                 // Flag Strip
                Row(Modifier.fillMaxWidth().height(4.dp)) {
                    Box(Modifier.weight(1f).fillMaxHeight().background(Amber500))
                    Box(Modifier.weight(1f).fillMaxHeight().background(Slate950))
                    Box(Modifier.weight(1f).fillMaxHeight().background(Red600))
                }
                
                Box(Modifier.weight(1f)) {
                    when (currentScreen) {
                        "Calendar" -> CalendarScreen(plans) { date, plan ->
                            plans = plans.toMutableMap().apply { if(plan == null) remove(date.toString()) else put(date.toString(), plan) }
                            savePlans(context, plans, gson)
                        }
                        "Itinerary" -> ItineraryScreen(plans) { date ->
                            plans = plans.toMutableMap().apply { remove(date) }
                            savePlans(context, plans, gson)
                        }
                        "Tools" -> ToolsScreen(budgetEntries) { newEntries ->
                            budgetEntries = newEntries
                            saveBudget(context, budgetEntries, gson)
                        }
                        "Settings" -> SettingsScreen(userProfile, plans) { updated ->
                            userProfile = updated; saveProfile(context, updated, gson)
                        }
                        "About" -> AboutScreen()
                    }
                }
            }
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("ALMANAC 2026", fontWeight = FontWeight.Black, color = Amber500, fontSize = 18.sp)
                        }
                    },
                    actions = { MalaysianStandardClock() },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Slate950)
                )
            },
            bottomBar = {
                NavigationBar(containerColor = Slate950, tonalElevation = 0.dp) {
                    val screens = listOf("Calendar", "Itinerary", "Tools", "Settings", "About")
                    screens.forEach { screen ->
                        NavigationBarItem(
                            selected = currentScreen == screen,
                            onClick = { currentScreen = screen },
                            label = { Text(screen, fontSize = 10.sp, color = if(currentScreen == screen) Amber500 else Color.Gray) },
                            icon = {
                                Icon(
                                    when(screen) {
                                        "Calendar" -> Icons.Default.CalendarMonth
                                        "Itinerary" -> Icons.Default.FormatListBulleted
                                        "Tools" -> Icons.Default.Build
                                        "Settings" -> Icons.Default.Person
                                        else -> Icons.Default.Info
                                    },
                                    contentDescription = screen,
                                    tint = if(currentScreen == screen) Amber500 else Color.Gray
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(indicatorColor = Color.White.copy(alpha = 0.1f))
                        )
                    }
                }
            }
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize()) {
                // Flag Strip
                Row(Modifier.fillMaxWidth().height(4.dp)) {
                    Box(Modifier.weight(1f).fillMaxHeight().background(Amber500))
                    Box(Modifier.weight(1f).fillMaxHeight().background(Slate950))
                    Box(Modifier.weight(1f).fillMaxHeight().background(Red600))
                }

                when (currentScreen) {
                    "Calendar" -> CalendarScreen(plans) { date, plan ->
                        plans = plans.toMutableMap().apply { if(plan == null) remove(date.toString()) else put(date.toString(), plan) }
                        savePlans(context, plans, gson)
                    }
                    "Itinerary" -> ItineraryScreen(plans) { date ->
                        plans = plans.toMutableMap().apply { remove(date) }
                        savePlans(context, plans, gson)
                    }
                    "Tools" -> ToolsScreen(budgetEntries) { newEntries ->
                        budgetEntries = newEntries
                        saveBudget(context, budgetEntries, gson)
                    }
                    "Settings" -> SettingsScreen(userProfile, plans) { updated ->
                        userProfile = updated; saveProfile(context, updated, gson)
                    }
                    "About" -> AboutScreen()
                }
            }
        }
    }
}

@Composable
fun ToolsScreen(budgetEntries: List<BudgetEntry>, onUpdateBudget: (List<BudgetEntry>) -> Unit) {
    var selectedTool by remember { mutableStateOf("Select") }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(16.dp)) {
            listOf("Calculator", "Budget Diary").forEach { tool ->
                FilterChip(
                    selected = selectedTool == tool,
                    onClick = { selectedTool = tool },
                    label = { Text(tool) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        when (selectedTool) {
            "Calculator" -> Calculator()
            "Budget Diary" -> BudgetDiary(budgetEntries, onUpdateBudget)
            else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Select a tool above", color = Color.Gray)
            }
        }
    }
}

@Composable
fun Calculator() {
    var display by remember { mutableStateOf("0") }
    var operand1 by remember { mutableStateOf(0.0) }
    var operator by remember { mutableStateOf("") }
    var newNumber by remember { mutableStateOf(true) }

    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Text(
            text = display,
            modifier = Modifier.fillMaxWidth().padding(16.dp).background(Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(8.dp)).padding(16.dp),
            fontSize = 32.sp,
            textAlign = TextAlign.End,
            fontWeight = FontWeight.Bold
        )

        val buttons = listOf(
            listOf("7", "8", "9", "/"),
            listOf("4", "5", "6", "*"),
            listOf("1", "2", "3", "-"),
            listOf("C", "0", "=", "+")
        )

        buttons.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { label ->
                    Button(
                        onClick = {
                            when (label) {
                                "C" -> { display = "0"; operand1 = 0.0; operator = ""; newNumber = true }
                                "=" -> {
                                    val operand2 = display.toDoubleOrNull() ?: 0.0
                                    val result = when (operator) {
                                        "+" -> operand1 + operand2
                                        "-" -> operand1 - operand2
                                        "*" -> operand1 * operand2
                                        "/" -> if(operand2 != 0.0) operand1 / operand2 else 0.0
                                        else -> operand2
                                    }
                                    display = if(result % 1.0 == 0.0) result.toInt().toString() else result.toString()
                                    newNumber = true
                                }
                                "+", "-", "*", "/" -> {
                                    operand1 = display.toDoubleOrNull() ?: 0.0
                                    operator = label
                                    newNumber = true
                                }
                                else -> {
                                    if (newNumber) { display = label; newNumber = false }
                                    else { display += label }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).aspectRatio(1.5f),
                        colors = ButtonDefaults.buttonColors(containerColor = if(label in listOf("C", "=")) Amber500 else Slate800)
                    ) { Text(label, fontSize = 20.sp) }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun BudgetDiary(entries: List<BudgetEntry>, onUpdate: (List<BudgetEntry>) -> Unit) {
    var item by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFF1F5F9))) {
            Column(Modifier.padding(16.dp)) {
                Text("ADD EXPENSE", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Slate500)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = item, onValueChange = { item = it }, label = { Text("Item Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount (RM)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                Button(
                    onClick = {
                        val amt = amount.toDoubleOrNull()
                        if (item.isNotBlank() && amt != null) {
                            onUpdate(entries + BudgetEntry(item = item, amount = amt, date = LocalDate.now().toString()))
                            item = ""; amount = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) { Text("Add Entry") }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("RECENT ENTRIES", fontWeight = FontWeight.Black, fontSize = 12.sp, color = Slate500)
        LazyColumn(Modifier.weight(1f)) {
            items(entries.sortedByDescending { it.id }) { entry ->
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(entry.item, fontWeight = FontWeight.Bold)
                        Text(entry.date, fontSize = 10.sp, color = Color.Gray)
                    }
                    Text("RM %.2f".format(entry.amount), color = Red600, fontWeight = FontWeight.Bold)
                }
                Divider()
            }
        }
        Text("TOTAL: RM %.2f".format(entries.sumOf { it.amount }), fontSize = 18.sp, fontWeight = FontWeight.Black, color = Amber500, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
fun MalaysianStandardClock() {
    var timeText by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while(true) {
            val mst = LocalDateTime.now(ZoneId.of("Asia/Kuala_Lumpur"))
            timeText = mst.format(DateTimeFormatter.ofPattern("HH:mm:ss"))
            delay(1000)
        }
    }
    Column(Modifier.padding(end = 16.dp), horizontalAlignment = Alignment.End) {
        Text("MST (SIRIM)", color = Slate500, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Text(timeText, color = Amber500, fontWeight = FontWeight.Black, fontSize = 14.sp)
    }
}

@Composable
fun CalendarScreen(plans: Map<String, UserPlan>, onUpdate: (LocalDate, UserPlan?) -> Unit) {
    var yearMonth by remember { mutableStateOf(YearMonth.of(2026, 1)) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    val context = LocalContext.current

    Column(Modifier.verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { yearMonth = yearMonth.minusMonths(1) }) { Icon(Icons.Default.ChevronLeft, null) }
            Text(yearMonth.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH).uppercase(), fontWeight = FontWeight.Black, fontSize = 20.sp)
            IconButton(onClick = { yearMonth = yearMonth.plusMonths(1) }) { Icon(Icons.Default.ChevronRight, null) }
        }

        CalendarGrid(yearMonth, plans) { selectedDate = it }

        if (selectedDate != null) {
            PlanOverlay(
                date = selectedDate!!,
                existing = plans[selectedDate.toString()],
                onDismiss = { selectedDate = null },
                onSave = { plan -> onUpdate(selectedDate!!, plan); selectedDate = null },
                onSync = { plan -> syncWithGoogleCalendar(context, selectedDate!!, plan) }
            )
        }
    }
}

@Composable
fun CalendarGrid(yearMonth: YearMonth, plans: Map<String, UserPlan>, onDateClick: (LocalDate) -> Unit) {
    val firstDay = yearMonth.atDay(1).dayOfWeek.value % 7
    val daysInMonth = yearMonth.lengthOfMonth()

    Card(Modifier.padding(16.dp), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFF1F5F9))) {
        Column(Modifier.padding(8.dp)) {
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                listOf("S","M","T","W","T","F","S").forEach { Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 10.sp, fontWeight = FontWeight.Black, color = Slate500) }
            }
            val rows = (daysInMonth + firstDay + 6) / 7
            for (r in 0 until rows) {
                Row {
                    for (c in 0 until 7) {
                        val dayNum = r * 7 + c - firstDay + 1
                        if (dayNum in 1..daysInMonth) {
                            val date = yearMonth.atDay(dayNum)
                            val info = getAlmanacData(date)
                            val plan = plans[date.toString()]
                            Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp).clip(CircleShape)
                                .background(if (info.isPH) Color(0xFFFFFBFA) else Color.Transparent)
                                .clickable { onDateClick(date) }, contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(dayNum.toString(), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if(info.isPH) Red600 else Slate950)
                                    if(plan != null) Box(Modifier.size(4.dp).background(if(plan.isLeave) Red600 else Amber500, CircleShape))
                                }
                            }
                        } else Box(Modifier.weight(1f).aspectRatio(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun PlanOverlay(date: LocalDate, existing: UserPlan?, onDismiss: () -> Unit, onSave: (UserPlan?) -> Unit, onSync: (UserPlan) -> Unit) {
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var time by remember { mutableStateOf(existing?.time ?: "") }
    var isLeave by remember { mutableStateOf(existing?.isLeave ?: false) }
    val info = getAlmanacData(date)

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(24.dp)) {
                Text(date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")), fontWeight = FontWeight.Black, color = Amber500, fontSize = 14.sp)
                if (info.remark != null) Text(info.remark, color = Red600, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))

                Spacer(Modifier.height(20.dp))
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note/Plan") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text("Time (Optional)") }, placeholder = { Text("e.g. 14:30") }, modifier = Modifier.fillMaxWidth())

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 12.dp)) {
                    Checkbox(checked = isLeave, onCheckedChange = { isLeave = it })
                    Text("Mark as Annual Leave", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }

                Button(onClick = { onSave(if(note.isBlank()) null else UserPlan(note, time, isLeave)) }, modifier = Modifier.fillMaxWidth()) { Text("Save Locally") }
                if (note.isNotBlank()) {
                    TextButton(onClick = { onSync(UserPlan(note, time, isLeave)) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Sync, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Sync to Google Calendar")
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Discard", color = Color.Gray) }
            }
        }
    }
}

@Composable
fun ItineraryScreen(plans: Map<String, UserPlan>, onDelete: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item { Text("ALL SCHEDULED EVENTS", fontWeight = FontWeight.Black, fontSize = 12.sp, color = Slate500, modifier = Modifier.padding(bottom = 12.dp)) }
        val sorted = plans.entries.sortedBy { it.key }
        if (sorted.isEmpty()) {
            item { Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) { Text("No plans recorded.", color = Color.LightGray) } }
        } else {
            items(sorted) { (date, plan) ->
                Card(Modifier.padding(vertical = 6.dp).fillMaxWidth(), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color(0xFFF1F5F9)), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(date, fontWeight = FontWeight.Black, fontSize = 10.sp, color = Amber500)
                            Text(if(plan.time.isNotBlank()) "${plan.time} - ${plan.note}" else plan.note, fontWeight = FontWeight.Bold)
                            if(plan.isLeave) Text("ANNUAL LEAVE", color = Red600, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                        IconButton(onClick = { onDelete(date) }) { Icon(Icons.Default.Delete, null, tint = Color(0xFFCBD5E1)) }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(profile: UserProfile, plans: Map<String, UserPlan>, onUpdate: (UserProfile) -> Unit) {
    var ent by remember { mutableStateOf(profile.entitlement.toString()) }
    val used = plans.values.count { it.isLeave }

    Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
        Text("LEAVE SETTINGS", fontWeight = FontWeight.Black, color = Slate500, fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = ent, onValueChange = { ent = it; onUpdate(profile.copy(entitlement = it.toIntOrNull() ?: 0)) }, label = { Text("Annual Leave Entitlement (Days)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))

        Spacer(Modifier.height(32.dp))
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Slate950)) {
            Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("LEAVE BALANCE", color = Amber500, fontWeight = FontWeight.Black, fontSize = 11.sp)
                Text("${profile.entitlement - used}", fontSize = 64.sp, fontWeight = FontWeight.Black, color = Color.White)
                Text("DAYS REMAINING", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AboutScreen() {
    val uriHandler = LocalUriHandler.current
    Column(Modifier.padding(32.dp).fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.AutoAwesome, null, tint = Amber500, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(24.dp))
        Text("ABOUT THIS APP", fontWeight = FontWeight.Black, fontSize = 24.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            "This application was fully generated with the assistance of Google Gemini, prompted by Jerry Chieng Chin Tung, a generative AI enthusiast from Sarawak.",
            textAlign = TextAlign.Center, fontWeight = FontWeight.Medium, color = Slate800, lineHeight = 24.sp
        )
        
        Spacer(Modifier.height(32.dp))
        
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "Learn Generative AI",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Slate950
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Interested in mastering Generative AI? Follow Coach Khairul Ikhwan Zulkefly or sign up as a TAOP+ member to get started.",
                    fontSize = 14.sp,
                    color = Slate500,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { uriHandler.openUri("https://www.facebook.com/Khairul01ikhwan") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Follow Coach Khairul Ikhwan")
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { uriHandler.openUri("https://gigventure.onpay.my/order/form/Taop_Plus2025/jerrychiengct") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Amber500)
                ) {
                    Text("Register for TAOP+")
                }
            }
        }
        
        Spacer(Modifier.height(32.dp))
        
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                 Box(
                     modifier = Modifier
                         .size(80.dp)
                         .clip(CircleShape)
                         .background(Color.LightGray),
                     contentAlignment = Alignment.Center
                 ) {
                     // Image(painter = painterResource(id = R.drawable.profile_jerry), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                     Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
                 }
                 Spacer(Modifier.height(12.dp))
                 Text("Jerry Chieng Chin Tung", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                 Text("Based in Bintulu", fontSize = 12.sp, color = Slate500, fontWeight = FontWeight.Medium)
                 Spacer(Modifier.height(8.dp))
                 Text(
                     "If you'd like to support my work or connect, please follow me. Besides being a Generative AI enthusiast, I also provide repair services for laptops and Android smartphones.",
                     textAlign = TextAlign.Center,
                     fontSize = 13.sp,
                     color = Slate500
                 )
                 Spacer(Modifier.height(16.dp))
                 Button(onClick = { uriHandler.openUri("https://facebook.com/jerrycct") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text("Facebook: jerrycct") }
                 Spacer(Modifier.height(8.dp))
                 Button(onClick = { uriHandler.openUri("https://tiktok.com/@jerrychiengct") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text("TikTok: @jerrychiengct") }
            }
        }

        Spacer(Modifier.height(32.dp))
        
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9))) {
             Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                 Column(Modifier.weight(1f)) {
                     Text("Support the Developer", fontWeight = FontWeight.Black, fontSize = 14.sp)
                     Text("Enjoying the app? Tips are appreciated! Contact me for details.", fontSize = 12.sp, color = Color.Gray)
                 }
                 Column {
                     IconButton(onClick = { uriHandler.openUri("https://facebook.com/jerrycct") }) {
                        Icon(Icons.Default.ThumbUp, contentDescription = "Facebook", tint = Amber500)
                     }
                 }
             }
        }
    }
}

// --- LOGIC HELPERS ---

fun syncWithGoogleCalendar(ctx: Context, date: LocalDate, plan: UserPlan) {
    val time = try {
        val parts = plan.time.split(":")
        LocalTime.of(parts[0].toInt(), parts[1].filter { it.isDigit() }.toInt())
    } catch (e: Exception) { LocalTime.of(9, 0) }

    val startMillis = date.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val intent = Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)
        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, startMillis + 3600000)
        .putExtra(CalendarContract.Events.TITLE, plan.note)
        .putExtra(CalendarContract.Events.DESCRIPTION, "Scheduled via Almanac Sarawak 2026")
    ctx.startActivity(intent)
}

fun getAlmanacData(d: LocalDate): AlmanacDayInfo {
    val dateStr = d.toString()
    val ph = mapOf(
        "2026-01-01" to "New Year 2026", "2026-01-16" to "Israk & Mikraj", "2026-02-11" to "Thaipusam",
        "2026-02-17" to "CNY Day 1", "2026-02-18" to "CNY Day 2", "2026-03-07" to "Nuzul Al-Quran",
        "2026-03-20" to "Hari Raya Puasa", "2026-03-21" to "Hari Raya Puasa Day 2", "2026-05-01" to "Labour Day",
        "2026-05-27" to "Hari Raya Korban", "2026-05-31" to "Wesak Day", "2026-06-01" to "Agong's Bday / Gawai Dayak",
        "2026-06-02" to "Gawai Dayak Day 2", "2026-06-17" to "Awal Muharram", "2026-07-22" to "Sarawak Day",
        "2026-08-26" to "Maulidur Rasul", "2026-08-31" to "National Day", "2026-09-16" to "Malaysia Day",
        "2026-10-10" to "Governor's Birthday", "2026-11-08" to "Deepavali", "2026-12-25" to "Christmas Day"
    )
    val remarks = mutableMapOf<String, String>()
    remarks["2026-01-01"] = "King Tide 1617 hrs (5.8 m)"
    remarks["2026-01-02"] = "King Tide 1712 hrs (6.0 m)"
    remarks["2026-01-03"] = "King Tide 1803 hrs (6.1 m)"
    remarks["2026-01-15"] = "Ponggal Festival / King Tide 2128 hrs (5.5 m)"
    remarks["2026-02-18"] = "Ash Wednesday"
    remarks["2026-02-19"] = "1st day of Ramadan"
    remarks["2026-04-05"] = "Easter Sunday / Qing Ming Jie"
    remarks["2026-05-16"] = "Miri City Day"
    remarks["2026-08-01"] = "Kuching City Day"

    // School Breaks (Sarawak Kumpulan B)
    if (d in LocalDate.of(2026, 3, 21)..LocalDate.of(2026, 3, 29)) remarks[dateStr] = "Cuti Penggal 1"
    if (d in LocalDate.of(2026, 5, 23)..LocalDate.of(2026, 6, 7)) remarks[dateStr] = "Cuti Pertengahan Tahun"
    if (d in LocalDate.of(2026, 8, 29)..LocalDate.of(2026, 9, 6)) remarks[dateStr] = "Cuti Penggal 2"
    if (d in LocalDate.of(2026, 12, 5)..LocalDate.of(2026, 12, 31)) remarks[dateStr] = "Cuti Akhir Tahun"

    return AlmanacDayInfo(ph.containsKey(dateStr), ph[dateStr] ?: remarks[dateStr])
}

fun loadProfile(c: Context, g: Gson) = c.getSharedPreferences("a", 0).getString("u", null)?.let { g.fromJson(it, UserProfile::class.java) } ?: UserProfile()
fun saveProfile(c: Context, p: UserProfile, g: Gson) = c.getSharedPreferences("a", 0).edit().putString("u", g.toJson(p)).apply()
fun loadPlans(c: Context, g: Gson): Map<String, UserPlan> = c.getSharedPreferences("a", 0).getString("p", null)?.let { g.fromJson(it, object : TypeToken<Map<String, UserPlan>>() {}.type) } ?: emptyMap()
fun savePlans(c: Context, p: Map<String, UserPlan>, g: Gson) = c.getSharedPreferences("a", 0).edit().putString("p", g.toJson(p)).apply()
fun loadBudget(c: Context, g: Gson): List<BudgetEntry> = c.getSharedPreferences("a", 0).getString("b", null)?.let { g.fromJson(it, object : TypeToken<List<BudgetEntry>>() {}.type) } ?: emptyList()
fun saveBudget(c: Context, b: List<BudgetEntry>, g: Gson) = c.getSharedPreferences("a", 0).edit().putString("b", g.toJson(b)).apply()