package com.bankapp.banking.entity;

import com.bankapp.banking.entity.enums.Role;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "username"),
        @UniqueConstraint(columnNames = "email")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String fullName;

    @NotBlank
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Email
    @NotBlank
    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @NotBlank
    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @Column(length = 20)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.ROLE_USER;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private boolean enabled = true;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    // Lombok's @Data generates toString/equals/hashCode over every field by default;
    // without these exclusions, User<->Account is a circular reference (Account.user
    // points back here) that would recurse infinitely and throw StackOverflowError
    // the first time toString()/equals() ran on either entity. Association fields are
    // excluded from generated toString/equals/hashCode on both sides for this reason.
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Account> accounts = new ArrayList<>();
}
