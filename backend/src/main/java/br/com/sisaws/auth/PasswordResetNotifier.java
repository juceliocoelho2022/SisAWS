package br.com.sisaws.auth;

import br.com.sisaws.user.AppUser;

public interface PasswordResetNotifier {
    void sendResetLink(AppUser user, String rawToken);
}
