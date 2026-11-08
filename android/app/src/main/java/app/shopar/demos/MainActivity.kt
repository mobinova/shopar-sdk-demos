package app.shopar.demos

import android.content.Context
import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.shopar.sdk.agency.AgencyConfiguration
import app.shopar.sdk.agency.AgencyStyle
import app.shopar.sdk.agency.ShoparAgency
import app.shopar.sdk.discovery.DiscoveryConfiguration
import app.shopar.sdk.discovery.DiscoveryStyle
import app.shopar.sdk.discovery.ShoparDiscovery
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private enum class DemoTab(val label: String) { Discovery("Discovery"), Agency("Agency"), Settings("Settings") }

private data class StartupConfiguration(val appId: String, val host: String, val port: Int, val tls: Boolean, val stylePreset: String)

private class DemoSettings(context: Context) {
    private val prefs = context.getSharedPreferences("shopar-sdk-demo-settings", Context.MODE_PRIVATE)
    var agencyAppId by mutableStateOf(prefs.getString("agency.appId", BuildConfig.SHOPAR_DEMO_AGENCY_APP_ID).orEmpty())
        private set
    var agencyHost by mutableStateOf(prefs.getString("agency.host", BuildConfig.SHOPAR_DEMO_AGENCY_HOST).orEmpty())
        private set
    var agencyPort by mutableStateOf(prefs.getString("agency.port", BuildConfig.SHOPAR_DEMO_AGENCY_PORT).orEmpty())
        private set
    var agencyTls by mutableStateOf(prefs.getBoolean("agency.tls", BuildConfig.SHOPAR_DEMO_AGENCY_TLS))
        private set
    var agencyStylePreset by mutableStateOf(prefs.getString("agency.style", "shopar").orEmpty())
        private set
    var discoveryAppId by mutableStateOf(prefs.getString("discovery.appId", BuildConfig.SHOPAR_DEMO_DISCOVERY_APP_ID).orEmpty())
        private set
    var discoveryHost by mutableStateOf(prefs.getString("discovery.host", BuildConfig.SHOPAR_DEMO_DISCOVERY_HOST).orEmpty())
        private set
    var discoveryPort by mutableStateOf(prefs.getString("discovery.port", BuildConfig.SHOPAR_DEMO_DISCOVERY_PORT).orEmpty())
        private set
    var discoveryTls by mutableStateOf(prefs.getBoolean("discovery.tls", BuildConfig.SHOPAR_DEMO_DISCOVERY_TLS))
        private set
    var discoveryStylePreset by mutableStateOf(prefs.getString("discovery.style", "shopar").orEmpty())
        private set
    var collectDeviceMetadata by mutableStateOf(prefs.getBoolean("discovery.collectDeviceMetadata", false))
        private set
    var placementIdsText by mutableStateOf(prefs.getString("discovery.placementIds", BuildConfig.SHOPAR_DEMO_DISCOVERY_PLACEMENT_IDS).orEmpty())
        private set
    var selectedPlacementId by mutableStateOf(prefs.getString("discovery.selectedPlacementId", BuildConfig.SHOPAR_DEMO_DISCOVERY_SELECTED_PLACEMENT_ID).orEmpty())
        private set
    var activeProfile by mutableStateOf(prefs.getString("activeProfile", "").orEmpty())
        private set
    var profileNames by mutableStateOf(prefs.getStringSet("profileNames", emptySet()).orEmpty().toSortedSet())
        private set

    val placementIds: List<String>
        get() = placementIdsText.split(',', '\n').map(String::trim).filter(String::isNotEmpty).distinct()

    fun updateAgencyAppId(value: String) { agencyAppId = value.trim(); save() }
    fun updateAgencyHost(value: String) { agencyHost = value.trim(); save() }
    fun updateAgencyPort(value: String) { agencyPort = value.filter(Char::isDigit).take(5); save() }
    fun updateAgencyTls(value: Boolean) { agencyTls = value; save() }
    fun updateAgencyStylePreset(value: String) { agencyStylePreset = value; save() }
    fun updateDiscoveryAppId(value: String) { discoveryAppId = value.trim(); save() }
    fun updateDiscoveryHost(value: String) { discoveryHost = value.trim(); save() }
    fun updateDiscoveryPort(value: String) { discoveryPort = value.filter(Char::isDigit).take(5); save() }
    fun updateDiscoveryTls(value: Boolean) { discoveryTls = value; save() }
    fun updateDiscoveryStylePreset(value: String) { discoveryStylePreset = value; save() }
    fun updateCollectDeviceMetadata(value: Boolean) { collectDeviceMetadata = value; save() }
    fun updatePlacementIds(value: String) { placementIdsText = value; save() }
    fun updateSelectedPlacementId(value: String) { selectedPlacementId = value; save() }

    val agencyStyle: AgencyStyle get() = if (agencyStylePreset == "midnight") AgencyStyle.Midnight else AgencyStyle()
    val discoveryStyle: DiscoveryStyle get() = if (discoveryStylePreset == "midnight") DiscoveryStyle.Midnight else DiscoveryStyle()

    fun agencyStartup(): StartupConfiguration? = startup(agencyAppId, agencyHost, agencyPort, agencyTls, agencyStylePreset)
    fun discoveryStartup(): StartupConfiguration? = startup(discoveryAppId, discoveryHost, discoveryPort, discoveryTls, discoveryStylePreset)

    fun saveProfile(nameInput: String) {
        val name = nameInput.trim().take(40)
        if (name.isEmpty()) return
        val profile = JSONObject().apply {
            put("agencyAppId", agencyAppId); put("agencyHost", agencyHost); put("agencyPort", agencyPort); put("agencyTls", agencyTls)
            put("agencyStylePreset", agencyStylePreset)
            put("discoveryAppId", discoveryAppId); put("discoveryHost", discoveryHost); put("discoveryPort", discoveryPort)
            put("discoveryStylePreset", discoveryStylePreset)
            put("discoveryTls", discoveryTls); put("collectDeviceMetadata", collectDeviceMetadata)
            put("placementIdsText", placementIdsText); put("selectedPlacementId", selectedPlacementId)
        }
        profileNames = (profileNames + name).toSortedSet()
        activeProfile = name
        prefs.edit().putString("profile.$name", profile.toString()).putStringSet("profileNames", profileNames)
            .putString("activeProfile", activeProfile).apply()
    }

    fun selectProfile(name: String) {
        val profile = runCatching { JSONObject(prefs.getString("profile.$name", null) ?: return) }.getOrNull() ?: return
        agencyAppId = profile.optString("agencyAppId"); agencyHost = profile.optString("agencyHost")
        agencyPort = profile.optString("agencyPort", "443"); agencyTls = profile.optBoolean("agencyTls", true)
        agencyStylePreset = profile.optString("agencyStylePreset", "shopar")
        discoveryAppId = profile.optString("discoveryAppId"); discoveryHost = profile.optString("discoveryHost")
        discoveryPort = profile.optString("discoveryPort", "443"); discoveryTls = profile.optBoolean("discoveryTls", true)
        discoveryStylePreset = profile.optString("discoveryStylePreset", "shopar")
        collectDeviceMetadata = profile.optBoolean("collectDeviceMetadata", false)
        placementIdsText = profile.optString("placementIdsText"); selectedPlacementId = profile.optString("selectedPlacementId")
        activeProfile = name
        save()
    }

    private fun save() {
        prefs.edit()
            .putString("agency.appId", agencyAppId).putString("agency.host", agencyHost).putString("agency.port", agencyPort).putBoolean("agency.tls", agencyTls)
            .putString("agency.style", agencyStylePreset)
            .putString("discovery.appId", discoveryAppId).putString("discovery.host", discoveryHost).putString("discovery.port", discoveryPort)
            .putString("discovery.style", discoveryStylePreset)
            .putBoolean("discovery.tls", discoveryTls).putBoolean("discovery.collectDeviceMetadata", collectDeviceMetadata)
            .putString("discovery.placementIds", placementIdsText).putString("discovery.selectedPlacementId", selectedPlacementId)
            .putString("activeProfile", activeProfile).putStringSet("profileNames", profileNames).apply()
    }

    private fun startup(appId: String, host: String, portText: String, tls: Boolean, stylePreset: String): StartupConfiguration? {
        if (!appId.matches(Regex("^app_[0-9A-HJKMNP-TV-Z]{26}$")) || host.isBlank() || host.contains('/') || host.any(Char::isWhitespace)) return null
        val port = portText.toIntOrNull()?.takeIf { it in 1..65535 } ?: return null
        return StartupConfiguration(appId, host, port, tls, stylePreset)
    }
}

private class DemoSdkCoordinator {
    var activeAgency: StartupConfiguration? = null
        private set
    var activeDiscovery: StartupConfiguration? = null
        private set

    fun startAgency(context: Context, config: StartupConfiguration, style: AgencyStyle): String? {
        if (activeAgency != null) return if (activeAgency == config) null else "Agency config or style changed. Relaunch this demo to apply it."
        return runCatching {
            ShoparAgency.initialize(context.applicationContext, AgencyConfiguration(config.appId, config.host, config.port, config.tls, style))
            activeAgency = config
        }.exceptionOrNull()?.message
    }

    fun startDiscovery(context: Context, config: StartupConfiguration, collectMetadata: Boolean, style: DiscoveryStyle): String? {
        if (activeDiscovery != null && activeDiscovery != config) return "Discovery config or style changed. Relaunch this demo to apply it."
        if (activeDiscovery == null) {
            val error = runCatching {
                ShoparDiscovery.initialize(context.applicationContext, DiscoveryConfiguration(config.appId, config.host, config.port, config.tls, collectMetadata, style))
                activeDiscovery = config
            }.exceptionOrNull()?.message
            if (error != null) return error
        } else {
            runCatching { ShoparDiscovery.setOptionalDeviceMetadataCollectionEnabled(collectMetadata) }
                .exceptionOrNull()?.let { return it.message }
        }
        return null
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settings = DemoSettings(applicationContext)
        setContent { ShoparDemoApp(settings) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShoparDemoApp(settings: DemoSettings) {
    val context = LocalContext.current
    val coordinator = remember { DemoSdkCoordinator() }
    var selectedTab by remember {
        mutableStateOf(
            if (settings.discoveryStartup() != null && settings.selectedPlacementId in settings.placementIds) DemoTab.Discovery
            else DemoTab.Settings,
        )
    }
    MaterialTheme(
        colorScheme = androidx.compose.material3.lightColorScheme(
            primary = androidx.compose.ui.graphics.Color(0xFF18221D),
            onPrimary = androidx.compose.ui.graphics.Color.White,
            primaryContainer = androidx.compose.ui.graphics.Color(0xFFD8F36A),
            onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF18221D),
            secondary = androidx.compose.ui.graphics.Color(0xFF18221D),
            onSecondary = androidx.compose.ui.graphics.Color.White,
            background = androidx.compose.ui.graphics.Color(0xFFF6F7F2),
            onBackground = androidx.compose.ui.graphics.Color(0xFF18221D),
            surface = androidx.compose.ui.graphics.Color.White,
            onSurface = androidx.compose.ui.graphics.Color(0xFF18221D),
            surfaceVariant = androidx.compose.ui.graphics.Color(0xFFEFF2E9),
            onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF59655D),
            outline = androidx.compose.ui.graphics.Color(0xFF7E8B80),
            outlineVariant = androidx.compose.ui.graphics.Color(0xFFDDE3D8),
        ),
    ) {
        Scaffold(containerColor = androidx.compose.ui.graphics.Color(0xFFF6F7F2),
            bottomBar = {
                NavigationBar(
                    containerColor = androidx.compose.ui.graphics.Color(0xFFF6F7F2),
                    contentColor = androidx.compose.ui.graphics.Color(0xFF18221D),
                ) {
                    listOf(DemoTab.Discovery, DemoTab.Agency, DemoTab.Settings).forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = androidx.compose.ui.graphics.Color(0xFF18221D),
                                selectedTextColor = androidx.compose.ui.graphics.Color(0xFF18221D),
                                indicatorColor = androidx.compose.ui.graphics.Color(0xFFD8F36A),
                                unselectedIconColor = androidx.compose.ui.graphics.Color(0xFF59655D),
                                unselectedTextColor = androidx.compose.ui.graphics.Color(0xFF59655D),
                            ),
                            icon = {
                                Icon(
                                    when (tab) {
                                        DemoTab.Discovery -> Icons.Default.Explore
                                        DemoTab.Agency -> Icons.Default.Person
                                        DemoTab.Settings -> Icons.Default.Settings
                                    }, contentDescription = tab.label,
                                )
                            },
                            label = { Text(tab.label) },
                        )
                    }
                }
            },
        ) { padding ->
            when (selectedTab) {
                DemoTab.Settings -> SettingsScreen(settings, coordinator, Modifier.padding(padding))
                DemoTab.Agency -> AgencyDemoScreen(settings, coordinator, context, onOpenSettings = { selectedTab = DemoTab.Settings }, Modifier.padding(padding))
                DemoTab.Discovery -> DiscoveryDemoScreen(settings, coordinator, context, onOpenSettings = { selectedTab = DemoTab.Settings }, Modifier.padding(padding))
            }
        }
    }
}

@Composable
private fun AgencyDemoScreen(settings: DemoSettings, coordinator: DemoSdkCoordinator, context: Context, onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    var error by remember { mutableStateOf<String?>("Preparing Agency SDK configuration…") }
    var showSecurityInfo by remember { mutableStateOf(false) }
    val startup = settings.agencyStartup()
    LaunchedEffect(startup) {
        error = if (startup == null) "Enter a valid Agency app ID and endpoint in Settings."
        else coordinator.startAgency(context, startup, settings.agencyStyle)
    }
    if (error != null) ConfigRequiredScreen("Agency", error ?: "Agency is not configured.", onOpenSettings, modifier)
    else androidx.compose.foundation.layout.Box(modifier.fillMaxSize()) {
        ShoparAgency.LinksScreen(onOpenAccountSecurity = { showSecurityInfo = true })
        if (showSecurityInfo) AlertDialog(
            onDismissRequest = { showSecurityInfo = false },
            title = { Text("Host account security") },
            text = { Text("The Agency demo manages sessions for its own app. Account-wide Shopar sessions belong to a first-party Shopar account security screen.") },
            confirmButton = { TextButton(onClick = { showSecurityInfo = false }) { Text("OK") } },
        )
    }
}

@Composable
private fun DiscoveryDemoScreen(settings: DemoSettings, coordinator: DemoSdkCoordinator, context: Context, onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    var error by remember { mutableStateOf<String?>("Preparing Discovery SDK configuration…") }
    var menuExpanded by remember { mutableStateOf(false) }
    val startup = settings.discoveryStartup()
    LaunchedEffect(startup, settings.collectDeviceMetadata) {
        error = if (startup == null) "Enter a valid Discovery app ID and endpoint in Settings."
        else coordinator.startDiscovery(context, startup, settings.collectDeviceMetadata, settings.discoveryStyle)
    }
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Discovery demo", style = MaterialTheme.typography.headlineSmall)
        if (error != null) {
            ConfigRequiredScreen("Discovery", error ?: "Discovery is not configured.", onOpenSettings)
            return@Column
        }
        val placementIDs = settings.placementIds
        if (placementIDs.isEmpty()) {
            ConfigRequiredScreen("Discovery placement", "Add real App Studio placement IDs in Settings.", onOpenSettings)
        } else {
            val selected = settings.selectedPlacementId.takeIf { it in placementIDs } ?: placementIDs.first()
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Placement: $selected", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                BoxMenu(selected, placementIDs, menuExpanded, { menuExpanded = it }, settings::updateSelectedPlacementId)
            }
            HorizontalDivider()
            ShoparDiscovery.Placement(placementId = selected, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun BoxMenu(selected: String, values: List<String>, expanded: Boolean, setExpanded: (Boolean) -> Unit, onSelect: (String) -> Unit) {
    androidx.compose.foundation.layout.Box {
        OutlinedButton(onClick = { setExpanded(true) }) { Text("Choose") }
        DropdownMenu(expanded = expanded, onDismissRequest = { setExpanded(false) }) {
            values.forEach { value -> DropdownMenuItem(text = { Text(value) }, onClick = { onSelect(value); setExpanded(false) }) }
        }
    }
}

@Composable
private fun SettingsScreen(settings: DemoSettings, coordinator: DemoSdkCoordinator, modifier: Modifier = Modifier) {
    var profileName by remember { mutableStateOf("") }
    var profileMenu by remember { mutableStateOf(false) }
    var showResetConfirmation by remember { mutableStateOf(false) }
    var resetMessage by remember { mutableStateOf<String?>(null) }
    val agencyPending = coordinator.activeAgency?.let { it != settings.agencyStartup() } == true
    val discoveryPending = coordinator.activeDiscovery?.let { it != settings.discoveryStartup() } == true

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = { Text("Reset Discovery installation ID?") },
            text = { Text("Use this only to honor an explicit test-user reset request. Historical server events remain unchanged.") },
            confirmButton = {
                TextButton(onClick = {
                    showResetConfirmation = false
                    resetMessage = runCatching { ShoparDiscovery.resetInstallationIdentity() }
                        .fold({ "Discovery installation ID reset. Historical server events remain unchanged." }, { "Reset failed: ${it.message ?: "try again"}" })
                }) { Text("Reset ID") }
            },
            dismissButton = { TextButton(onClick = { showResetConfirmation = false }) { Text("Cancel") } },
        )
    }

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("SDK configuration", style = MaterialTheme.typography.headlineSmall)
        Text("Values are local to this demo. Enter app IDs and placement IDs created in the Shopar dashboards.", style = MaterialTheme.typography.bodyMedium)
        if (agencyPending || discoveryPending) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
        Text("Startup configuration changed. Relaunch the demo app to apply changed app IDs, endpoints, or appearance.", Modifier.padding(14.dp))
            }
        }
        if (!settings.agencyTls || !settings.discoveryTls) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text("TLS is disabled for at least one module. Use this only on an isolated local test network; Agency sessions are sensitive.", Modifier.padding(14.dp))
            }
        }

        Card(shape = MaterialTheme.shapes.large, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("Agency SDK", style = MaterialTheme.typography.titleMedium)
            ConfigField("Agency app_id", settings.agencyAppId, settings::updateAgencyAppId)
            ConfigField("gRPC host", settings.agencyHost, settings::updateAgencyHost)
            ConfigField("gRPC port", settings.agencyPort, settings::updateAgencyPort)
            LabeledSwitch("Use TLS", settings.agencyTls, settings::updateAgencyTls)
            StyleSelector("UI style", settings.agencyStylePreset, settings::updateAgencyStylePreset)
        } }

        Card(shape = MaterialTheme.shapes.large, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("Discovery SDK", style = MaterialTheme.typography.titleMedium)
            ConfigField("Discovery app_id", settings.discoveryAppId, settings::updateDiscoveryAppId)
            ConfigField("gRPC host", settings.discoveryHost, settings::updateDiscoveryHost)
            ConfigField("gRPC port", settings.discoveryPort, settings::updateDiscoveryPort)
            LabeledSwitch("Use TLS", settings.discoveryTls, settings::updateDiscoveryTls)
            StyleSelector("UI style", settings.discoveryStylePreset, settings::updateDiscoveryStylePreset)
            LabeledSwitch("Collect optional device metadata", settings.collectDeviceMetadata, settings::updateCollectDeviceMetadata)
            ConfigField("Placement IDs (comma or newline separated)", settings.placementIdsText, settings::updatePlacementIds, singleLine = false)
            ConfigField("Selected placement ID", settings.selectedPlacementId, settings::updateSelectedPlacementId)
            OutlinedButton(enabled = coordinator.activeDiscovery != null, onClick = { showResetConfirmation = true }) { Text("Reset Discovery installation ID") }
            if (coordinator.activeDiscovery == null) Text("Open the Discovery tab once before resetting its installation ID.", style = MaterialTheme.typography.bodySmall)
            resetMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        } }

        Card(shape = MaterialTheme.shapes.large, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("Local demo profiles", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(profileName, { profileName = it }, label = { Text("Profile name") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedButton(onClick = { settings.saveProfile(profileName); profileName = "" }, enabled = profileName.isNotBlank()) { Text("Save") }
            }
            Box {
                OutlinedButton(onClick = { profileMenu = true }, enabled = settings.profileNames.isNotEmpty()) {
                    Text(settings.activeProfile.ifBlank { "Choose saved profile" })
                }
                DropdownMenu(expanded = profileMenu, onDismissRequest = { profileMenu = false }) {
                    settings.profileNames.forEach { name -> DropdownMenuItem(text = { Text(name) }, onClick = { settings.selectProfile(name); profileMenu = false }) }
                }
            }
            Text("Profiles contain only non-secret configuration values. They never contain SDK tokens.", style = MaterialTheme.typography.bodySmall)
        } }

        Card(shape = MaterialTheme.shapes.large, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("Demo diagnostics", style = MaterialTheme.typography.titleMedium)
            Text("Demo build: ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall)
            Text("Shopar Android SDK source: ${BuildConfig.SHOPAR_SDK_SOURCE_REVISION}", style = MaterialTheme.typography.bodySmall)
            Text("Device OS: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})", style = MaterialTheme.typography.bodySmall)
            Text("Agency app: ${settings.agencyAppId.ifBlank { "not configured" }}", style = MaterialTheme.typography.bodySmall)
            Text("Discovery app: ${settings.discoveryAppId.ifBlank { "not configured" }}", style = MaterialTheme.typography.bodySmall)
            Text("Selected placement: ${settings.selectedPlacementId.ifBlank { "not configured" }}", style = MaterialTheme.typography.bodySmall)
        } }

        Text("Agency credentials remain inside the SDK. Discovery uses no Agency auth. Placement format, offers, and product ordering come from Shopar.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ConfigField(label: String, value: String, onChange: (String) -> Unit, singleLine: Boolean = true) {
    OutlinedTextField(value, onChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), singleLine = singleLine)
}

@Composable
private fun StyleSelector(label: String, value: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text("$label: ${if (value == "midnight") "Midnight" else "Shopar"}")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Shopar") }, onClick = { onSelect("shopar"); expanded = false })
            DropdownMenuItem(text = { Text("Midnight") }, onClick = { onSelect("midnight"); expanded = false })
        }
    }
}

@Composable
private fun LabeledSwitch(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = value, onCheckedChange = onChange)
    }
}

@Composable
private fun ConfigRequiredScreen(title: String, message: String, onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text(message)
        Button(onClick = onOpenSettings) { Text("Open Settings") }
    }
}
