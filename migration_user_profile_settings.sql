-- UserProfileSettings Tablosu Oluştur
CREATE TABLE IF NOT EXISTS user_profile_settings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    show_profile BOOLEAN NOT NULL DEFAULT true,
    show_clans BOOLEAN NOT NULL DEFAULT true,
    show_ratings BOOLEAN NOT NULL DEFAULT true,
    show_bio BOOLEAN NOT NULL DEFAULT true,
    allow_direct_messages BOOLEAN NOT NULL DEFAULT true,
    bio VARCHAR(500),
    is_private BOOLEAN NOT NULL DEFAULT false,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- İndeks
CREATE INDEX idx_user_profile_settings_user_id ON user_profile_settings(user_id);

-- Var olan kullanıcılar için default ayarlar ekle (opsiyonel)
-- INSERT INTO user_profile_settings (user_id, show_profile, show_clans, show_ratings, show_bio, allow_direct_messages, is_private)
-- SELECT id, true, true, true, true, true, false FROM users
-- ON CONFLICT (user_id) DO NOTHING;

