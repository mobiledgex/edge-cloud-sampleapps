//package com.mobiledgex.sdkdemo;
//
//import android.content.Intent;
//import android.os.Bundle;
//import android.support.v7.app.AppCompatActivity;
//import android.util.Log;
//import android.view.View;
//import android.widget.Button;
//import android.widget.ProgressBar;
//import android.widget.TextView;

import UIKit    // JT 18.10.24

public class CloudletDetailsActivity : SpeedTestResultsListener // JT 18.11.01
        // JT 18.10.23 todo
//extends AppCompatActivity implements SpeedTestResultsListener
{

    public static let  BYTES_TO_MBYTES:Int = 1024*1024;
    private static let  TAG:String = "CloudletDetailsActivity";
 //   private var intent: Intent;   // JT 18.10.24
    private var cloudlet: Cloudlet?
    @IBOutlet open weak var  cloudletNameTv: UILabel?
    @IBOutlet open weak var  appNameTv: UILabel?
    @IBOutlet open weak var  speedtestResultsTv: UILabel?
    @IBOutlet open weak var  latencyMinTv: UILabel?
    @IBOutlet open weak var  latencyAvgTv: UILabel?
    @IBOutlet open weak var  latencyMaxTv: UILabel?
    @IBOutlet open weak var  latencyStddevTv: UILabel?
    @IBOutlet open weak var  latencyMessageTv: UILabel?
    @IBOutlet open weak var  carrierNameTv: UILabel?
    @IBOutlet open weak var  distanceTv: UILabel?
    @IBOutlet open weak var  latitudeTv: UILabel?
    @IBOutlet open weak var  longitudeTv: UILabel?
    @IBOutlet open weak var  buttonSpeedTest: UIButton?;
    @IBOutlet open weak var  buttonLatencyTest: UIButton?
    @IBOutlet open weak var  progressBarDownload: UIProgressView? //ProgressBar
    @IBOutlet open weak var  progressBarLatency: UIProgressView?

    init () // JT 18.10.24
    {
    }
    
    func onCreate(_ cloudletName:String    )    //savedInstanceState: Bundle)
    {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_cloudlet_details);
//
//        setTitle("Cloudlet Details");
//
//        intent = getIntent(); // getIntent todo undefined

     //   let cloudletName:String = intent.getStringExtra("CloudletName");
        cloudlet = CloudletListHolder.getSingleton().getCloudletList()[cloudletName]!  // JT 18.11.02
            // JT 18.11.01 todo
        Swift.print("cloudlet=\(cloudlet) \(cloudlet!.getCloudletName())")
        cloudlet!.setSpeedTestResultsListener(self);

            // JT 18.11.01 todo in IB
//        cloudletNameTv = findViewById(R.id.cloudletName);
//        appNameTv = findViewById(R.id.appName);
//        carrierNameTv = findViewById(R.id.carrierName);
//        distanceTv = findViewById(R.id.distance);
//        latitudeTv = findViewById(R.id.latitude);
//        longitudeTv = findViewById(R.id.longitude);
//        speedtestResultsTv = findViewById(R.id.speedtestResults);
//        latencyMinTv = findViewById(R.id.latencyMin);
//        latencyAvgTv = findViewById(R.id.latencyAvg);
//        latencyMaxTv = findViewById(R.id.latencyMax);
//        latencyStddevTv = findViewById(R.id.latencyStddev);
//        latencyMessageTv = findViewById(R.id.latencyMessage);
//        progressBarDownload = findViewById(R.id.progressBarDownload);
//        progressBarLatency = findViewById(R.id.progressBarLatency);

        cloudletNameTv!.text = (cloudlet!.getCloudletName());
        appNameTv!.text = (cloudlet!.getAppName());
        carrierNameTv!.text = (cloudlet!.getCarrierName());
        distanceTv!.text = (String(format:"%.4f", cloudlet!.getDistance()));
        latitudeTv!.text = String(format:"%f", cloudlet!.getLatitude())
        longitudeTv!.text =  String(format:"%f", cloudlet!.getLongitude())

        // IB
//        buttonSpeedTest = findViewById(R.id.buttonSpeedtest);
//        buttonLatencyTest = findViewById(R.id.buttonLatencytest);

            // JT 18.10.23 todo
//        buttonSpeedTest.setOnClickListener(new View.OnClickListener() {
//            public func onClick(View view) {
//                cloudlet.startBandwidthTest();
//            }
//        });
//        buttonLatencyTest.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public func onClick(View view) {
//                cloudlet.startLatencyTest();
//            }
//        });

        updateUi();

    }

    public func onLatencyProgress() // JT 18.11.01 protocol
    {
        updateUi();
    }

    public func onBandwidthProgress()   // JT 18.11.01 protocol
    {
        updateUi();
    }

    private func updateUi()
    {
    

              DispatchQueue.main.async  // JT 18.10.24
             {
                let latencyTestMethod:   CloudletListHolder.LatencyTestMethod = CloudletListHolder.getSingleton().getLatencyTestMethod();
                
                if(self.cloudlet!.getCarrierName().caseInsensitiveCompare("azure") == .orderedSame)
                {
                    if(latencyTestMethod == CloudletListHolder.LatencyTestMethod.ping) {
                        self.latencyMessageTv!.text = "Socket test forced"
                    }
                }

                if(self.cloudlet!.getLatencyMin() != 9999) {
                    self.latencyMinTv!.text = "\(self.formatValue(self.cloudlet!.getLatencyMin()))  ms"
                } else {
                    self.latencyMinTv!.text = "\(self.formatValue(0)) ms"
                }
                if(self.cloudlet!.isPingFailed()) {
                    self.latencyMessageTv!.text = "Ping failed"
                }
                self.latencyAvgTv!.text = "\(self.formatValue(self.cloudlet!.getLatencyAvg())) ms"
                self.latencyMaxTv!.text = "\(self.formatValue(self.cloudlet!.getLatencyMax())) ms"
                self.latencyStddevTv!.text = "\(self.formatValue(self.cloudlet!.getLatencyStddev()))) ms"
                
                self.progressBarLatency!.progress = Float(self.cloudlet!.getLatencyTestProgress())
                self.progressBarDownload!.progress = Float(self.cloudlet!.getSpeedTestProgress())
               
                self.speedtestResultsTv!.text = "\(String(format:"%.2f", self.cloudlet!.getMbps())) Mbits/sec"

                self.distanceTv!.text = "\((String(format:"%.4f", self.cloudlet!.getDistance())))"
            }
       
    }

    private func  formatValue(_ value: Double) ->String
{
        return String(format:"%.3f", value);
    }

}
