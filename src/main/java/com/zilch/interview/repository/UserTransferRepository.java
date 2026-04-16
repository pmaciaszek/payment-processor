package com.zilch.interview.repository;

import com.zilch.interview.entity.UserTransferEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserTransferRepository extends JpaRepository<UserTransferEntity, UUID> {
}
