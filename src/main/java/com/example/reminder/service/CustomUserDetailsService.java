package com.example.reminder.service;

import com.example.reminder.model.User;
import com.example.reminder.repository.UserRepository;
import com.example.reminder.security.CustomUserDetails;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private  final UserRepository userRepo;

    public CustomUserDetailsService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername (String email) throws UsernameNotFoundException {
        User user = userRepo.findByEmailWithRoles(email)
                .orElseThrow(() -> new UsernameNotFoundException("No user found with email :" + email));

        return new CustomUserDetails(user);
    }

}
