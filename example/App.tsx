import React, { useEffect, useRef, useState } from "react";
import { View, StyleSheet, Alert, Button } from "react-native";
import MapboxNavigationView, {
  NativeCoordinate,
  Waypoint,
  RouteProgressEvent,
  NavigationErrorEvent,
  MapboxNavigationViewRef,
} from "expo-mapbox-navigation";
import * as Location from "expo-location";
const App: React.FC = () => {
  const mapboxRef = useRef<MapboxNavigationViewRef>(null);
  const [isMuted, setIsMuted] = useState(false);
  const [errorMsg, setErrorMsg] = useState(false);

  // Example coordinates (New York to Boston)
  const startOrigin: NativeCoordinate = {
    latitude: 22.954779,
    longitude: 88.447378,
  };

  const destination: NativeCoordinate = {
    latitude: 22.974991,
    longitude: 88.451382,
  };

  // Optional waypoints
  const waypoints: Waypoint[] = [
    {
      latitude: 41.2033,
      longitude: -77.1945,
      name: "Rest Stop",
      separatesLegs: false,
    },
  ];

  const handleRouteProgress = (event: { nativeEvent: RouteProgressEvent }) => {
    const progress = event.nativeEvent;
    console.log("Route progress:", {
      distanceRemaining: progress.distanceRemaining,
      durationRemaining: progress.durationRemaining,
      fractionTraveled: progress.fractionTraveled,
    });
  };

  const handleStopNavigation = async () => {
    console.log("handleStopNavigation");
    if (mapboxRef.current) {
      console.log("handleStopNavigation22222");
      await mapboxRef.current.stopNavigation();
      console.log("Navigation stopped!");
    }
  };

  const handleNavigationReady = (event: {
    nativeEvent: { ready: boolean };
  }) => {
    // console.log("Navigation ready:", event);
    if (event.nativeEvent.ready) {
      Alert.alert("Navigation Ready", "The navigation system is ready to use");
    }
  };

  const handleNavigationCanceled = (event: {
    nativeEvent: { reason: string };
  }) => {
    console.log("Navigation canceled:", event.nativeEvent.reason);
    Alert.alert("Navigation Canceled", `Reason: ${event.nativeEvent.reason}`);
  };

  const handleNavigationFinished = (completed: boolean) => {
    console.log("Navigation finished:", completed);
    Alert.alert(
      "Navigation Finished",
      completed ? "You have arrived!" : "Navigation ended"
    );
  };

  const handleNavigationError = (event: {
    nativeEvent: NavigationErrorEvent;
  }) => {
    console.error("Navigation error:", event);
    Alert.alert("Navigation Error", event.nativeEvent.error);
  };
  const getLocationPermission = async () => {
    const { status } = await Location.requestForegroundPermissionsAsync();
    if (status !== "granted") {
      Alert.alert(
        "Permission denied",
        "Please enable location permission in settings."
      );
      return;
    }
    setErrorMsg(true);
  };
  useEffect(() => {
    const requestPermission = async () => {
      await getLocationPermission();
    };
    requestPermission();
  }, []);

  return (
    <View style={styles.container}>
      {errorMsg && (
        <MapboxNavigationView
          ref={mapboxRef}
          style={styles.navigationView}
          // Required props
          startOrigin={startOrigin}
          destination={destination}
          // Optional configuration
          // waypoints={waypoints}
          // destinationTitle="Boston, MA"
          // mute={isMuted}
          // separateLegs={false}
          distanceUnit="metric"
          language="en"
          // showCancelButton={true}
          // shouldSimulateRoute={false}
          // showsEndOfRouteFeedback={true}
          // hideStatusView={false}
          travelMode="driving"
          // Event handlers
          onRouteProgressChanged={handleRouteProgress}
          // onRouteProgress={handleRouteProgress}
          onNavigationReady={handleNavigationReady}
          onNavigationCanceled={handleNavigationCanceled}
          // onCanceled={handleNavigationCanceled}
          // onFinished={handleNavigationFinished}
          onNavigationError={handleNavigationError}
        />
      )}
      {/* <Button title="Stop Navigation" onPress={handleStopNavigation} /> */}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  navigationView: {
    flex: 1,
  },
  controls: {
    position: "absolute",
    top: 50,
    left: 20,
    right: 20,
    flexDirection: "row",
    justifyContent: "space-around",
    backgroundColor: "rgba(255, 255, 255, 0.9)",
    padding: 10,
    borderRadius: 10,
  },
});

export default App;
