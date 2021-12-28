//
//  RCTShareAPI.m
//  RCTShareAPI
//
//  Created by gkk on 28/12/2021.
//  Copyright © 2021 eleduck. All rights reserved.
//

#import "RCTShareAPI.h"
#import "WeiboSDK.h"
#import <TencentOpenAPI/TencentOAuth.h>
#import <TencentOpenAPI/TencentOAuth.h>
#import <TencentOpenAPI/QQApiInterface.h>
#import <TencentOpenAPI/QQApiInterfaceObject.h>

#import <React/RCTLog.h>
#import <React/RCTBridge.h>
#import <React/RCTEventDispatcher.h>
#import <React/RCTImageLoader.h>

#define RCTShareEventName (@"Share_Resp")

#define RCTSharePlatform @"platform"
#define RCTShareTitle @"title"
#define RCTShareDescription @"description"
#define RCTShareWebpageUrl @"webpageUrl"
#define RCTShareImageUrl @"imageUrl"

BOOL gRegister = NO;

@interface RCTShareAPI()
{
    TencentOAuth* _qqapi;
}

@end

@implementation RCTShareAPI

@synthesize bridge = _bridge;

RCT_EXPORT_MODULE();

+(BOOL)requiresMainQueueSetup {
    return YES;
}

- (NSArray<NSString *> *)supportedEvents
{
  return @[RCTShareEventName];
}

- (dispatch_queue_t)methodQueue
{
    return dispatch_get_main_queue();
}

RCT_EXPORT_METHOD(isQQInstalled:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject)
{
    resolve([NSNumber numberWithBool:[QQApiInterface isSupportShareToQQ]]);
}

RCT_EXPORT_METHOD(isWeiboInstalled:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject)
{
    resolve([NSNumber numberWithBool:WeiboSDK.isWeiboAppInstalled]);
}

RCT_EXPORT_METHOD(share:(NSDictionary *)data resolve:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject)
{
    NSString *plactform = data[RCTSharePlatform];
    NSString *title = data[RCTShareTitle];
    NSString *description= data[RCTShareDescription];
    NSString *imgPath = data[RCTShareImageUrl];
    NSString *webpageUrl = data[RCTShareWebpageUrl]? : @"";
    
    if ([plactform isEqualToString: @"qq"]) {
        [self _autoRegisterQQAPI];
        QQApiObject *message = [QQApiNewsObject
                                objectWithURL:[NSURL URLWithString:webpageUrl]
                                title:title
                                description:description
                                previewImageURL:[NSURL URLWithString:imgPath]];
        SendMessageToQQReq *req = [SendMessageToQQReq reqWithContent:message];
        QQApiSendResultCode sent = [QQApiInterface sendReq:req];
        if (sent == EQQAPISENDSUCESS) {
            resolve(@[[NSNull null]]);
        } else if (sent == EQQAPIAPPSHAREASYNC) {
            resolve(@[[NSNull null]]);
        } else {
            reject(@"-1",@"",nil);
        }
    } else {
        [self _autoRegisterWeiboAPI];
        
        WBMessageObject *message = [WBMessageObject message];
        WBWebpageObject *webpageObject = [WBWebpageObject new];
        webpageObject.webpageUrl = webpageUrl;
        webpageObject.title = title;
        webpageObject.description = description;
        message.mediaObject = webpageObject;
        message.mediaObject.objectID = [NSDate date].description;
        //不传thumbnail链接会失效
        message.mediaObject.thumbnailData = UIImageJPEGRepresentation([UIImage imageNamed:@"ic_logo"], 0.1);
        
        WBAuthorizeRequest *authRequest = [WBAuthorizeRequest request];
        authRequest.redirectURI = @"https://www.sina.com";
        authRequest.scope = @"all";
        WBSendMessageToWeiboRequest *request = [WBSendMessageToWeiboRequest requestWithMessage:message authInfo:authRequest access_token:nil];
        BOOL success = [WeiboSDK sendRequest:request];
        if (success) {
            resolve(@[[NSNull null]]);
        } else {
            reject(@"-1",@"",nil);
        }
    }
}

#pragma mark - private

- (void)_autoRegisterQQAPI
{
    if (_qqapi) {
        return;
    }
    NSString *appId = nil;
    NSArray *list = [[[NSBundle mainBundle] infoDictionary] valueForKey:@"CFBundleURLTypes"];
    for (NSDictionary *item in list) {
        NSString *name = item[@"CFBundleURLName"];
        if ([name isEqualToString:@"qq"]) {
            NSArray *schemes = item[@"CFBundleURLSchemes"];
            if (schemes.count > 0)
            {
                appId = [schemes[0] substringFromIndex:@"tencent".length];
                break;
            }
        }
    }
    _qqapi = [[TencentOAuth alloc] initWithAppId:appId andDelegate:nil];

}

// 如果js没有调用registerApp，自动从plist中读取appId
- (void)_autoRegisterWeiboAPI
{
    if (gRegister) {
        return;
    }
    
    NSArray *list = [[[NSBundle mainBundle] infoDictionary] valueForKey:@"CFBundleURLTypes"];
    for (NSDictionary *item in list) {
        NSString *name = item[@"CFBundleURLName"];
        if ([name isEqualToString:@"sina"]) {
            NSArray *schemes = item[@"CFBundleURLSchemes"];
            if (schemes.count > 0)
            {
                NSString *appId = [schemes[0] substringFromIndex:@"wb".length];
#ifdef DEBUG
                [WeiboSDK enableDebugMode:YES];
#endif
                if ([WeiboSDK registerApp:appId]) {
                    gRegister = YES;
                }
                break;
            }
        }
    }
}


@end
