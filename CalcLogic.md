Algorithms always process data from a moving window of about 15 seconds, depending on an algorithm. The data from the accelerometer is stored into sensorArray vector and only most recent data points are processed.

# Calibration Process

During calibration, BLE on mobile phones establish connection to the button and activates accelerometer. Once BLE connection is established, there is a 15 second "Calibration" period. This is done on a scheduled timer:

    executorService.schedule(new EventDelayCommand(), EVENT_DELAY, TimeUnit.MILLISECONDS);
    
During those 15 seconds, two things happen:
* the sensorArray vector is populated with at least 15 seconds of the data
* one of the algorithms (Rollover) stores initial position. 

            if (xinit == -999.0 && yinit == -999.0 && zinit == -999.0) {
                // make sure we accumulated enough points during 5 seconds
                if (mcnt > MONBABY_EVENT_FREQ * SENSOR_RT_TIMEPERIOD * 2) {
                    // now make sure the sensor was not moving when we
                    // recorded initial positions
                    if (xval != 0.0
                            && yval != 0.0
                            && zval != 0.0
                            // if maximum and minimum values are not too far
                            // from mean value, it's a proxy for standard deviation
                            // that means the sensor was steady
                            && xval > maxval[0] - 0.1
                            && xval < minval[0] + 0.1
                            && yval > maxval[1] - 0.1
                            && yval < minval[1] + 0.1
                            && zval > maxval[2] - 0.1
                            && zval < minval[2] + 0.1) {
                        xinit = xval;
                        yinit = yval;
                        zinit = zval;
                    } else {
                        // else, sensor was moving too much and we revert to
                        // default settings
                        xinit = 0.0;
                        yinit = 0.0;
                        zinit = 1.0;
                    }
                }
            }

It only populates initial settings if they are not set (xinit==-999.) and if there is not much variance. If there is too much variance and values within the interval exceed max and min, then the default "vertical" setting is stored, xinit = 0, yinit = 0, zinit = 1.0;

# Rollover Logic

Here is the logic for calculating the rollover angle

    public double calcRolloverAngle(double timeWindow1) {
        int mcnt = 0;
        double xval = 0.0, yval = 0.0, zval = 0.0;
        double maxval[] = {-999.0, -999.0, -999.0}, minval[] = {999.0,
            999.0, 999.0};
        // store initial position as average angle during 5 seconds of calibration
        synchronized (sensorArray) {
            double timeWindowCut = timeWindow1;
            // use longer window during initialization, check if init values were not set already
            if (xinit == -999.0 && yinit == -999.0 && zinit == -999.0) {
                timeWindowCut = SENSOR_STATUS_TIMEPERIOD; // 5 seconds
            }
            // calculate average, min and max
            for (int icnt = 0; icnt < sensorArray.size(); icnt++) {
                SensorVal val = sensorArray.get(icnt);
                if (val.timestamp > System.currentTimeMillis() / 1000.
                        - timeWindowCut) { // go back 5 seconds
                    xval += val.xvalue;
                    yval += val.yvalue;
                    zval += val.zvalue;
                    if (val.xvalue > maxval[0])
                        maxval[0] = val.xvalue;
                    if (val.xvalue < minval[0])
                        minval[0] = val.xvalue;
                    if (val.yvalue > maxval[1])
                        maxval[1] = val.yvalue;
                    if (val.yvalue < minval[1])
                        minval[1] = val.yvalue;
                    if (val.zvalue > maxval[2])
                        maxval[2] = val.zvalue;
                    if (val.zvalue < minval[2])
                        minval[2] = val.zvalue;
                    mcnt++;
                }
            }
            // calculate average values
            if (mcnt > 0) {
                xval /= mcnt;
                yval /= mcnt;
                zval /= mcnt;
            }

            // now we know initial positionslogic to remember initial position, 
            // for example when the sensor is on the stomach, or side, or anywhere
            // remember sensor could be placed anywhere
            if (xinit == -999.0 && yinit == -999.0 && zinit == -999.0) {
                // make sure we accumulated enough points during 5 seconds
                if (mcnt > MONBABY_EVENT_FREQ * SENSOR_RT_TIMEPERIOD * 2) {
                    // now make sure the sensor was not moving when we
                    // recorded initial positions
                    if (xval != 0.0
                            && yval != 0.0
                            && zval != 0.0
                            // if maximum and minimum values are not too far
                            // from mean value, it's a proxy for standard deviation
                            // that means the sensor was steady
                            && xval > maxval[0] - 0.1
                            && xval < minval[0] + 0.1
                            && yval > maxval[1] - 0.1
                            && yval < minval[1] + 0.1
                            && zval > maxval[2] - 0.1
                            && zval < minval[2] + 0.1) {
                        xinit = xval;
                        yinit = yval;
                        zinit = zval;
                    } else {
                        // else, sensor was moving too much and we revert to
                        // default settings
                        xinit = 0.0;
                        yinit = 0.0;
                        zinit = 1.0;
                    }
                }
            }

            // do not proceed if xinit values are not initialized
            if (xinit == -999.0 && yinit == -999.0 && zinit == -999.0)
                return 0.0;
            if (mcnt > 0) {
                // now calculate angle with respect to recorded initial
                // position
                mbAngle = Math.acos((xval * xinit + yval * yinit + zval
                        * zinit)
                        / (Math.sqrt(xinit * xinit + yinit * yinit + zinit
                        * zinit) * Math.sqrt(xval * xval + yval
                        * yval + zval * zval)))
                        * 180 / Math.PI;
            }
        }
        // NSLog(@"SensorData:flip:%f",flipAngle);
        return mbAngle;
    }

Here is the logic for calculation if baby is on the back, I've added comments and the code snippet below:

    // if acceleration is in a steady state (more than 0.8g and less than 1.2g)
    if (sensorValArray.getAcceleration() > 0.8
            && sensorValArray.getAcceleration() < 1.2) {
        if (sensorValArray.getMbAngle() < 135.0) { // not on a back
            if (sensorValArray.getMbAngle() < 45.0) { // if angle less than 45 then on the stomach, otherwise on a side
                babyBack = true;
                sensorValArray.incrementOnBack();
            }
        } else {
            babyBack = false;
            // if the angle more than 135 then sound alert
            sensorValArray.incrementOnStomach();
            if (sharedPreferencesHelper.getOrientation()
                    && mode.isCalibrateFinish()) {
                AlarmItem orientation = AlarmItem.ORIENTATION;
                orientation.setTimestamp(Calendar.getInstance()
                        .getTimeInMillis());
                uiObservable.setAlarmItem(orientation);
                eventNotificationObservable.setAlarmItem(orientation);
            }
        }
    }

# Average activity

Average activity is a detrended squared sum of accelerometer measurements. Detrending is done by subtracting averaged over analysis timeperiod (20 seconds) value of squared sum of accelerometer measurements.

        ANALYSIS_TIMEPERIOD = 20.0f;
        for (int ii = 1; ii < windowsize; ii++) {
            //take most recent within 20 seconds interval values
            if (buft_ptr[ii] >= System.currentTimeMillis() / 1000. - ANALYSIS_TIMEPERIOD) {
                mean_disp += Math.sqrt(bufx_ptr[ii] * bufx_ptr[ii] + bufy_ptr[ii] * bufy_ptr[ii] + bufz_ptr[ii]
                    * bufz_ptr[ii]);
                wcnt++;
        }
        if (wcnt > 0) mean_disp /= wcnt;
        for (int ii = 1; ii < windowsize; ii++) {
            if (buft_ptr[ii] < System.currentTimeMillis() / 1000. - ANALYSIS_TIMEPERIOD) {
                double d1 = Math.abs(Math.sqrt(bufx_ptr[ii] * bufx_ptr[ii] + bufy_ptr[ii] * bufy_ptr[ii]
                    + bufz_ptr[ii] * bufz_ptr[ii])
                    - mean_disp);
            }
            activityLevel += d1;
        }

# Sleep time

TBD

# Awake time

TBD


# Fall detection

Fall is detected by finding within 5 second time period, falling time-period followed by a hit

		for (ii = 1; ii < windowsize; ii++) {
            // calculate detrended squared sum
			sig[ii] = Math.sqrt(vx[ii] * vx[ii] + vy[ii] * vy[ii] + vz[ii] * vz[ii]) - meanVal;
            // first detect falling time period
			if (sig[ii] < (-0.7) && jneg == 0) {
				neg[jneg++] = ii;
			}
			// followed by a hit
			if (sig[ii] > 0.2 && jpos == 0) {
				pos[jpos++] = ii;
			}
			if (jneg > 0 && jpos > 0 && (neg[jpos - 1] - pos[jneg - 1]) > 0) {
	        	isFoundFlag = 1;
	        	return isFoundFlag;
			}
        }

# Prone time
TBA


# Battery alert

We have 2 services for battery level, one service outputs voltage, another outputs percentage. 
Both levels are normalized to 5 percent, which corresponds roughly to 2.37 Volts. Below that the device starts malfunctioning.

                if(batteryLevel < 5 && mode.isCalibrateFinish()) {
                    AlarmItem battery = AlarmItem.BATTERY;
                    eventNotificationObservable.setAlarmItem(battery);
                }
