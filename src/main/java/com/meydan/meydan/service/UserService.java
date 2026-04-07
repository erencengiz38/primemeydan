package com.meydan.meydan.service;

import com.meydan.meydan.models.entities.User;
import com.meydan.meydan.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public Page<User> findAvailableUsersForClan(Long categoryId, Pageable pageable) {
        return userRepository.findUsersNotInClanByCategory(categoryId, pageable);
    }
}
