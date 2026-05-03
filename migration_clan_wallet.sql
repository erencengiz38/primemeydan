-- Clan Tablosuna Kasa (Wallet) Alanlarını Ekle
ALTER TABLE clan
ADD COLUMN IF NOT EXISTS real_balance DOUBLE PRECISION DEFAULT 0.0 NOT NULL,
ADD COLUMN IF NOT EXISTS meydan_coin DOUBLE PRECISION DEFAULT 0.0 NOT NULL,
ADD COLUMN IF NOT EXISTS version INTEGER DEFAULT 0;

-- İndeks (opsiyonel)
CREATE INDEX IF NOT EXISTS idx_clan_meydan_coin ON clan(meydan_coin);
CREATE INDEX IF NOT EXISTS idx_clan_real_balance ON clan(real_balance);

-- Eğer version sütunu yoksa ekle (Optimistic Locking için)
ALTER TABLE clan
ADD COLUMN IF NOT EXISTS version INTEGER DEFAULT 0;

