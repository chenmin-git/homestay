# Security Policy

This project is a teaching and demo system. Before using it beyond local demos,
review the following items.

## Secrets

- Do not use the default `JWT_SECRET` in production.
- Keep database passwords, JWT secrets, and deployment credentials in environment
  variables or a secret manager.
- Never commit `.env`, database dumps with private data, access tokens, or cloud
  credentials.

## Data

- Demo accounts and generated homestay records are for local testing only.
- Remove or anonymize user phone numbers, order data, uploads, and logs before
  sharing a real deployment snapshot.

## Reporting

If you find a vulnerability, open a private issue or contact the maintainer
directly. Include reproduction steps, affected endpoints, and the expected
impact.
