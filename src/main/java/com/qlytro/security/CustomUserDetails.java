package com.qlytro.security;

import com.qlytro.entity.NguoiDung;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;

/**
 * Wrapper cua NguoiDung entity cho Spring Security.
 * Tach biet entity khoi tang Security.
 */
public class CustomUserDetails implements UserDetails {

    private final NguoiDung nguoiDung;

    public CustomUserDetails(NguoiDung nguoiDung) {
        this.nguoiDung = nguoiDung;
    }

    public NguoiDung getNguoiDung() {
        return nguoiDung;
    }

    public Integer getId() {
        return nguoiDung.getId();
    }

    public boolean isMustChangePassword() {
        return Boolean.TRUE.equals(nguoiDung.getMustChangePassword());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(nguoiDung.getVaiTro().getSpringRole()));
    }

    @Override
    public String getPassword() {
        return nguoiDung.getPassword();
    }

    @Override
    public String getUsername() {
        return nguoiDung.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return com.qlytro.entity.enums.TrangThaiNguoiDung.ACTIVE.equals(nguoiDung.getTrangThai());
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return com.qlytro.entity.enums.TrangThaiNguoiDung.ACTIVE.equals(nguoiDung.getTrangThai());
    }
}
