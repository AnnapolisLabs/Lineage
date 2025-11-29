package com.annapolislabs.lineage.security;

import com.annapolislabs.lineage.entity.User;
import com.annapolislabs.lineage.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Loads {@link UserDetails} instances from the primary {@link UserRepository} so authentication filters can resolve authorities for JWT subjects.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Builds the user-details adapter with access to the backing repository so authentication filters can query
     * accounts on demand.
     *
     * @param userRepository source of persisted user records keyed by email address.
     */
    @Autowired
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Resolves the authenticated principal by email so JWT filters can hydrate SecurityContext with fresh roles.
     *
     * @throws UsernameNotFoundException when no active account owns the requested email.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPasswordHash(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getGlobalRole().name()))
        );
    }
}
