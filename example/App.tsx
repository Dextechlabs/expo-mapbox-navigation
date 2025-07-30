import { View, Button, StyleSheet } from "react-native";
import ExpoMapboxNavigationModule from "expo-mapbox-navigation";

export default function App() {
  const startNavigation = async () => {
    // Start with specific coordinates (San Francisco)

    // Or use the simple launch
    try {
      await ExpoMapboxNavigationModule.startNavigation({
        latitude: 22.977007,
        longitude: 88.445722,
      });
      // await ExpoMapboxNavigationModule.launchNavigation();
      console.log("Navigation started successfully");
    } catch (error) {
      console.error("Failed to start navigation:", error);
    }
  };

  return (
    <View style={styles.container}>
      <Button title="Start Navigation" onPress={startNavigation} />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: "center",
    alignItems: "center",
  },
});
