import { requireNativeModule, requireNativeView } from 'expo';
import { NativeModule, ViewProps } from 'react-native';

export interface NativeCoordinate
{
    latitude: number;
    longitude: number;
}

export interface Waypoint
{
    latitude: number;
    longitude: number;
    name?: string;
    separatesLegs?: boolean;
}

export interface RouteProgressEvent
{
    distanceRemaining: number;
    durationRemaining: number;
    distanceTraveled: number;
    fractionTraveled: number;
}

export interface NavigationErrorEvent
{
    error: string;
    reasons?: string[];
}

export interface MapboxNavigationViewProps extends ViewProps
{
    // Navigation configuration
    mute?: boolean;
    separateLegs?: boolean;
    distanceUnit?: 'metric' | 'imperial';
    startOrigin: NativeCoordinate;
    waypoints?: Waypoint[];
    destinationTitle?: string;
    destination: NativeCoordinate;
    language?: string;
    showCancelButton?: boolean;
    shouldSimulateRoute?: boolean;
    showsEndOfRouteFeedback?: boolean;
    hideStatusView?: boolean;
    travelMode?: 'driving' | 'walking' | 'cycling';

    // Event handlers
    onRouteProgressChanged?: (event: { nativeEvent: RouteProgressEvent }) => void;
    onNavigationReady?: (event: { nativeEvent: { ready: boolean } }) => void;
    onNavigationCanceled?: (event: { nativeEvent: { reason: string } }) => void;
    onNavigationFinished?: (event: { nativeEvent: { completed: boolean } }) => void;
    onNavigationError?: (event: { nativeEvent: NavigationErrorEvent }) => void;
}

// Define the ref type for the native component
export interface MapboxNavigationViewRef
{
    startNavigation: () => Promise<void>;
    stopNavigation: () => Promise<void>;
    toggleMute: () => Promise<void>;
}

// Extend the props to include ref support
export interface MapboxNavigationViewPropsWithRef extends MapboxNavigationViewProps
{
    ref?: React.Ref<MapboxNavigationViewRef>;
}

// Native component with proper ref typing
const NativeMapboxNavigationView = requireNativeView<MapboxNavigationViewPropsWithRef>('MapboxNavigationView');

export default NativeMapboxNavigationView;

// Additional utility functions
export interface ExpoMapboxNavigationModule extends NativeModule
{
    stopNavigation(): Promise<void>;
}

export const NativeMapboxNavigation = requireNativeModule<ExpoMapboxNavigationModule>('MapboxNavigationView');