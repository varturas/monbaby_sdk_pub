//
// Created by Oleksiy Grechnyev on 1/11/19.
//

#pragma once

namespace algolib {

    enum class BreathState {
        UNKNOWN,    /* Initial state where nothing is known */
        BREATHING,  /* Normal state when a respiratory signal is present */
        MOVEMENT,   /* Transient state when movement is detected */
        WARNING     /* State where user should be alerted */
    };

    enum class BabyPosition {
        ON_BACK,
        ON_SIDE,
        ON_STOMACH
    };

    /// The state of AlgoEngine
    struct AlgoEngineState {
        /// ----- "Initial axis" used in RollOver found from calibration
        double xInit = 0.;
        double yInit = 0.;
        double zInit = 1.;


        //----- Alarms
        // Alarms shoot not more than once every 10s (except orientation) and never during calibration
        /// Fall detected
        bool alarmFall = false;
        /// No breath
        bool alarmBreath = false;
        /// Seizure detected
        bool alarmSeizure = false;
        /// No motion (not even smallest ones !)
        bool alarmMotion = false;
        /// Changes back-side-stomach
        bool alarmOrientationChange = false;
        /// High activity detected
        bool alarmActivity = false;

        //----- Calibration
        /// Is the calibration finished at all (success or failure)
        bool isCalibrationFinished = false;
        /// Is the calibration successful
        bool isCalibrationSuccess = false;

        //-----

        /// Result of Fall
        bool isFallDetected = false;
        /// Resul of Move: is any (weak) move detected, including breath or pulse
        bool isMoveDetected = false;

        /// How many times in a row we have "MOVE" ? 0 if no move.
        int countMove = 0;

        /// How many times in a row we have "NO MOVE" ? 0 if move (motion alarm if >=3 ?).
        int countNoMove = 0;

        /// Awake activity detected
        bool isAwakeDetected = false;

        /// Awake activity for over 15 s
        bool isLongAwakeDetected = false;

        /// Seizure (result of Shake)
        bool isSeisureDetected = false;

        /// Something Set by Move
        double activityLevel = 0.;

        /// Last abs(acceleration)
        double acceleration = 0.;

        /// Result of RollOver (orientation in degrees relative to the axis xInit, yInit, zInit)
        double sensorOrientation = 0.;

        /// Position based on sensorOrientation
        BabyPosition babyPosition = BabyPosition::ON_BACK;

        /// Result of Flip (rotation during last few seconds in degrees)
        double flipAngle = 0.;

        /// Breath result: Breaths per minute (BPM)
        double respRate = 0.;

        /// Breath algo flag
        BreathState breathState = BreathState::UNKNOWN;

        // --- This you can change with setters !
        /// Battery level
        double batteryLevel = 0.;

        /// High level parameter (need it to analyze activity)
        bool highLevel = false;

    };


}