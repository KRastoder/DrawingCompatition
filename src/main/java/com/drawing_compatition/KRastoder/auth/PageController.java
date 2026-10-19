package com.drawing_compatition.KRastoder.auth;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/login")
    public String loginPage() {
        return "forward:/login.html";
    }

    @GetMapping("/")
    public String homePage() {
        return "forward:/home.html";
    }

    @GetMapping("/newdrawing")
    public String newDrawingPage() {
        return "forward:/newdrawing.html";
    }

    @GetMapping("/my-drawings")
    public String myDrawingsPage() {
        return "forward:/my-drawings.html";
    }
}
