
package com.humaxdigital.automotive.statusbar.controllers;

import com.humaxdigital.automotive.statusbar.service.IStatusBarService;

public interface BaseController {
    public void init(IStatusBarService service); 
    public void deinit(); 
}