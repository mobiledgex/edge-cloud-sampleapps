/**
 * Copyright 2019 MobiledgeX, Inc. All rights and licenses reserved.
 * MobiledgeX, Inc. 156 2nd Street #408, San Francisco, CA 94105
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//
//  PlatformIntegration.m
//

#import <Foundation/Foundation.h>
#import <CoreTelephony/CTTelephonyNetworkInfo.h>
#import <CoreTelephony/CTCarrier.h>

// The subscriber callback is set, and notifies new subscriber info.
// Simple state object.
@interface NetworkState : NSObject
@property CTTelephonyNetworkInfo *networkInfo;
@property NSDictionary<NSString *,CTCarrier *>* ctCarriers;
@property CTCarrier* lastCarrier;
@end
@implementation NetworkState
@end

NetworkState *networkState = NULL;

void _ensureMatchingEnginePlatformIntegration() {
    if (networkState == NULL)
    {
        networkState = [[NetworkState alloc] init];
        networkState.networkInfo = [[CTTelephonyNetworkInfo alloc] init];
        // Give it an initial value, if any.
        if (@available(iOS 12.1, *))
        {
            networkState.ctCarriers = [networkState.networkInfo serviceSubscriberCellularProviders];
        }
        else {
            networkState.ctCarriers = NULL;
        }
        networkState.lastCarrier = [networkState.networkInfo subscriberCellularProvider];

        if (@available(iOS 12.1, *))
        {
            networkState.networkInfo.serviceSubscriberCellularProvidersDidUpdateNotifier = ^(NSString *name) {
                networkState.ctCarriers = [networkState.networkInfo serviceSubscriberCellularProviders];
                if (networkState.ctCarriers != NULL)
                {
                    networkState.lastCarrier = networkState.ctCarriers[name];
                }
            };
        }
    }
}

char* convertToCStr(const char* str) {
    if (str == NULL) {
        return (char*)NULL;
    }

    char* out = (char*)malloc(strlen(str) + 1);
    strcpy(out, str);
    return out;
}

char* _getCurrentCarrierName()
{
    _ensureMatchingEnginePlatformIntegration();
    NSString* nsstr = @"";

    if (@available(iOS 12.1, *))
    {
        nsstr = [networkState.lastCarrier carrierName];
    }
    else
    {
        CTTelephonyNetworkInfo *netinfo = [[CTTelephonyNetworkInfo alloc] init];
        CTCarrier *carrier = [netinfo subscriberCellularProvider]; // s for dual SIM?
        NSLog(@"Carrier Name: %@", [carrier carrierName]);
        // Ref counted.

        nsstr = [carrier carrierName];
    }

    NSLog(@"Mobile CarrierName: %@", nsstr);
    return convertToCStr([nsstr UTF8String]);
}

// Atomically retrieves the last subscriber network carrier's MCCMNC as a "mccmnc" concatenated
// string combination.
char* _getMccMnc(NSString* name)
{
    _ensureMatchingEnginePlatformIntegration();
    NSMutableString* mccmnc = [NSMutableString stringWithString:@""];
    NSString* mcc;
    NSString* mnc;

    if (@available(iOS 12.1, *))
    {
        if (networkState.lastCarrier == NULL)
        {
            networkState.lastCarrier = [networkState.networkInfo subscriberCellularProvider];
        }
        mcc = [networkState.lastCarrier mobileCountryCode];
        mnc = [networkState.lastCarrier mobileNetworkCode];
    }
    else
    {
        CTTelephonyNetworkInfo *netinfo = [[CTTelephonyNetworkInfo alloc] init];
        CTCarrier *carrier = [netinfo subscriberCellularProvider];

        mcc = [carrier mobileCountryCode];
        mnc = [carrier mobileNetworkCode];
    }

    if (mcc == NULL || mnc == NULL)
    {
        return convertToCStr([@"" UTF8String]);
    }

    [mccmnc appendString: mcc];
    [mccmnc appendString: mnc];

    NSLog(@"Mobile Country Code and Mobile Network Code: %@", mccmnc);
    return convertToCStr([mccmnc UTF8String]);
}
