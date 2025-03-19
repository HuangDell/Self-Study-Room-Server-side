package com.studyroom.service;

import com.studyroom.model.Admin;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CompositeUserDetailsService implements UserDetailsService {


    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UsernameNotFoundException lastException = null;

        try {
            // TODO
        } catch (UsernameNotFoundException e) {
            lastException = e;
            // 继续尝试下一个服务
        }

        // 如果所有服务都未找到用户，抛出最后捕获的异常
        if (lastException != null) {
            throw lastException;
        }

        throw new UsernameNotFoundException("User not found: " + username);
    }
}