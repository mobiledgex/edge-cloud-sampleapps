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


