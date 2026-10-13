---
title: "System Management and Logs"
description: Accounts, scheduled tasks and the log center.
---

System management covers backend account security and runtime logs, an important entry for daily operations.

## Account management

- Maintain backend administrator accounts and passwords.
- Change the default password immediately after the first login.
- In production, use a sufficiently long and random login secret.

## Scheduled tasks

Quartz-based scheduled tasks handle periodic jobs (such as data sync). You can view execution status and history, and run them manually.

## Log center

| Log type | Description |
| --- | --- |
| Task log | Scheduled task execution records |
| Login log | Backend login information |
| Operation log | Key write-operation records |
| Exception log | System exception stack traces |
| Access log | Visitor access records |

## Usage tips

- Trace key content changes through the operation log.
- Locate backend issues quickly through the exception log.
- Analyze site traffic with the access log.

## Next steps

- [Dashboard](/v1/en/guide/admin/dashboard)
- [Troubleshooting](/v1/en/deploy/troubleshooting)
