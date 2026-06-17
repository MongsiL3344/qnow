package io.github.mongsil3344.qnow.user.api;

import java.io.Serial;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public final class UserPrincipal implements UserDetails {

    @Serial
    private static final long serialVersionUID = 1L;

    private final UUID id;
    private final String email;
    private final String nickname;
    private final String password;
    private final List<GrantedAuthority> authorities;

    public UserPrincipal(UUID id, String email, String nickname, String password) {
        this(id, email, nickname, password, List.of());
    }

    public UserPrincipal(
        UUID id,
        String email,
        String nickname,
        String password,
        Collection<? extends GrantedAuthority> authorities
    ) {
        this.id = Objects.requireNonNull(id);
        this.email = Objects.requireNonNull(email);
        this.nickname = Objects.requireNonNull(nickname);
        this.password = Objects.requireNonNull(password);
        this.authorities = List.copyOf(Objects.requireNonNull(authorities));
    }

    public UUID id() {
        return id;
    }

    public String email() {
        return email;
    }

    public String nickname() {
        return nickname;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
