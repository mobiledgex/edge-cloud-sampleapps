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
//  Unity-iPhone
//

#import <Foundation/Foundation.h>
#import <CoreTelephony/CTTelephonyNetworkInfo.h>
#import <CoreTelephony/CTCarrier.h>


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
    CTTelephonyNetworkInfo *netinfo = [[CTTelephonyNetworkInfo alloc] init];
    CTCarrier *carrier = [netinfo subscriberCellularProvider]; // s for dual SIM?
    NSLog(@"Carrier Name: %@", [carrier carrierName]);
    // Ref counted.
    
    NSString* nsstr = [carrier carrierName];
                      
    return convertToCStr([nsstr UTF8String]);
}


