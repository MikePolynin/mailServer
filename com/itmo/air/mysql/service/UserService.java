package com.itmo.air.mysql.service;

import com.itmo.air.mysql.entity.Account;
import com.itmo.air.mysql.entity.User;
import com.itmo.air.mysql.repo.AccountRepository;
import com.itmo.air.mysql.repo.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class UserService {

    public enum Result {
        User_already_exist,
        User_created,
        Wrong_secureWord,
        Password_changed,
        No_such_user,
        User_updated,
        Success,
        Wrong_password,
        User_deleted,
        Unauthorized_403
    }

    @Autowired
    UserRepository userRepository;
    @Autowired
    AccountRepository accountRepository;
    @Autowired
    CodeService codeService;

    Long milliSecTo30Day  = 2592000000L;

    public Page<User> findAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public Result save(User user) {
        if (!userRepository.findByUserName(user.getUserName()).isPresent()) {
            user.setPassword(codeService.encode(user.getPassword()));
            userRepository.save(user);
            Account newAccount = new Account();
            newAccount.setNicName(user.getUserName());
            newAccount.setPassword(user.getPassword());
            newAccount.setUser(user);
            accountRepository.save(newAccount);
            return Result.User_created;
        } else {
            return Result.User_already_exist;
        }
    }

    public Result change(Long userId, User user) {
        if (userRepository.findById(userId).isPresent()) {
            User newUser = userRepository.findById(userId).get();
            if (!newUser.getUserName().equals(user.getUserName())) {
                return Result.No_such_user;
            } else if (!newUser.getSecureWord().equals(user.getSecureWord())) {
                return Result.Wrong_secureWord;
            } else {
                newUser.setPassword(codeService.encode(user.getPassword()));
                userRepository.save(newUser);
                return Result.Password_changed;
            }
        } else {
            return Result.No_such_user;
        }
    }

    public Result update(Long userId, User user) {
        if (userRepository.findById(userId).isPresent()) {
            User newUser = userRepository.findById(userId).get();
            if (user.getToken().equals(newUser.getToken())) {
                if (!newUser.getUserName().equals(user.getUserName())) {
                    return Result.No_such_user;
                } else {
                    newUser.setFirstName(user.getFirstName());
                    newUser.setLastName(user.getLastName());
                    newUser.setPassword(codeService.encode(user.getPassword()));
                    userRepository.save(newUser);
                    return Result.User_updated;
                }
            } else {
                return Result.Unauthorized_403;
            }
        } else {
            return Result.No_such_user;
        }
    }

    public Result delete(Long userId, User user) {
        if (userRepository.findById(userId).isPresent()) {
            User newUser = userRepository.findById(userId).get();
            if (user.getToken().equals(newUser.getToken())) {
                if (!newUser.getUserName().equals(user.getUserName())) {
                    return Result.No_such_user;
                } else if (!newUser.getSecureWord().equals(user.getSecureWord())) {
                    return Result.Wrong_secureWord;
                } else {
                    newUser.getAccounts().clear();
                    userRepository.delete(newUser);
                    return Result.User_deleted;
                }
            } else {
                return Result.Unauthorized_403;
            }
        } else {
            return Result.No_such_user;
        }
    }

    public Result logIn(User user) {
        if (userRepository.findByUserName(user.getUserName()).isPresent()) {
            if (user.getToken() != null) {
                if (user.getToken().equals(userRepository.findByUserName(user.getUserName()).get().getToken()) &
                        (System.currentTimeMillis() - user.getTokenTime()) <= milliSecTo30Day ) {
                    return Result.Success;
                } else {
                    String token = codeService.encode(codeService.encode(user.getUserName() + user.getPassword() + System.currentTimeMillis()));
                    user.setToken(token);
                    user.setTokenTime(System.currentTimeMillis());
                    return Result.Success;
                }
            } else if (userRepository.findByUserName(user.getUserName()).get().getPassword().equals(codeService.encode(user.getPassword()))) {
                String token = codeService.encode(codeService.encode(user.getUserName() + user.getPassword() + System.currentTimeMillis()));
                user.setToken(token);
                user.setTokenTime(System.currentTimeMillis());
                return Result.Success;
            } else {
                return Result.Wrong_password;
            }
        } else {
            return Result.No_such_user;
        }
    }

    public Result logOff(User user) {
        if (userRepository.findByUserName(user.getUserName()).isPresent()) {
            User newUser = userRepository.findByUserName(user.getUserName()).get();
            if (user.getToken().equals(newUser.getToken())) {
                user.setToken(null);
                return Result.Success;
            } else {
                return Result.Unauthorized_403;
            }
        } else {
            return Result.No_such_user;
        }
    }
}