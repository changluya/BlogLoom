---
title: "Email notification"
description: Configure comment email notifications.
---

With email notifications enabled, new comments can be delivered to the owner or relevant visitors.

## Settings

| Setting | Description |
| --- | --- |
| Sender server | SMTP server address |
| Port | SMTP port |
| Account | Sender mailbox |
| Authorization code | Mailbox authorization code or password |
| Recipient | Notification address |

## Usage

1. Fill in the mail settings in the backend config.
2. Enable comment notifications in the console.
3. Post a test comment to verify delivery.

## Notes

- Use a mailbox authorization code, not the login password.
- Never commit the authorization code to version control.
- Be mindful of sending rate limits to avoid spam classification.

## Next steps

- [Comments](/v1/en/guide/concepts/comments)
- [Configuration](/v1/en/deploy/configuration)
