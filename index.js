/**
 * Created by lvbingru on 1/5/16.
 */

import {
  NativeModules,
} from "react-native";

const { ShareAPI } = NativeModules;

export const isQQInstalled = ShareAPI.isQQInstalled;
export const isWeiboInstalled = ShareAPI.isWeiboInstalled;
export const share = ShareAPI.share;
