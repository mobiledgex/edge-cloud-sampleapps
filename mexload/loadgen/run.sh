#!/bin/bash



echo "POST TEST: Rate is $THERATE/s DURATION is $THEDURATION URL is $THEURL"
jq -ncM 'while(true; .+1) | {method: "POST", url: "'$THEURL'", body: {id: .} | @base64 }' | \
  /usr/local/bin/vegeta attack -rate=$THERATE/s -lazy -format=json -duration=$THEDURATION | \
  tee results.bin | \
  /usr/local/bin/vegeta report

#echo "GET TEST: Rate is $THERATE/s DURATION is $THEDURATION"
#jq -ncM 'while(true; .+1) | {method: "GET", url: ("https://hamburg-main.tdg.mobiledgex.net:10001/posts/"+ (.| tostring))}' |
#  /usr/local/bin/vegeta attack -rate=$THERATE/s -lazy -format=json -duration=$THEDURATION | \
#  tee results.bin | \
#  /usr/local/bin/vegeta report
  
