//
//  MyAgoraRtcEngineKit.h
//  RCTAgora
//
//

#import <Foundation/Foundation.h>
#import "YMVoiceService.h"

static NSString *YOUME_ON_EVENT = @"YOUME_ON_EVENT";
static NSString *YOUME_ON_RESTAPI = @"YOUME_ON_RESTAPI";
static NSString *YOUME_ON_MEMBER_CHANGE = @"YOUME_ON_MEMBER_CHANGE";
static NSString *YOUME_ON_STATISTIC_UPDATE = @"YOUME_ON_STATISTIC_UPDATE";

@interface YoumeConst : NSObject

@property (nonatomic, copy) NSString *appKey;

@property (nonatomic, copy) NSString *secretKey;

@property (nonatomic, assign) int region;

@property (nonatomic, copy) NSString *extRegion;

@property (nonatomic, copy) NSString *localUid;

@property (nonatomic, copy) NSString *currentShareUserId; //shareid 完成的标识是: self.currentShareUserId + "_share"

@property (strong, nonatomic) YMVoiceService *engine;

+ (instancetype)share;

@end
