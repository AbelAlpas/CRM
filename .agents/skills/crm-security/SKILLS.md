\---

name: crm-security

description: Implement CRM authentication and authorization. Use when changing login, users, roles, permissions or protected operations.

\---



Use Spring Security.



Do not create custom authentication mechanisms unless explicitly required.



Use PasswordEncoder for passwords.



Never log passwords.



Never return password hashes to templates.



Roles:



ADMIN

MANAGER

SALES



Enforce authorization server-side.



Do not rely on hidden buttons or navigation items for security.



Use method or request authorization where appropriate.



Check ownership when SALES users access records assigned to other users.



Security changes must include tests for:



\- authorized access

\- unauthorized access

\- unauthenticated access



Prefer least privilege.

