// import { requireNativeViewManager } from "expo-modules-core"; //
// import { ViewProps } from "react-native";

// interface MapboxNavigationViewProps extends ViewProps {
//   // Define any props specific to your MapboxNavigationView here
//   destinationLatitude: number;
//   destinationLongitude: number;
// }

// const NativeView = requireNativeViewManager("ExpoMapboxNavigation");

// export default function MapboxNavigationView(props: MapboxNavigationViewProps) {
//   return <NativeView {...props} />;
// }
import { requireNativeModule } from 'expo'; //
import { NativeModule } from 'react-native';

interface ExpoMapboxNavigationModule extends NativeModule
{
    startNavigation(destination: { latitude: number; longitude: number }): Promise<void>;
}

export default requireNativeModule<ExpoMapboxNavigationModule>('ExpoMapboxNavigation'); //
