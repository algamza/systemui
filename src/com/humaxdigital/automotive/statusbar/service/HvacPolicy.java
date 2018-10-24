/*
 * Copyright (c) 2016, The Android Open Source Project
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
package com.humaxdigital.automotive.statusbar.service;

import android.car.hardware.CarPropertyConfig;
import android.car.hardware.hvac.CarHvacManager;
import android.content.Context;

import java.util.List;

public class HvacPolicy {
    public static class SystemStatus {
        enum MuteStatus { AV_MUTE, NAV_MUTE, AV_NAV_MUTE }
        enum BLEStatus { BLE_0, BLE_1, BLE_2, BLE_3 }
        enum BTBatteryStatus { BT_BATTERY_0, BT_BATTERY_1, BT_BATTERY_2, BT_BATTERY_3, BT_BATTERY_4, BT_BATTERY_5 }
        enum BTCallStatus { STREAMING_CONNECTED, HANDS_FREE_CONNECTED, HF_FREE_STREAMING_CONNECTED
            , CALL_HISTORY_DOWNLOADING, CONTACTS_HISTORY_DOWNLOADING, TMU_CALLING, BT_CALLING, BT_PHONE_MIC_MUTE }
        enum AntennaStatus { BT_ANTENNA_NO, BT_ANTENNA_0, BT_ANTENNA_1, BT_ANTENNA_2, BT_ANTENNA_3, BT_ANTENNA_4, BT_ANTENNA_5
            , TMU_ANTENNA_NO, TMU_ANTENNA_0, TMU_ANTENNA_1, TMU_ANTENNA_2, TMU_ANTENNA_3, TMU_ANTENNA_4, TMU_ANTENNA_5}
        enum DataStatus { DATA_4G, DATA_4G_NO, DATA_E, DATA_E_NO }
        enum WifiStatus { WIFI_1, WIFI_2, WIFI_3, WIFI_4 }
        enum WirelessChargeStatus { WIRELESS_CHARGING, WIRELESS_CHARGE_100, WIRELESS_CHARGING_ERROR }
        enum ModeStatus { LOCATION_SHARING, QUIET_MODE }
    }
    public static class ClimateStatus {
        enum SeatStatus { NONE, HEATER1, HEATER2, HEATER3, COOLER1, COOLER2, COOLER3 }
        enum IntakeStatus { FRE, REC }
        enum ClimateModeStatus { BELOW, MIDDLE, BELOW_MIDDLE, BELOW_WINDOW }
        enum BlowerSpeedStatus { STEP_1, STEP_2, STEP_3, STEP_4, STEP_5, STEP_6, STEP_7, STEP_8 }
    }

    private float mMaxHardwareTemp;
    private float mMinHardwareTemp;
    private int mMaxHardwareFanSpeed;

    private final boolean mHardwareUsesCelsius;
    private final boolean mUserUsesCelsius;

    public HvacPolicy(Context context, List<CarPropertyConfig> properties) {
        //TODO handle max / min per each zone
        for (CarPropertyConfig config : properties) {
            switch (config.getPropertyId()) {
                case CarHvacManager.ID_ZONED_FAN_SPEED_SETPOINT: {
                    mMaxHardwareFanSpeed = (int) config.getMaxValue();
                } break;

                case CarHvacManager.ID_ZONED_TEMP_SETPOINT: {
                    mMaxHardwareTemp = (float) config.getMaxValue();
                    mMinHardwareTemp = (float) config.getMinValue();
                } break;
            }
        }

        mHardwareUsesCelsius = true;//context.getResources().getBoolean(R.bool.config_hardwareUsesCelsius);
        mUserUsesCelsius = false;//context.getResources().getBoolean(R.bool.config_userUsesCelsius);
    }

    /*
    public float userToHardwareTemp(int temp) {
        if (!mUserUsesCelsius && mHardwareUsesCelsius) {
            return fahrenheitToCelsius(temp);
        }

        if (mUserUsesCelsius && !mHardwareUsesCelsius) {
            return celsiusToFahrenheit(temp);
        }

        return temp;
    }

    public int hardwareToUserTemp(float temp) {
        if (mHardwareUsesCelsius && !mUserUsesCelsius) {
            return (int) celsiusToFahrenheit(temp);
        }

        if (!mHardwareUsesCelsius && mUserUsesCelsius) {
            return (int) fahrenheitToCelsius(temp);
        }

        return (int) temp;
    }
*/
    public float hardwareToUserTemp(float temp) {
        float _temp = temp; 
        // todo : check celsius, fahrenheit 
        //_temp = celsiusToFahrenheit(temp);
        //_temp = fahrenheitToCelsius(temp);
        return _temp; 
    }

    private float celsiusToFahrenheit(float c) {
        return c * 9 / 5 + 32;
    }

    private float fahrenheitToCelsius(float f) {
        return (f - 32) * 5 / 9;
    }

    public int userToHardwareFanSpeed(int speed) {
        return getMaxHardwareFanSpeed() * speed / 100;
    }

    public int hardwareToUserFanSpeed(int speed) {
        return speed * 100 / getMaxHardwareFanSpeed();
    }

    public int getMaxHardwareFanSpeed() {
        return mMaxHardwareFanSpeed;
    }

    public float getMaxHardwareTemp() {
        return mMaxHardwareTemp;
    }

    public float getMinHardwareTemp() {
        return mMinHardwareTemp;
    }
}
