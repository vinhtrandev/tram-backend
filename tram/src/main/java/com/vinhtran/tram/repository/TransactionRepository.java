// com/vinhtran/tram/repository/TransactionRepository.java
package com.vinhtran.tram.repository;

import com.vinhtran.tram.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findTop50ByUserNicknameOrderByCreatedAtDesc(String nickname);
}