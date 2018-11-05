package com.humaxdigital.automotive.statusbar.service;

oneway interface IStatusBarCallback {
    void onInitialized();
    void onUpdated();
}