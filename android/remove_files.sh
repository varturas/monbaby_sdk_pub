#!/bin/bash

adb ls /sdcard/Android/data/com.alexnik.accelerometertest/files/
adb shell rm /sdcard/Android/data/com.alexnik.accelerometertest/files/*
adb ls /sdcard/Android/data/com.alexnik.accelerometertest/files/
