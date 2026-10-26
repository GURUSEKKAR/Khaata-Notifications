-- =====================================================================
-- V3: Optional Telegram chat id per customer, so reminders can be sent
-- over Telegram. If null, the sender falls back to a configured default
-- chat id (handy for demos where everything goes to the owner's chat).
-- =====================================================================
ALTER TABLE customer ADD COLUMN telegram_chat_id VARCHAR(40);

-- Delivery detail (provider response / failure reason) for the notification log UI.
ALTER TABLE reminder_log ADD COLUMN detail VARCHAR(500);
