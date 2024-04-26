package com.guidopierri.pantrybe.services;

import com.guidopierri.pantrybe.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service    // TODO - Is this optional?
public class SpringSecurityUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Autowired
    public SpringSecurityUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Goes inside database -> tries to find user (with username)
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        // TODO - What if we do not FIND user?

        return userRepository.findByUsername(username);
    }

}
