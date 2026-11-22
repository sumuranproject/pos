import "react-native-url-polyfill/auto";
import { Buffer } from "buffer";
import { registerRootComponent } from "expo";
import App from "./src/App";

global.Buffer = global.Buffer || Buffer;
registerRootComponent(App);
