export interface NavigationDestination
{
  longitude: number;
  latitude: number;
}

export interface NavigationViewProps
{
  destination: NavigationDestination;
  // showsUserLocation?: boolean;
  // followsUserLocation?: boolean;
  // onRouteFound?: () => void;
  // onNavigationFinished?: () => void;
  // onLocationUpdate?: (location: { latitude: number; longitude: number }) => void;
  style?: any;
}

export interface NavigationEvents
{
  onNavigationStarted: (data: { longitude?: number; latitude?: number; address?: string }) => void;
  onNavigationStopped: () => void;
  onRouteProgress: (data: any) => void;
  onNavigationError: (data: { error: string }) => void;
}

export interface ExpoMapboxNavigationModuleProps
{
  /**
   * Start navigation to a specific coordinate
   */
  startNavigation(longitude: number, latitude: number): Promise<boolean>;
  launchNavigation(): Promise<boolean>;

  /**
   * Start navigation to an address
   */
  // startNavigationWithAddress(address: string): Promise<boolean>;

  // /**
  //  * Check if navigation is currently active
  //  */
  // isNavigationActive(): boolean;

  /**
   * Add event listeners for navigation events
   */
  addListener<K extends keyof NavigationEvents>(
    eventName: K,
    listener: NavigationEvents[K]
  ): { remove: () => void };
}
export type NativeEvent<T> = {
  nativeEvent: T;
};
export type NativeEventsProps = {
  // onLocationChange?: (event: NativeEvent<Location>) => void;
  // onRouteProgressChange?: (event: NativeEvent<RouteProgress>) => void;
  // onError?: (event: NativeEvent<MapboxEvent>) => void;
  // onCancelNavigation?: (event: NativeEvent<MapboxEvent>) => void;
  // onArrive?: (event: NativeEvent<WaypointEvent>) => void;
};