package dev.rupom.project.airbnb.controller;

import dev.rupom.project.airbnb.service.HotelService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class RoomController {
    private final HotelService hotelService;


}
