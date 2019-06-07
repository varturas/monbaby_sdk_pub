# Introduction

There is **Algorithms.framework** placed in root folder. It can be used for iOS application directed on **arm64**. 

Algorithms are placed into a single engine AlgoEngine which processes raw accelerometer data (acceleration+timestamp).

# Usage

To start using the library you have to include `AlgoEngine.h` and create an `algolib:AlgoEngine` object. After that you can start sendindg values into the engine and retrieve state from it. Each time you get a new accelerometer data, call method `process` _(ax, ay, az, ts)_. _(ax, ay, az)_ is the  acceleration in the 9.8 m/s2 units (gravity is approximately one at rest), `ts` is the timestamp in **seconds** (not any other units !!!). After each `process` (or whenever you want), get the curent engine state with
the method `getState`. The state contains various flags and other data, and _bool_ variables for alarms.  State is represented as `algolib:AlgoEngineState`. Detailed information about fields and methods of the engine and state of it can be found in header files of the framework.

**Important** : `AlgoEngine` is not thread-safe, do not call under any circumstances `process` concurrently in different threads! Also `process` runs all algorithms, it might take some time. Translation for noobs: do not put `process` inside any sort of 'new BLE data has arrived' callback on the mobile, it is safer to put some small buffer in between.

**Note on Alarms** : Alarm of each type (i.e. "breath" or "fall") shoots not more often than once per 10 seconds. Check the flags after each call of `process()` in order
not to miss it!

Note on **calibration**: You can check in the state `isCalibrationFinished` and `isCalibrationSuccess`. "Calibration" means determining the average gravity 
direction as the world `z`-direction from the first few seconds of data. The result is stored in `xInit`, `yInit`, `zInit` of the state. It should be close to 
the default 0, 0, 1 if the button was placed upwards during calibration. The axis is used to determine baby position relative to the starting "ON_BACK" position. 
Calibration fails if there was too much shaking (`isCalibrationFinished` = true, `isCalibrationSuccess` = false). Use these falgs in your app
to determine if the calibration is finished already and whether it was successful.  Run `reCalibrate()` if needed.

# Example

Example can be found under `Example` folder. There is very simple project, which shows how to include header, create engine and send values to the framework.