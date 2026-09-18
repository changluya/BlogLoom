---
title: "System and logs"
description: Accounts, scheduled jobs and the log center.
---

System management covers console account security and runtime logs, a key entry for daily operations.

## Accounts

- Maintain admin accounts and passwords.
- Change the default password immediately after the first login.
- Use a long, random secret for production.

## Scheduled jobs

Quartz-based jobs handle periodic work; their status and history are visible in the console.

## Log center

| Log | Description |
| --- | --- |
| Job log | Scheduled job executions |
| Login log | Console sign-ins |
| Operation log | Key write operations |
| Exception log | System stack traces |
| Access log | Visitor access records |

## Tips

- Trace key content changes through operation logs.
- Locate backend issues quickly through exception logs.
- Analyze traffic with access logs.

## Next steps

- [Dashboard](/v1/en/guide/admin/dashboard)
- [Troubleshooting](/v1/en/deploy/troubleshooting)
