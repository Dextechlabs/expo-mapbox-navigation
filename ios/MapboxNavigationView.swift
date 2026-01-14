import ExpoModulesCore
import UIKit
import CoreLocation
import Combine
import MapboxNavigationCore
import MapboxMaps
import MapboxNavigationUIKit
import MapboxDirections

class ExpoMapboxNavigationView: ExpoView {
    private let onRouteProgressChanged = EventDispatcher()
    private let onCancelNavigation = EventDispatcher()
    private let onWaypointArrival = EventDispatcher()
    private let onFinalDestinationArrival = EventDispatcher()
    private let onRouteChanged = EventDispatcher()
    private let onUserOffRoute = EventDispatcher()

    let controller = ExpoMapboxNavigationViewController()

    required init(appContext: AppContext? = nil) {
        super.init(appContext: appContext)
        clipsToBounds = true
        addSubview(controller.view)

        controller.onRouteProgressChanged = onRouteProgressChanged
        controller.onCancelNavigation = onCancelNavigation
        controller.onWaypointArrival = onWaypointArrival
        controller.onFinalDestinationArrival = onFinalDestinationArrival
        controller.onRouteChanged = onRouteChanged
        controller.onUserOffRoute = onUserOffRoute
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        controller.view.frame = bounds
    }
    
    // MARK: - Property setters called from React Native
    
    func setMute(_ mute: Bool) {
        controller.setMute(mute)
    }
    
    func setSeparateLegs(_ separateLegs: Bool) {
        controller.setSeparateLegs(separateLegs)
    }
    
    func setDistanceUnit(_ distanceUnit: String) {
        controller.setDistanceUnit(distanceUnit)
    }
    
    func applyStartOriginDict(_ dict: [String: Double]) {
        guard let latitude = dict["latitude"], let longitude = dict["longitude"] else {
            print("Invalid start origin coordinates")
            return
        }
        let coordinate = CLLocationCoordinate2D(latitude: latitude, longitude: longitude)
        controller.setStartOrigin(coordinate)
    }
    
    func applyWaypointsArray(_ waypoints: [[String: Any]]) {
        var coordinates: [CLLocationCoordinate2D] = []
        var waypointIndices: [Int] = []
        
        for (index, waypointDict) in waypoints.enumerated() {
            if let latitude = waypointDict["latitude"] as? Double,
               let longitude = waypointDict["longitude"] as? Double {
                coordinates.append(CLLocationCoordinate2D(latitude: latitude, longitude: longitude))
                
                if let separatesLegs = waypointDict["separatesLegs"] as? Bool, separatesLegs {
                    waypointIndices.append(index)
                }
            }
        }
        
        controller.setCoordinates(coordinates)
        controller.setWaypointIndices(waypointIndices)
    }
    
    func setDestinationTitle(_ destinationTitle: String?) {
        controller.setDestinationTitle(destinationTitle)
    }
    
    func applyDestinationDict(_ dict: [String: Double]) {
        guard let latitude = dict["latitude"], let longitude = dict["longitude"] else {
            print("Invalid destination coordinates")
            return
        }
        let coordinate = CLLocationCoordinate2D(latitude: latitude, longitude: longitude)
        controller.setDestination(coordinate)
    }
    
    func setLanguage(_ language: String) {
        controller.setLanguage(language)
    }
    
    func setShowCancelButton(_ showCancelButton: Bool) {
        controller.setShowCancelButton(showCancelButton)
    }
    
    func setShouldSimulateRoute(_ shouldSimulateRoute: Bool) {
        controller.setShouldSimulateRoute(shouldSimulateRoute)
    }
    
    func setShowsEndOfRouteFeedback(_ showsEndOfRouteFeedback: Bool) {
        controller.setShowsEndOfRouteFeedback(showsEndOfRouteFeedback)
    }
    
    func setHideStatusView(_ hideStatusView: Bool) {
        controller.setHideStatusView(hideStatusView)
    }
    
    func setTravelMode(_ travelMode: String) {
        controller.setTravelMode(travelMode)
    }
}

class ExpoMapboxNavigationViewController: UIViewController {
    // MARK: - Mapbox Navigation Core Components
    static let navigationProvider: MapboxNavigationProvider = MapboxNavigationProvider(coreConfig: .init(locationSource: .live))
    var mapboxNavigation: MapboxNavigation? = nil
    var routingProvider: RoutingProvider? = nil
    var navigation: NavigationController? = nil
    var tripSession: SessionController? = nil
    
    // MARK: - Navigation State
    var currentCoordinates: [CLLocationCoordinate2D]? = nil
    var currentWaypointIndices: [Int]? = nil
    var currentLocale: Locale = Locale.current
    var currentRouteProfile: String? = nil
    var currentRouteExcludeList: [String]? = nil
    var currentMapStyle: String? = nil
    var isUsingRouteMatchingApi: Bool = false
    var startOrigin: CLLocationCoordinate2D? = nil
    var destination: CLLocationCoordinate2D? = nil
    var destinationTitle: String? = nil
    var isMuted: Bool = false
    var separateLegs: Bool = true
    var distanceUnit: String = "metric"
    var language: String = "en"
    var showCancelButton: Bool = true
    var shouldSimulateRoute: Bool = false
    var showsEndOfRouteFeedback: Bool = true
    var hideStatusView: Bool = false
    var travelMode: String = "driving"
    
    // MARK: - Event Dispatchers
    var onRouteProgressChanged: EventDispatcher? = nil
    var onCancelNavigation: EventDispatcher? = nil
    var onWaypointArrival: EventDispatcher? = nil
    var onFinalDestinationArrival: EventDispatcher? = nil
    var onRouteChanged: EventDispatcher? = nil
    var onUserOffRoute: EventDispatcher? = nil
    
    // MARK: - Combine Subscriptions
    var calculateRoutesTask: Task<Void, Error>? = nil
    private var routeProgressCancellable: AnyCancellable? = nil
    private var waypointArrivalCancellable: AnyCancellable? = nil
    private var reroutingCancellable: AnyCancellable? = nil
    private var sessionCancellable: AnyCancellable? = nil
    
    // MARK: - Navigation View Controller
    private var navigationViewController: NavigationViewController? = nil
    
    init() {
        super.init(nibName: nil, bundle: nil)
        setupMapboxNavigation()
    }
    
    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        fatalError("This controller should not be loaded through a story board")
    }
    
    deinit {
        routeProgressCancellable?.cancel()
        waypointArrivalCancellable?.cancel()
        reroutingCancellable?.cancel()
        sessionCancellable?.cancel()
        calculateRoutesTask?.cancel()
    }
    
    // MARK: - Setup
    private func setupMapboxNavigation() {
        mapboxNavigation = ExpoMapboxNavigationViewController.navigationProvider.mapboxNavigation
        routingProvider = mapboxNavigation?.routingProvider()
        navigation = mapboxNavigation?.navigation()
        tripSession = mapboxNavigation?.tripSession()
        
        setupEventSubscriptions()
    }
    
    private func setupEventSubscriptions() {
        guard let navigation = navigation, let tripSession = tripSession else { return }
        
        // Route Progress Events
        routeProgressCancellable = navigation.routeProgress.sink { [weak self] progressState in
            if let progressState = progressState {
                self?.onRouteProgressChanged?([
                    "distanceRemaining": progressState.routeProgress.distanceRemaining,
                    "distanceTraveled": progressState.routeProgress.distanceTraveled,
                    "durationRemaining": progressState.routeProgress.durationRemaining,
                    "fractionTraveled": progressState.routeProgress.fractionTraveled,
                ])
            }
        }
        
        // Waypoint Arrival Events
        waypointArrivalCancellable = navigation.waypointsArrival.sink { [weak self] arrivalStatus in
            let event = arrivalStatus.event
            if event is WaypointArrivalStatus.Events.ToFinalDestination {
                self?.onFinalDestinationArrival?()
            } else if event is WaypointArrivalStatus.Events.ToWaypoint {
                self?.onWaypointArrival?()
            }
        }
        
        // Rerouting Events
        reroutingCancellable = navigation.rerouting.sink { [weak self] rerouteStatus in
            self?.onRouteChanged?()
        }
        
        // Session Events
        sessionCancellable = tripSession.session.sink { [weak self] session in
            let state = session.state
            switch state {
            case .activeGuidance(let activeGuidanceState):
                switch(activeGuidanceState) {
                case .offRoute:
                    self?.onUserOffRoute?()
                default: break
                }
            default: break
            }
        }
    }
    
    // MARK: - Property Setters
    func setMute(_ mute: Bool) {
        isMuted = mute
        // Apply mute to voice controller if available
        if let voiceController = navigationViewController?.voiceController {
            voiceController.volume = mute ? 0.0 : 1.0
        }
    }
    
    func setSeparateLegs(_ separate: Bool) {
        separateLegs = separate
    }
    
    func setDistanceUnit(_ unit: String) {
        distanceUnit = unit
    }
    
    func setStartOrigin(_ origin: CLLocationCoordinate2D) {
        startOrigin = origin
        updateRouteIfNeeded()
    }
    
    func setDestination(_ dest: CLLocationCoordinate2D) {
        destination = dest
        updateRouteIfNeeded()
    }
    
    func setDestinationTitle(_ title: String?) {
        destinationTitle = title
    }
    
    func setLanguage(_ lang: String) {
        language = lang
        currentLocale = Locale(identifier: lang)
    }
    
    func setShowCancelButton(_ show: Bool) {
        showCancelButton = show
        // Update cancel button visibility in navigation view
        navigationViewController?.navigationView.bottomBannerContainerView.isHidden = !show
    }
    
    func setShouldSimulateRoute(_ simulate: Bool) {
        shouldSimulateRoute = simulate
    }
    
    func setShowsEndOfRouteFeedback(_ show: Bool) {
        showsEndOfRouteFeedback = show
    }
    
    func setHideStatusView(_ hide: Bool) {
        hideStatusView = hide
    }
    
    func setTravelMode(_ mode: String) {
        travelMode = mode
        currentRouteProfile = mapTravelModeToProfile(mode)
        updateRouteIfNeeded()
    }
    
    func setCoordinates(_ coordinates: [CLLocationCoordinate2D]) {
        currentCoordinates = coordinates
        updateRouteIfNeeded()
    }
    
    func setWaypointIndices(_ indices: [Int]) {
        currentWaypointIndices = indices
    }
    
    // MARK: - Route Management
    private func updateRouteIfNeeded() {
        guard let startOrigin = startOrigin, let destination = destination else { return }
        
        let coordinates = [startOrigin] + (currentCoordinates ?? []) + [destination]
        setCoordinates(coordinates)
        calculateRoutes()
    }
    
    private func calculateRoutes() {
        calculateRoutesTask?.cancel()
        
        guard let coordinates = currentCoordinates, coordinates.count >= 2 else { return }
        
        let waypoints = coordinates.enumerated().map { index, coordinate in
            var waypoint = Waypoint(coordinate: coordinate)
            waypoint.separatesLegs = currentWaypointIndices?.contains(index) ?? (index > 0 && index < coordinates.count - 1)
            return waypoint
        }
        
        if isUsingRouteMatchingApi {
            calculateMapMatchingRoutes(waypoints: waypoints)
        } else {
            calculateStandardRoutes(waypoints: waypoints)
        }
    }
    
    private func calculateStandardRoutes(waypoints: [Waypoint]) {
        let routeOptions = NavigationRouteOptions(
            waypoints: waypoints,
            profileIdentifier: currentRouteProfile != nil ? ProfileIdentifier(rawValue: currentRouteProfile!) : nil,
            queryItems: [URLQueryItem(name: "exclude", value: currentRouteExcludeList?.joined(separator: ","))],
            locale: currentLocale,
            distanceUnit: currentLocale.usesMetricSystem ? LengthFormatter.Unit.meter : LengthFormatter.Unit.mile
        )
        
        calculateRoutesTask = Task { [weak self] in
            guard let self = self, let routingProvider = self.routingProvider else { return }
            
            switch await routingProvider.calculateRoutes(options: routeOptions).result {
            case .failure(let error):
                print("Route calculation failed: \(error.localizedDescription)")
            case .success(let navigationRoutes):
                await MainActor.run {
                    self.onRoutesCalculated(navigationRoutes: navigationRoutes)
                }
            }
        }
    }
    
    private func calculateMapMatchingRoutes(waypoints: [Waypoint]) {
        let matchOptions = NavigationMatchOptions(
            waypoints: waypoints,
            profileIdentifier: currentRouteProfile != nil ? ProfileIdentifier(rawValue: currentRouteProfile!) : nil,
            queryItems: [URLQueryItem(name: "exclude", value: currentRouteExcludeList?.joined(separator: ","))],
            distanceUnit: currentLocale.usesMetricSystem ? LengthFormatter.Unit.meter : LengthFormatter.Unit.mile
        )
        matchOptions.locale = currentLocale
        
        calculateRoutesTask = Task { [weak self] in
            guard let self = self, let routingProvider = self.routingProvider else { return }
            
            switch await routingProvider.calculateRoutes(options: matchOptions).result {
            case .failure(let error):
                print("Map matching route calculation failed: \(error.localizedDescription)")
            case .success(let navigationRoutes):
                await MainActor.run {
                    self.onRoutesCalculated(navigationRoutes: navigationRoutes)
                }
            }
        }
    }
    
    private func onRoutesCalculated(navigationRoutes: NavigationRoutes) {
        // Remove existing navigation view controller
        navigationViewController?.removeFromParent()
        navigationViewController?.view.removeFromSuperview()
        
        // Create new navigation view controller
        let topBanner = TopBannerViewController()
        topBanner.instructionsBannerView.distanceFormatter.locale = currentLocale
        
        let bottomBanner = BottomBannerViewController()
        bottomBanner.distanceFormatter.locale = currentLocale
        bottomBanner.dateFormatter.locale = currentLocale
        
        let navigationOptions = NavigationOptions(
            mapboxNavigation: self.mapboxNavigation!,
            voiceController: ExpoMapboxNavigationViewController.navigationProvider.routeVoiceController,
            eventsManager: ExpoMapboxNavigationViewController.navigationProvider.eventsManager(),
            topBanner: topBanner,
            bottomBanner: bottomBanner
        )
        
        navigationViewController = NavigationViewController(
            navigationRoutes: navigationRoutes,
            navigationOptions: navigationOptions
        )
        
        guard let navigationViewController = navigationViewController else { return }
        
        // Configure navigation view
        let navigationMapView = navigationViewController.navigationMapView
        navigationMapView?.puckType = .puck2D(.navigationDefault)
        
        // Set map style
        let style = currentMapStyle != nil ? StyleURI(rawValue: currentMapStyle!) : StyleURI.streets
        navigationMapView?.mapView.mapboxMap.loadStyle(style!) { _ in
            navigationMapView?.localizeLabels(locale: self.currentLocale)
            do {
                try navigationMapView?.mapView.mapboxMap.localizeLabels(into: self.currentLocale)
            } catch {
                print("Failed to localize labels: \(error)")
            }
        }
        
        // Configure cancel button
        if showCancelButton {
            let cancelButton = navigationViewController.navigationView.bottomBannerContainerView.findViews(subclassOf: CancelButton.self).first
            cancelButton?.addTarget(self, action: #selector(cancelButtonClicked), for: .touchUpInside)
        }
        
        // Set delegate
        navigationViewController.delegate = self
        
        // Add to view hierarchy
        addChild(navigationViewController)
        view.addSubview(navigationViewController.view)
        navigationViewController.view.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            navigationViewController.view.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            navigationViewController.view.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            navigationViewController.view.topAnchor.constraint(equalTo: view.topAnchor),
            navigationViewController.view.bottomAnchor.constraint(equalTo: view.bottomAnchor),
        ])
        navigationViewController.didMove(toParent: self)
        
        // Start navigation
        mapboxNavigation?.tripSession().startActiveGuidance(with: navigationRoutes, startLegIndex: 0)
    }
    
    @objc private func cancelButtonClicked(_ sender: AnyObject?) {
        onCancelNavigation?()
    }
    
    // MARK: - Helper Methods
    private func mapTravelModeToProfile(_ mode: String) -> String {
        switch mode.lowercased() {
        case "walking":
            return "walking"
        case "cycling", "biking":
            return "cycling"
        case "driving", "car":
            return "driving-traffic"
        default:
            return "driving-traffic"
        }
    }
}

// MARK: - NavigationViewControllerDelegate
extension ExpoMapboxNavigationViewController: NavigationViewControllerDelegate {
    func navigationViewController(_ navigationViewController: NavigationViewController, didRerouteAlong route: Route) {
        // Handle rerouting if needed
    }
    
    func navigationViewControllerDidDismiss(_ navigationViewController: NavigationViewController, byCanceling canceled: Bool) {
        // Handle navigation dismissal
        if canceled {
            onCancelNavigation?()
        }
    }
}

// MARK: - UIView Extension for Finding Subviews
extension UIView {
    func findViews<T: UIView>(subclassOf: T.Type) -> [T] {
        return recursiveSubviews.compactMap { $0 as? T }
    }
    
    var recursiveSubviews: [UIView] {
        return subviews + subviews.flatMap { $0.recursiveSubviews }
    }
}
