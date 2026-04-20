package com.meydan.meydan.service;

import com.meydan.meydan.dto.response.UserResponseDTO;
import com.meydan.meydan.models.entities.User;
import com.meydan.meydan.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    public Page<UserResponseDTO> findAvailableUsersForClan(Long categoryId, Pageable pageable) {
        return userRepository.findUsersNotInClanByCategory(categoryId, pageable)
                .map(user -> modelMapper.map(user, UserResponseDTO.class));
    }
}