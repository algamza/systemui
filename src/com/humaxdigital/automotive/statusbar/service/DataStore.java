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
    @GuardedBy("mAntennaState")
    private Integer mAntennaState = 0; 
    @GuardedBy("mBTBatteryLevel")
    private Integer mBTBatteryLevel = 0; 
    @GuardedBy("mBTBatteryType")
    private Integer mBTBatteryType = 0; 
    @GuardedBy("mBLEStatus")
    private Integer mBLEStatus = 0; 

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
    @GuardedBy("mAntennaState")
    private long mAntennaLastSet = 0; 
    @GuardedBy("mBTBatteryLevel")
    private long mBTBatteryLevelLastSet = 0; 
    @GuardedBy("mBLEStatus")
    private long mBLEStatusLastSet = 0; 

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

    public int getAntennaState() {
        synchronized (mAntennaState) {
            return mAntennaState;
        }
    }

    public void setAntennaState(int state) {
        synchronized (mAntennaState) {
            mAntennaState = state;
            mAntennaLastSet = SystemClock.uptimeMillis();
        }
    }

    public boolean shouldPropagateAntennaUpdate(int state) {
        synchronized (mAntennaState) {
            if (SystemClock.uptimeMillis() - mAntennaLastSet < COALESCE_TIME_MS) {
                return false;
            }
            if ( mAntennaState == state ) return false; 
            mAntennaState = state;
        }
        return true;
    }

    public int getBTBatterylevel() {
        synchronized (mBTBatteryLevel) {
            return mBTBatteryLevel;
        }
    }

    public int getBTBatteryType() {
        synchronized (mBTBatteryType) {
            return mBTBatteryType;
        }
    }

    public void setBTBatterylevel(int type, int level) {
        synchronized ( mBTBatteryType ) {
            mBTBatteryType = type; 
            synchronized (mBTBatteryLevel) {
                mBTBatteryLevel = level;
            }
            mBTBatteryLevelLastSet = SystemClock.uptimeMillis();
        }
    }

    public boolean shouldPropagateBTBatteryLevelUpdate(int type, int level) {
        synchronized (mBTBatteryLevel) {
            if (SystemClock.uptimeMillis() - mBTBatteryLevelLastSet < COALESCE_TIME_MS) {
                return false;
            }
            if ( mBTBatteryType == type && mBTBatteryLevel == level ) return false; 
            mBTBatteryType = type; 
            mBTBatteryLevel = level;
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
}
