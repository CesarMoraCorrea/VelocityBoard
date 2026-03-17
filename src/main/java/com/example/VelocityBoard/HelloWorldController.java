package com.example.VelocityBoard;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloWorldController {

    @GetMapping("/hello")
    public String holaMundo() {
        return "¡Hola Mundo! 🚀 Todo funciona muuy bien good!";
    }
}
