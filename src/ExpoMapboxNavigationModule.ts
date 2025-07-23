import { NativeModule, requireNativeModule } from 'expo';

import { ExpoMapboxNavigationModuleEvents } from './ExpoMapboxNavigation.types';

declare class ExpoMapboxNavigationModule extends NativeModule<ExpoMapboxNavigationModuleEvents>
{
  setValueAsync(value: string): Promise<void>;
}

// This call loads the native module object from the JSI.
export default requireNativeModule<ExpoMapboxNavigationModule>('ExpoMapboxNavigation');
