package com.smartcontact.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.smartcontact.entities.User;
import com.smartcontact.repository.UserRepository;

public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
       User user = userRepository.getUserByUserName(username);
       if(user == null){
        throw new UsernameNotFoundException("Cound not found user");
       }
   
      return new CustomUserPrincipal(user.getEmail(),
        user.getPassword(), 
        AuthorityUtils.createAuthorityList(user.getRole()),
        null,
        null,
        null);
        }

}
