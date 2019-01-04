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

import android.os.SystemClock;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.util.SparseLongArray;

import java.util.concurrent.TimeUnit;

import javax.annotation.concurrent.GuardedBy;

/**
 * The hvac unit can be controller from two places, the ui and the hardware buttons. Each of these
 * request updates to the current state from different threads. Moreover, there can be conditions
 * where the hvac could send spurious updates so this class routes everything through and coalesces
 * them, keeping the application's view of the world sane.
 */
public class DataStore {
    private static final long COALESCE_TIME_MS = TimeUnit.SECONDS.toMillis(2);

    @GuardedBy("mTemperature")
    private SparseArray<Integer> mTemperature = new SparseArray<Integer>();
    @GuardedBy("mFanSpeed")
    private Integer mFanSpeed = 0;
    @GuardedBy("mAirflow")
    private SparseIntArray mAirflow = new SparseIntArray();
    @GuardedBy("mFanDirection")
    private Integer mFanDirection = 0;
    @GuardedBy("mDefrosterState")
    private SparseBooleanArray mDefrosterState = new SparseBooleanArray();
    @GuardedBy("mAcState")
    private Boolean mAcState = false;
    @GuardedBy("mSeatWarmerLevel")
    private SparseIntArray mSeatWarmerLevel = new SparseIntArray();
    @GuardedBy("mAirCirculationState")
    private Boolean mAirCirculationState = false;
    @GuardedBy("mAirConditionerState")
    private Boolean mAirConditionerState = false;
    @GuardedBy("mAirCleaningState")
    private Integer mAirCleaningState = 0;
    @GuardedBy("mAutoModeState")
    private Boolean mAutoModeState = false;
    @GuardedBy("mHvacPowerState")
    private Boolean mHvacPowerState = false;
    @GuardedBy("mUserId")
    private Integer mUserId = 0;
    @GuardedBy("mLocationShare")
    private Boolean mLocationShare = false; 
    @GuardedBy("mWifiLevel")
    private Integer mWifiLevel = 0;
    @GuardedBy("mDateTime")
    private String mDateTime = ""; 
    @GuardedBy("mNetworkDataType")
    private Integer mNetworkDataType = 0; 
    @GuardedBy("mNetworkDataUsing")
    private Boolean mNetworkDataUsing = false; 
    @GuardedBy("mBTDeviceConnectionState")
    private SparseIntArray mBTDeviceConnectionState = new SparseIntArray();
    @GuardedBy("mBTDeviceAntennaLevel")
    private SparseIntArray mBTDeviceAntennaLevel = new SparseIntArray();
    @GuardedBy("mBTDeviceBatteryState")
    private SparseIntArray mBTDeviceBatteryState = new SparseIntArray();
    @GuardedBy("mCallingState")
    private SparseIntArray mCallingState= new SparseIntArray();
    @GuardedBy("mBLEStatus")
    private Integer mBLEStatus = 0; 
    @GuardedBy("mLocationSharingStatus")
    private Integer mLocationSharingStatus = 0; 
    @GuardedBy("mWirelessChargeStatus")
    private Integer mWirelessChargeStatus = 0; 

    @GuardedBy("mTemperature")
    private SparseLongArray mLastTemperatureSet = new SparseLongArray();
    @GuardedBy("mFanSpeed")
    private long mLastFanSpeedSet;
    @GuardedBy("mAirflow")
    private SparseLongArray mLastAirflowSet = new SparseLongArray();
    @GuardedBy("mFanDirection")
    private long mLastFanDirectionSet;
    @GuardedBy("mDefrosterState")
    private SparseLongArray mLastDefrosterSet = new SparseLongArray();
    @GuardedBy("mAcState")
    private long mLastAcSet;
    @GuardedBy("mSeatWarmerLevel")
    private SparseLongArray mLastSeatWarmerLevel = new SparseLongArray();
    @GuardedBy("mAirCirculationState")
    private long mAirCirculationLastSet;
    @GuardedBy("mAirConditionerState")
    private long mAirConditionerLastSet;
    @GuardedBy("mAirCleaningState")
    private long mAirCleaningStateLastSet;
    @GuardedBy("mAutoModeState")
    private long mAutoModeLastSet;
    @GuardedBy("mHvacPowerState")
    private long mHvacPowerLastSet;
    @GuardedBy("mUserId")
    private long mUserIdLastSet;
    @GuardedBy("mLocationShare")
    private long mLocationShareLastSet; 
    @GuardedBy("mWifiLevel")
    private long mWifiLevelLastSet; 
    @GuardedBy("mDateTime")
    private long mDateTimeLastSet; 
    @GuardedBy("mNetworkDataType")
    private long mNetworkDataTypeLastSet; 
    @GuardedBy("mBTDeviceConnectionState")
    private SparseLongArray mBTDeviceConnectionStateLastSet = new SparseLongArray();
    @GuardedBy("mBTDeviceAntennaLevel")
    private SparseLongArray mBTDeviceAntennaLevelLastSet = new SparseLongArray();
    @GuardedBy("mBTDeviceBatteryState")
    private SparseLongArray mBTDeviceBatteryStateLastSet = new SparseLongArray();
    @GuardedBy("mCallingState")
    private SparseLongArray mCallingStateLastSet = new SparseLongArray();
    @GuardedBy("mBLEStatus")
    private long mBLEStatusLastSet = 0; 
    @GuardedBy("mLocationSharingStatus")
    private long mLocationSharingStatusLastSet = 0; 
    @GuardedBy("mWirelessChargeStatus")
    private long mWirelessChargeStatusLastSet = 0; 

    public int getNetworkDataType() {
        synchronized (mNetworkDataType) {
            return mNetworkDataType;
        }
    }

    public boolean getNetworkDataUsing() {
        synchronized (mNetworkDataUsing) {
            return mNetworkDataUsing;
        }
    }

    public void setNetworkData(int type, boolean using) {
        synchronized (mNetworkDataType) {
            mNetworkDataType = type;
            synchronized (mNetworkDataUsing) {
                mNetworkDataUsing = using; 
            }
            mNetworkDataTypeLastSet = SystemClock.uptimeMillis();
        }
    }

    public boolean shouldPropagateNetworkDataUpdate(int type, boolean using) {
        synchronized (mNetworkDataType) {
            if (SystemClock.uptimeMillis() - mNetworkDataTypeLastSet < COALESCE_TIME_MS) {
                return false;
            }
            if ( mNetworkDataUsing == using && mNetworkDataType == type) return false; 
            mNetworkDataType = type;
            mNetworkDataUsing = using;
        }
        return true;
    }

    public String getDateTime() {
        synchronized (mDateTime) {
            return mDateTime;
        }
    }

    public void setDateTime(String date) {
        synchronized (mDateTime) {
            mDateTime = date;
            mDateTimeLastSet = SystemClock.uptimeMillis();
        }
    }

    public boolean shouldPropagateDateTimeUpdate(String date) {
        synchronized (mDateTime) {
            if (SystemClock.uptimeMillis() - mDateTimeLastSet < COALESCE_TIME_MS) {
                return false;
            }
            if ( mDateTime.equals(date) ) return false; 
            mDateTime = date;
        }
        return true;
    }

    public int getWifiLevel() {
        synchronized (mWifiLevel) {
            return mWifiLevel;
        }
    }

    public void setWifiLevel(int level) {
        synchronized (mWifiLevel) {
            mWifiLevel = level;
            mWifiLevelLastSet = SystemClock.uptimeMillis();
        }
    }

    public boolean shouldPropagateWifiLevelUpdate(int level) {
        synchronized (mWifiLevel) {
            if (SystemClock.uptimeMillis() - mWifiLevelLastSet < COALESCE_TIME_MS) {
                return false;
            }
            if ( mWifiLevel == level ) return false; 
            mWifiLevel = level;
        }
        return true;
    }

    public boolean getLocationShareState() {
        synchronized (mLocationShare) {
            return mLocationShare;
        }
    }

    public void setLocationShareState(boolean share) {
        synchronized (mLocationShare) {
            mLocationShare = share;
            mLocationShareLastSet = SystemClock.uptimeMillis();
        }
    }

    public boolean shouldPropagateLocationShareUpdate(boolean share) {
        synchronized (mLocationShare) {
            if (SystemClock.uptimeMillis() - mLocationShareLastSet < COALESCE_TIME_MS) {
                return false;
            }
            if ( mLocationShare == share ) return false; 
            mLocationShare = share;
        }
        return true;
    }

    public int getStatusUserId() {
        synchronized (mUserId) {
            return mUserId;
        }
    }

    public void setStatusUserId(int userid) {
        synchronized (mUserId) {
            mUserId = userid;
            mUserIdLastSet = SystemClock.uptimeMillis();
        }
    }

    public boolean shouldPropagateStatusUserIdUpdate(int userid) {
        synchronized (mUserId) {
            if (SystemClock.uptimeMillis() - mUserIdLastSet < COALESCE_TIME_MS) {
                return false;
            }
            if ( mUserId == userid ) return false; 
            mUserId = userid;
        }
        return true;
    }

    public int getTemperature(int zone) {
        synchronized (mTemperature) {
            return mTemperature.get(zone);
        }
    }

    public void setTemperature(int zone, int temperature) {
        synchronized (mTemperature) {
            mTemperature.put(zone, temperature);
            mLastTemperatureSet.put(zone, SystemClock.uptimeMillis());
        }
    }

    public boolean shouldPropagateTempUpdate(int zone, int temperature) {
        synchronized (mTemperature) {
            if (SystemClock.uptimeMillis() - mLastTemperatureSet.get(zone) < COALESCE_TIME_MS) {
                return false;
            }
            setTemperature(zone, temperature);
        }
        return true;
    }

    public boolean getDefrosterState(int zone) {
        synchronized (mDefrosterState) {
            return mDefrosterState.get(zone);
        }
    }

    public void setDefrosterState(int zone, boolean state) {
        synchronized (mDefrosterState) {
            mDefrosterState.put(zone, state);
            mLastDefrosterSet.put(zone, SystemClock.uptimeMillis());
        }
    }

    public boolean shouldPropagateDefrosterUpdate(int zone, boolean defrosterState) {
        synchronized (mDefrosterState) {
            if (SystemClock.uptimeMillis() - mLastDefrosterSet.get(zone) < COALESCE_TIME_MS) {
                return false;
            }
            mDefrosterState.put(zone, defrosterState);
        }
        return true;
    }

    public int getFanSpeed() {
        synchronized (mFanSpeed) {
            return mFanSpeed;
        }
    }

    public void setFanSpeed(int speed) {
        synchronized (mFanSpeed) {
            mFanSpeed = speed;
            mLastFanSpeedSet = SystemClock.uptimeMillis();
        }
    }

    public boolean shouldPropagateFanSpeedUpdate(int zone, int speed) {
        // TODO: We ignore fan speed zones for now because we dont have a multi zone car.
        synchronized (mFanSpeed) {
            if (SystemClock.uptimeMillis() - mLastFanSpeedSet < COALESCE_TIME_MS) {
                return false;
            }
            if ( mFanSpeed == speed ) return false; 
            mFanSpeed = speed;
        }
        return true;
    }

    public int getFanDirection() {
        synchronized (mFanDirection) {
            return mFanDirection;
        }
    }

    public void setFanDirection(int direction) {
        synchronized (mFanDirection) {
            mFanDirection = direction;
            mLastFanDirectionSet = SystemClock.uptimeMillis();
        }
    }

    public boolean shouldPropagateFanDirectionUpdate(int direction) {
        synchronized (mFanDirection) {
            if (SystemClock.uptimeMillis() - mLastFanDirectionSet < COALESCE_TIME_MS) {
                return false;
            }
            mFanDirection = direction;
        }
        return true;
    }

    public boolean getAcState() {
        synchronized (mAcState) {
            return mAcState;
        }
    }

    public void setAcState(boolean acState) {
        synchronized (mAcState) {
            mAcState = acState;
            mLastAcSet = SystemClock.uptimeMillis();
        }
    }

    public boolean shouldPropagateAcUpdate(boolean acState) {
        synchronized (mAcState) {
            if (SystemClock.uptimeMillis() - mLastAcSet < COALESCE_TIME_MS) {
                return false;
            }
            if ( mAcState == acState ) return false; 
            mAcState = acState;
        }
        return true;
    }

    public int getAirflow(int zone) {
        synchronized (mAirflow) {
            return mAirflow.get(zone);
        }
    }

    public void setAirflow(int zone, int index) {
        synchronized (mAirflow) {
            mAirflow.put(zone, index);
            mLastAirflowSet.put(zone, SystemClock.uptimeMillis());
        }
    }

    public boolean shouldPropagateFanPositionUpdate(int zone, int index) {
        synchronized (mAirflow) {
            if (SystemClock.uptimeMillis() - mLastAirflowSet.get(zone) < COALESCE_TIME_MS) {
                return false;
            }
            mAirflow.put(zone, index);
        }
        return true;
    }

    public int getSeatWarmerLevel(int zone) {
        synchronized (mSeatWarmerLevel) {
            return mSeatWarmerLevel.get(zone);
        }
    }

    public void setSeatWarmerLevel(int zone, int level) {
        synchronized (mSeatWarmerLevel) {
            mSeatWarmerLevel.put(zone, level);
            mLastSeatWarmerLevel.put(zone, SystemClock.uptimeMillis());
        }
    }

    public boolean shouldPropagateSeatWarmerLevelUpdate(int zone, int level) {
        synchronized (mSeatWarmerLevel) {
            if (SystemClock.uptimeMillis() - mLastSeatWarmerLevel.get(zone) < COALESCE_TIME_MS) {
                return false;
            }
            mSeatWarmerLevel.put(zone, level);
        }
        return true;
    }

    public boolean getAirCirculationState() {
        synchronized (mAirCirculationState) {
            return mAirCirculationState;
        }
    }

    public void setAirCirculationState(boolean airCirculationState) {
        synchronized (mAirCirculationState) {
            mAirCirculationState = airCirculationState;
            mAirCirculationLastSet = SystemClock.uptimeMillis();
        }
    }

    public boolean shouldPropagateAirCirculationUpdate(boolean airCirculationState) {
        synchronized (mAirCirculationState) {
            if (SystemClock.uptimeMillis() - mAirCirculationLastSet < COALESCE_TIME_MS) {
                return false;
            }
            if ( mAirCirculationState == airCirculationState ) return false; 
            mAirCirculationState = airCirculationState;
        }
        return true;
    }

    public boolean getAirConditionerState() {
        synchronized (mAirConditionerState) {
            return mAirConditionerState;
        }
    }

    public void setAirConditionerState(boolean airConditionerState) {
        synchronized (mAirConditionerState) {
            mAirConditionerState = airConditionerState;
            mAirConditionerLastSet = SystemClock.uptimeMillis();
        }
    }

    public boolean shouldPropagateAirConditionerUpdate(boolean airConditionerState) {
        synchronized (mAirCirculationState) {
            if (SystemClock.uptimeMillis() - mAirConditionerLastSet < COALESCE_TIME_MS) {
                return false;
            }
            if ( mAirConditionerState == airConditionerState ) return false; 
            mAirConditionerState = airConditionerState;
        }
        return true;
    }

    public Integer getAirCleaningState() {
        synchronized (mAirCleaningState) {
            return mAirCleaningState;
        }
    }

    public void setAirCleaningState(int airCleaningState) {
        synchronized (mAirCleaningState) {
            mAirCleaningState = airCleaningState;
            mAirCleaningStateLastSet = SystemClock.uptimeMillis();
        }
    }

    public Boolean shouldPropagateAirCleaningStateUpdate(int airCleaningState) {
        synchronized (mAirCleaningState) {
            if (SystemClock.uptimeMillis() - mAirCleaningStateLastSet < COALESCE_TIME_MS) {
                return false;
            }
            if ( mAirCleaningState == airCleaningState ) return false; 
            mAirCleaningState = airCleaningState;
        }
        return true;
    }

    public boolean getAutoModeState() {
        synchronized (mAutoModeState) {
            return mAutoModeState;
        }
    }

    public void setAutoModeState(boolean autoModeState) {
        synchronized (mAutoModeState) {
            mAutoModeState = autoModeState;
            mAutoModeLastSet = SystemClock.uptimeMillis();
        }
    }

    public boolean shouldPropagateAutoModeUpdate(boolean autoModeState) {
        synchronized (mAutoModeState) {
            if (SystemClock.uptimeMillis() - mAutoModeLastSet < COALESCE_TIME_MS) {
                return false;
            }
            if ( mAcState == autoModeState ) return false; 
            mAcState = autoModeState;
        }
        return true;
    }

    public boolean getHvacPowerState() {
        synchronized (mHvacPowerState) {
            return mHvacPowerState;
        }
    }

    public void setHvacPowerState(boolean hvacPowerState) {
        synchronized (mHvacPowerState) {
            mHvacPowerState = hvacPowerState;
            mHvacPowerLastSet = SystemClock.uptimeMillis();
        }
    }

    public boolean shouldPropagateHvacPowerUpdate(boolean hvacPowerState) {
        synchronized (mHvacPowerState) {
            if (SystemClock.uptimeMillis() - mHvacPowerLastSet < COALESCE_TIME_MS) {
                return false;
            }
            if ( mHvacPowerState == hvacPowerState ) return false; 
            mHvacPowerState = hvacPowerState;
        }
        return true;
    }

    public int getBTDeviceConnectionState(int profile) {
        synchronized (mBTDeviceConnectionState) {
            return mBTDeviceConnectionState.get(profile);
        }
    }

    public void setBTDeviceConnectionState(int profile, int state) {
        synchronized (mBTDeviceConnectionState) {
            mBTDeviceConnectionState.put(profile, state);
            mBTDeviceConnectionStateLastSet.put(profile, SystemClock.uptimeMillis());
        }
    }

    public boolean shouldPropagateBTDeviceConnectionStateUpdate(int profile, int state) {
        synchronized (mBTDeviceConnectionState) {
            if (SystemClock.uptimeMillis() - mBTDeviceConnectionStateLastSet.get(profile) < COALESCE_TIME_MS) {
                return false;
            }
            if ( mBTDeviceConnectionState.get(profile) == state ) return false; 
            mBTDeviceConnectionState.put(profile, state);
        }
        return true;
    }

    public int getBTDeviceAntennaLevel(int profile) {
        synchronized (mBTDeviceAntennaLevel) {
            return mBTDeviceAntennaLevel.get(profile);
        }
    }

    public void setBTDeviceAntennaLevel(int profile, int state) {
        synchronized (mBTDeviceAntennaLevel) {
            mBTDeviceAntennaLevel.put(profile, state);
            mBTDeviceAntennaLevelLastSet.put(profile, SystemClock.uptimeMillis());
        }
    }

    public boolean shouldPropagateBTDeviceAntennaLevelUpdate(int profile, int state) {
        synchronized (mBTDeviceAntennaLevel) {
            if (SystemClock.uptimeMillis() - mBTDeviceAntennaLevelLastSet.get(profile) < COALESCE_TIME_MS) {
                return false;
            }
            if ( mBTDeviceAntennaLevel.get(profile) == state ) return false; 
            mBTDeviceAntennaLevel.put(profile, state);
        }
        return true;
    }

    public int getBTDeviceBatteryState(int profile) {
        synchronized (mBTDeviceBatteryState) {
            return mBTDeviceBatteryState.get(profile);
        }
    }

    public void setBTDeviceBatteryState(int profile, int state) {
        synchronized (mBTDeviceBatteryState) {
            mBTDeviceBatteryState.put(profile, state);
            mBTDeviceBatteryStateLastSet.put(profile, SystemClock.uptimeMillis());
        }
    }

    public boolean shouldPropagateBTDeviceBatteryStateUpdate(int profile, int state) {
        synchronized (mBTDeviceBatteryState) {
            if (SystemClock.uptimeMillis() - mBTDeviceBatteryStateLastSet.get(profile) < COALESCE_TIME_MS) {
                return false;
            }
            if ( mBTDeviceBatteryState.get(profile) == state ) return false; 
            mBTDeviceBatteryState.put(profile, state);
        }
        return true;
    }

    public int getBTCallingState(int mode) {
        synchronized (mCallingState) {
            return mCallingState.get(mode);
        }
    }

    public void setBTCallingState(int mode, int state) {
        synchronized (mCallingState) {
            mCallingState.put(mode, state);
            mCallingStateLastSet.put(mode, SystemClock.uptimeMillis());
        }
    }

    public boolean shouldPropagateBTCallingStateUpdate(int mode, int state) {
        synchronized (mCallingState) {
            if (SystemClock.uptimeMillis() - mCallingStateLastSet.get(mode) < COALESCE_TIME_MS) {
                return false;
            }
            if ( mCallingState.get(mode) == state ) return false; 
            mCallingState.put(mode, state);
        }
        return true;
    }

    public int getBLEState() {
        synchronized (mBLEStatus) {
            return mBLEStatus;
        }
    }

    public void setBLEState(int state) {
        synchronized (mBLEStatus) {
            mBLEStatus = state;
            mBLEStatusLastSet = SystemClock.uptimeMillis();
        }
    }

    public boolean shouldPropagateBLEStatusUpdate(int state) {
        synchronized (mBLEStatus) {
            if (SystemClock.uptimeMillis() - mBLEStatusLastSet < COALESCE_TIME_MS) {
                return false;
            }
            if ( mBLEStatus == state ) return false; 
            mBLEStatus = state;
        }
        return true;
    }

    public int getLocationSharingState() {
        synchronized (mLocationSharingStatus) {
            return mLocationSharingStatus;
        }
    }

    public void setLocationSharingState(int state) {
        synchronized (mLocationSharingStatus) {
            mLocationSharingStatus = state;
            mLocationSharingStatusLastSet = SystemClock.uptimeMillis();
        }
    }

    public boolean shouldPropagateLocationSharingStatusUpdate(int state) {
        synchronized (mLocationSharingStatus) {
            if (SystemClock.uptimeMillis() - mLocationSharingStatusLastSet < COALESCE_TIME_MS) {
                return false;
            }
            if ( mLocationSharingStatus == state ) return false; 
            mLocationSharingStatus = state;
        }
        return true;
    }

    public int getWirelessChargeState() {
        synchronized (mWirelessChargeStatus) {
            return mWirelessChargeStatus;
        }
    }

    public void setWirelessChargeState(int state) {
        synchronized (mLocationSharingStatus) {
            mWirelessChargeStatus = state;
            mWirelessChargeStatusLastSet = SystemClock.uptimeMillis();
        }
    }

    public boolean shouldPropagateWirelessChargeStatusUpdate(int state) {
        synchronized (mWirelessChargeStatus) {
            if (SystemClock.uptimeMillis() - mWirelessChargeStatusLastSet < COALESCE_TIME_MS) {
                return false;
            }
            if ( mWirelessChargeStatus == state ) return false; 
            mWirelessChargeStatus = state;
        }
        return true;
    }
}
