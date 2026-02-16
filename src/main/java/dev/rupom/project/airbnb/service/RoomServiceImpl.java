package dev.rupom.project.airbnb.service;

import dev.rupom.project.airbnb.mapper.RoomMapper;
import dev.rupom.project.airbnb.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService{
    private final RoomRepository roomRepository;
    private final RoomMapper mapper;
}
