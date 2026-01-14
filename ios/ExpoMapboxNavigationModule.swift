import ExpoModulesCore
import UIKit



public class ExpoMapboxNavigationModule: Module {
  // Each module class must implement the definition function. The definition consists of components
  // that describes the module's functionality and behavior.
  // See https://docs.expo.dev/modules/module-api for more details about available components.
  public func definition() -> ModuleDefinition {
    // Sets the name of the module that JavaScript code will use to refer to the module. Takes a string as an argument.
    // Can be inferred from module's class name, but it's recommended to set it explicitly for clarity.
    // The module will be accessible from `requireNativeModule('ExpoMapboxNavigation')` in JavaScript.
    Name("MapboxNavigationView")
    // view definition: Prop, Events.
    View(ExpoMapboxNavigationView.self) {
      // Defines a setter for the `url` prop.
      Prop("mute") { (view: ExpoMapboxNavigationView, mute: Bool) in
        view.setMute(mute)
      }

      Prop("separateLegs") { (view: ExpoMapboxNavigationView, separateLegs: Bool) in
        view.setSeparateLegs(separateLegs)
      }

      Prop("distanceUnit") { (view: ExpoMapboxNavigationView, distanceUnit: String) in
        view.setDistanceUnit(distanceUnit)
      }

      Prop("startOrigin") { (view: ExpoMapboxNavigationView, startOrigin: [String: Double]) in
        view.applyStartOriginDict(startOrigin)
      }

      Prop("waypoints") { (view: ExpoMapboxNavigationView, waypoints: [[String: Any]]) in
        view.applyWaypointsArray(waypoints)
      }

      Prop("destinationTitle") { (view: ExpoMapboxNavigationView, destinationTitle: String?) in
        view.setDestinationTitle(destinationTitle)
      }

      Prop("destination") { (view: ExpoMapboxNavigationView, destination: [String: Double]) in
        view.applyDestinationDict(destination)
      }

      Prop("language") { (view: ExpoMapboxNavigationView, language: String) in
        view.setLanguage(language)
      }

      Prop("showCancelButton") { (view: ExpoMapboxNavigationView, showCancelButton: Bool) in
        view.setShowCancelButton(showCancelButton)
      }

      Prop("shouldSimulateRoute") { (view: ExpoMapboxNavigationView, shouldSimulateRoute: Bool) in
        view.setShouldSimulateRoute(shouldSimulateRoute)
      }

      Prop("showsEndOfRouteFeedback") {
        (view: ExpoMapboxNavigationView, showsEndOfRouteFeedback: Bool) in
        view.setShowsEndOfRouteFeedback(showsEndOfRouteFeedback)
      }

      Prop("hideStatusView") { (view: ExpoMapboxNavigationView, hideStatusView: Bool) in
        view.setHideStatusView(hideStatusView)
      }

      Prop("travelMode") { (view: ExpoMapboxNavigationView, travelMode: String) in
        view.setTravelMode(travelMode)
      }

      // Event definitions
      Events(
        "onRouteProgressChanged", "onNavigationReady", "onNavigationCanceled",
        "onNavigationFinished", "onNavigationError")
    }

    // Expose a simple function callable from JS for global dismissal
    Function("stopNavigation") { () -> Void in
      if let top = ExpoMapboxNavigationModule.topViewController() {
        if let presented = top.presentedViewController as? NavigationViewController {
          presented.dismiss(animated: true)
          return
        }
        if let presented = top.presentedViewController {
          presented.dismiss(animated: true)
        }
      }
    }
  }
}

extension ExpoMapboxNavigationModule {
  static func topViewController(base: UIViewController? = UIApplication.shared.connectedScenes
    .compactMap { ($0 as? UIWindowScene)?.keyWindow }
    .first?
    .rootViewController) -> UIViewController? {
    if let nav = base as? UINavigationController {
      return topViewController(base: nav.visibleViewController)
    }
    if let tab = base as? UITabBarController, let selected = tab.selectedViewController {
      return topViewController(base: selected)
    }
    if let presented = base?.presentedViewController {
      return topViewController(base: presented)
    }
    return base
  }
}
