package expo.modules.mapboxnavigation

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.mapbox.api.directions.v5.models.Bearing
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.Expected
import com.mapbox.common.location.Location
import com.mapbox.geojson.Point
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.ImageHolder
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.TimeFormat
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.lifecycle.requireMapboxNavigation
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver
import com.mapbox.navigation.tripdata.maneuver.api.MapboxManeuverApi
import com.mapbox.navigation.tripdata.progress.api.MapboxTripProgressApi
import com.mapbox.navigation.tripdata.progress.model.DistanceRemainingFormatter
import com.mapbox.navigation.tripdata.progress.model.EstimatedTimeToArrivalFormatter
import com.mapbox.navigation.tripdata.progress.model.PercentDistanceTraveledFormatter
import com.mapbox.navigation.tripdata.progress.model.TimeRemainingFormatter
import com.mapbox.navigation.tripdata.progress.model.TripProgressUpdateFormatter
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.components.maneuver.view.MapboxManeuverView
import com.mapbox.navigation.ui.components.tripprogress.view.MapboxTripProgressView
import com.mapbox.navigation.ui.maps.NavigationStyles
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.camera.lifecycle.NavigationBasicGesturesHandler
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
import com.mapbox.navigation.ui.maps.camera.transition.NavigationCameraTransitionOptions
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineApiOptions
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineViewOptions
import com.mapbox.navigation.voice.api.MapboxSpeechApi
import com.mapbox.navigation.voice.api.MapboxVoiceInstructionsPlayer
import com.mapbox.navigation.voice.model.SpeechAnnouncement
import com.mapbox.navigation.voice.model.SpeechError
import com.mapbox.navigation.voice.model.SpeechValue
import com.mapbox.navigation.voice.model.SpeechVolume
import expo.modules.kotlin.AppContext
import expo.modules.kotlin.viewevent.EventDispatcher
import expo.modules.kotlin.views.ExpoView
import expo.modules.mapboxnavigation.databinding.ActivityMainBinding
import java.util.Locale
import com.mapbox.navigation.core.lifecycle.requireMapboxNavigation

data class NativeCoordinate(
    val latitude: Double,
    val longitude: Double
)

data class Waypoint(
    val latitude: Double,
    val longitude: Double,
    val name: String? = null,
    val separatesLegs: Boolean? = false
)

@SuppressLint("ViewConstructor")
class MapboxNavigationView(context: Context, appContext: AppContext) : ExpoView(context, appContext), DefaultLifecycleObserver {

    private companion object {
        private const val BUTTON_ANIMATION_DURATION = 1500L
    }

    // Event dispatchers for React Native events
    private val onRouteProgressChanged by EventDispatcher()
    private val onNavigationReady by EventDispatcher()
    private val onNavigationCanceled by EventDispatcher()
    private val onNavigationFinished by EventDispatcher()
    private val onNavigationError by EventDispatcher()

    // Props from React Native
    private var isMuted: Boolean = false
    private var separateLegs: Boolean = false
    private var distanceUnit: String = "metric"
    private var startOrigin: NativeCoordinate? = null
    private var waypoints: List<Waypoint>? = null
    private var destinationTitle: String? = null
    private var destination: NativeCoordinate? = null
    private var language: String = "en"
    private var showCancelButton: Boolean = true
    private var shouldSimulateRoute: Boolean = false
    private var showsEndOfRouteFeedback: Boolean = true
    private var hideStatusView: Boolean = false
    private var travelMode: String = "driving"

    // Navigation components
    private lateinit var binding: ActivityMainBinding
    private lateinit var navigationCamera: NavigationCamera
    private lateinit var viewportDataSource: MapboxNavigationViewportDataSource
    private lateinit var maneuverApi: MapboxManeuverApi
    private lateinit var tripProgressApi: MapboxTripProgressApi
    private lateinit var routeLineApi: MapboxRouteLineApi
    private lateinit var routeLineView: MapboxRouteLineView
    private val routeArrowApi: MapboxRouteArrowApi = MapboxRouteArrowApi()
    private lateinit var routeArrowView: MapboxRouteArrowView
    private lateinit var speechApi: MapboxSpeechApi
    private lateinit var voiceInstructionsPlayer: MapboxVoiceInstructionsPlayer
    private val navigationLocationProvider = NavigationLocationProvider()

    private var isNavigationActive = false
    private var mapboxNavigation: MapboxNavigation? = null

    // Camera padding values
    private val pixelDensity = Resources.getSystem().displayMetrics.density
    private val overviewPadding: EdgeInsets by lazy {
        EdgeInsets(
            200.0 * pixelDensity,
            40.0 * pixelDensity,
            120.0 * pixelDensity,
            40.0 * pixelDensity
        )
    }
    private val landscapeOverviewPadding: EdgeInsets by lazy {
        EdgeInsets(
            30.0 * pixelDensity,
            380.0 * pixelDensity,
            110.0 * pixelDensity,
            20.0 * pixelDensity
        )
    }
    private val followingPadding: EdgeInsets by lazy {
        EdgeInsets(
            240.0 * pixelDensity,
            40.0 * pixelDensity,
            150.0 * pixelDensity,
            40.0 * pixelDensity
        )
    }
    private val landscapeFollowingPadding: EdgeInsets by lazy {
        EdgeInsets(
            30.0 * pixelDensity,
            380.0 * pixelDensity,
            110.0 * pixelDensity,
            40.0 * pixelDensity
        )
    }

    init {
        setupView()
    }

    private fun setupView() {
        // Inflate the layout
        binding = ActivityMainBinding.inflate(LayoutInflater.from(context), this, true)
println("destination per${hasLocationPermission()}")
        // Check permissions first
        if (hasLocationPermission()) {
            initializeNavigation()
        } else {
            onNavigationError(mapOf("error" to "Location permission not granted"))
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    // 5. FIX: Improve initialization flow
    private fun initializeNavigation() {
        try {
            initializeNavigationComponents()
            setupUIInteractions()
            loadMapStyle()
            // Remove initNavigation() call here since it's handled in setupMapboxNavigation()
        } catch (e: Exception) {
            println("Error initializing navigation: ${e.message}")
            onNavigationError(mapOf("error" to (e.message ?: "Unknown error")))
        }
    }

    private fun initializeNavigationComponents() {
        // Initialize Navigation Camera
        viewportDataSource = MapboxNavigationViewportDataSource(binding.mapView.mapboxMap)
        navigationCamera = NavigationCamera(
            binding.mapView.mapboxMap,
            binding.mapView.camera,
            viewportDataSource
        )

        binding.mapView.camera.addCameraAnimationsLifecycleListener(
            NavigationBasicGesturesHandler(navigationCamera)
        )

        navigationCamera.registerNavigationCameraStateChangeObserver { navigationCameraState ->
            when (navigationCameraState) {
                NavigationCameraState.TRANSITION_TO_FOLLOWING,
                NavigationCameraState.FOLLOWING -> binding.recenter.visibility = View.INVISIBLE
                NavigationCameraState.TRANSITION_TO_OVERVIEW,
                NavigationCameraState.OVERVIEW,
                NavigationCameraState.IDLE -> binding.recenter.visibility = View.VISIBLE
            }
        }

        // Set padding values based on orientation
        if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            viewportDataSource.overviewPadding = landscapeOverviewPadding
            viewportDataSource.followingPadding = landscapeFollowingPadding
        } else {
            viewportDataSource.overviewPadding = overviewPadding
            viewportDataSource.followingPadding = followingPadding
        }

        // Initialize APIs
        val distanceFormatterOptions = DistanceFormatterOptions.Builder(context)
            .unitType(if (distanceUnit == "imperial")
                com.mapbox.navigation.base.formatter.UnitType.IMPERIAL
                else com.mapbox.navigation.base.formatter.UnitType.METRIC)
            .build()

        maneuverApi = MapboxManeuverApi(
            MapboxDistanceFormatter(distanceFormatterOptions)
        )

        tripProgressApi = MapboxTripProgressApi(
            TripProgressUpdateFormatter.Builder(context)
                .distanceRemainingFormatter(
                    DistanceRemainingFormatter(distanceFormatterOptions)
                )
                .timeRemainingFormatter(
                    TimeRemainingFormatter(context)
                )
                .percentRouteTraveledFormatter(
                    PercentDistanceTraveledFormatter()
                )
                .estimatedTimeToArrivalFormatter(
                    EstimatedTimeToArrivalFormatter(context, TimeFormat.NONE_SPECIFIED)
                )
                .build()
        )

        val locale = Locale.forLanguageTag(language)
        speechApi = MapboxSpeechApi(context, locale.language)
        voiceInstructionsPlayer = MapboxVoiceInstructionsPlayer(context, locale.language)

        val mapboxRouteLineViewOptions = MapboxRouteLineViewOptions.Builder(context)
            .routeLineBelowLayerId("road-label-navigation")
            .build()

        routeLineApi = MapboxRouteLineApi(MapboxRouteLineApiOptions.Builder().build())
        routeLineView = MapboxRouteLineView(mapboxRouteLineViewOptions)

        val routeArrowOptions = RouteArrowOptions.Builder(context).build()
        routeArrowView = MapboxRouteArrowView(routeArrowOptions)
    }

    private fun setupUIInteractions() {
        binding.stop.setOnClickListener {
            clearRouteAndStopNavigation()
        }

        binding.recenter.setOnClickListener {
            navigationCamera.requestNavigationCameraToFollowing()
            binding.routeOverview.showTextAndExtend(BUTTON_ANIMATION_DURATION)
        }

        binding.routeOverview.setOnClickListener {
            navigationCamera.requestNavigationCameraToOverview()
            binding.recenter.showTextAndExtend(BUTTON_ANIMATION_DURATION)
        }

        binding.soundButton.setOnClickListener {
            toggleMute()
        }

        // Hide cancel button if not needed
        if (!showCancelButton) {
            binding.stop.visibility = View.GONE
        }

        // Hide status view if requested
        if (hideStatusView) {
            binding.tripProgressCard.visibility = View.GONE
        }

        // Set initial mute state
        if (isMuted) {
            binding.soundButton.mute()
            voiceInstructionsPlayer.volume(SpeechVolume(0f))
        } else {
            binding.soundButton.unmute()
            voiceInstructionsPlayer.volume(SpeechVolume(1f))
        }
    }

    // 2. FIX: Update loadMapStyle to trigger route finding when ready
    private fun loadMapStyle() {
        binding.mapView.mapboxMap.loadStyle(NavigationStyles.NAVIGATION_DAY_STYLE) { style ->
            println("Map style loaded, initializing route line layers")

            // FIX: Ensure route line layers are properly initialized
            try {
                routeLineView.initializeLayers(style)
                println("Route line layers initialized successfully")
            } catch (e: Exception) {
                println("Error initializing route line layers: ${e.message}")
                onNavigationError(mapOf("error" to "Failed to initialize route layers: ${e.message}"))
                return@loadStyle
            }

            // Find route if destination is already set
            destination?.let { dest ->
                println("Map ready, finding route to destination")
                findRoute(Point.fromLngLat(dest.longitude, dest.latitude))
            }

            onNavigationReady(mapOf("ready" to true))
        }
    }


    private fun initNavigation() {
        mapboxNavigation = MapboxNavigationProvider.create(
            NavigationOptions.Builder(context)
                .build()
        )

        // Initialize location puck
        binding.mapView.location.apply {
            setLocationProvider(navigationLocationProvider)
            this.locationPuck = LocationPuck2D(
                bearingImage = ImageHolder.Companion.from(
                    com.mapbox.navigation.R.drawable.mapbox_navigation_puck_icon
                )
            )
            puckBearingEnabled = true
            enabled = true
        }
    }

    // Voice instructions observer
    private val voiceInstructionsObserver = VoiceInstructionsObserver { voiceInstructions ->
        speechApi.generate(voiceInstructions, speechCallback)
    }

    // Speech callback
    private val speechCallback =
        MapboxNavigationConsumer<Expected<SpeechError, SpeechValue>> { expected ->
            expected.fold(
                { error ->
                    voiceInstructionsPlayer.play(
                        error.fallback,
                        voiceInstructionsPlayerCallback
                    )
                },
                { value ->
                    voiceInstructionsPlayer.play(
                        value.announcement,
                        voiceInstructionsPlayerCallback
                    )
                }
            )
        }

    private val voiceInstructionsPlayerCallback =
        MapboxNavigationConsumer<SpeechAnnouncement> { value ->
            speechApi.clean(value)
        }

    // Location observer
    private val locationObserver = object : LocationObserver {
        var firstLocationUpdateReceived = false

        override fun onNewRawLocation(rawLocation: Location) {
            // not handled
        }

        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            val enhancedLocation = locationMatcherResult.enhancedLocation
            navigationLocationProvider.changePosition(
                location = enhancedLocation,
                keyPoints = locationMatcherResult.keyPoints,
            )

            viewportDataSource.onLocationChanged(enhancedLocation)
            viewportDataSource.evaluate()

            if (!firstLocationUpdateReceived) {
                firstLocationUpdateReceived = true
                navigationCamera.requestNavigationCameraToOverview(
                    stateTransitionOptions = NavigationCameraTransitionOptions.Builder()
                        .maxDuration(0)
                        .build()
                )
            }
        }
    }

    // Route progress observer
    private val routeProgressObserver = RouteProgressObserver { routeProgress ->
        viewportDataSource.onRouteProgressChanged(routeProgress)
        viewportDataSource.evaluate()

        val style = binding.mapView.mapboxMap.style
        if (style != null) {
            val maneuverArrowResult = routeArrowApi.addUpcomingManeuverArrow(routeProgress)
            routeArrowView.renderManeuverUpdate(style, maneuverArrowResult)
        }

        val maneuvers = maneuverApi.getManeuvers(routeProgress)
        maneuvers.fold(
            { error ->
                Toast.makeText(context, error.errorMessage, Toast.LENGTH_SHORT).show()
            },
            {
                binding.maneuverView.visibility = View.VISIBLE
                binding.maneuverView.renderManeuvers(maneuvers)
            }
        )

        binding.tripProgressView.render(
            tripProgressApi.getTripProgress(routeProgress)
        )

        // Send progress to React Native
        onRouteProgressChanged(mapOf(
            "distanceRemaining" to routeProgress.distanceRemaining,
            "durationRemaining" to routeProgress.durationRemaining,
            "distanceTraveled" to routeProgress.distanceTraveled,
            "fractionTraveled" to routeProgress.fractionTraveled
        ))
    }

    // Routes observer
    private val routesObserver = RoutesObserver { routeUpdateResult ->
        println("Routes observer triggered with ${routeUpdateResult.navigationRoutes.size} routes")

        if (routeUpdateResult.navigationRoutes.isNotEmpty()) {
            println("Setting route line on map")
            routeLineApi.setNavigationRoutes(
                routeUpdateResult.navigationRoutes
            ) { value ->
                binding.mapView.mapboxMap.style?.apply {
                    println("Rendering route line")
                    routeLineView.renderRouteDrawData(this, value)
                } ?: println("Map style not available for route rendering")
            }

            viewportDataSource.onRouteChanged(routeUpdateResult.navigationRoutes.first())
            viewportDataSource.evaluate()
        } else {
            println("Clearing route lines")
            val style = binding.mapView.mapboxMap.style
            if (style != null) {
                routeLineApi.clearRouteLine { value ->
                    routeLineView.renderClearRouteLineValue(style, value)
                }
                routeArrowView.render(style, routeArrowApi.clearArrows())
            }

            viewportDataSource.clearRouteData()
            viewportDataSource.evaluate()
        }
    }

    // 3. FIX: Ensure MapboxNavigation is properly initialized before use
    @SuppressLint("MissingPermission")
    private fun findRoute(destination: Point) {
        // FIX: Check if mapboxNavigation is available
        val navigation = mapboxNavigation
        if (navigation == null) {
            println("MapboxNavigation not initialized yet")
            onNavigationError(mapOf("error" to "Navigation not initialized"))
            return
        }

        val originLocation = navigationLocationProvider.lastLocation

        val originPoint = originLocation?.let {
            Point.fromLngLat(it.longitude, it.latitude)
        } ?: run {
            // Use startOrigin if provided and no current location
            startOrigin?.let { origin ->
                Point.fromLngLat(origin.longitude, origin.latitude)
            } ?: run {
                // FIX: Better error handling when no origin is available
                println("No origin location available")
                onNavigationError(mapOf("error" to "No origin location available"))
                return
            }
        }

        // Build coordinate list including waypoints
        val coordinatesList = mutableListOf<Point>().apply {
            add(originPoint)
            waypoints?.forEach { waypoint ->
                add(Point.fromLngLat(waypoint.longitude, waypoint.latitude))
            }
            add(destination)
        }

        println("Finding route with coordinates: ${coordinatesList.map { "${it.latitude()}, ${it.longitude()}" }}")

        val routeOptionsBuilder = RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .applyLanguageAndVoiceUnitOptions(context)
            .coordinatesList(coordinatesList)

        // Apply travel mode
        when (travelMode.lowercase()) {
            "walking" -> routeOptionsBuilder.profile("walking")
            "cycling" -> routeOptionsBuilder.profile("cycling")
            else -> routeOptionsBuilder.profile("driving-traffic")
        }

        // Add bearing if available
        originLocation?.bearing?.let { bearing ->
            val bearingsList = mutableListOf<Bearing?>().apply {
                add(Bearing.builder().angle(bearing).degrees(45.0).build())
                // Add null bearings for waypoints and destination
                repeat(coordinatesList.size - 1) { add(null) }
            }
            routeOptionsBuilder.bearingsList(bearingsList)
        }

        println("Requesting route...")
        navigation.requestRoutes(
            routeOptionsBuilder.build(),
            object : NavigationRouterCallback {
                override fun onCanceled(routeOptions: RouteOptions, routerOrigin: String) {
                    println("Route request canceled")
                    onNavigationCanceled(mapOf("reason" to "canceled"))
                }

                override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {
                    println("Route request failed: ${reasons.map { it.message }}")
                    onNavigationError(mapOf("error" to "Route request failed", "reasons" to reasons.map { it.message }))
                }

                override fun onRoutesReady(routes: List<NavigationRoute>, routerOrigin: String) {
                    println("Routes ready: ${routes.size} routes found")
                    setRouteAndStartNavigation(routes)
                }
            }
        )
    }

    // 6. FIX: Better route rendering with error handling
    private fun setRouteAndStartNavigation(routes: List<NavigationRoute>) {
        val navigation = mapboxNavigation
        if (navigation == null) {
            println("MapboxNavigation not available for setting routes")
            return
        }

        if (routes.isEmpty()) {
            println("No routes available to set")
            onNavigationError(mapOf("error" to "No routes found"))
            return
        }

        println("Setting ${routes.size} routes")
        navigation.setNavigationRoutes(routes)

        // Show UI elements based on props
        binding.soundButton.visibility = View.VISIBLE
        binding.routeOverview.visibility = View.VISIBLE
        if (!hideStatusView) {
            binding.tripProgressCard.visibility = View.VISIBLE
        }

        // Move camera to overview
        navigationCamera.requestNavigationCameraToOverview()

        isNavigationActive = true
        println("Navigation started successfully")
    }

    fun clearRouteAndStopNavigation() {
        mapboxNavigation?.setNavigationRoutes(listOf())

        // Hide UI elements
        binding.soundButton.visibility = View.INVISIBLE
        binding.maneuverView.visibility = View.INVISIBLE
        binding.routeOverview.visibility = View.INVISIBLE
        binding.tripProgressCard.visibility = View.INVISIBLE

        isNavigationActive = false
        onNavigationCanceled(mapOf("reason" to "user_canceled"))
    }

    private fun toggleMute() {
        isMuted = !isMuted
        if (isMuted) {
            binding.soundButton.muteAndExtend(BUTTON_ANIMATION_DURATION)
            voiceInstructionsPlayer.volume(SpeechVolume(0f))
        } else {
            binding.soundButton.unmuteAndExtend(BUTTON_ANIMATION_DURATION)
            voiceInstructionsPlayer.volume(SpeechVolume(1f))
        }
    }

    // Prop setters - called from React Native
    fun setMute(mute: Boolean) {
        if (this.isMuted != mute) {
            this.isMuted = mute
            if (::voiceInstructionsPlayer.isInitialized) {
                voiceInstructionsPlayer.volume(SpeechVolume(if (mute) 0f else 1f))
                if (mute) {
                    binding.soundButton.mute()
                } else {
                    binding.soundButton.unmute()
                }
            }
        }
    }

    fun setSeparateLegs(separate: Boolean) {
        this.separateLegs = separate
    }

    fun setDistanceUnit(unit: String) {
        this.distanceUnit = unit
    }

    fun setStartOrigin(origin: NativeCoordinate) {
        this.startOrigin = origin
    }

    fun setWaypoints(waypoints: List<Waypoint>) {
        this.waypoints = waypoints
    }

    fun setDestinationTitle(title: String?) {
        this.destinationTitle = title
    }

    // 1. MAIN ISSUE: MapboxNavigation initialization timing
    fun setDestination(destination: NativeCoordinate) {
        println("destination with coordinates222: ${destination.latitude}, ${destination.longitude}")
        this.destination = destination

        // FIX: Don't call initializeNavigation() again, just find route if ready
        if (::binding.isInitialized &&
            binding.mapView.mapboxMap.style != null &&
            mapboxNavigation != null) {
            println("destination with coordinates333: ${destination.latitude}, ${destination.longitude}")
            findRoute(Point.fromLngLat(destination.longitude, destination.latitude))
        }
        // If not ready, the route will be found when onNavigationReady is called
    }

    fun setLanguage(language: String) {
        this.language = language
    }

    fun setShowCancelButton(show: Boolean) {
        this.showCancelButton = show
        if (::binding.isInitialized) {
            binding.stop.visibility = if (show) View.VISIBLE else View.GONE
        }
    }

    fun setShouldSimulateRoute(simulate: Boolean) {
        this.shouldSimulateRoute = simulate
    }

    fun setShowsEndOfRouteFeedback(show: Boolean) {
        this.showsEndOfRouteFeedback = show
    }

    fun setHideStatusView(hide: Boolean) {
        this.hideStatusView = hide
        if (::binding.isInitialized) {
            binding.tripProgressCard.visibility = if (hide) View.GONE else View.VISIBLE
        }
    }

    fun setTravelMode(mode: String) {
        this.travelMode = mode
    }

    // Lifecycle methods
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        mapboxNavigation?.let { nav ->
            nav.registerRoutesObserver(routesObserver)
            nav.registerLocationObserver(locationObserver)
            nav.registerRouteProgressObserver(routeProgressObserver)
            nav.registerVoiceInstructionsObserver(voiceInstructionsObserver)
            nav.startTripSession()
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        mapboxNavigation?.let { nav ->
            nav.unregisterRoutesObserver(routesObserver)
            nav.unregisterLocationObserver(locationObserver)
            nav.unregisterRouteProgressObserver(routeProgressObserver)
            nav.unregisterVoiceInstructionsObserver(voiceInstructionsObserver)
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        if (::maneuverApi.isInitialized) maneuverApi.cancel()
        if (::routeLineApi.isInitialized) routeLineApi.cancel()
        if (::routeLineView.isInitialized) routeLineView.cancel()
        if (::speechApi.isInitialized) speechApi.cancel()
        if (::voiceInstructionsPlayer.isInitialized) voiceInstructionsPlayer.shutdown()
    }

    // Initialize MapboxNavigation when attached
    // 4. FIX: Improve MapboxNavigation setup timing
    private fun setupMapboxNavigation() {
        try {
             if (MapboxNavigationProvider.isCreated()) {
                 mapboxNavigation =MapboxNavigationProvider.retrieve()
            } else {
                initNavigation()
            }

            // FIX: Try to find route if destination is already set
            destination?.let { dest ->
                if (binding.mapView.mapboxMap.style != null) {
                    findRoute(Point.fromLngLat(dest.longitude, dest.latitude))
                }
            }

            println("MapboxNavigation setup complete")
        } catch (e: Exception) {
            println("Error setting up MapboxNavigation: ${e.message}")
            onNavigationError(mapOf("error" to "Failed to setup navigation: ${e.message}"))
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setupMapboxNavigation()

        // FIX: Register observers here if navigation is ready
        mapboxNavigation?.let { nav ->
            nav.registerRoutesObserver(routesObserver)
            nav.registerLocationObserver(locationObserver)
            nav.registerRouteProgressObserver(routeProgressObserver)
            nav.registerVoiceInstructionsObserver(voiceInstructionsObserver)
            nav.startTripSession()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mapboxNavigation = null
    }
}