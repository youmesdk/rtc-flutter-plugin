//
//  MyAgoraRtcEngineKit.m
//  RCTAgora
//
//

#import "YoumeConst.h"

@implementation YoumeConst

static YoumeConst *_person;
+ (instancetype)allocWithZone:(struct _NSZone *)zone{
    static dispatch_once_t predicate;
    dispatch_once(&predicate, ^{
        _person = [super allocWithZone:zone];
    });
    return _person;
}

+ (instancetype)share {
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        _person = [[self alloc]init];
    });
    return _person;
}

- (id)copyWithZone:(NSZone *)zone {
    return _person;
}



@end
