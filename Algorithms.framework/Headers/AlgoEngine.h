//
// Created by Oleksiy Grechnyev on 1/11/19.
//

#pragma once

#include <memory>

#include "./AlgoEngineState.h"

namespace algolib {
    class AlgoEngineImpl;


    /** @brief Algorithm engine for Monbaby Mobile
     *
     *  For usage example, see testapp.cpp
     *
     *  Call process() for every new accelerometer value, setState() to get current state.
     *  The state is changed while process() works.
     *  This class is not thread-safe! Don't try to do anything else, including accessing the state while process() is running.
     */
    class AlgoEngine {
    public: //========= Methods
        /** @brief Ctor
         *
         * @param fs  Sampling freq
         * Note: Algorithms are currently tuned for fs = 6.25 Hz, for any other
         * frequency they might not work properly, especially Breath.
         */
        AlgoEngine(double fs = 6.25);

        ~AlgoEngine();

        /** @brief Get the engine state
         *
         * Note: State changes every time process() is run.
         * @return State
         */
        const AlgoEngineState &getState() const;

        /** @brief Process an input accelerometer value
         *
         * This runs all algorithms and updates the engine state.
         * Note: process() might take some time.
         * Not thread safe:
         * You must absolutely NOT call second process() from other thread while the first is running!
         * Careful using it in callbacks, you might introduce a short buffer on the mobile side.
         *
         * @param ax     Acceleration in g-units including gravity (approximately 1 at rest)
         * @param ay
         * @param az
         * @param ts     Timestamp in SECONDS
         */
        void process(double ax, double ay, double az, double ts);

        /// Run the calibration again
        void reCalibrate();

        /// Set batteryLevel parameter of the state
        void setBatteryLevel(double batteryLevel);

        /// Set highLevel parameter of the state
        void setHighLevel(bool highLevel);

    private: //======== Fields
        std::unique_ptr<AlgoEngineImpl> impl;
    };

}