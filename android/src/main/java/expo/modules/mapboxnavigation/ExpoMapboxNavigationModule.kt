package expo.modules.mapboxnavigation

import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition

class ExpoMapboxNavigationModule : Module() {
    override fun definition() = ModuleDefinition {
        Name("MapboxNavigationView")

        View(MapboxNavigationView::class) {
            // Event definitions
            Events("onRouteProgressChanged", "onNavigationReady", "onNavigationCanceled", "onNavigationFinished", "onNavigationError")

            // Props from TypeScript interface
            Prop("mute") { view: MapboxNavigationView, mute: Boolean ->
                view.setMute(mute)
            }

            Prop("separateLegs") { view: MapboxNavigationView, separateLegs: Boolean ->
                view.setSeparateLegs(separateLegs)
            }

            Prop("distanceUnit") { view: MapboxNavigationView, distanceUnit: String ->
                view.setDistanceUnit(distanceUnit)
            }

            Prop("startOrigin") { view: MapboxNavigationView, startOrigin: Map<String, Double> ->
                val origin = NativeCoordinate(
                    latitude = startOrigin["latitude"] ?: 0.0,
                    longitude = startOrigin["longitude"] ?: 0.0
                )
                view.setStartOrigin(origin)
            }

            Prop("waypoints") { view: MapboxNavigationView, waypoints: List<Map<String, Any>> ->
                val waypointList = waypoints.map { waypointMap ->
                    Waypoint(
                        latitude = waypointMap["latitude"] as? Double ?: 0.0,
                        longitude = waypointMap["longitude"] as? Double ?: 0.0,
                        name = waypointMap["name"] as? String,
                        separatesLegs = waypointMap["separatesLegs"] as? Boolean ?: false
                    )
                }
                view.setWaypoints(waypointList)
            }

            Prop("destinationTitle") { view: MapboxNavigationView, destinationTitle: String? ->
                view.setDestinationTitle(destinationTitle)
            }

            Prop("destination") { view: MapboxNavigationView, destination: Map<String, Double> ->
                println("destination with coordinates: ${destination["latitude"]}, ${destination["longitude"]}")
                val dest = NativeCoordinate(
                    latitude = destination["latitude"] ?: 0.0,
                    longitude = destination["longitude"] ?: 0.0
                )

                view.setDestination(dest)
            }

            Prop("language") { view: MapboxNavigationView, language: String ->
                view.setLanguage(language)
            }

            Prop("showCancelButton") { view: MapboxNavigationView, showCancelButton: Boolean ->
                view.setShowCancelButton(showCancelButton)
            }

            Prop("shouldSimulateRoute") { view: MapboxNavigationView, shouldSimulateRoute: Boolean ->
                view.setShouldSimulateRoute(shouldSimulateRoute)
            }

            Prop("showsEndOfRouteFeedback") { view: MapboxNavigationView, showsEndOfRouteFeedback: Boolean ->
                view.setShowsEndOfRouteFeedback(showsEndOfRouteFeedback)
            }

            Prop("hideStatusView") { view: MapboxNavigationView, hideStatusView: Boolean ->
                view.setHideStatusView(hideStatusView)
            }

            Prop("travelMode") { view: MapboxNavigationView, travelMode: String ->
                view.setTravelMode(travelMode)
            }
        }

        // Additional functions for imperative control
        AsyncFunction("startNavigation") { viewTag: Int ->
            // This can be used to programmatically start navigation
            // The actual navigation will be controlled by the destination prop
        }

        AsyncFunction("stopNavigation") { view: MapboxNavigationView,->
            println("stopNavigation")
            view.clearRouteAndStopNavigation()
            // This can be used to programmatically stop navigation
            // Implementation would require finding the view by tag and calling stop
        }

        AsyncFunction("toggleMute") { viewTag: Int ->
            // This can be used to programmatically toggle mute
            // Implementation would require finding the view by tag and calling toggleMute
        }
    }
}