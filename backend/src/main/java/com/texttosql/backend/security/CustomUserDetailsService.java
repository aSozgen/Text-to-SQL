package com.texttosql.backend.security;

import com.texttosql.backend.entity.UserEntity;
import com.texttosql.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        UserEntity user = userRepository.findByUsernameAndActiveTrue(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        return CustomUserDetails.fromUserEntity(user);
    }
}
