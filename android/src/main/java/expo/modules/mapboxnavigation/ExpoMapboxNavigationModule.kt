package expo.modules.mapboxnavigation

import android.content.Intent
import com.mapbox.geojson.Point
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition

class ExpoMapboxNavigationModule : Module() {
  override fun definition() = ModuleDefinition {
    Name("ExpoMapboxNavigation")

    // Expose a function to start navigation with coordinates
    AsyncFunction("startNavigation") { destination: Map<String, Double> ->
      val point = Point.fromLngLat(
        destination["longitude"]!!,
        destination["latitude"]!!
      )
      startTurnByTurn(point)
    }

    // Alternatively expose a simple function to launch navigation
    Function("launchNavigation") {
      startTurnByTurn()
    }
  }

  private fun startTurnByTurn(destination: Point? = null) {
    val context = appContext.currentActivity ?: return
    val intent = Intent(context, TurnByTurnExperienceActivity::class.java).apply {
      destination?.let {
        putExtra("destination_latitude", it.latitude())
        putExtra("destination_longitude", it.longitude())
      }
    }
    context.startActivity(intent)
  }
}