import Foundation
import SwiftUI
import UIKit
import ShoparAgency
import ShoparDiscovery

private struct DemoConfiguration: Codable, Equatable {
    var agencyAppID = ""
    var agencyHost = ""
    var agencyPort = "443"
    var agencyTLS = true
    var agencyStylePreset: String? = nil
    var discoveryAppID = ""
    var discoveryHost = ""
    var discoveryPort = "443"
    var discoveryTLS = true
    var discoveryStylePreset: String? = nil
    var collectDeviceMetadata = false
    var placementIDsText = ""
    var selectedPlacementID = ""

    var agencyStyle: AgencyStyle { agencyStylePreset == "midnight" ? .midnight : .shopar }
    var discoveryStyle: DiscoveryStyle { discoveryStylePreset == "midnight" ? .midnight : .shopar }

    init() {
        let defaults = Bundle.main.infoDictionary ?? [:]
        agencyAppID = defaults["ShoparDemoAgencyAppID"] as? String ?? ""
        agencyHost = defaults["ShoparDemoAgencyHost"] as? String ?? "grpc.shopar.app"
        agencyPort = defaults["ShoparDemoAgencyPort"] as? String ?? "443"
        agencyTLS = (defaults["ShoparDemoAgencyTLS"] as? String).map { $0.lowercased() != "no" && $0.lowercased() != "false" } ?? true
        discoveryAppID = defaults["ShoparDemoDiscoveryAppID"] as? String ?? ""
        discoveryHost = defaults["ShoparDemoDiscoveryHost"] as? String ?? "grpc.shopar.app"
        discoveryPort = defaults["ShoparDemoDiscoveryPort"] as? String ?? "443"
        discoveryTLS = (defaults["ShoparDemoDiscoveryTLS"] as? String).map { $0.lowercased() != "no" && $0.lowercased() != "false" } ?? true
        placementIDsText = defaults["ShoparDemoDiscoveryPlacementIDs"] as? String ?? ""
        selectedPlacementID = defaults["ShoparDemoDiscoverySelectedPlacementID"] as? String ?? ""
    }

    var placementIDs: [String] {
        placementIDsText
            .components(separatedBy: CharacterSet(charactersIn: ",\n"))
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
            .reduce(into: [String]()) { if !$0.contains($1) { $0.append($1) } }
    }

    var agencyStartup: DemoStartupConfiguration? {
        DemoStartupConfiguration(appID: agencyAppID, host: agencyHost, port: agencyPort, tls: agencyTLS, stylePreset: agencyStylePreset ?? "shopar")
    }

    var discoveryStartup: DemoStartupConfiguration? {
        DemoStartupConfiguration(appID: discoveryAppID, host: discoveryHost, port: discoveryPort, tls: discoveryTLS, stylePreset: discoveryStylePreset ?? "shopar")
    }
}

private struct DemoStartupConfiguration: Equatable {
    let appID: String
    let host: String
    let port: String
    let tls: Bool
    let stylePreset: String

    var parsedPort: Int? { Int(port).flatMap { (1...65535).contains($0) ? $0 : nil } }
    var isValid: Bool {
        appID.range(of: #"^app_[0-9A-HJKMNP-TV-Z]{26}$"#, options: .regularExpression) != nil
            && !host.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            && !host.contains("/")
            && parsedPort != nil
    }
}

private struct DemoConfigurationProfile: Codable, Identifiable, Equatable {
    var id: String { name }
    var name: String
    var configuration: DemoConfiguration
}

@MainActor
private final class DemoSettingsStore: ObservableObject {
    @Published var configuration: DemoConfiguration { didSet { persist() } }
    @Published private(set) var profiles: [DemoConfigurationProfile]
    @Published var activeProfileName = ""

    private let defaults = UserDefaults.standard
    private let configurationKey = "shopar.sdk.demo.configuration.v1"
    private let profilesKey = "shopar.sdk.demo.profiles.v1"
    private let activeProfileKey = "shopar.sdk.demo.active-profile.v1"

    init() {
        if let data = UserDefaults.standard.data(forKey: "shopar.sdk.demo.configuration.v1"),
           let decoded = try? JSONDecoder().decode(DemoConfiguration.self, from: data) {
            configuration = decoded
        } else {
            configuration = DemoConfiguration()
        }
        if let data = UserDefaults.standard.data(forKey: "shopar.sdk.demo.profiles.v1"),
           let decoded = try? JSONDecoder().decode([DemoConfigurationProfile].self, from: data) {
            profiles = decoded
        } else {
            profiles = []
        }
        activeProfileName = UserDefaults.standard.string(forKey: "shopar.sdk.demo.active-profile.v1") ?? ""
    }

    func saveProfile(named rawName: String) {
        let name = String(rawName.trimmingCharacters(in: .whitespacesAndNewlines).prefix(40))
        guard !name.isEmpty else { return }
        let profile = DemoConfigurationProfile(name: name, configuration: configuration)
        profiles.removeAll { $0.name == name }
        profiles.append(profile)
        profiles.sort { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
        activeProfileName = name
        persistProfiles()
    }

    func loadProfile(named name: String) {
        guard let profile = profiles.first(where: { $0.name == name }) else { return }
        configuration = profile.configuration
        activeProfileName = profile.name
        defaults.set(profile.name, forKey: activeProfileKey)
    }

    private func persist() {
        guard let data = try? JSONEncoder().encode(configuration) else { return }
        defaults.set(data, forKey: configurationKey)
    }

    private func persistProfiles() {
        guard let data = try? JSONEncoder().encode(profiles) else { return }
        defaults.set(data, forKey: profilesKey)
        defaults.set(activeProfileName, forKey: activeProfileKey)
    }
}

@MainActor
private final class DemoSDKCoordinator: ObservableObject {
    @Published private(set) var activeAgency: DemoStartupConfiguration?
    @Published private(set) var activeDiscovery: DemoStartupConfiguration?

    func startAgency(_ config: DemoStartupConfiguration?, style: AgencyStyle) -> String? {
        guard let config, config.isValid, let port = config.parsedPort else {
            return "Enter a valid Agency app ID, host, and port in Settings."
        }
        if let activeAgency {
            return activeAgency == config ? nil : "Agency app ID, endpoint, or style changed. Relaunch the demo to apply it."
        }
        do {
            try ShoparAgency.configure(AgencyConfiguration(appID: config.appID, grpcHost: config.host, grpcPort: port, useTLS: config.tls, style: style))
            activeAgency = config
            return nil
        } catch {
            return "Agency could not be configured: \(error.localizedDescription)"
        }
    }

    func startDiscovery(_ config: DemoStartupConfiguration?, collectMetadata: Bool, style: DiscoveryStyle) -> String? {
        guard let config, config.isValid, let port = config.parsedPort else {
            return "Enter a valid Discovery app ID, host, and port in Settings."
        }
        if let activeDiscovery {
            guard activeDiscovery == config else {
                return "Discovery app ID, endpoint, or style changed. Relaunch the demo to apply it."
            }
            do {
                try ShoparDiscovery.setOptionalDeviceMetadataCollectionEnabled(collectMetadata)
                return nil
            } catch {
                return "Discovery preference could not be updated: \(error.localizedDescription)"
            }
        }
        do {
            try ShoparDiscovery.configure(DiscoveryConfiguration(
                appID: config.appID,
                grpcHost: config.host,
                grpcPort: port,
                useTLS: config.tls,
                collectDeviceMetadata: collectMetadata,
                style: style
            ))
            activeDiscovery = config
            return nil
        } catch {
            return "Discovery could not be configured: \(error.localizedDescription)"
        }
    }

    func resetDiscoveryInstallationIdentity() throws {
        try ShoparDiscovery.resetInstallationIdentity()
    }
}

private enum DemoTab: String, CaseIterable, Identifiable {
    case discovery = "Discovery"
    case agency = "Agency"
    case settings = "Settings"
    var id: String { rawValue }
    var symbol: String {
        switch self {
        case .discovery: "rectangle.stack"
        case .agency: "person.crop.rectangle"
        case .settings: "slider.horizontal.3"
        }
    }
}

@main
@MainActor
struct ShoparSDKDemoApp: App {
    @StateObject private var settings = DemoSettingsStore()
    @StateObject private var sdk = DemoSDKCoordinator()
    @State private var selectedTab: DemoTab = .settings

    var body: some Scene {
        WindowGroup {
            TabView(selection: $selectedTab) {
                DiscoveryDemoTab(settings: settings, sdk: sdk)
                    .tabItem { Label(DemoTab.discovery.rawValue, systemImage: DemoTab.discovery.symbol) }
                    .tag(DemoTab.discovery)
                AgencyDemoTab(settings: settings, sdk: sdk)
                    .tabItem { Label(DemoTab.agency.rawValue, systemImage: DemoTab.agency.symbol) }
                    .tag(DemoTab.agency)
                DemoSettingsTab(settings: settings, sdk: sdk)
                    .tabItem { Label(DemoTab.settings.rawValue, systemImage: DemoTab.settings.symbol) }
                    .tag(DemoTab.settings)
            }
            .tint(Color(red: 24 / 255, green: 34 / 255, blue: 29 / 255))
            .onAppear {
                if settings.configuration.discoveryStartup?.isValid == true,
                   settings.configuration.placementIDs.contains(settings.configuration.selectedPlacementID) {
                    selectedTab = .discovery
                }
            }
        }
    }
}

@MainActor
private struct AgencyDemoTab: View {
    @ObservedObject var settings: DemoSettingsStore
    @ObservedObject var sdk: DemoSDKCoordinator
    @State private var errorMessage: String?
    @State private var showingHostSecurityInfo = false

    var body: some View {
        Group {
            if let errorMessage {
                DemoConfigurationRequired(title: "Agency SDK", message: errorMessage)
            } else {
                AgencyLinksView(onOpenAccountSecurity: { showingHostSecurityInfo = true })
            }
        }
        .task(id: settings.configuration.agencyStartup) {
            errorMessage = sdk.startAgency(settings.configuration.agencyStartup, style: settings.configuration.agencyStyle)
        }
        .alert("Account security belongs to the host app", isPresented: $showingHostSecurityInfo) {
            Button("OK", role: .cancel) { }
        } message: {
            Text("The Agency SDK manages sessions from this demo app. Account-wide Shopar session management belongs to a Shopar host screen.")
        }
    }
}

@MainActor
private struct DiscoveryDemoTab: View {
    @ObservedObject var settings: DemoSettingsStore
    @ObservedObject var sdk: DemoSDKCoordinator
    @State private var errorMessage: String?

    private var configuration: DemoConfiguration { settings.configuration }
    private var placementIDs: [String] { configuration.placementIDs }
    private var selectedPlacementID: String? {
        placementIDs.contains(configuration.selectedPlacementID) ? configuration.selectedPlacementID : placementIDs.first
    }

    var body: some View {
        Group {
            if let errorMessage {
                DemoConfigurationRequired(title: "Discovery SDK", message: errorMessage)
            } else if placementIDs.isEmpty {
                DemoConfigurationRequired(title: "No placement configured", message: "Add a real App Studio placement ID in Settings.")
            } else if let selectedPlacementID {
                VStack(spacing: 0) {
                    HStack {
                        Text("Placement")
                            .font(.headline)
                        Spacer()
                        Picker("Placement", selection: Binding(
                            get: { selectedPlacementID },
                            set: { settings.configuration.selectedPlacementID = $0 }
                        )) {
                            ForEach(placementIDs, id: \.self) { id in Text(id).tag(id) }
                        }
                        .pickerStyle(.menu)
                    }
                    .padding()
                    Divider()
                    ShoparDiscovery.Placement(placementID: selectedPlacementID)
                }
            } else {
                DemoConfigurationRequired(title: "Discovery SDK", message: "Choose a placement in Settings.")
            }
        }
        .task(id: settings.configuration.discoveryStartup) {
            errorMessage = sdk.startDiscovery(
                settings.configuration.discoveryStartup,
                collectMetadata: settings.configuration.collectDeviceMetadata,
                style: settings.configuration.discoveryStyle
            )
        }
        .onChange(of: settings.configuration.collectDeviceMetadata) { value in
            if sdk.activeDiscovery == settings.configuration.discoveryStartup {
                errorMessage = sdk.startDiscovery(settings.configuration.discoveryStartup, collectMetadata: value, style: settings.configuration.discoveryStyle)
            }
        }
    }
}

@MainActor
private struct DemoSettingsTab: View {
    @ObservedObject var settings: DemoSettingsStore
    @ObservedObject var sdk: DemoSDKCoordinator
    @State private var newProfileName = ""
    @State private var showingResetConfirmation = false
    @State private var resetMessage: String?

    private var config: Binding<DemoConfiguration> { $settings.configuration }
    private var agencyNeedsRestart: Bool { sdk.activeAgency.map { $0 != settings.configuration.agencyStartup } ?? false }
    private var discoveryNeedsRestart: Bool { sdk.activeDiscovery.map { $0 != settings.configuration.discoveryStartup } ?? false }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("Agency app_id", text: config.agencyAppID)
                        .textInputAutocapitalization(.never).autocorrectionDisabled()
                    endpointFields(host: config.agencyHost, port: config.agencyPort, tls: config.agencyTLS)
                    Picker("SDK appearance", selection: Binding(
                        get: { settings.configuration.agencyStylePreset ?? "shopar" },
                        set: { config.agencyStylePreset.wrappedValue = $0 }
                    )) {
                        Text("Shopar").tag("shopar")
                        Text("Midnight").tag("midnight")
                    }
                    startupStatus(active: sdk.activeAgency, pending: agencyNeedsRestart)
                } header: {
                    Text("Agency SDK")
                } footer: {
                    Text("Agency app IDs are created in Agency Dashboard. Creator sessions and credentials remain SDK-managed.")
                }

                Section {
                    TextField("Discovery app_id", text: config.discoveryAppID)
                        .textInputAutocapitalization(.never).autocorrectionDisabled()
                    endpointFields(host: config.discoveryHost, port: config.discoveryPort, tls: config.discoveryTLS)
                    Picker("SDK appearance", selection: Binding(
                        get: { settings.configuration.discoveryStylePreset ?? "shopar" },
                        set: { config.discoveryStylePreset.wrappedValue = $0 }
                    )) {
                        Text("Shopar").tag("shopar")
                        Text("Midnight").tag("midnight")
                    }
                    Toggle("Collect optional device metadata", isOn: config.collectDeviceMetadata)
                    TextField("Placement IDs (comma or newline separated)", text: config.placementIDsText, axis: .vertical)
                        .lineLimit(2...5).textInputAutocapitalization(.never).autocorrectionDisabled()
                    if !settings.configuration.placementIDs.isEmpty {
                        Picker("Active placement", selection: Binding(
                            get: {
                                settings.configuration.placementIDs.contains(settings.configuration.selectedPlacementID)
                                    ? settings.configuration.selectedPlacementID
                                    : (settings.configuration.placementIDs.first ?? "")
                            },
                            set: { config.selectedPlacementID.wrappedValue = $0 }
                        )) {
                            ForEach(settings.configuration.placementIDs, id: \.self) { id in Text(id).tag(id) }
                        }
                    }
                    startupStatus(active: sdk.activeDiscovery, pending: discoveryNeedsRestart)
                    Button("Reset Discovery installation ID", role: .destructive) { showingResetConfirmation = true }
                        .disabled(sdk.activeDiscovery == nil)
                } header: {
                    Text("Discovery SDK")
                } footer: {
                    Text("Create placements in App Studio. Format, product selection, offer, price, and attribution are read-only server decisions.")
                }

                Section {
                    TextField("Profile name", text: $newProfileName)
                    Button("Save current settings") {
                        settings.saveProfile(named: newProfileName)
                        newProfileName = ""
                    }
                    .disabled(newProfileName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                    if !settings.profiles.isEmpty {
                        Picker("Load profile", selection: Binding(
                            get: { settings.activeProfileName },
                            set: { settings.loadProfile(named: $0) }
                        )) {
                            Text("Choose a profile").tag("")
                            ForEach(settings.profiles) { profile in Text(profile.name).tag(profile.name) }
                        }
                    }
                    Text("Profiles contain only non-secret settings. Missing IDs stay inactive; the demo never uses placeholder IDs for live requests.")
                        .font(.footnote).foregroundStyle(.secondary)
                } header: {
                    Text("Local profiles")
                }

                Section {
                    Text("Each SDK initializes once per process. Changes to an initialized module's app ID, endpoint, or appearance are pending until the demo is relaunched. Placement selection and optional metadata preference update live.")
                    Text("Discovery sends no Agency PWT. The two modules do not share identity, session, or owner state.")
                } header: {
                    Text("Runtime behavior")
                }

                Section {
                    Text("Demo build: \(Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "unknown")")
                    Text("Shopar iOS SDK source: \(Bundle.main.infoDictionary?["ShoparSDKSourceRevision"] as? String ?? "unknown")")
                    Text("Device OS: iOS \(UIDevice.current.systemVersion)")
                    Text("Agency app: \(settings.configuration.agencyAppID.isEmpty ? "not configured" : settings.configuration.agencyAppID)")
                    Text("Discovery app: \(settings.configuration.discoveryAppID.isEmpty ? "not configured" : settings.configuration.discoveryAppID)")
                    Text("Selected placement: \(settings.configuration.selectedPlacementID.isEmpty ? "not configured" : settings.configuration.selectedPlacementID)")
                } header: {
                    Text("Demo diagnostics")
                }

                if let resetMessage {
                    Section { Text(resetMessage).font(.footnote) }
                }
            }
            .navigationTitle("Settings")
            .confirmationDialog("Reset Discovery installation identity?", isPresented: $showingResetConfirmation, titleVisibility: .visible) {
                Button("Reset installation ID", role: .destructive) {
                    do {
                        try sdk.resetDiscoveryInstallationIdentity()
                        resetMessage = "Discovery installation identity reset. Historical server events remain unchanged."
                    } catch {
                        resetMessage = "Reset failed: \(error.localizedDescription)"
                    }
                }
                Button("Cancel", role: .cancel) { }
            } message: {
                Text("Only do this to honor an explicit test-user request. This cannot be undone.")
            }
        }
    }

    @ViewBuilder
    private func endpointFields(host: Binding<String>, port: Binding<String>, tls: Binding<Bool>) -> some View {
        TextField("gRPC host", text: host).textInputAutocapitalization(.never).autocorrectionDisabled()
        TextField("gRPC port", text: port).keyboardType(.numberPad)
        Toggle("Use TLS", isOn: tls)
    }

    @ViewBuilder
    private func startupStatus(active: DemoStartupConfiguration?, pending: Bool) -> some View {
        if pending {
            Label("Pending: save and relaunch the demo to apply this SDK's app ID or endpoint.", systemImage: "arrow.clockwise.circle")
                .font(.footnote).foregroundStyle(.orange)
        } else if active != nil {
            Label("SDK initialized for the current settings.", systemImage: "checkmark.circle")
                .font(.footnote).foregroundStyle(.secondary)
        }
    }
}

private struct DemoConfigurationRequired: View {
    let title: String
    let message: String

    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: "slider.horizontal.3").font(.largeTitle).foregroundStyle(.secondary)
            Text(title).font(.title2.weight(.semibold))
            Text(message).multilineTextAlignment(.center).foregroundStyle(.secondary)
        }
        .padding(28)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
