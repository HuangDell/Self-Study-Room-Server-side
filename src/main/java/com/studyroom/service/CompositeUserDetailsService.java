package com.studyroom.service;

import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CompositeUserDetailsService implements UserDetailsService {

    private final List<UserDetailsService> userDetailsServices;

    public CompositeUserDetailsService(List<UserDetailsService> userDetailsServices) {
        // 过滤掉自己，防止循环引用
        this.userDetailsServices = userDetailsServices.stream()
                .filter(service -> !(service instanceof CompositeUserDetailsService))
                .toList();
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UsernameNotFoundException lastException = null;

        for (UserDetailsService service : userDetailsServices) {
            try {
                return service.loadUserByUsername(username);
            } catch (UsernameNotFoundException e) {
                lastException = e;
                // 继续尝试下一个服务
            }
        }

        // 如果所有服务都未找到用户，抛出最后捕获的异常
        if (lastException != null) {
            throw lastException;
        }

        throw new UsernameNotFoundException("User not found: " + username);
    }
}