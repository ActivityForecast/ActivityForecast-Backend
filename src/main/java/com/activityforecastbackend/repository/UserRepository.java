package com.activityforecastbackend.repository;

import com.activityforecastbackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailAndIsDeletedFalse(String email);
    
    Optional<User> findByUserIdAndIsDeletedFalse(Long userId);
    
    Optional<User> findByProviderAndProviderIdAndIsDeletedFalse(String provider, String providerId);
    
    boolean existsByEmailAndIsDeletedFalse(String email);
    
    List<User> findByIsDeletedFalse();
    
    List<User> findByRoleAndIsDeletedFalse(User.UserRole role);
    
    @Query("SELECT u FROM User u WHERE u.isDeleted = false AND u.lastLoginAt >= :since")
    List<User> findActiveUsersSince(@Param("since") LocalDateTime since);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.isDeleted = false")
    long countActiveUsers();
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.isDeleted = false AND u.createdAt >= :since")
    long countNewUsersSince(@Param("since") LocalDateTime since);
}