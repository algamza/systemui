package com.humaxdigital.automotive.statusbar.service;

oneway interface IDateTimeCallback {
    void onDateTimeChanged(String time); 
    void onTimeTypeChanged(String type);
}