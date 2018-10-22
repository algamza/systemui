
package com.humaxdigital.automotive.statusbar.controllers;

import com.humaxdigital.automotive.statusbar.IStatusBarService;

public interface BaseController {
    public void init(IStatusBarService service); 
    public void deinit(); 
    public void update(); 
}