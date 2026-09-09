package com.bankapp.banking.repository;

import com.bankapp.banking.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    // ----------------------------------------------------------------
    // Ownership-scoped lookups.
    //
    // Account.user is FetchType.LAZY (kept that way deliberately - see
    // AccountService). Rather than loading an Account and then reading
    // account.getUser().getId() to check ownership - which triggers a
    // lazy-init query and can blow up with LazyInitializationException
    // once the Hibernate session that loaded the entity is gone - we push
    // the ownership check down into the query itself. These methods never
    // touch the `user` association, so they're always safe to call.
    // ----------------------------------------------------------------

    Optional<Account> findByIdAndUserId(Long id, Long userId);

    boolean existsByIdAndUserId(Long id, Long userId);

    List<Account> findByUserId(Long userId);

    // ----------------------------------------------------------------
    // JOIN FETCH variants - used only where the response genuinely needs
    // user data (e.g. AccountResponse.ownerName). These fetch the User in
    // the same query instead of relying on a second lazy-load, and are
    // still ownership-scoped so a user can never fetch someone else's
    // account by guessing an id.
    // ----------------------------------------------------------------

    @Query("SELECT a FROM Account a JOIN FETCH a.user WHERE a.user.id = :userId")
    List<Account> findByUserIdWithUser(@Param("userId") Long userId);

    @Query("SELECT a FROM Account a JOIN FETCH a.user WHERE a.id = :id AND a.user.id = :userId")
    Optional<Account> findByIdAndUserIdWithUser(@Param("id") Long id, @Param("userId") Long userId);

    @Query("SELECT a FROM Account a JOIN FETCH a.user WHERE a.id = :id")
    Optional<Account> findByIdWithUser(@Param("id") Long id);

    // Pessimistic lock used inside the transfer transaction to prevent
    // lost-update / race conditions when two transfers touch the same account.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.accountNumber = :accountNumber")
    Optional<Account> findByAccountNumberForUpdate(@Param("accountNumber") String accountNumber);
}
