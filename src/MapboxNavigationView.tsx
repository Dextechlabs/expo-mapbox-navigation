import React, { forwardRef, useImperativeHandle, useRef } from "react";
import { StyleSheet, View } from "react-native";
import NativeMapboxNavigationView, {
  MapboxNavigationViewProps,
  NativeCoordinate,
  Waypoint,
  RouteProgressEvent,
  NavigationErrorEvent,
  NativeMapboxNavigation,
  MapboxNavigationViewRef,
} from "./index";

interface MapboxNavigationComponentProps
  extends Omit<MapboxNavigationViewProps, "startOrigin" | "destination"> {
  startOrigin: NativeCoordinate;
  destination: NativeCoordinate;
  style?: React.ComponentProps<typeof View>["style"];
  onRouteProgress?: (progress: RouteProgressEvent) => void;
  onReady?: (ready: boolean) => void;
  onCanceled?: (reason: string) => void;
  onFinished?: (completed: boolean) => void;
  onError?: (error: NavigationErrorEvent) => void;
}

const MapboxNavigationView = forwardRef<
  MapboxNavigationViewRef,
  MapboxNavigationComponentProps
>((props, ref) => {
  const {
    style,
    onRouteProgress,
    onReady,
    onCanceled,
    onFinished,
    onError,
    mute = false,
    separateLegs = false,
    distanceUnit = "metric",
    language = "en",
    showCancelButton = true,
    shouldSimulateRoute = false,
    showsEndOfRouteFeedback = true,
    hideStatusView = false,
    travelMode = "driving",
    ...rest
  } = props;
  const nativeViewRef = useRef<MapboxNavigationViewRef>(null);

  useImperativeHandle(ref, () => ({
    startNavigation: async () => {
      if (nativeViewRef.current) {
        // You'll need to access the native view's tag
        const viewTag = (nativeViewRef.current as any)._nativeTag;
        // Call your native module function
        // await MapboxNavigationModule.startNavigation(viewTag);
        console.log("Start navigation called with view tag:", viewTag);
      }
    },
    stopNavigation: async () => {
      // await MapboxNavigationModule.stopNavigation();
      console.log("Stop navigation called");
      NativeMapboxNavigation.stopNavigation();
    },
    toggleMute: async () => {
      if (nativeViewRef.current) {
        const viewTag = (nativeViewRef.current as any)._nativeTag;
        // await MapboxNavigationModule.toggleMute(viewTag);
        console.log("Toggle mute called with view tag:", viewTag);
      }
    },
  }));

  const handleRouteProgressChanged = (event: {
    nativeEvent: RouteProgressEvent;
  }) => {
    onRouteProgress?.(event.nativeEvent);
  };

  const handleNavigationReady = (event: {
    nativeEvent: { ready: boolean };
  }) => {
    onReady?.(event.nativeEvent.ready);
  };

  const handleNavigationCanceled = (event: {
    nativeEvent: { reason: string };
  }) => {
    onCanceled?.(event.nativeEvent.reason);
  };

  const handleNavigationFinished = (event: {
    nativeEvent: { completed: boolean };
  }) => {
    onFinished?.(event.nativeEvent.completed);
  };

  const handleNavigationError = (event: {
    nativeEvent: NavigationErrorEvent;
  }) => {
    onError?.(event.nativeEvent);
  };

  return (
    <NativeMapboxNavigationView
      ref={nativeViewRef}
      style={[styles.container, style]}
      mute={mute}
      separateLegs={separateLegs}
      distanceUnit={distanceUnit}
      language={language}
      showCancelButton={showCancelButton}
      shouldSimulateRoute={shouldSimulateRoute}
      showsEndOfRouteFeedback={showsEndOfRouteFeedback}
      hideStatusView={hideStatusView}
      travelMode={travelMode}
      onRouteProgressChanged={handleRouteProgressChanged}
      onNavigationReady={handleNavigationReady}
      onNavigationCanceled={handleNavigationCanceled}
      onNavigationFinished={handleNavigationFinished}
      onNavigationError={handleNavigationError}
      {...rest}
    />
  );
});

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
});

MapboxNavigationView.displayName = "MapboxNavigationView";

export default MapboxNavigationView;

export type {
  NativeCoordinate,
  Waypoint,
  RouteProgressEvent,
  NavigationErrorEvent,
};
